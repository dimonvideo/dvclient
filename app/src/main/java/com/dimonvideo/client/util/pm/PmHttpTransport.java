package com.dimonvideo.client.util.pm;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** PM transport that forbids hidden connection retries and redirects for server-side mutations. */
public final class PmHttpTransport {
    /** Tracks network exchanges for one call, including OkHttp's protocol-level follow-ups. */
    private static final class Attempt {
        final AtomicBoolean sent = new AtomicBoolean();
    }

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                    .tag(Attempt.class, new Attempt()).build()))
            .addNetworkInterceptor(chain -> {
                Attempt attempt = chain.request().tag(Attempt.class);
                if (attempt == null || !attempt.sent.compareAndSet(false, true)) {
                    // retryOnConnectionFailure(false) does not cover 503/Retry-After:0 or
                    // HTTP/2 421 protocol follow-ups. Never allow a second sent exchange.
                    throw new IOException("Private-message follow-up is disabled");
                }
                return chain.proceed(chain.request());
            })
            .build();
    private static RequestQueue requestQueue;

    /** Prevents instantiation of the process-wide explicit single-attempt transport. */
    private PmHttpTransport() { }

    /** Checks account, cancellation, and deadline guards while consuming a worker response. */
    interface Guard {
        /** Throws when continuing this attempt would no longer be allowed. */
        void check() throws IOException;
    }

    /**
     * Reads one response with normal TLS verification, bounded memory, and no replay on I/O failure.
     * Android's HttpURLConnection automatically retries some sent GETs; this explicit client does not.
     */
    static String get(String address, int maxBytes, Guard guard) throws IOException {
        guard.check();
        okhttp3.Request request = new okhttp3.Request.Builder().url(address)
                .header("Accept", "application/json").get().build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() != 200 || response.body() == null) {
                throw new IOException("Private-message request failed");
            }
            try (InputStream input = response.body().byteStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    guard.check();
                    if (output.size() + count > maxBytes) {
                        throw new IOException("Private-message response is too large");
                    }
                    output.write(buffer, 0, count);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }

    /** Returns a separate Volley queue whose HTTP stack also disables hidden connection retries. */
    public static synchronized RequestQueue queue(Context context) {
        if (requestQueue == null) {
            requestQueue = new RequestQueue(new DiskBasedCache(
                    new File(context.getApplicationContext().getCacheDir(), "pm-volley")),
                    new SingleAttemptNetwork(), 2);
            requestQueue.start();
        }
        return requestQueue;
    }

    /** Performs one Volley request without BasicNetwork's retries or authenticated-URL logging. */
    private static final class SingleAttemptNetwork implements Network {
        private final SingleAttemptStack stack = new SingleAttemptStack();

        /** Converts one bounded response to Volley types without logging credentials or replaying I/O. */
        @Override
        public NetworkResponse performRequest(Request<?> request) throws VolleyError {
            long startedAt = android.os.SystemClock.elapsedRealtime();
            try {
                HttpResponse response = stack.executeRequest(request, java.util.Collections.emptyMap());
                byte[] bytes;
                try (InputStream input = response.getContent();
                     ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    if (input != null) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            if (output.size() + count > 4 * 1024 * 1024) {
                                throw new IOException("Private-message response is too large");
                            }
                            output.write(buffer, 0, count);
                        }
                    }
                    bytes = output.toByteArray();
                }
                int status = response.getStatusCode();
                NetworkResponse result = new NetworkResponse(status, bytes, false,
                        android.os.SystemClock.elapsedRealtime() - startedAt, response.getHeaders());
                if (status == 401 || status == 403) throw new AuthFailureError(result);
                if (status < 200 || status > 299) throw new ServerError(result);
                return result;
            } catch (SocketTimeoutException exception) {
                throw new TimeoutError();
            } catch (IOException exception) {
                throw new NoConnectionError(exception);
            }
        }
    }

    /** Adapts one explicit OkHttp call to Volley without changing unrelated app requests. */
    private static final class SingleAttemptStack extends BaseHttpStack {
        /** Executes one PM GET/POST; Volley closes the response stream after parsing it. */
        @Override
        public HttpResponse executeRequest(Request<?> request, Map<String, String> extraHeaders)
                throws IOException, AuthFailureError {
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(request.getUrl());
            for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }
            if (request.getMethod() == Request.Method.GET) {
                builder.get();
            } else if (request.getMethod() == Request.Method.POST) {
                byte[] body = request.getBody();
                builder.post(RequestBody.create(body == null ? new byte[0] : body,
                        MediaType.get(request.getBodyContentType())));
            } else {
                throw new IOException("Unsupported private-message request method");
            }
            Response response = CLIENT.newCall(builder.build()).execute();
            List<Header> headers = new ArrayList<>();
            for (int index = 0; index < response.headers().size(); index++) {
                headers.add(new Header(response.headers().name(index), response.headers().value(index)));
            }
            ResponseBody body = response.body();
            if (body == null) {
                response.close();
                return new HttpResponse(response.code(), headers);
            }
            long length = body.contentLength();
            return new HttpResponse(response.code(), headers,
                    length < 0 || length > Integer.MAX_VALUE ? -1 : (int) length, body.byteStream());
        }
    }
}

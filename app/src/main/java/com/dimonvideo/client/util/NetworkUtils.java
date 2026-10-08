/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.ui.main.MainFragmentOpros;
import com.dimonvideo.client.util.pm.PmDeletionQueue;
import com.dimonvideo.client.util.pm.PmHttpTransport;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class NetworkUtils {

    private static final String UTF_8 = "utf-8";

    /** Retains the session that owns an asynchronous legacy authentication refresh. */
    private static final class AuthSession {
        private final SharedPreferences preferences;
        private final Object login;
        private final Object password;
        private final Object state;
        private final Object userId;
        private final SharedPreferences.OnSharedPreferenceChangeListener listener;
        private boolean invalidated, closed;

        /** Captures identity and tracks replacements even when a later sign-in restores the same values. */
        AuthSession(SharedPreferences preferences) {
            this.preferences = preferences;
            Map<String, ?> snapshot = preferences.getAll();
            login = snapshot.get("dvc_login");
            password = snapshot.get("dvc_password");
            state = snapshot.get("auth_state");
            userId = snapshot.get("user_id");
            listener = (changedPreferences, key) -> {
                synchronized (AuthSession.this) {
                    if (!closed && (key == null || "dvc_login".equals(key)
                            || "dvc_password".equals(key) || "auth_state".equals(key)
                            || "user_id".equals(key))) invalidated = true;
                }
            };
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }

        /** Rejects responses whose original credentials or authenticated identity were replaced. */
        synchronized boolean matches() {
            Map<String, ?> current = preferences.getAll();
            return !closed && !invalidated && Objects.equals(login, current.get("dvc_login"))
                    && Objects.equals(password, current.get("dvc_password"))
                    && Objects.equals(state, current.get("auth_state"))
                    && Objects.equals(userId, current.get("user_id"));
        }

        /** Claims one owned terminal callback and unsubscribes before its own session writes occur. */
        synchronized boolean finishIfCurrent() {
            boolean current = matches();
            close();
            return current;
        }

        /** Releases the listener after response, error, cancellation, or a request that cannot be queued. */
        synchronized void close() {
            if (closed) return;
            closed = true;
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    /** Encodes existing account credentials for the legacy authenticated API. */
    private static boolean getEncodedAuthData(AppController appController, String[] authData, Context context) {
        String login = appController.userName("null");
        String password = appController.userPassword();

        if (login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
            return true;
        }
        if (password.length() < 5) {
            Toast.makeText(context, context.getString(R.string.password_invalid), Toast.LENGTH_LONG).show();
            return true;
        }

        try {
            authData[0] = URLEncoder.encode(login, UTF_8);
            authData[1] = URLEncoder.encode(password, UTF_8);
            return false;
        } catch (UnsupportedEncodingException e) {
            Log.e(Config.TAG, "Encoding error: " + e.getMessage());
            return true;
        }
    }

    /** Refreshes metadata only while the originating credentials and session still own the response. */
    public static void checkPassword(Context context, String password, String razdel) {
        AppController appController = AppController.getInstance();
        View view = MainActivity.binding.getRoot();

        if (password == null || password.length() < 5 || password.length() > 71) {
            Snackbar.make(view, context.getString(R.string.password_invalid), Snackbar.LENGTH_LONG).show();
            return;
        }

        AuthSession session = new AuthSession(appController.getSharedPreferences());
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context) || !session.matches()) {
            session.close();
            return;
        }

        String url = Config.CHECK_AUTH_URL + "&login_name=" + authData[0] + "&login_password=" + authData[1];
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (!session.finishIfCurrent()) return;

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int state = jsonObject.getInt(Config.TAG_STATE);
                        appController.putAuthState(state);

                        if (state > 0) {
                            appController.putImage(jsonObject.getString(Config.TAG_IMAGE_URL));
                            appController.putRang(jsonObject.getString(Config.TAG_HEADERS));
                            appController.putLastDate(jsonObject.getString(Config.TAG_TIME));
                            appController.putReputation(jsonObject.getString(Config.TAG_REP));
                            appController.putRegDate(jsonObject.getString(Config.TAG_REG));
                            appController.putRating(jsonObject.getString(Config.TAG_COMMENTS));
                            appController.putPosts(jsonObject.getString(Config.TAG_COUNT));
                            appController.putUserId(jsonObject.getInt(Config.TAG_UID));
                            appController.putUserGroup(jsonObject.getInt(Config.TAG_USER_GROUP));
                            appController.putPmUnread(jsonObject.getInt(Config.TAG_PM_UNREAD));

                            if (appController.isAuth() == 0) {
                                Snackbar.make(view, context.getString(R.string.success_auth), Snackbar.LENGTH_LONG).show();
                            }
                            EventBus.getDefault().post(new MessageEvent(razdel, null, null, String.valueOf(jsonObject.getInt(Config.TAG_PM_UNREAD)), null, null));

                            String token = jsonObject.getString(Config.TAG_TOKEN);
                            if (!token.equals(appController.isToken())) {
                                GetToken.getToken(context);
                            }
                        } else {
                            Snackbar.make(view, context.getString(R.string.unsuccess_auth), Snackbar.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                    }
                }, error -> {
                    if (session.finishIfCurrent()) showErrorToast(context, error);
                }) {
            /** Unsubscribes immediately because Volley does not deliver a callback for cancelled requests. */
            @Override
            public void cancel() {
                session.close();
                super.cancel();
            }

            /** Keeps Volley debug and cancellation messages free of the authenticated request URL. */
            @Override
            public String toString() { return "Site account refresh"; }
        };

        stringRequest.setShouldCache(false);
        try {
            appController.addToRequestQueue(stringRequest);
        } catch (RuntimeException exception) {
            stringRequest.cancel();
            throw exception;
        }
    }

    /** Verifies a legacy login only while its original saved session still owns the response. */
    public static void checkLogin(Context context, String login) {
        AppController appController = AppController.getInstance();
        View view = MainActivity.binding.getRoot();

        if (login == null || login.length() < 2 || login.length() > 71) {
            Snackbar.make(view, context.getString(R.string.login_invalid), Snackbar.LENGTH_LONG).show();
            return;
        }

        AuthSession session = new AuthSession(appController.getSharedPreferences());
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context) || !session.matches()) {
            session.close();
            return;
        }

        String url = Config.CHECK_AUTH_URL + "&login_name=" + login + "&login_password=" + authData[1];
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (!session.finishIfCurrent()) return;

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int state = jsonObject.getInt(Config.TAG_STATE);
                        appController.putAuthState(state);

                        if (state > 0) {
                            Snackbar.make(view, context.getString(R.string.success_auth), Snackbar.LENGTH_LONG).show();
                            context.sendBroadcast(new Intent(Config.INTENT_AUTH)
                                    .setPackage(context.getPackageName()));
                            GetToken.getToken(context);
                        } else {
                            Snackbar.make(view, context.getString(R.string.unsuccess_auth), Snackbar.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                    }
                }, error -> {
                    if (session.finishIfCurrent()) showErrorToast(context, error);
                }) {
            /** Unsubscribes immediately when the legacy request is cancelled without a Volley callback. */
            @Override
            public void cancel() {
                session.close();
                super.cancel();
            }

            /** Keeps the legacy login's credential-bearing URL out of Volley diagnostic messages. */
            @Override
            public String toString() { return "Site login verification"; }
        };

        stringRequest.setShouldCache(false);
        try {
            appController.addToRequestQueue(stringRequest);
        } catch (RuntimeException exception) {
            stringRequest.cancel();
            throw exception;
        }
    }

    /** Queues deletion durably, or performs one acknowledged restore/archive operation. */
    public static void deletePm(Context context, int pm_id, int delete) {
        deletePm(context, pm_id, delete, null);
    }

    /**
     * Accepts local removal only after a deletion intent is saved, or a server edit succeeds.
     * The callback runs on the main thread; a queue or API failure keeps the item visible.
     */
    public static void deletePm(Context context, int pm_id, int delete, Runnable onAccepted) {
        deletePm(context, pm_id, delete, 0, 1, onAccepted);
    }

    /** Queues deletion with folder/page hints while requiring an exact server ID before mutation. */
    public static void deletePm(Context context, int pm_id, int delete, int sourceFolder,
                                int sourcePage, Runnable onAccepted) {
        if (delete == 0) {
            PmDeletionQueue.enqueue(context, pm_id, sourceFolder, sourcePage, onAccepted);
            return;
        }
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context)) return;
        String accountKey = PmDeletionQueue.currentAccountKey();

        String url = Config.PM_URL + 1 + "&login_name=" + authData[0] + "&login_password=" + authData[1] + "&pm_id=" + pm_id + "&pm=10&delete=" + delete;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (accountKey != null && !accountKey.equals(PmDeletionQueue.currentAccountKey())) return;
                    try {
                        JSONArray result = new JSONArray(response);
                        // pm.php's restore branch is documented to return an empty JSON array.
                        boolean restored = delete == 1 && result.length() == 0;
                        if (!restored && !hasPmSuccess(result, "status")) {
                            Toast.makeText(context, R.string.error_network, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (onAccepted != null) onAccepted.run();
                        refreshPmUnread(appController, authData, accountKey, "deletePm");
                    } catch (JSONException exception) {
                        Toast.makeText(context, R.string.error_network, Toast.LENGTH_LONG).show();
                    }
                }, error -> showErrorToast(context, error));
        enqueuePmRequest(appController, stringRequest);
    }

    /** Receives the result of a single PM action without discarding a composer on failure. */
    public interface PmOperationCallback {
        /** Receives an acknowledged operation result on the main thread. */
        void onSuccess();
        /** Receives a transport, validation, or server rejection on the main thread. */
        void onError();
    }

    /** Marks a message read using the documented API without an automatic transport retry. */
    public static void readPm(Context context, int pm_id) {
        readPm(context, pm_id, null);
    }

    /** Marks a message read and updates local UI only after a valid JSON acknowledgement. */
    public static void readPm(Context context, int pm_id, PmOperationCallback callback) {
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context)) {
            if (callback != null) callback.onError();
            return;
        }
        String accountKey = PmDeletionQueue.currentAccountKey();

        String url = Config.PM_URL + 1 + "&login_name=" + authData[0] + "&login_password=" + authData[1] + "&pm_id=" + pm_id + "&pm=11";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (accountKey != null && !accountKey.equals(PmDeletionQueue.currentAccountKey())) {
                        if (callback != null) callback.onError();
                        return;
                    }
                    try {
                        JSONArray result = new JSONArray(response);
                        // The read branch returns [] on success and status=0 for a missing row.
                        if (result.length() != 0) throw new JSONException("Read was not acknowledged");
                        // The API returns [] even when this message was already read. Refresh
                        // the authoritative count instead of decrementing it on repeated opens.
                        refreshPmUnread(appController, authData, accountKey, "readPm");
                        if (callback != null) callback.onSuccess();
                    } catch (JSONException exception) {
                        Toast.makeText(context, R.string.error_network, Toast.LENGTH_LONG).show();
                        if (callback != null) callback.onError();
                    }
                }, error -> {
                    showErrorToast(context, error);
                    if (callback != null) callback.onError();
                });
        enqueuePmRequest(appController, stringRequest);
    }

    /** Sends a private message once and reports success only for the API's state=1 response. */
    public static void sendPm(Context context, int pm_id, String text, int delete, String razdel, int uid) {
        sendPm(context, pm_id, text, delete, razdel, uid, null);
    }

    /** Keeps a message draft available until the single server send attempt is acknowledged. */
    public static void sendPm(Context context, int pm_id, String text, int delete, String razdel,
                              int uid, PmOperationCallback callback) {
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context)) {
            if (callback != null) callback.onError();
            return;
        }
        String accountKey = PmDeletionQueue.currentAccountKey();

        if (text == null || text.length() <= 1) {
            Toast.makeText(context, context.getString(R.string.error_network), Toast.LENGTH_LONG).show();
            if (callback != null) callback.onError();
            return;
        }

        String url = Config.PM_URL + 1 + "&login_name=" + authData[0] + "&login_password=" + authData[1] + "&pm_id=" + pm_id + "&pm=12&delete=" + delete + "&razdel=" + razdel + "&uid=" + uid;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (accountKey != null && !accountKey.equals(PmDeletionQueue.currentAccountKey())) {
                        if (callback != null) callback.onError();
                        return;
                    }
                    try {
                        if (!hasPmSuccess(new JSONArray(response), "state")) {
                            throw new JSONException("Send was not acknowledged");
                        }
                        Toast.makeText(context, R.string.success_send_pm, Toast.LENGTH_LONG).show();
                        GetToken.getToken(context);
                        if (callback != null) callback.onSuccess();
                    } catch (JSONException exception) {
                        Toast.makeText(context, R.string.error_network, Toast.LENGTH_LONG).show();
                        if (callback != null) callback.onError();
                    }
                }, error -> {
                    showErrorToast(context, error);
                    if (callback != null) callback.onError();
                }) {
            /** Supplies the message body as form data instead of adding it to the URL. */
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> postMap = new HashMap<>();
                postMap.put("pm_text", text);
                return postMap;
            }
        };

        enqueuePmRequest(appController, stringRequest);
    }

    /** Checks the legacy API's single-object positive status rather than trusting HTTP alone. */
    private static boolean hasPmSuccess(JSONArray response, String key) throws JSONException {
        return response.length() == 1 && response.getJSONObject(0).optInt(key, 0) == 1;
    }

    /** Refreshes the badge from an acknowledged account, guarding late responses after account changes. */
    private static void refreshPmUnread(AppController controller, String[] authData,
                                        String accountKey, String action) {
        if (accountKey == null) return;
        int userId = controller.isUserId();
        String url = Config.CHECK_AUTH_URL + "&login_name=" + authData[0]
                + "&login_password=" + authData[1];
        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            if (!accountKey.equals(PmDeletionQueue.currentAccountKey())) return;
            try {
                JSONObject identity = new JSONObject(response);
                if (identity.optInt("state") != 1 || identity.optInt("user_id") != userId) return;
                int unread = Math.max(0, identity.getInt("pm_unread"));
                controller.putPmUnread(unread);
                EventBus.getDefault().post(new MessageEvent(null, null, null,
                        String.valueOf(unread), action, null));
            } catch (JSONException exception) {
                // A failed badge refresh cannot invalidate or replay the acknowledged PM action.
            }
        }, error -> { });
        enqueuePmRequest(controller, request);
    }

    /** Bypasses the app's three automatic retries for operations with server-side side effects. */
    private static void enqueuePmRequest(AppController controller, Request<?> request) {
        request.setShouldCache(false);
        request.setTag(Config.TAG);
        request.setRetryPolicy(new DefaultRetryPolicy(6000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        PmHttpTransport.queue(controller).add(request);
    }

    public static void loadAvatar(Context context, Toolbar toolbar) {
        AppController appController = AppController.getInstance();
        if (appController.isAuth() <= 0) return;

        String image_url = appController.imageUrl();
        Glide.with(context)
                .asDrawable()
                .load(image_url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerInside()
                .apply(RequestOptions.circleCropTransform())
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                        toolbar.setNavigationIcon(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    public static byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    /** Receives one PM image result without publishing an unrelated global sticky attachment. */
    public interface PmAttachmentCallback {
        /** Receives the server filename for a validated successful image upload. */
        void onSuccess(String filename);
        /** Receives image validation, JSON, or transport failure on the main thread. */
        void onError();
    }

    /** Uploads an image for legacy composers using their existing sticky event contract. */
    public static String uploadBitmap(Bitmap bitmap, Context context, String razdel) {
        return uploadBitmap(bitmap, context, razdel, null);
    }

    /** Uploads an image and optionally returns its correlated result to a PM composer. */
    public static String uploadBitmap(Bitmap bitmap, Context context, String razdel,
                                      PmAttachmentCallback callback) {
        AppController controller = AppController.getInstance();
        String user_name = controller.userName("dvclient");

        ProgressHelper.showDialog(context, context.getString(R.string.please_wait));
        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, Config.UPLOAD_URL,
                response -> {
                    ProgressHelper.dismissDialog();
                    try {
                        JSONObject obj = new JSONObject(new String(response.data));
                        String msg = obj.getString(Config.TAG_LINK);
                        if (callback != null && (msg.isEmpty() || !msg.endsWith(".png")
                                || obj.optBoolean("error", true))) {
                            callback.onError();
                            return;
                        }
                        if (callback != null) callback.onSuccess(msg);
                        else EventBus.getDefault().postSticky(new MessageEvent(razdel, null, msg, null, null, bitmap));
                        Toast.makeText(context, R.string.success_image, Toast.LENGTH_SHORT).show();
                    } catch (JSONException exception) {
                        if (callback != null) callback.onError();
                    }
                }, error -> {
            ProgressHelper.dismissDialog();
            showErrorToast(context, error);
            if (callback != null) callback.onError();
        }) {
            /** Supplies the original account name as multipart upload metadata. */
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", user_name);
                return params;
            }

            /** Supplies the processed PNG bytes under the API's expected pic field. */
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                long imagename = System.currentTimeMillis();
                params.put("pic", new DataPart(imagename + ".png", getFileDataFromDrawable(bitmap)));
                return params;
            }
        };

        volleyMultipartRequest.setShouldCache(false);
        volleyMultipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                (int) TimeUnit.SECONDS.toMillis(10),
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        controller.addToRequestQueue(volleyMultipartRequest);
        return user_name;
    }

    public static void getOprosTitle(TextView opros, Context context) {
        AppController appController = AppController.getInstance();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Config.VOTE_URL,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        Feed jsonFeed = new Feed();
                        try {
                            JSONObject json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            opros.setText(jsonFeed.getTitle());
                            opros.setOnClickListener(v -> {
                                MainFragmentOpros fragment = new MainFragmentOpros();
                                Bundle bundle = new Bundle();
                                bundle.putString(Config.TAG_TITLE, jsonFeed.getTitle());
                                fragment.setArguments(bundle);
                                if (context instanceof AppCompatActivity) {
                                    fragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "MainFragmentOpros");
                                }
                            });
                        } catch (JSONException e) {
                            Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                        }
                    }
                }, error -> showErrorToast(context, error));

        jsonArrayRequest.setShouldCache(false);
        appController.addToRequestQueue(jsonArrayRequest);
    }

    public static void getOdob(String razdel, int lid) {
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, null)) return;

        View view = MainActivity.binding.getRoot();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Config.APPROVE_URL + razdel + "&u=" + authData[0] + "&p=" + authData[1] + "&lid=" + lid,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        Feed jsonFeed = new Feed();
                        try {
                            JSONObject json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            Snackbar.make(view, jsonFeed.getTitle(), Snackbar.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                        }
                    }
                }, error -> showErrorToast(null, error));

        jsonArrayRequest.setShouldCache(false);
        appController.addToRequestQueue(jsonArrayRequest);
    }

    public static void putToNews(String razdel, int lid) {
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, null)) return;

        View view = MainActivity.binding.getRoot();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Config.PUTTONEWS_URL + razdel + "&u=" + authData[0] + "&p=" + authData[1] + "&lid=" + lid,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        Feed jsonFeed = new Feed();
                        try {
                            JSONObject json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            Snackbar.make(view, jsonFeed.getTitle(), Snackbar.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                        }
                    }
                }, error -> showErrorToast(null, error));

        jsonArrayRequest.setShouldCache(false);
        appController.addToRequestQueue(jsonArrayRequest);
    }

    private static void showErrorToast(Context context, VolleyError error) {
        @StringRes int errorTextRes = getErrorTextResId(error);
        if (errorTextRes != 0 && context != null) {
            Toast.makeText(context, context.getString(errorTextRes), Toast.LENGTH_LONG).show();
        }
    }

    @StringRes
    private static int getErrorTextResId(VolleyError error) {
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            return R.string.error_network_timeout;
        } else if (error instanceof AuthFailureError) {
            return R.string.unsuccess_auth;
        } else if (error instanceof ServerError || error instanceof ParseError) {
            return R.string.error_server;
        } else if (error instanceof NetworkError) {
            return R.string.error_network;
        }
        return 0;
    }
}

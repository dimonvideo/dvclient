package com.dimonvideo.client.util.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.util.pm.PmHttpTransport;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/** Verifies a complete credential pair before atomically replacing the active site session. */
public final class LoginService {
    /** Distinguishes field validation, rejected credentials, server failures, and stale sessions. */
    public enum Failure {
        INVALID_LOGIN, INVALID_PASSWORD, REJECTED, MALFORMED_RESPONSE, NETWORK_ERROR,
        SESSION_CHANGED
    }

    /** Receives one result; success means all verified session preferences are already saved. */
    public interface Callback {
        /** Receives the canonical account identity and metadata after its atomic preference update. */
        void onSuccess(Profile profile);

        /** Reports a failure while leaving the previously saved session and credentials unchanged. */
        void onError(Failure failure);
    }

    /** Authenticated account metadata returned by the documented op=10 response. */
    public static final class Profile {
        public final String login;
        public final int userId;
        public final int userGroup;
        public final int pmUnread;
        public final String image;
        public final String rank;
        public final String lastDate;
        public final String reputation;
        public final String registrationDate;
        public final String rating;
        public final String posts;
        public final String serverToken;

        /** Parses account identity strictly and tolerates omitted optional display metadata. */
        private Profile(JSONObject response, String enteredLogin) throws JSONException {
            userId = integer(response, Config.TAG_UID);
            if (userId <= 0) throw new JSONException("Missing positive account identity");
            login = text(response, "user", enteredLogin).trim();
            if (login.isEmpty()) throw new JSONException("Missing account name");
            userGroup = Math.max(1, response.optInt(Config.TAG_USER_GROUP, 4));
            pmUnread = Math.max(0, response.optInt(Config.TAG_PM_UNREAD, 0));
            image = text(response, Config.TAG_IMAGE_URL, Config.WRITE_URL + "/images/noavatar.png");
            rank = text(response, Config.TAG_HEADERS, "---");
            lastDate = text(response, Config.TAG_TIME, "---");
            reputation = text(response, Config.TAG_REP, "0");
            registrationDate = text(response, Config.TAG_REG, "0");
            rating = text(response, Config.TAG_COMMENTS, "0");
            posts = text(response, Config.TAG_COUNT, "0");
            serverToken = text(response, Config.TAG_TOKEN, "");
        }

        /** Rejects fractional, absent, out-of-range, and nonnumeric required identity fields. */
        private static int integer(JSONObject response, String key) throws JSONException {
            Object value = response.get(key);
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException exception) {
                throw new JSONException("Invalid account response field");
            }
        }

        /** Converts scalar API display metadata without retaining nested objects or null values. */
        private static String text(JSONObject response, String key, String fallback) {
            Object value = response.opt(key);
            if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                return String.valueOf(value);
            }
            return fallback;
        }
    }

    /** Captures only session ownership fields, so unrelated setting edits do not abort a login. */
    private static final class Session {
        private final Object login;
        private final Object password;
        private final Object state;
        private final Object userId;

        /** Takes one consistent preference snapshot rather than reading session fields separately. */
        Session(SharedPreferences preferences) {
            Map<String, ?> snapshot = preferences.getAll();
            login = snapshot.get("dvc_login");
            password = snapshot.get("dvc_password");
            state = snapshot.get("auth_state");
            userId = snapshot.get("user_id");
        }

        /** Checks whether an intervening logout or account switch replaced the originating session. */
        boolean matches(SharedPreferences preferences) {
            Session current = new Session(preferences);
            return Objects.equals(login, current.login) && Objects.equals(password, current.password)
                    && Objects.equals(state, current.state) && Objects.equals(userId, current.userId);
        }
    }

    /** Allows tests to deliver real API payloads without touching the production server. */
    interface Sender {
        /** Enqueues exactly one uncached verification request. */
        void send(StringRequest request);
    }

    /** Cancellable ownership of one login attempt, including its unsaved temporary credentials. */
    public final class Attempt {
        private Session originalSession;
        private String login;
        private String password;
        private Callback callback;
        private StringRequest request;
        private boolean finished;
        private boolean sessionChanged;
        private final SharedPreferences.OnSharedPreferenceChangeListener sessionListener;

        /** Captures the current saved session before accepting new credentials for verification. */
        private Attempt(String login, String password, Callback callback) {
            originalSession = new Session(preferences);
            this.login = login;
            this.password = password;
            this.callback = callback;
            sessionListener = (changedPreferences, key) -> {
                synchronized (Attempt.this) {
                    if (!finished && isSessionKey(key)) sessionChanged = true;
                }
            };
            preferences.registerOnSharedPreferenceChangeListener(sessionListener);
        }

        /** Stops callbacks and preference writes; repeated cancellation is harmless. */
        public synchronized void cancel() {
            if (request != null) request.cancel();
            clear();
        }

        /** Releases temporary credentials, request, and UI callback after completion or cancellation. */
        private void clear() {
            finished = true;
            preferences.unregisterOnSharedPreferenceChangeListener(sessionListener);
            login = null;
            password = null;
            originalSession = null;
            callback = null;
            request = null;
        }

        /** Reports one failure without storing even a partially validated credential pair. */
        private synchronized void fail(Failure failure) {
            if (finished) return;
            Callback recipient = callback;
            clear();
            recipient.onError(failure);
        }

        /** Accepts only a positive identity response still owned by this uncancelled login attempt. */
        private synchronized void accept(String body) {
            if (finished) return;
            if (sessionChanged || !originalSession.matches(preferences)) {
                fail(Failure.SESSION_CHANGED);
                return;
            }
            final Profile profile;
            try {
                JSONObject response = new JSONObject(body);
                int state = Profile.integer(response, Config.TAG_STATE);
                if (state == 0) {
                    fail(Failure.REJECTED);
                    return;
                }
                if (state != 1) throw new JSONException("Unexpected account state");
                profile = new Profile(response, login);
            } catch (JSONException exception) {
                fail(Failure.MALFORMED_RESPONSE);
                return;
            }
            // One editor atomically replaces credentials and metadata. In particular, current_token
            // belongs to this device's push registration and must not become the server's token.
            Callback recipient = callback;
            preferences.unregisterOnSharedPreferenceChangeListener(sessionListener);
            preferences.edit()
                    .putString("dvc_login", profile.login)
                    .putString("dvc_password", password)
                    .putInt("auth_state", 1)
                    .putInt("user_id", profile.userId)
                    .putInt("user_group", profile.userGroup)
                    .putInt("pm_unread", profile.pmUnread)
                    .putString("auth_foto", profile.image)
                    .putString("auth_rang", profile.rank)
                    .putString("auth_last", profile.lastDate)
                    .putString("auth_rep", profile.reputation)
                    .putString("auth_reg", profile.registrationDate)
                    .putString("auth_rat", profile.rating)
                    .putString("auth_posts", profile.posts)
                    .apply();
            if (finished) return;
            clear();
            recipient.onSuccess(profile);
        }

        /** Tracks account ownership edits even when another operation later restores the same values. */
        private boolean isSessionKey(String key) {
            return "dvc_login".equals(key) || "dvc_password".equals(key)
                    || "auth_state".equals(key) || "user_id".equals(key) || key == null;
        }
    }

    private final SharedPreferences preferences;
    private final Sender sender;

    /** Uses the existing preference store and HTTPS stack without its generic queue's hidden retries. */
    public LoginService(Context context) {
        Context application = context.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        sender = request -> PmHttpTransport.queue(application).add(request);
    }

    /** Injects storage and request delivery for deterministic credential and cancellation tests. */
    LoginService(SharedPreferences preferences, Sender sender) {
        this.preferences = preferences;
        this.sender = sender;
    }

    /**
     * Verifies the supplied pair once without first saving it; cancelled or stale replies are ignored.
     * Callers keep their dialog open on failure and close it only after the success callback.
     */
    public Attempt verify(String login, String password, Callback callback) {
        String normalizedLogin = login == null ? "" : login.trim();
        Attempt attempt = new Attempt(normalizedLogin, password, Objects.requireNonNull(callback));
        if (normalizedLogin.length() < 2 || normalizedLogin.length() > 71) {
            attempt.fail(Failure.INVALID_LOGIN);
            return attempt;
        }
        if (password == null || password.length() < 5 || password.length() > 71) {
            attempt.fail(Failure.INVALID_PASSWORD);
            return attempt;
        }
        String address = Config.CHECK_AUTH_URL + "&login_name=" + encode(normalizedLogin)
                + "&login_password=" + encode(password);
        StringRequest request = new StringRequest(Request.Method.GET, address, attempt::accept,
                error -> attempt.fail(Failure.NETWORK_ERROR)) {
            /** Prevents Volley debug and cancellation messages from exposing the authenticated URL. */
            @Override
            public String toString() { return "Site login verification"; }
        };
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(12_000, 0, 1));
        request.setTag(attempt);
        attempt.request = request;
        try {
            sender.send(request);
        } catch (RuntimeException exception) {
            attempt.fail(Failure.NETWORK_ERROR);
        }
        return attempt;
    }

    /** Encodes API query values as UTF-8 while preserving the exact entered password. */
    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException impossible) {
            throw new AssertionError("UTF-8 is required by Android", impossible);
        }
    }
}

package com.dimonvideo.client.util.auth;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Verifies real op=10 payload handling, session isolation, and cancellable credential transactions. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 28, application = Application.class)
public class LoginServiceTest {
    private SharedPreferences preferences;
    private FakeSender sender;
    private LoginService service;
    private Result result;

    /** Starts with an existing authenticated account that every failed new login must preserve. */
    @Before
    public void setUp() {
        preferences = RuntimeEnvironment.getApplication()
                .getSharedPreferences("login-service", Context.MODE_PRIVATE);
        preferences.edit().clear()
                .putString("dvc_login", "old-user")
                .putString("dvc_password", "old-password")
                .putInt("auth_state", 1)
                .putInt("user_id", 7)
                .putInt("user_group", 3)
                .putInt("pm_unread", 9)
                .putString("auth_rang", "Old rank")
                .putString("current_token", "device-push-token")
                .apply();
        sender = new FakeSender();
        service = new LoginService(preferences, sender);
        result = new Result();
    }

    /** Draft credentials are correctly encoded and never replace the saved pair before verification. */
    @Test
    public void suppliedPairIsEncodedWithoutSavingItOrLoggingTheAuthenticatedAddress() {
        Map<String, ?> before = preferences.getAll();
        service.verify("  Имя & member  ", "pass & + ? ", result);
        assertEquals(before, preferences.getAll());
        assertEquals(1, sender.requests.size());
        StringRequest request = sender.latest();
        Uri uri = Uri.parse(request.getUrl());
        assertEquals("Имя & member", uri.getQueryParameter("login_name"));
        assertEquals("pass & + ? ", uri.getQueryParameter("login_password"));
        assertEquals("10", uri.getQueryParameter("op"));
        assertFalse(request.shouldCache());
        assertEquals(12_000, request.getRetryPolicy().getCurrentTimeout());
        assertThrows(VolleyError.class, () -> request.getRetryPolicy().retry(new VolleyError()));
        assertEquals("Site login verification", request.toString());
        assertNull(result.profile);
        assertNull(result.failure);
    }

    /** Listeners and success callbacks observe the complete verified session, never mixed credentials. */
    @Test
    public void successfulPayloadAtomicallyCommitsCanonicalIdentityAndAllMetadata() {
        List<Map<String, ?>> observed = new ArrayList<>();
        SharedPreferences.OnSharedPreferenceChangeListener listener =
                (updated, key) -> observed.add(updated.getAll());
        preferences.registerOnSharedPreferenceChangeListener(listener);
        service.verify("entered-alice", "fresh-password", result);
        sender.respond("{\"state\":1,\"user_id\":25,\"user\":\"CanonicalAlice\","
                + "\"user_group\":2,\"pm_unread\":4,\"image\":\"https://example.test/avatar.jpg\","
                + "\"headers\":\"Master\",\"time\":\"Today\",\"reputation\":11,"
                + "\"reg_date\":\"Yesterday\",\"rating\":15,\"count\":23,"
                + "\"token\":\"remote-push-token\",\"current_token\":\"\"}");
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
        assertNotNull(result.profile);
        assertNull(result.failure);
        assertEquals("CanonicalAlice", result.profile.login);
        assertEquals(25, result.profile.userId);
        assertEquals(4, result.profile.pmUnread);
        assertEquals("remote-push-token", result.profile.serverToken);
        assertEquals("device-push-token", preferences.getString("current_token", ""));
        assertEquals(2, preferences.getInt("user_group", 0));
        assertEquals("Master", preferences.getString("auth_rang", ""));
        assertEquals("Today", preferences.getString("auth_last", ""));
        assertEquals("11", preferences.getString("auth_rep", ""));
        assertEquals("Yesterday", preferences.getString("auth_reg", ""));
        assertEquals("15", preferences.getString("auth_rat", ""));
        assertEquals("23", preferences.getString("auth_posts", ""));
        assertEquals("https://example.test/avatar.jpg", preferences.getString("auth_foto", ""));
        assertFalse(observed.isEmpty());
        for (Map<String, ?> snapshot : observed) {
            assertEquals("CanonicalAlice", snapshot.get("dvc_login"));
            assertEquals("fresh-password", snapshot.get("dvc_password"));
            assertEquals(25, snapshot.get("user_id"));
            assertEquals(4, snapshot.get("pm_unread"));
        }
    }

    /** Explicit invalid-credential responses leave the current account and its badge untouched. */
    @Test
    public void rejectedPairPreservesExistingSession() {
        Map<String, ?> before = preferences.getAll();
        service.verify("new-user", "new-password", result);
        sender.respond("{\"state\":0}");
        assertEquals(LoginService.Failure.REJECTED, result.failure);
        assertEquals(before, preferences.getAll());
        assertNull(result.profile);
    }

    /** A nominal success cannot authorize a login without a complete, strictly positive identity. */
    @Test
    public void malformedAndFractionalIdentitiesNeverReplaceSavedCredentials() {
        Map<String, ?> before = preferences.getAll();
        for (String body : new String[]{"[]", "not json", "{}", "{\"state\":2,\"user_id\":12}",
                "{\"state\":1}", "{\"state\":1,\"user_id\":0}",
                "{\"state\":1,\"user_id\":-4}", "{\"state\":1,\"user_id\":1.5}",
                "{\"state\":1,\"user_id\":2147483648}"}) {
            result = new Result();
            service.verify("new-user", "new-password", result);
            sender.respond(body);
            assertEquals(LoginService.Failure.MALFORMED_RESPONSE, result.failure);
            assertEquals(before, preferences.getAll());
            assertNull(result.profile);
        }
    }

    /** Authenticated numeric string IDs remain compatible with the legacy PHP endpoint. */
    @Test
    public void numericStringResponseUsesEnteredNameWhenCanonicalNameIsAbsent() {
        service.verify("  Alice  ", "new-password", result);
        sender.respond("{\"state\":\"1\",\"user_id\":\"19\"}");
        assertNotNull(result.profile);
        assertEquals(19, result.profile.userId);
        assertEquals("Alice", preferences.getString("dvc_login", ""));
        assertEquals(0, preferences.getInt("pm_unread", -1));
        assertEquals("---", preferences.getString("auth_rang", ""));
    }

    /** A timeout is distinguishable from rejected credentials and leaves the old session usable. */
    @Test
    public void networkFailureKeepsSavedSession() {
        Map<String, ?> before = preferences.getAll();
        service.verify("new-user", "new-password", result);
        sender.latest().deliverError(new VolleyError());
        assertEquals(LoginService.Failure.NETWORK_ERROR, result.failure);
        assertEquals(before, preferences.getAll());
    }

    /** Cancel closes request ownership and prevents even manually delivered late success or error. */
    @Test
    public void cancellationCannotCommitLateReplies() {
        Map<String, ?> before = preferences.getAll();
        LoginService.Attempt attempt = service.verify("new-user", "new-password", result);
        StringRequest request = sender.latest();
        attempt.cancel();
        attempt.cancel();
        assertTrue(request.isCanceled());
        sender.respond("{\"state\":1,\"user_id\":19}");
        request.deliverError(new VolleyError());
        assertEquals(before, preferences.getAll());
        assertNull(result.profile);
        assertNull(result.failure);
    }

    /** A logout occurring during verification cannot be undone by the old request's delayed success. */
    @Test
    public void logoutDuringVerificationInvalidatesTheAttempt() {
        service.verify("new-user", "new-password", result);
        preferences.edit().putInt("auth_state", 0).remove("dvc_login")
                .remove("dvc_password").apply();
        Map<String, ?> loggedOut = preferences.getAll();
        sender.respond("{\"state\":1,\"user_id\":19}");
        assertEquals(LoginService.Failure.SESSION_CHANGED, result.failure);
        assertEquals(loggedOut, preferences.getAll());
    }

    /** A later verified account takes precedence over an older concurrent login request. */
    @Test
    public void accountSwitchDuringVerificationInvalidatesTheOlderAttempt() {
        service.verify("new-user", "new-password", result);
        preferences.edit().putString("dvc_login", "other-user")
                .putString("dvc_password", "other-password").putInt("user_id", 30).apply();
        Map<String, ?> switched = preferences.getAll();
        sender.respond("{\"state\":1,\"user_id\":19}");
        assertEquals(LoginService.Failure.SESSION_CHANGED, result.failure);
        assertEquals(switched, preferences.getAll());
    }

    /** Logout remains a cancellation boundary even if a different flow restores the same account. */
    @Test
    public void temporaryLogoutCannotRevalidateAnOlderAttempt() {
        service.verify("new-user", "new-password", result);
        preferences.edit().putInt("auth_state", 0).apply();
        preferences.edit().putInt("auth_state", 1).apply();
        Map<String, ?> restored = preferences.getAll();
        sender.respond("{\"state\":1,\"user_id\":19}");
        assertEquals(LoginService.Failure.SESSION_CHANGED, result.failure);
        assertEquals(restored, preferences.getAll());
    }

    /** Ordinary settings changes do not interfere with the pending authenticated account update. */
    @Test
    public void unrelatedPreferenceChangeDoesNotRejectAValidLogin() {
        service.verify("new-user", "new-password", result);
        preferences.edit().putBoolean("dvc_uploader", false).apply();
        sender.respond("{\"state\":1,\"user_id\":19}");
        assertNotNull(result.profile);
        assertFalse(preferences.getBoolean("dvc_uploader", true));
    }

    /** Legacy unsaved-auth metadata does not prevent verification of a complete replacement pair. */
    @Test
    public void previouslyUnvalidatedCredentialsCanBeReplacedOnlyAfterSuccess() {
        preferences.edit().putInt("auth_state", 0).putInt("user_id", 0).apply();
        Map<String, ?> before = preferences.getAll();
        service.verify("new-user", "new-password", result);
        assertEquals(before, preferences.getAll());
        sender.respond("{\"state\":1,\"user_id\":19}");
        assertEquals(1, preferences.getInt("auth_state", 0));
        assertEquals(19, preferences.getInt("user_id", 0));
    }

    /** Invalid local fields fail before a request is created and retain the previous credentials. */
    @Test
    public void invalidInputNeverQueuesOrPersistsCredentials() {
        Map<String, ?> before = preferences.getAll();
        service.verify("a", "new-password", result);
        assertEquals(LoginService.Failure.INVALID_LOGIN, result.failure);
        result = new Result();
        service.verify("new-user", "x", result);
        assertEquals(LoginService.Failure.INVALID_PASSWORD, result.failure);
        assertTrue(sender.requests.isEmpty());
        assertEquals(before, preferences.getAll());
    }

    /** A queue construction failure gives the same retryable outcome as an unavailable network. */
    @Test
    public void queueFailureDoesNotSaveTheDraftPair() {
        Map<String, ?> before = preferences.getAll();
        LoginService unavailable = new LoginService(preferences, request -> {
            throw new IllegalStateException("Unavailable request queue");
        });
        unavailable.verify("new-user", "new-password", result);
        assertEquals(LoginService.Failure.NETWORK_ERROR, result.failure);
        assertEquals(before, preferences.getAll());
    }

    /** Captures request delivery without exposing the temporary credentials through callback state. */
    private static final class FakeSender implements LoginService.Sender {
        private final List<StringRequest> requests = new ArrayList<>();

        /** Records each request to inspect its real URL, cancellation, and Volley retry settings. */
        @Override
        public void send(StringRequest request) { requests.add(request); }

        /** Returns the current request for deterministic asynchronous-result simulation. */
        StringRequest latest() { return requests.get(requests.size() - 1); }

        /** Runs Volley's protected response delivery against an actual documented API payload. */
        void respond(String body) {
            ReflectionHelpers.callInstanceMethod(latest(), "deliverResponse",
                    ReflectionHelpers.ClassParameter.from(String.class, body));
        }
    }

    /** Stores the public outcome and asserts that every attempt has a single terminal callback. */
    private static final class Result implements LoginService.Callback {
        LoginService.Profile profile;
        LoginService.Failure failure;

        /** Records the verified profile exactly once after its preference transaction completes. */
        @Override
        public void onSuccess(LoginService.Profile profile) {
            assertNull(this.profile);
            assertNull(failure);
            this.profile = profile;
        }

        /** Records one failure without receiving or retaining any supplied password. */
        @Override
        public void onError(LoginService.Failure failure) {
            assertNull(profile);
            assertNull(this.failure);
            this.failure = failure;
        }
    }
}

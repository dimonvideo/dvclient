package com.dimonvideo.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.StringRequest;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;

import java.util.HashMap;
import java.util.Map;

/** Exercises delayed legacy auth responses against real saved sessions without server access. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = NetworkUtilsAuthTest.TestApp.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class NetworkUtilsAuthTest {
    private ActivityController<AppCompatActivity> activity;
    private TestApp application;
    private SharedPreferences preferences;

    /** Supplies the legacy screen binding and an existing account while capturing its queued requests. */
    @Before
    public void setUp() {
        application = (TestApp) RuntimeEnvironment.getApplication();
        preferences = application.getSharedPreferences();
        preferences.edit().clear().commit();
        saveAccount("Alice", "alice-password", 12);
        activity = Robolectric.buildActivity(AppCompatActivity.class);
        activity.get().setTheme(R.style.AppTheme);
        activity.setup();
        MainActivity.binding = ActivityMainBinding.inflate(activity.get().getLayoutInflater());
        activity.get().setContentView(MainActivity.binding.getRoot());
    }

    /** Removes the legacy static view reference before destroying its themed host. */
    @After
    public void tearDown() {
        if (application.latestRequest != null) application.latestRequest.cancel();
        MainActivity.binding = null;
        activity.pause().stop().destroy();
    }

    /** The delayed old account's success cannot replace a newly verified account's identity or metadata. */
    @Test
    public void passwordRefreshSuccessAfterAccountSwitchLeavesNewSessionUntouched() throws JSONException {
        StringRequest request = passwordRequest();
        saveAccount("Bob", "bob-password", 24);
        Map<String, ?> expected = snapshot();
        deliver(request, successfulResponse());
        assertEquals(expected, snapshot());
    }

    /** Rejected old credentials cannot sign out the replacement account. */
    @Test
    public void passwordRefreshRejectionAfterAccountSwitchLeavesNewSessionUntouched() {
        StringRequest request = passwordRequest();
        saveAccount("Bob", "bob-password", 24);
        Map<String, ?> expected = snapshot();
        deliver(request, "{\"state\":0}");
        assertEquals(expected, snapshot());
    }

    /** Logging out while the request is pending must not restore authentication or account metadata. */
    @Test
    public void passwordRefreshSuccessAfterLogoutDoesNotRestoreOldAccount() throws JSONException {
        StringRequest request = passwordRequest();
        preferences.edit().clear().putInt("auth_state", 0).commit();
        Map<String, ?> expected = snapshot();
        deliver(request, successfulResponse());
        assertEquals(expected, snapshot());
    }

    /** A state change remains a session replacement even when the stored credentials were retained. */
    @Test
    public void passwordRefreshCannotUndoSignOutWithRetainedCredentials() throws JSONException {
        StringRequest request = passwordRequest();
        preferences.edit().putInt("auth_state", 0).commit();
        Map<String, ?> expected = snapshot();
        deliver(request, successfulResponse());
        assertEquals(expected, snapshot());
    }

    /** An intervening identity update must also invalidate an old rejection using identical credentials. */
    @Test
    public void passwordRefreshRejectionCannotInvalidateReplacementIdentity() {
        StringRequest request = passwordRequest();
        preferences.edit().putInt("user_id", 24).commit();
        Map<String, ?> expected = snapshot();
        deliver(request, "{\"state\":0}");
        assertEquals(expected, snapshot());
    }

    /** Returning to the same account after logout must not make an earlier rejection current again. */
    @Test
    public void passwordRefreshRejectionAfterSameAccountSignInLeavesNewSessionUntouched() {
        StringRequest request = passwordRequest();
        preferences.edit().clear().putInt("auth_state", 0).commit();
        saveAccount("Alice", "alice-password", 12);
        Map<String, ?> expected = snapshot();
        deliver(request, "{\"state\":0}");
        assertEquals(expected, snapshot());
    }

    /** A pre-logout success must not overwrite newer metadata after a same-account sign-in. */
    @Test
    public void passwordRefreshSuccessAfterSameAccountSignInLeavesNewSessionUntouched() throws JSONException {
        StringRequest request = passwordRequest();
        preferences.edit().clear().putInt("auth_state", 0).commit();
        saveAccount("Alice", "alice-password", 12);
        Map<String, ?> expected = snapshot();
        deliver(request, successfulResponse());
        assertEquals(expected, snapshot());
    }

    /** Unrelated settings edits must not stop a valid refresh of the same account. */
    @Test
    public void currentSessionRefreshStillUpdatesMetadataAfterUnrelatedSettingEdit() throws JSONException {
        StringRequest request = passwordRequest();
        preferences.edit().putString("font_size", "large").commit();
        deliver(request, successfulResponse());
        assertEquals(1, preferences.getInt("auth_state", 0));
        assertEquals(12, preferences.getInt("user_id", 0));
        assertEquals(9, preferences.getInt("pm_unread", 0));
        assertEquals("updated-rank", preferences.getString("auth_rang", ""));
        assertEquals("Alice", preferences.getString("dvc_login", ""));
        assertEquals("alice-password", preferences.getString("dvc_password", ""));
        assertEquals("large", preferences.getString("font_size", ""));
    }

    /** A network error from the old session must not show a misleading error for the replacement account. */
    @Test
    public void passwordRefreshTransportFailureAfterAccountSwitchIsSilent() {
        StringRequest request = passwordRequest();
        saveAccount("Bob", "bob-password", 24);
        Map<String, ?> expected = snapshot();
        request.deliverError(new TimeoutError());
        assertEquals(expected, snapshot());
        assertNull(ShadowToast.getLatestToast());
    }

    /** Legacy login success cannot reauthenticate a session whose owner changed after it was queued. */
    @Test
    public void legacyLoginSuccessAfterLogoutCannotRestoreAuthentication() {
        StringRequest request = loginRequest();
        preferences.edit().clear().putInt("auth_state", 0).commit();
        Map<String, ?> expected = snapshot();
        deliver(request, "{\"state\":1}");
        assertEquals(expected, snapshot());
    }

    /** Legacy login rejection cannot sign out the newly verified account. */
    @Test
    public void legacyLoginRejectionAfterAccountSwitchLeavesNewSessionUntouched() {
        StringRequest request = loginRequest();
        saveAccount("Bob", "bob-password", 24);
        Map<String, ?> expected = snapshot();
        deliver(request, "{\"state\":0}");
        assertEquals(expected, snapshot());
    }

    /** The obsolete login flow must also remember a logout even when all identity values are restored. */
    @Test
    public void legacyLoginRejectionAfterSameAccountSignInLeavesNewSessionUntouched() {
        StringRequest request = loginRequest();
        preferences.edit().clear().putInt("auth_state", 0).commit();
        saveAccount("Alice", "alice-password", 12);
        Map<String, ?> expected = snapshot();
        deliver(request, "{\"state\":0}");
        assertEquals(expected, snapshot());
    }

    /** Login transport failures are also ignored after their original session has been replaced. */
    @Test
    public void legacyLoginTransportFailureAfterAccountSwitchIsSilent() {
        StringRequest request = loginRequest();
        saveAccount("Bob", "bob-password", 24);
        Map<String, ?> expected = snapshot();
        request.deliverError(new TimeoutError());
        assertEquals(expected, snapshot());
        assertNull(ShadowToast.getLatestToast());
    }

    /** Explicit cancellation prevents either legacy API from accepting even forcibly delivered late callbacks. */
    @Test
    public void cancelledAuthenticationRequestsCannotCommitOrReportLateResults() throws JSONException {
        StringRequest[] requests = {passwordRequest(), loginRequest()};
        Map<String, ?> expected = snapshot();
        for (StringRequest request : requests) {
            request.cancel();
            assertTrue(request.isCanceled());
            deliver(request, successfulResponse());
            request.deliverError(new TimeoutError());
            assertEquals(expected, snapshot());
            assertNull(ShadowToast.getLatestToast());
        }
    }

    /** Completed ownership is released before metadata writes and cannot be claimed by a second callback. */
    @Test
    public void completedPasswordRefreshIgnoresDuplicateTerminalCallbacks() throws JSONException {
        StringRequest request = passwordRequest();
        deliver(request, successfulResponse());
        Map<String, ?> expected = snapshot();
        deliver(request, "{\"state\":0}");
        request.deliverError(new TimeoutError());
        assertEquals(expected, snapshot());
        assertNull(ShadowToast.getLatestToast());
    }

    /** Volley request diagnostics must describe the operation without including credential-bearing URLs. */
    @Test
    public void authenticationRequestDiagnosticsDoNotExposeCredentials() {
        assertEquals("Site account refresh", passwordRequest().toString());
        application.latestRequest.cancel();
        assertEquals("Site login verification", loginRequest().toString());
    }

    /** Starts the actual legacy refresh, allowing its real callback to be delivered after a session change. */
    private StringRequest passwordRequest() {
        NetworkUtils.checkPassword(activity.get(), "alice-password", "10");
        return latestRequest();
    }

    /** Captures the obsolete login API as well because it still mutates saved authentication state. */
    private StringRequest loginRequest() {
        NetworkUtils.checkLogin(activity.get(), "Alice");
        return latestRequest();
    }

    /** Retrieves the request that production code would otherwise have sent through Volley. */
    private StringRequest latestRequest() {
        assertNotNull(application.latestRequest);
        assertTrue(application.latestRequest instanceof StringRequest);
        return (StringRequest) application.latestRequest;
    }

    /** Invokes Volley's protected response delivery so tests exercise the production response listener. */
    private static void deliver(StringRequest request, String response) {
        ReflectionHelpers.callInstanceMethod(request, "deliverResponse",
                ReflectionHelpers.ClassParameter.from(String.class, response));
    }

    /** Saves an account atomically, as the new verified sign-in dialog does. */
    private void saveAccount(String login, String password, int id) {
        preferences.edit().putString("dvc_login", login).putString("dvc_password", password)
                .putInt("auth_state", 1).putInt("user_id", id).putInt("user_group", 4)
                .putInt("pm_unread", 2).putString("auth_foto", "old-image")
                .putString("auth_rang", "old-rank").putString("current_token", "same-token").commit();
    }

    /** Copies every saved value so unexpected metadata writes are caught along with identity changes. */
    private Map<String, ?> snapshot() { return new HashMap<>(preferences.getAll()); }

    /** Returns a complete documented auth payload with an unchanged push token to avoid Firebase startup. */
    private static String successfulResponse() throws JSONException {
        return new JSONObject().put("state", 1).put("user_id", 12).put("user_group", 4)
                .put("pm_unread", 9).put("image", "updated-image").put("headers", "updated-rank")
                .put("time", "today").put("reputation", "3").put("reg_date", "2020")
                .put("rating", "4").put("count", "5").put("token", "same-token").toString();
    }

    /** Supplies preferences and captures network requests without Room, worker, or Firebase initialization. */
    public static class TestApp extends AppController {
        Request<?> latestRequest;

        /** Installs the singleton used by the unmodified public legacy authentication APIs. */
        @Override
        public void onCreate() { ReflectionHelpers.setStaticField(AppController.class, "sInstance", this); }

        /** Uses this test application's preference store instead of any prior static application cache. */
        @Override
        public SharedPreferences getSharedPreferences() {
            return PreferenceManager.getDefaultSharedPreferences(this);
        }

        /** Records each production request while keeping its callback and URL construction unchanged. */
        @Override
        public <T> void addToRequestQueue(Request<T> request) { latestRequest = request; }
    }
}

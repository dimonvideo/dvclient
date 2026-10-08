package com.dimonvideo.client;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.dimonvideo.client.util.auth.LoginService;
import com.google.android.material.textfield.TextInputLayout;

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
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Exercises registration's two server acknowledgements without starting HTTP or push services. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = SettingsActivityTest.TestApp.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class SettingsRegistrationTest {
    private ActivityController<SettingsActivity> activity;
    private SettingsActivity.SettingsFragment fragment;
    private SharedPreferences preferences;
    private FakeOperations operations;
    private Dialog dialog;
    private boolean paused;

    /** Opens the production registration form over an isolated signed-out preference store. */
    @Before
    public void setUp() {
        preferences = getDefaultSharedPreferences(RuntimeEnvironment.getApplication());
        preferences.edit().clear().putInt("auth_state", 0)
                .putString("current_token", "existing-device-token").commit();
        activity = Robolectric.buildActivity(SettingsActivity.class);
        activity.get().setTheme(R.style.AppTheme);
        activity.setup();
        fragment = (SettingsActivity.SettingsFragment) activity.get().getSupportFragmentManager()
                .findFragmentById(R.id.settings);
        assertNotNull(fragment);
        operations = new FakeOperations();
        fragment.setRegistrationOperations(operations);
        fragment.loadReg(activity.get());
        dialog = ShadowDialog.getLatestDialog();
        assertTrue(dialog.isShowing());
        enterRegistration();
    }

    /** Releases the form and all requests before the next case inflates an independent activity. */
    @After
    public void tearDown() {
        if (!paused) activity.pause();
        activity.stop().destroy();
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Creation alone must not expose a stub session; a verified positive identity enables account features. */
    @Test
    public void createdAccountWaitsForVerificationBeforeSavingAndEnablingFeatures() {
        Map<String, ?> before = preferences.getAll();
        submit().performClick();
        submit().performClick();
        assertEquals(1, operations.registrations.size());
        respondToRegistration("{\"state\":2}");
        assertEquals(before, preferences.getAll());
        assertTrue(dialog.isShowing());
        assertFalse(submit().isEnabled());
        assertFalse(fragment.findPreference("dvc_pm").isEnabled());
        assertEquals("new-account", operations.latestVerification().login);
        assertEquals("candidate-password", operations.latestVerification().password);

        preferences.edit().putString("dvc_login", "new-account")
                .putString("dvc_password", "candidate-password")
                .putInt("auth_state", 1).putInt("user_id", 123).commit();
        operations.latestVerification().callback.onSuccess();
        assertFalse(dialog.isShowing());
        assertTrue(fragment.findPreference("dvc_pm").isEnabled());
        assertEquals(123, preferences.getInt("user_id", 0));
        assertEquals("existing-device-token", preferences.getString("current_token", ""));
        assertEquals("", input(R.id.txtPwd).getText().toString());
    }

    /** Verification errors preserve the created-account form and retry authentication without another POST. */
    @Test
    public void failedVerificationRetainsFieldsAndRetriesWithoutRegisteringAgain() {
        submit().performClick();
        respondToRegistration("{\"state\":2}");
        operations.latestVerification().callback.onError(LoginService.Failure.NETWORK_ERROR);
        assertTrue(dialog.isShowing());
        assertTrue(submit().isEnabled());
        assertEquals("candidate-password", input(R.id.txtPwd).getText().toString());
        assertNotNull(passwordLayout().getError());
        assertFalse(input(R.id.txtName).isEnabled());
        assertFalse(input(R.id.txtEmail).isEnabled());
        assertFalse(preferences.contains("dvc_login"));
        assertFalse(preferences.contains("dvc_password"));

        input(R.id.txtPwd).setText("corrected-password");
        submit().performClick();
        assertEquals(1, operations.registrations.size());
        assertEquals(2, operations.verifications.size());
        assertEquals("new-account", operations.latestVerification().login);
        assertEquals("corrected-password", operations.latestVerification().password);
        assertNull(passwordLayout().getError());
    }

    /** Dismissal cancels the verifier, clears the candidate secret and makes late success ineffective. */
    @Test
    public void cancelDuringVerificationIgnoresLateSuccessWithoutPublishingAccount() {
        submit().performClick();
        respondToRegistration("{\"state\":2}");
        Verification verification = operations.latestVerification();
        Map<String, ?> before = preferences.getAll();
        dialog.cancel();
        ShadowLooper.shadowMainLooper().idle();
        assertTrue(verification.canceled);
        verification.callback.onSuccess();
        assertFalse(dialog.isShowing());
        assertEquals(before, preferences.getAll());
        assertFalse(fragment.findPreference("dvc_pm").isEnabled());
        assertEquals("", input(R.id.txtPwd).getText().toString());
    }

    /** Screen pause cancels account creation so a response from an obsolete form cannot start sign-in. */
    @Test
    public void pauseDuringRegistrationCancelsPostAndIgnoresLateCreatedAccount() {
        submit().performClick();
        StringRequest request = operations.registrations.get(0);
        activity.pause();
        paused = true;
        assertTrue(request.isCanceled());
        assertFalse(dialog.isShowing());
        respondToRegistration("{\"state\":2}");
        assertEquals(0, operations.verifications.size());
        assertFalse(preferences.contains("dvc_login"));
        assertFalse(preferences.contains("user_id"));
        assertEquals("", input(R.id.txtPwd).getText().toString());
    }

    /** Pause cancels the second stage too, and resuming does not replay registration or verification. */
    @Test
    public void pauseDuringVerificationCancelsItAndDoesNotResubmitOnResume() {
        submit().performClick();
        respondToRegistration("{\"state\":2}");
        Verification verification = operations.latestVerification();
        activity.pause();
        paused = true;
        assertTrue(verification.canceled);
        verification.callback.onSuccess();
        activity.resume();
        paused = false;
        assertEquals(1, operations.registrations.size());
        assertEquals(1, operations.verifications.size());
        assertEquals(0, preferences.getInt("auth_state", 0));
        assertFalse(dialog.isShowing());
    }

    /** Immediate validation callbacks leave a retryable form rather than retaining a completed handle. */
    @Test
    public void synchronousVerificationFailureDoesNotLeaveFormBusy() {
        operations.synchronousFailure = LoginService.Failure.INVALID_PASSWORD;
        submit().performClick();
        respondToRegistration("{\"state\":2}");
        assertTrue(dialog.isShowing());
        assertTrue(submit().isEnabled());
        assertTrue(operations.latestVerification().canceled);
        assertEquals(activity.get().getString(R.string.password_invalid), passwordLayout().getError());
        operations.synchronousFailure = null;
        submit().performClick();
        assertEquals(1, operations.registrations.size());
        assertEquals(2, operations.verifications.size());
    }

    /** Queue startup failures remain visible and a verification retry does not recreate the account. */
    @Test
    public void verificationTransportExceptionKeepsCreatedAccountRetryable() {
        operations.throwDuringVerification = true;
        submit().performClick();
        respondToRegistration("{\"state\":2}");
        assertTrue(dialog.isShowing());
        assertTrue(submit().isEnabled());
        assertEquals(activity.get().getString(R.string.site_login_network_error), passwordLayout().getError());
        operations.throwDuringVerification = false;
        submit().performClick();
        assertEquals(1, operations.registrations.size());
        assertEquals(2, operations.verifications.size());
    }

    /** Registration enqueue failures cancel that POST and permit an explicit fresh attempt. */
    @Test
    public void registrationTransportExceptionCancelsPostWithoutSavingCredentials() {
        operations.throwDuringRegistration = true;
        submit().performClick();
        assertTrue(operations.registrations.get(0).isCanceled());
        assertTrue(dialog.isShowing());
        assertTrue(submit().isEnabled());
        assertNotNull(passwordLayout().getError());
        assertFalse(preferences.contains("dvc_password"));
        operations.throwDuringRegistration = false;
        submit().performClick();
        assertEquals(2, operations.registrations.size());
    }

    /** Failed or malformed creation replies retain the form and never begin authentication. */
    @Test
    public void registrationRejectionAndMalformedRepliesRemainRecoverable() {
        submit().performClick();
        respondToRegistration("{\"state\":0}");
        assertTrue(dialog.isShowing());
        assertTrue(submit().isEnabled());
        assertNotNull(passwordLayout().getError());
        submit().performClick();
        respondToRegistration("not-json");
        assertTrue(submit().isEnabled());
        assertEquals(0, operations.verifications.size());
        assertFalse(preferences.contains("dvc_login"));
    }

    /** The HTTP request captures submitted values and never automatically repeats a registration POST. */
    @Test
    public void registrationRequestCapturesFieldsAndDisablesCacheAndAutomaticRetry() {
        submit().performClick();
        StringRequest request = operations.registrations.get(0);
        assertEquals(Request.Method.POST, request.getMethod());
        assertFalse(request.shouldCache());
        assertEquals(0, request.getRetryPolicy().getCurrentRetryCount());
        try {
            request.getRetryPolicy().retry(new VolleyError());
            throw new AssertionError("Registration must not automatically retry its POST");
        } catch (VolleyError expected) {
            // A user retries an unacknowledged account creation deliberately.
        }
        input(R.id.txtName).setText("edited-after-submission");
        input(R.id.txtEmail).setText("edited@example.test");
        input(R.id.txtPwd).setText("edited-password");
        Map<String, String> params = ReflectionHelpers.callInstanceMethod(request, "getParams");
        assertEquals("new-account", params.get("userName"));
        assertEquals("member@example.test", params.get("userEmail"));
        assertEquals("candidate-password", params.get("userPassword"));
        respondToRegistration("{\"state\":2}");
        assertEquals("new-account", operations.latestVerification().login);
        assertEquals("candidate-password", operations.latestVerification().password);
        assertFalse(request.toString().contains("candidate-password"));
    }

    /** The candidate password must not enter a saved dialog hierarchy while waiting for either server. */
    @Test
    public void registrationPasswordIsExcludedFromSerializedDialogState() {
        EditText password = input(R.id.txtPwd);
        assertFalse(password.isSaveEnabled());
        Bundle saved = dialog.onSaveInstanceState();
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(saved);
            assertFalse(contains(parcel.marshall(), "candidate-password".getBytes(StandardCharsets.UTF_16LE)));
        } finally {
            parcel.recycle();
        }
        assertFalse(preferences.contains("dvc_password"));
    }

    /** Fills the real signup fields and accepts its existing site-rules checkbox. */
    private void enterRegistration() {
        input(R.id.txtName).setText(" new-account ");
        input(R.id.txtEmail).setText(" member@example.test ");
        input(R.id.txtPwd).setText("candidate-password");
        ((CheckBox) dialog.findViewById(R.id.regCheckBox)).setChecked(true);
    }

    /** Returns the production register/verify button rather than a simulated click callback. */
    private Button submit() { return dialog.findViewById(R.id.btnLogin); }

    /** Returns a real registration field for input-retention and state-saving assertions. */
    private EditText input(int id) { return dialog.findViewById(id); }

    /** Finds the password's Material container without assumptions about its internal input frame. */
    private TextInputLayout passwordLayout() {
        ViewParent parent = input(R.id.txtPwd).getParent();
        while (parent instanceof View) {
            if (parent instanceof TextInputLayout) return (TextInputLayout) parent;
            parent = parent.getParent();
        }
        throw new AssertionError("The registration password must have a Material error container");
    }

    /** Delivers an actual Volley payload so JSON state handling and generation guards are exercised. */
    private void respondToRegistration(String response) {
        StringRequest request = operations.registrations.get(operations.registrations.size() - 1);
        ReflectionHelpers.callInstanceMethod(request, "deliverResponse",
                ReflectionHelpers.ClassParameter.from(String.class, response));
    }

    /** Looks for password bytes in Android's serialized hierarchy rather than only inspecting save flags. */
    private static boolean contains(byte[] data, byte[] value) {
        for (int offset = 0; offset <= data.length - value.length; offset++) {
            boolean match = true;
            for (int index = 0; index < value.length; index++) {
                if (data[offset + index] != value[index]) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }

    /** Captures one verifier's callbacks and cancellation without touching persistent session data. */
    private static final class Verification {
        final String login;
        final String password;
        final SettingsActivity.SettingsFragment.RegistrationCallback callback;
        boolean canceled;

        /** Records the exact supplied pair for testing immutable creation values and explicit retries. */
        Verification(String login, String password,
                SettingsActivity.SettingsFragment.RegistrationCallback callback) {
            this.login = login;
            this.password = password;
            this.callback = callback;
        }
    }

    /** Controls both transport stages while leaving preference commits under each test's control. */
    private static final class FakeOperations implements SettingsActivity.SettingsFragment.RegistrationOperations {
        final List<StringRequest> registrations = new ArrayList<>();
        final List<Verification> verifications = new ArrayList<>();
        boolean throwDuringRegistration;
        boolean throwDuringVerification;
        LoginService.Failure synchronousFailure;

        /** Captures the actual POST and can simulate queue startup failing before server acknowledgement. */
        @Override
        public void register(StringRequest request) {
            registrations.add(request);
            if (throwDuringRegistration) throw new IllegalStateException("Test transport unavailable");
        }

        /** Records one explicit sign-in attempt and permits immediate validation or startup failure. */
        @Override
        public SettingsActivity.SettingsFragment.RegistrationCancellation verify(String login, String password,
                SettingsActivity.SettingsFragment.RegistrationCallback callback) {
            Verification attempt = new Verification(login, password, callback);
            verifications.add(attempt);
            if (throwDuringVerification) throw new IllegalStateException("Test transport unavailable");
            if (synchronousFailure != null) callback.onError(synchronousFailure);
            return () -> attempt.canceled = true;
        }

        /** Returns the latest verifier for deterministic late-success, failure and cancellation scenarios. */
        Verification latestVerification() { return verifications.get(verifications.size() - 1); }
    }
}

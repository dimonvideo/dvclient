package com.dimonvideo.client.ui.settings;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.os.Parcel;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.FragmentActivity;

import com.dimonvideo.client.R;
import com.dimonvideo.client.util.auth.LoginService;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.shape.MaterialShapeDrawable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Exercises the real login dialog without a server, credentials or application startup services. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class SiteLoginDialogFragmentTest {
    private ActivityController<FragmentActivity> controller;
    private SiteLoginDialogFragment fragment;
    private FakeVerifier verifier;
    private int successResults;

    /** Opens a themed dialog over an existing verified account and injects controllable authentication. */
    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(FragmentActivity.class);
        controller.get().setTheme(R.style.AppTheme);
        controller.setup();
        getDefaultSharedPreferences(controller.get()).edit().clear()
                .putString("dvc_login", "verified-name")
                .putString("dvc_password", "existing-secret")
                .putInt("auth_state", 1).commit();
        controller.get().getSupportFragmentManager().setFragmentResultListener(
                SiteLoginDialogFragment.RESULT_KEY, controller.get(), (key, result) -> successResults++);
        verifier = new FakeVerifier();
        fragment = new SiteLoginDialogFragment();
        fragment.setVerifier(verifier);
        fragment.showNow(controller.get().getSupportFragmentManager(), SiteLoginDialogFragment.TAG);
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Cancels every pending test attempt and destroys the host without retaining dialog views. */
    @After
    public void tearDown() {
        if (fragment.isAdded()) fragment.dismissNow();
        controller.pause().stop().destroy();
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Rejected credentials must retain the candidate, preserve the existing session and allow a retry. */
    @Test
    public void rejectionStaysOpenAndRetainsCandidateWithoutChangingVerifiedAccount() {
        enter("  another-name  ", "candidate-password");
        positive().performClick();
        assertEquals("another-name", verifier.login);
        assertEquals("candidate-password", verifier.password);
        assertFalse(positive().isEnabled());
        assertFalse(input(R.id.site_login_name_input).isEnabled());
        assertTrue(negative().isEnabled());
        verifier.callback(0).onError(LoginService.Failure.REJECTED);
        assertTrue(dialog().isShowing());
        assertTrue(positive().isEnabled());
        assertEquals("candidate-password", input(R.id.site_login_password_input).getText().toString());
        assertEquals(View.VISIBLE, dialog().findViewById(R.id.site_login_error).getVisibility());
        assertEquals(0, successResults);
        assertEquals("existing-secret", getDefaultSharedPreferences(controller.get())
                .getString("dvc_password", ""));
        positive().performClick();
        assertEquals(2, verifier.callbacks.size());
    }

    /** A network failure must keep both fields intact and permit an explicit retry through the IME. */
    @Test
    public void networkFailureRemainsOpenAndDoneActionRetriesBothFields() {
        enter("other-user", "network-candidate");
        positive().performClick();
        verifier.callback(0).onError(LoginService.Failure.NETWORK_ERROR);
        assertTrue(dialog().isShowing());
        assertEquals(dialog().getContext().getString(R.string.site_login_network_error),
                ((TextView) dialog().findViewById(R.id.site_login_error)).getText().toString());
        input(R.id.site_login_password_input).onEditorAction(EditorInfo.IME_ACTION_DONE);
        assertEquals(2, verifier.callbacks.size());
        assertEquals("other-user", verifier.login);
        assertEquals("network-candidate", verifier.password);
    }

    /** Empty fields show their own inline errors before authentication and never dismiss the dialog. */
    @Test
    public void missingFieldValidationKeepsDialogOpenAndDoesNotStartRequest() {
        enter("", "");
        positive().performClick();
        TextInputLayout login = dialog().findViewById(R.id.site_login_name_layout);
        assertNotNull(login.getError());
        input(R.id.site_login_name_input).setText("valid-user");
        positive().performClick();
        TextInputLayout password = dialog().findViewById(R.id.site_login_password_layout);
        assertNotNull(password.getError());
        assertEquals(0, verifier.callbacks.size());
        assertTrue(dialog().isShowing());
    }

    /** Cancel remains available during verification and makes a late success incapable of closing or publishing. */
    @Test
    public void cancelAbortsInFlightRequestAndIgnoresItsLateSuccess() {
        enter("other-user", "cancel-candidate");
        positive().performClick();
        AlertDialog dialog = dialog();
        negative().performClick();
        controller.get().getSupportFragmentManager().executePendingTransactions();
        ShadowLooper.shadowMainLooper().idle();
        assertFalse(dialog.isShowing());
        assertEquals(1, verifier.canceled);
        verifier.callback(0).onSuccess();
        assertEquals(0, successResults);
        assertEquals("verified-name", getDefaultSharedPreferences(controller.get())
                .getString("dvc_login", ""));
    }

    /** A committed success publishes the settings refresh result and is the only positive-button dismiss path. */
    @Test
    public void successfulVerificationPublishesOneResultAndClosesDialog() {
        enter("other-user", "success-candidate");
        positive().performClick();
        AlertDialog dialog = dialog();
        assertTrue(dialog.isShowing());
        verifier.callback(0).onSuccess();
        controller.get().getSupportFragmentManager().executePendingTransactions();
        ShadowLooper.shadowMainLooper().idle();
        assertFalse(dialog.isShowing());
        assertEquals(1, successResults);
        verifier.callback(0).onSuccess();
        assertEquals(1, successResults);
    }

    /** Repeated OK and IME actions cannot start another request while the first is pending. */
    @Test
    public void pendingAttemptRejectsDuplicateSubmissionsAndSessionChangeAllowsRetry() {
        enter("other-user", "pending-candidate");
        positive().performClick();
        positive().performClick();
        input(R.id.site_login_password_input).onEditorAction(EditorInfo.IME_ACTION_DONE);
        assertEquals(1, verifier.callbacks.size());
        verifier.callback(0).onError(LoginService.Failure.SESSION_CHANGED);
        assertTrue(dialog().isShowing());
        assertTrue(positive().isEnabled());
        assertEquals(dialog().getContext().getString(R.string.site_login_session_changed),
                ((TextView) dialog().findViewById(R.id.site_login_error)).getText().toString());
        positive().performClick();
        assertEquals(2, verifier.callbacks.size());
    }

    /** Saving a real dialog hierarchy must not serialize the password entered in its text field. */
    @Test
    public void passwordIsNeverPrefilledOrSerializedIntoDialogState() {
        assertEquals("verified-name", input(R.id.site_login_name_input).getText().toString());
        EditText password = input(R.id.site_login_password_input);
        assertEquals("", password.getText().toString());
        assertFalse(password.isSaveEnabled());
        String candidate = "never-save-password_123";
        password.setText(candidate);
        Bundle state = dialog().onSaveInstanceState();
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(state);
            assertFalse(contains(parcel.marshall(), candidate.getBytes(StandardCharsets.UTF_16LE)));
        } finally {
            parcel.recycle();
        }
        assertEquals("existing-secret", getDefaultSharedPreferences(controller.get())
                .getString("dvc_password", ""));
    }

    /** Backgrounding cancels a request before state saving, retains the candidate and rejects its late callback. */
    @Test
    public void pauseCancelsPendingRequestWithoutAutoSubmittingOnResume() {
        enter("other-user", "pause-candidate");
        positive().performClick();
        controller.pause();
        verifier.callback(0).onSuccess();
        assertEquals(1, verifier.canceled);
        assertEquals(0, successResults);
        controller.resume();
        assertEquals(1, verifier.callbacks.size());
        assertTrue(dialog().isShowing());
        assertTrue(positive().isEnabled());
        assertEquals("pause-candidate", input(R.id.site_login_password_input).getText().toString());
    }

    /** Handles services which synchronously reject a malformed field without keeping a stale busy state. */
    @Test
    public void synchronousValidationFailureNeverLeavesDialogBusy() {
        verifier.synchronousFailure = LoginService.Failure.INVALID_PASSWORD;
        enter("valid-user", "short");
        positive().performClick();
        assertTrue(dialog().isShowing());
        assertTrue(positive().isEnabled());
        assertNotNull(((TextInputLayout) dialog().findViewById(R.id.site_login_password_layout)).getError());
        assertEquals(1, verifier.canceled);
    }

    /** Light-theme field text, errors and actions must remain readable with one hint per field. */
    @Test
    @Config(qualifiers = "notnight")
    public void lightThemeUsesReadableActualWidgetColorsAndSingleHints() {
        assertFormContrastAndHints();
    }

    /** Night-theme field text, errors and actions must stay visible on the actual dialog surface. */
    @Test
    @Config(qualifiers = "night")
    public void darkThemeUsesReadableActualWidgetColorsAndSingleHints() {
        assertFormContrastAndHints();
    }

    /** Checks the rendered dialog surface and actual widget colors rather than color-resource names. */
    private void assertFormContrastAndHints() {
        Drawable background = dialog().getWindow().getDecorView().getBackground();
        while (background instanceof InsetDrawable) background = ((InsetDrawable) background).getDrawable();
        assertTrue(background instanceof MaterialShapeDrawable);
        int surface = ((MaterialShapeDrawable) background).getFillColor().getDefaultColor();
        TypedValue expectedSurface = new TypedValue();
        assertTrue(dialog().getContext().getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorSurface, expectedSurface, true));
        assertEquals(expectedSurface.data, surface);
        assertReadable(input(R.id.site_login_name_input).getCurrentTextColor(), surface);
        assertReadable(input(R.id.site_login_password_input).getCurrentTextColor(), surface);
        assertReadable(input(R.id.site_login_name_input).getCurrentHintTextColor(), surface);
        assertReadable(input(R.id.site_login_password_input).getCurrentHintTextColor(), surface);
        assertReadable(positive().getCurrentTextColor(), surface);
        assertReadable(negative().getCurrentTextColor(), surface);
        TextView error = dialog().findViewById(R.id.site_login_error);
        assertReadable(error.getCurrentTextColor(), surface);
        TextInputLayout login = dialog().findViewById(R.id.site_login_name_layout);
        TextInputLayout password = dialog().findViewById(R.id.site_login_password_layout);
        assertEquals(dialog().getContext().getString(R.string.site_login_name), login.getHint());
        assertEquals(dialog().getContext().getString(R.string.site_login_password), password.getHint());
        assertNull(ReflectionHelpers.getField(input(R.id.site_login_name_input), "mHint"));
        assertNull(ReflectionHelpers.getField(input(R.id.site_login_password_input), "mHint"));
    }

    /** Requires at least WCAG's 4.5:1 contrast for normal text on its actual assigned background. */
    private static void assertReadable(int foreground, int background) {
        assertTrue("Login text/action contrast must be at least 4.5:1",
                ColorUtils.calculateContrast(foreground, background) >= 4.5);
    }

    /** Sets candidate credentials using the production layout's actual editable fields. */
    private void enter(String login, String password) {
        input(R.id.site_login_name_input).setText(login);
        input(R.id.site_login_password_input).setText(password);
    }

    /** Returns the current dialog controlled by the real fragment lifecycle. */
    private AlertDialog dialog() { return (AlertDialog) fragment.requireDialog(); }

    /** Locates a real editable field so tests exercise Material containers and enabled states. */
    private EditText input(int id) { return dialog().findViewById(id); }

    /** Returns the explicit verification button whose listener must prevent automatic dismissal. */
    private Button positive() { return dialog().getButton(DialogInterface.BUTTON_POSITIVE); }

    /** Returns the always available cancellation action. */
    private Button negative() { return dialog().getButton(DialogInterface.BUTTON_NEGATIVE); }

    /** Searches parcel bytes without assuming alignment of Android's UTF-16 strings. */
    private static boolean contains(byte[] bytes, byte[] needle) {
        for (int start = 0; start <= bytes.length - needle.length; start++) {
            int offset = 0;
            while (offset < needle.length && bytes[start + offset] == needle[offset]) offset++;
            if (offset == needle.length) return true;
        }
        return false;
    }

    /** Records only test credentials and delivers outcomes under the test's explicit control. */
    private static final class FakeVerifier implements SiteLoginDialogFragment.Verifier {
        final List<SiteLoginDialogFragment.VerificationCallback> callbacks = new ArrayList<>();
        String login, password;
        int canceled;
        LoginService.Failure synchronousFailure;

        /** Captures a request and returns a cancellation handle without making a network connection. */
        @Override
        public SiteLoginDialogFragment.CancelHandle verify(String login, String password,
                SiteLoginDialogFragment.VerificationCallback callback) {
            this.login = login;
            this.password = password;
            callbacks.add(callback);
            if (synchronousFailure != null) callback.onError(synchronousFailure);
            return () -> canceled++;
        }

        /** Returns a recorded callback to simulate success, rejection or delivery after cancellation. */
        SiteLoginDialogFragment.VerificationCallback callback(int index) { return callbacks.get(index); }
    }
}

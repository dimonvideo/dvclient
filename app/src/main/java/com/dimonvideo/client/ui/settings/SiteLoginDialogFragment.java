package com.dimonvideo.client.ui.settings;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.dimonvideo.client.R;
import com.dimonvideo.client.util.auth.LoginService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Verifies both credentials in one cancelable dialog and publishes only a committed login. */
public final class SiteLoginDialogFragment extends DialogFragment {
    public static final String TAG = "site-login";
    public static final String RESULT_KEY = "site-login-verified";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextInputLayout loginLayout, passwordLayout;
    private TextInputEditText loginInput, passwordInput;
    private TextView errorText;
    private View progress, status;
    private Button submitButton;
    private Verifier verifier;
    private CancelHandle activeAttempt;
    private int attemptGeneration;
    private boolean busy;

    /** Allows Android to restore the fragment without retaining a password or a pending request. */
    public SiteLoginDialogFragment() { }

    /** Injects controlled authentication for UI tests before the dialog is created. */
    void setVerifier(Verifier verifier) {
        this.verifier = verifier;
    }

    /** Builds accessible fields using a scoped day/night theme and disables password state saving. */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_DVClient_FormDialog);
        View content = requireActivity().getLayoutInflater().cloneInContext(builder.getContext())
                .inflate(R.layout.dialog_site_login,
                new FrameLayout(builder.getContext()), false);
        loginLayout = content.findViewById(R.id.site_login_name_layout);
        passwordLayout = content.findViewById(R.id.site_login_password_layout);
        loginInput = content.findViewById(R.id.site_login_name_input);
        passwordInput = content.findViewById(R.id.site_login_password_input);
        errorText = content.findViewById(R.id.site_login_error);
        progress = content.findViewById(R.id.site_login_progress);
        status = content.findViewById(R.id.site_login_status);
        passwordInput.setSaveEnabled(false);
        passwordInput.setFreezesText(false);
        String savedLogin = getDefaultSharedPreferences(requireContext())
                .getString("dvc_login", "");
        if (savedLogin != null && !"null".equals(savedLogin)) loginInput.setText(savedLogin);
        passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId != EditorInfo.IME_ACTION_DONE && !enter) return false;
            if (!enter || event.getAction() == KeyEvent.ACTION_UP) submit();
            return true;
        });
        if (verifier == null) verifier = createVerifier();
        return builder.setTitle(R.string.site_login_title)
                .setView(content)
                .setPositiveButton(R.string.site_login_submit, null)
                .setNegativeButton(R.string.site_login_cancel, (dialog, which) -> cancelVerification())
                .create();
    }

    /** Replaces the positive button's automatic dismiss action with a verified login attempt. */
    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) requireDialog();
        submitButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        submitButton.setOnClickListener(view -> submit());
        Window window = dialog.getWindow();
        if (window != null) window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setBusy(false);
    }

    /** Cancels backgrounded requests before saved fragment state can outlive their callbacks. */
    @Override
    public void onPause() {
        cancelVerification();
        super.onPause();
    }

    /** Aborts verification when Back or an outside tap cancels the dialog. */
    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        cancelVerification();
        super.onCancel(dialog);
    }

    /** Invalidates callbacks for every dismissal path, including the always available Cancel button. */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        cancelVerification();
        super.onDismiss(dialog);
    }

    /** Releases view references and the in-memory password when the dialog or activity is destroyed. */
    @Override
    public void onDestroyView() {
        cancelVerification();
        if (passwordInput != null) passwordInput.setText(null);
        loginLayout = null;
        passwordLayout = null;
        loginInput = null;
        passwordInput = null;
        errorText = null;
        progress = null;
        status = null;
        submitButton = null;
        super.onDestroyView();
    }

    /** Starts one explicit attempt while leaving invalid input and transport failures editable. */
    private void submit() {
        if (busy || loginInput == null || passwordInput == null) return;
        String login = loginInput.getText() == null ? "" : loginInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();
        loginLayout.setError(null);
        passwordLayout.setError(null);
        errorText.setVisibility(View.GONE);
        if (login.isEmpty()) {
            loginLayout.setError(getString(R.string.site_login_required_login));
            loginInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.site_login_required_password));
            passwordInput.requestFocus();
            return;
        }
        int generation = ++attemptGeneration;
        setBusy(true);
        CancelHandle attempt = verifier.verify(login, password, new VerificationCallback() {
            /** Displays success only after the authentication service has saved the verified session. */
            @Override
            public void onSuccess() {
                deliver(generation, () -> {
                    activeAttempt = null;
                    setBusy(false);
                    passwordInput.setText(null);
                    getParentFragmentManager().setFragmentResult(RESULT_KEY, new Bundle());
                    dismiss();
                });
            }

            /** Keeps candidate credentials in the dialog and allows another deliberate attempt. */
            @Override
            public void onError(LoginService.Failure failure) {
                deliver(generation, () -> {
                    activeAttempt = null;
                    setBusy(false);
                    showFailure(failure);
                });
            }
        });
        // Validation may complete synchronously; never retain its already completed request.
        if (busy && generation == attemptGeneration) activeAttempt = attempt;
        else if (attempt != null) attempt.cancel();
    }

    /** Runs a current callback on the UI thread while discarding canceled or detached attempts. */
    private void deliver(int generation, Runnable action) {
        Runnable guarded = () -> {
            Dialog dialog = getDialog();
            if (generation != attemptGeneration || !busy || !isAdded()
                    || dialog == null || !dialog.isShowing() || passwordInput == null) return;
            action.run();
        };
        if (Looper.myLooper() == Looper.getMainLooper()) guarded.run();
        else mainHandler.post(guarded);
    }

    /** Separates invalid fields, server rejection, network failure and a competing account change. */
    private void showFailure(LoginService.Failure failure) {
        if (failure == LoginService.Failure.INVALID_LOGIN) {
            loginLayout.setError(getString(R.string.login_invalid));
            loginInput.requestFocus();
        } else if (failure == LoginService.Failure.INVALID_PASSWORD) {
            passwordLayout.setError(getString(R.string.password_invalid));
            passwordInput.requestFocus();
        } else {
            int message = failure == LoginService.Failure.REJECTED ? R.string.site_login_rejected
                    : failure == LoginService.Failure.SESSION_CHANGED ? R.string.site_login_session_changed
                    : R.string.site_login_network_error;
            errorText.setText(message);
            errorText.setVisibility(View.VISIBLE);
        }
    }

    /** Locks duplicate submissions and field edits while preserving the dialog's Cancel action. */
    private void setBusy(boolean busy) {
        this.busy = busy;
        if (loginLayout != null) loginLayout.setEnabled(!busy);
        if (passwordLayout != null) passwordLayout.setEnabled(!busy);
        if (submitButton != null) submitButton.setEnabled(!busy);
        if (progress != null) progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (status != null) status.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    /** Cancels the transport and invalidates late results without changing the verified account. */
    private void cancelVerification() {
        attemptGeneration++;
        CancelHandle attempt = activeAttempt;
        activeAttempt = null;
        if (attempt != null) attempt.cancel();
        setBusy(false);
    }

    /** Adapts the production verifier without exposing candidate credentials to preferences or state. */
    private Verifier createVerifier() {
        LoginService service = new LoginService(requireContext());
        return (login, password, callback) -> {
            LoginService.Attempt attempt = service.verify(login, password, new LoginService.Callback() {
                /** Notifies the UI only after the complete valid session has been committed. */
                @Override
                public void onSuccess(LoginService.Profile profile) { callback.onSuccess(); }

                /** Delivers the authentication failure without logging credentials or server output. */
                @Override
                public void onError(LoginService.Failure failure) { callback.onError(failure); }
            });
            return attempt::cancel;
        };
    }

    /** Makes a cancellable login attempt injectable without starting a real network request in tests. */
    interface Verifier {
        /** Verifies both submitted fields and returns an idempotent cancellation handle. */
        CancelHandle verify(String login, String password, VerificationCallback callback);
    }

    /** Cancels a request while retaining the last verified application session. */
    interface CancelHandle {
        /** Aborts the pending operation without committing its candidate credentials. */
        void cancel();
    }

    /** Receives a committed login or a recoverable failure for the still open dialog. */
    interface VerificationCallback {
        /** Signals that the service has saved the verified session successfully. */
        void onSuccess();

        /** Reports why the unverified credentials were rejected without modifying preferences. */
        void onError(LoginService.Failure failure);
    }
}

/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.android.volley.Request;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.GetToken;
import com.dimonvideo.client.util.SetPrefsBackup;
import com.dimonvideo.client.util.auth.LoginService;
import com.dimonvideo.client.util.pm.PmHttpTransport;
import com.dimonvideo.client.ui.settings.SiteLoginDialogFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    /** Displays themed settings with the unified account entry before the feature preferences. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);

        // onBackPressed
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            /** Recreates the main screen so its navigation reflects confirmed account changes. */
            @Override
            public void handleOnBackPressed() {

                Intent i = new Intent(SettingsActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        private RegistrationForm registrationForm;
        private RegistrationOperations registrationOperations;

        /** Binds settings actions and opens one two-field dialog for server-verified sign-in. */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference dvc_theme = findPreference("dvc_theme_new");
            Preference siteLogin = findPreference("dvc_site_login");
            Preference dvc_pm = findPreference("dvc_pm");
            Preference dvc_clear_login = findPreference("dvc_clear_login");
            Preference dvc_register = findPreference("dvc_register");
            Preference dvc_favor = findPreference("dvc_favor");
            Preference dvc_more = findPreference("dvc_more");
            Preference dvc_comment = findPreference("dvc_comment");

            // переключение темы на лету
            assert dvc_theme != null;
            dvc_theme.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue.equals("yes")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                else if (newValue.equals("system")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });


            assert dvc_favor != null;
            dvc_favor.setOnPreferenceChangeListener((preference, newValue) -> {
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            assert dvc_more != null;
            dvc_more.setOnPreferenceChangeListener((preference, newValue) -> {
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            assert dvc_comment != null;
            dvc_comment.setOnPreferenceChangeListener((preference, newValue) -> {
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            assert siteLogin != null;
            siteLogin.setOnPreferenceClickListener(preference -> {
                if (getParentFragmentManager().findFragmentByTag(SiteLoginDialogFragment.TAG) == null) {
                    new SiteLoginDialogFragment().showNow(getParentFragmentManager(), SiteLoginDialogFragment.TAG);
                }
                return true;
            });
            getParentFragmentManager().setFragmentResultListener(
                    SiteLoginDialogFragment.RESULT_KEY, this, (key, result) -> onSignedIn());

            assert dvc_pm != null;
            dvc_pm.setOnPreferenceChangeListener((preference, newValue) -> {
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            assert dvc_clear_login != null;
            dvc_clear_login.setOnPreferenceClickListener(preference -> {
                alertForClearData();

                return true;
            });

            assert dvc_register != null;
            dvc_register.setOnPreferenceClickListener(preference -> {
                loadReg(getContext());

                return true;
            });
            // интеграция с DVGet
            SwitchPreferenceCompat dvc_dvget = findPreference("dvc_dvget");
            SwitchPreferenceCompat dvc_idm = findPreference("dvc_idm");
            assert dvc_idm != null;
            assert dvc_dvget != null;
            dvc_idm.setEnabled(!dvc_dvget.isChecked());
            dvc_dvget.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isEnabled = (Boolean) newValue;
                dvc_idm.setEnabled(!isEnabled);
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });
            dvc_idm.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isEnabled = (Boolean) newValue;
                dvc_dvget.setEnabled(!isEnabled);
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            Preference sett_backup = findPreference("sett_cloud_backup");
            assert sett_backup != null;
            sett_backup.setOnPreferenceClickListener(preference -> {
                Intent intentExport = new Intent(requireContext(), SetPrefsBackup.class);
                startActivity(intentExport);
                requireActivity().finish();
                return true;
            });

            Preference sett_token = findPreference("dvc_new_token");
            assert sett_token != null;
            sett_token.setOnPreferenceClickListener(preference -> {

                GetToken.getToken(requireContext());
                Snackbar.make(requireView(), this.getString(R.string.success), Snackbar.LENGTH_LONG).show();
                return true;
            });

            updateAccountPreferences();
        }



        /** Keeps account summaries and dependent controls current after one atomic session update. */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ("auth_state".equals(key) || "user_id".equals(key) || "dvc_login".equals(key)
                    || "dvc_password".equals(key) || "dvc_pm".equals(key)) {
                updateAccountPreferences();
            }
        }

        /** Subscribes only while the settings screen is active and refreshes its account status. */
        @Override
        public void onResume() {
            super.onResume();
            getDefaultSharedPreferences(requireContext())
                    .registerOnSharedPreferenceChangeListener(this);
            updateAccountPreferences();
        }

        /** Releases the preference callback when the screen no longer owns visible settings. */
        @Override
        public void onPause() {
            if (registrationForm != null) registrationForm.close();
            getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        /** Shows the verified account and enables features by authentication rather than a password field. */
        private void updateAccountPreferences() {
            if (getPreferenceScreen() == null) return;
            SharedPreferences preferences = getDefaultSharedPreferences(requireContext());
            boolean signedIn = preferences.getInt("auth_state", 0) > 0
                    && preferences.getInt("user_id", 0) > 0;
            Preference login = findPreference("dvc_site_login");
            if (login != null) login.setSummary(signedIn
                    ? getString(R.string.site_login_signed_in, preferences.getString("dvc_login", ""))
                    : getString(R.string.site_login_summary));
            for (String key : new String[]{"dvc_pm", "dvc_pm_notify", "dvc_pm_outbox", "dvc_pm_arc",
                    "sett_cloud_backup", "dvc_new_files", "dvc_add_file"}) {
                Preference preference = findPreference(key);
                if (preference != null) preference.setEnabled(signedIn);
            }
            Preference token = findPreference("dvc_new_token");
            if (token != null) token.setEnabled(signedIn
                    && !"off".equals(preferences.getString("dvc_pm", "off")));
            Preference register = findPreference("dvc_register");
            if (register != null) register.setVisible(!signedIn);
            Preference logout = findPreference("dvc_clear_login");
            if (logout != null) logout.setVisible(signedIn || preferences.contains("dvc_login")
                    || preferences.contains("dvc_password"));
        }

        /** Refreshes visible account controls and push registration after confirmed sign-in. */
        private void onSignedIn() {
            updateAccountPreferences();
            Context context = requireContext();
            context.sendBroadcast(new Intent(Config.INTENT_AUTH).setPackage(context.getPackageName()));
            GetToken.getToken(context);
            Toast.makeText(context, R.string.success_auth, Toast.LENGTH_SHORT).show();
        }

        /** Clears credentials and account metadata together only after the logout confirmation. */
        private void alertForClearData() {

            AlertDialog.Builder alert = new AlertDialog.Builder(requireActivity());

            alert.setTitle(getString(R.string.clear_alert_title));
            alert.setMessage(getString(R.string.clear_alert_message));

            alert.setCancelable(false);
            alert.setPositiveButton(R.string.ok, (dialog, which) -> {
                SharedPreferences.Editor editor;
                SharedPreferences sharedPrefs = getDefaultSharedPreferences(requireContext());
                editor = sharedPrefs.edit();
                editor.putInt("auth_state", 0);
                editor.remove("dvc_password");
                editor.remove("dvc_login");
                editor.remove("dvc_pm");
                for (String key : new String[]{"user_id", "user_group", "pm_unread", "auth_foto",
                        "auth_rang", "auth_last", "auth_rep", "auth_reg", "auth_rat", "auth_posts"}) {
                    editor.remove(key);
                }
                editor.apply();
                updateAccountPreferences();
            });
            alert.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
            alert.show();
        }

        /** Opens registration and verifies the created account before exposing authenticated features. */
        public void loadReg(Context mContext) {
            if (registrationForm != null) return;
            final Dialog dialog = new Dialog(mContext);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.registration);
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            if (registrationOperations == null) registrationOperations = createRegistrationOperations(mContext);
            registrationForm = new RegistrationForm(dialog);
        }

        /** Injects controlled registration and verification without starting real API requests in tests. */
        void setRegistrationOperations(RegistrationOperations operations) {
            registrationOperations = operations;
        }

        /** Shares the cancellable HTTPS transport and the same atomic verifier as ordinary sign-in. */
        private RegistrationOperations createRegistrationOperations(Context context) {
            LoginService service = new LoginService(context);
            return new RegistrationOperations() {
                /** Enqueues one uncached registration attempt without replaying its POST automatically. */
                @Override
                public void register(StringRequest request) { PmHttpTransport.queue(context).add(request); }

                /** Verifies newly registered credentials before reporting a complete authenticated session. */
                @Override
                public RegistrationCancellation verify(String login, String password,
                        RegistrationCallback callback) {
                    LoginService.Attempt attempt = service.verify(login, password, new LoginService.Callback() {
                        /** Notifies the form only after the verified identity and credentials are committed. */
                        @Override
                        public void onSuccess(LoginService.Profile profile) { callback.onSuccess(); }

                        /** Keeps verification failures recoverable without saving the candidate account. */
                        @Override
                        public void onError(LoginService.Failure failure) { callback.onError(failure); }
                    });
                    return attempt::cancel;
                }
            };
        }

        /** Owns one registration window and its requests without persisting unverified credentials. */
        private final class RegistrationForm {
            private final Dialog dialog;
            private final EditText name, email, password;
            private final Button submit;
            private final CheckBox rules;
            private final TextInputLayout passwordLayout;
            private StringRequest signupRequest;
            private RegistrationCancellation verification;
            private String registeredLogin;
            private int generation;
            private boolean busy, disposed;

            /** Captures the form once, disables password state saving and binds its cancelable lifecycle. */
            RegistrationForm(Dialog dialog) {
                this.dialog = dialog;
                name = dialog.findViewById(R.id.txtName);
                email = dialog.findViewById(R.id.txtEmail);
                password = dialog.findViewById(R.id.txtPwd);
                submit = dialog.findViewById(R.id.btnLogin);
                rules = dialog.findViewById(R.id.regCheckBox);
                passwordLayout = findTextInputLayout(password);
                password.setSaveEnabled(false);
                password.setFreezesText(false);
                rules.setOnCheckedChangeListener((button, checked) -> setBusy(busy));
                submit.setOnClickListener(view -> {
                    if (busy || disposed) return;
                    if (passwordLayout != null) passwordLayout.setError(null);
                    if (registeredLogin == null) register();
                    else verify(registeredLogin, password.getText().toString());
                });
                dialog.setOnDismissListener(ignored -> dispose());
                setBusy(false);
            }

            /** Creates the account once using immutable submitted fields rather than later text edits. */
            private void register() {
                if (!rules.isChecked()) return;
                String submittedLogin = name.getText().toString().trim();
                String submittedPassword = password.getText().toString();
                Map<String, String> submitted = new HashMap<>();
                submitted.put("userName", submittedLogin);
                submitted.put("userEmail", email.getText().toString().trim());
                submitted.put("userPassword", submittedPassword);
                int token = ++generation;
                setBusy(true);
                signupRequest = new StringRequest(Request.Method.POST, Config.REGISTRATION_URL, response -> {
                    if (!owns(token)) return;
                    signupRequest = null;
                    try {
                        if (new JSONObject(response).getInt("state") != 2) {
                            fail(R.string.unsuccess_auth);
                            return;
                        }
                    } catch (JSONException exception) {
                        fail(R.string.site_login_network_error);
                        return;
                    }
                    registeredLogin = submittedLogin;
                    submit.setText(R.string.site_login_submit);
                    verify(submittedLogin, submittedPassword);
                }, error -> {
                    if (!owns(token)) return;
                    signupRequest = null;
                    fail(R.string.site_login_network_error);
                }) {
                    /** Returns the captured registration fields even if the view is edited or destroyed. */
                    @Override
                    protected Map<String, String> getParams() { return submitted; }

                    /** Keeps transport diagnostics free of user-entered registration values. */
                    @Override
                    public String toString() { return "Site account registration"; }
                };
                signupRequest.setShouldCache(false);
                signupRequest.setRetryPolicy(new DefaultRetryPolicy(12_000, 0, 1));
                StringRequest submittedRequest = signupRequest;
                try {
                    registrationOperations.register(submittedRequest);
                } catch (RuntimeException exception) {
                    submittedRequest.cancel();
                    if (owns(token)) {
                        signupRequest = null;
                        fail(R.string.site_login_network_error);
                    }
                }
            }

            /** Retries sign-in after account creation without submitting registration a second time. */
            private void verify(String login, String enteredPassword) {
                int token = ++generation;
                setBusy(true);
                RegistrationCancellation attempt;
                try {
                    attempt = registrationOperations.verify(login, enteredPassword,
                        new RegistrationCallback() {
                            /** Refreshes settings only after the service saved a valid positive account identity. */
                            @Override
                            public void onSuccess() {
                                if (!owns(token)) return;
                                verification = null;
                                onSignedIn();
                                close();
                            }

                            /** Keeps the created account's form open for a deliberate verification retry. */
                            @Override
                            public void onError(LoginService.Failure failure) {
                                if (!owns(token)) return;
                                verification = null;
                                int message = failure == LoginService.Failure.REJECTED
                                        ? R.string.site_login_rejected
                                        : failure == LoginService.Failure.INVALID_LOGIN ? R.string.login_invalid
                                        : failure == LoginService.Failure.INVALID_PASSWORD ? R.string.password_invalid
                                        : failure == LoginService.Failure.SESSION_CHANGED
                                        ? R.string.site_login_session_changed : R.string.site_login_network_error;
                                fail(message);
                            }
                        });
                } catch (RuntimeException exception) {
                    if (owns(token)) fail(R.string.site_login_network_error);
                    return;
                }
                if (busy && owns(token)) verification = attempt;
                else if (attempt != null) attempt.cancel();
            }

            /** Accepts callbacks only for the request still owned by this visible registration window. */
            private boolean owns(int token) {
                return !disposed && registrationForm == this && generation == token
                        && isAdded() && dialog.isShowing();
            }

            /** Preserves candidate input and allows retry while showing an inline error beside the password. */
            private void fail(int message) {
                generation++;
                setBusy(false);
                if (passwordLayout != null) passwordLayout.setError(getString(message));
                else password.setError(getString(message));
            }

            /** Keeps account identity fixed after creation and blocks duplicate request submissions. */
            private void setBusy(boolean busy) {
                this.busy = busy;
                name.setEnabled(!busy && registeredLogin == null);
                email.setEnabled(!busy && registeredLogin == null);
                password.setEnabled(!busy);
                rules.setEnabled(!busy && registeredLogin == null);
                submit.setEnabled(!busy && (registeredLogin != null || rules.isChecked()));
            }

            /** Dismisses the form and releases its requests immediately rather than waiting for a response. */
            private void close() {
                dispose();
                dialog.dismiss();
            }

            /** Cancels both request stages and clears the password on dismissal or screen pause. */
            private void dispose() {
                if (disposed) return;
                disposed = true;
                generation++;
                if (signupRequest != null) signupRequest.cancel();
                signupRequest = null;
                if (verification != null) verification.cancel();
                verification = null;
                password.setText(null);
                if (registrationForm == this) registrationForm = null;
            }
        }

        /** Finds Material's container without relying on its internal input-frame hierarchy. */
        private static TextInputLayout findTextInputLayout(EditText input) {
            ViewParent parent = input.getParent();
            while (parent instanceof View) {
                if (parent instanceof TextInputLayout) return (TextInputLayout) parent;
                parent = parent.getParent();
            }
            return null;
        }

        /** Separates registration transport and authentication so tests can simulate both acknowledgements. */
        interface RegistrationOperations {
            /** Sends one registration request without persisting the submitted credentials. */
            void register(StringRequest request);

            /** Verifies credentials and calls success only after the complete session is committed. */
            RegistrationCancellation verify(String login, String password, RegistrationCallback callback);
        }

        /** Receives a verified account or an error while retaining the registration window. */
        interface RegistrationCallback {
            /** Reports that credentials and positive user identity have been atomically saved. */
            void onSuccess();

            /** Reports a verification failure without altering the previous saved account. */
            void onError(LoginService.Failure failure);
        }

        /** Owns cancellation of the verification stage independently of the registration POST. */
        interface RegistrationCancellation {
            /** Stops transport callbacks and prevents candidate account commits after dismissal. */
            void cancel();
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            finish();
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

package com.dimonvideo.client;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.GetToken;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.SetPrefsBackup;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private boolean doubleBackToExitPressedOnce = false, THEME;

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
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        // onBackPressed
        Intent intent = new Intent(this, MainActivity.class);
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finish();
                    startActivity(intent);
                }
                SettingsActivity.this.doubleBackToExitPressedOnce = true;
                Toast.makeText(getApplicationContext(), getString(R.string.press_twice), Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
            }
        };

        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback);

    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference dvc_theme = findPreference("dvc_theme_list");
            Preference dvc_password = findPreference("dvc_password");
            Preference dvc_login = findPreference("dvc_login");
            Preference dvc_pm = findPreference("dvc_pm");
            Preference dvc_clear_login = findPreference("dvc_clear_login");
            Preference dvc_register = findPreference("dvc_register");
            Preference dvc_scale = findPreference("dvc_scale");
            Preference dvc_favor = findPreference("dvc_favor");
            Preference dvc_more = findPreference("dvc_more");
            Preference dvc_comment = findPreference("dvc_comment");

            // переключение темы на лету
            assert dvc_theme != null;
            dvc_theme.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue.equals("on")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                else if (newValue.equals("system")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            // изменение размера шрифтов
            assert dvc_scale != null;
            dvc_scale.setOnPreferenceChangeListener((preference, newValue) -> {
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

            assert dvc_password != null;
            dvc_password.setOnPreferenceChangeListener((preference, newValue) -> {
                NetworkUtils.checkPassword(getContext(), (String) newValue, "10");
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            EditTextPreference PasPreference = findPreference("dvc_password");
            if (PasPreference!= null) {
                PasPreference.setOnBindEditTextListener(
                        editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            }

            assert dvc_login != null;
            dvc_login.setOnPreferenceChangeListener((preference, newValue) -> {
                   NetworkUtils.checkLogin(getContext(), (String) newValue);
                return true;
            });

            assert dvc_pm != null;
            dvc_pm.setOnPreferenceChangeListener((preference, newValue) -> {
                SharedPreferences sharedPrefs = getDefaultSharedPreferences(requireContext());
                final String password = sharedPrefs.getString("dvc_password", "null");
                NetworkUtils.checkPassword(getContext(), password, "10");
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            assert dvc_clear_login != null;
            dvc_clear_login.setOnPreferenceClickListener(preference -> {
                alertForClearData();

                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            assert dvc_register != null;
            dvc_register.setOnPreferenceClickListener(preference -> {
                loadReg(getContext());

                return true;
            });
            int auth_state = AppController.getInstance().isAuth();
            if (auth_state > 0) {
                dvc_register.setVisible(false);
                dvc_login.setVisible(false);
                dvc_password.setVisible(false);

            }


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

        }



        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }

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
                editor.remove("auth_foto");
                editor.apply();
                requireActivity().getOnBackPressedDispatcher();
            });
            alert.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
            alert.show();
        }

        // загрузить форму реги в окне
        public void loadReg(Context mContext) {

            final Dialog dialog = new Dialog(mContext);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.registration);
            Objects.requireNonNull(dialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

            dialog.show();

            Button btnLogin = dialog.findViewById(R.id.btnLogin);
            CheckBox regCheckBox = dialog.findViewById(R.id.regCheckBox);

            regCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> btnLogin.setEnabled(true));

            btnLogin.setOnClickListener(view -> {

                EditText userName = dialog.findViewById(R.id.txtName);
                EditText userEmail = dialog.findViewById(R.id.txtEmail);
                EditText userPassword = dialog.findViewById(R.id.txtPwd);

                String url = Config.REGISTRATION_URL;

                StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {

                    Log.e("Volley Result", "" + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String state = jsonObject.getString("state");

                        if (state.equals("2")) {

                            SharedPreferences.Editor editor;
                            SharedPreferences sharedPrefs = getDefaultSharedPreferences(mContext);
                            editor = sharedPrefs.edit();
                            editor.putInt("auth_state", 1);
                            editor.putString("dvc_password", userPassword.getText().toString());
                            editor.putString("dvc_login", userName.getText().toString());
                            editor.apply();

                            GetToken.getToken(mContext);

                            Toast.makeText(mContext, mContext.getString(R.string.success_auth), Toast.LENGTH_LONG).show();
                            requireActivity().getOnBackPressedDispatcher();
                            dialog.dismiss();

                        } else
                            Toast.makeText(mContext, mContext.getString(R.string.unsuccess_auth), Toast.LENGTH_LONG).show();

                    } catch (JSONException ignored) {

                    }
                }, Throwable::printStackTrace) {

                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> postMap = new HashMap<>();
                        postMap.put("userName", userName.getText().toString());
                        postMap.put("userEmail", userEmail.getText().toString());
                        postMap.put("userPassword", userPassword.getText().toString());
                        return postMap;
                    }
                };

                Volley.newRequestQueue(mContext).add(stringRequest);

            });

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
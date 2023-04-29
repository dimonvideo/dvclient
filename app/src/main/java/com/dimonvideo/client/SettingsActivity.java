package com.dimonvideo.client;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.util.GetToken;
import com.dimonvideo.client.util.NetworkUtils;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

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
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String value = sharedPrefs.getString("dvc_scale", "1.0f");
        adjustFontScale(getResources().getConfiguration(), this, value);

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
            Preference dvc_export = findPreference("dvc_export");
            Preference dvc_import = findPreference("dvc_import");
            Preference dvc_favor = findPreference("dvc_favor");
            Preference dvc_more = findPreference("dvc_more");
            Preference dvc_comment = findPreference("dvc_comment");

            // переключение темы на лету
            assert dvc_theme != null;
            dvc_theme.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue.equals("true")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                else if (newValue.equals("system")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                return true;
            });

            // изменение размера шрифтов
            assert dvc_scale != null;
            dvc_scale.setOnPreferenceChangeListener((preference, newValue) -> {
                Snackbar.make(requireView(), this.getString(R.string.restart_app), Snackbar.LENGTH_LONG).show();
                adjustFontScale(getResources().getConfiguration(), requireContext(), (String) newValue);
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
                String listValue = (String) newValue;
                View view = getView();
                NetworkUtils.checkPassword(getContext(), listValue);
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
                String listValue = (String) newValue;
                View view = getView();
                NetworkUtils.checkLogin(getContext(), listValue);
                return true;
            });

            assert dvc_pm != null;
            dvc_pm.setOnPreferenceChangeListener((preference, newValue) -> {
                SharedPreferences sharedPrefs = getDefaultSharedPreferences(requireContext());
                final String password = sharedPrefs.getString("dvc_password", "null");
                View view = getView();
                NetworkUtils.checkPassword(getContext(), password);
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

            assert dvc_export != null;
            dvc_export.setOnPreferenceClickListener(preference -> {
                saveSharedPreferencesToFile(new File(Environment.getExternalStorageDirectory(), "dvclient.settings"));
                return true;
            });

            assert dvc_import != null;
            dvc_import.setOnPreferenceClickListener(preference -> {
                loadSharedPreferencesFromFile(new File(Environment.getExternalStorageDirectory(), "dvclient.settings"));
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
                requireActivity().onBackPressed();
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
                            requireActivity().onBackPressed();
                            dialog.dismiss();

                        } else
                            Toast.makeText(mContext, mContext.getString(R.string.unsuccess_auth), Toast.LENGTH_LONG).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
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

        // save settings
        private void saveSharedPreferencesToFile(File dst) {
            ObjectOutputStream output = null;

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    output = new ObjectOutputStream(Files.newOutputStream(dst.toPath()));
                }
                SharedPreferences pref = getDefaultSharedPreferences(requireContext());
                if (output != null) {
                    output.writeObject(pref.getAll());
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (output != null) {
                        output.flush();
                        output.close();
                        Toast.makeText(requireContext(), requireContext().getString(R.string.export_success), Toast.LENGTH_LONG).show();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        // import settings
        @SuppressWarnings({"unchecked"})
        private void loadSharedPreferencesFromFile(File src) {
            boolean res = false;
            ObjectInputStream input = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    input = new ObjectInputStream(Files.newInputStream(src.toPath()));
                }
                SharedPreferences.Editor prefEdit = getDefaultSharedPreferences(requireContext()).edit();
                prefEdit.clear();
                Map<String, ?> entries = null;
                if (input != null) {
                    entries = (Map<String, ?>) input.readObject();
                }
                if (entries != null) {
                    for (Map.Entry<String, ?> entry : entries.entrySet()) {
                        Object v = entry.getValue();
                        String key = entry.getKey();

                        if (v instanceof Boolean)
                            prefEdit.putBoolean(key, (Boolean) v);
                        else if (v instanceof Float)
                            prefEdit.putFloat(key, (Float) v);
                        else if (v instanceof Integer)
                            prefEdit.putInt(key, (Integer) v);
                        else if (v instanceof Long)
                            prefEdit.putLong(key, (Long) v);
                        else if (v instanceof String)
                            prefEdit.putString(key, ((String) v));
                    }
                }
                prefEdit.apply();
                res = true;
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (input != null) {
                        input.close();
                        Toast.makeText(requireContext(), requireContext().getString(R.string.import_success), Toast.LENGTH_LONG).show();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
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

    public void onBackPressed() {

        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // масштабирование шрифтов
    static void adjustFontScale(Configuration configuration, Context context, String value) {
        configuration.fontScale = Float.parseFloat(value);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        assert wm != null;
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        context.getResources().updateConfiguration(configuration, metrics);
        Log.e("---", value);
    }
}
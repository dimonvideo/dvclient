package com.dimonvideo.client;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.util.GetToken;
import com.dimonvideo.client.util.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adjustFontScale(getResources().getConfiguration());
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
    }

    private void adjustFontScale(Configuration configuration) {
        SharedPreferences sharedPrefs = getDefaultSharedPreferences(this);
        configuration.fontScale = Float.parseFloat(sharedPrefs.getString("dvc_scale", "1.0f"));
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        assert wm != null;
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference dvc_theme = findPreference("dvc_theme");
            Preference dvc_password = findPreference("dvc_password");
            Preference dvc_login = findPreference("dvc_login");
            Preference dvc_pm = findPreference("dvc_pm");
            Preference dvc_clear_login = findPreference("dvc_clear_login");
            Preference dvc_register = findPreference("dvc_register");
            Preference dvc_scale = findPreference("dvc_scale");
            Preference dvc_export = findPreference("dvc_export");
            Preference dvc_import = findPreference("dvc_import");
            assert dvc_theme != null;
            dvc_theme.setOnPreferenceClickListener(
                    arg0 -> {
                        Toast.makeText(requireContext(), requireContext().getString(R.string.restart_app), Toast.LENGTH_LONG).show();

                        return true;
                    });
            assert dvc_scale != null;
            dvc_scale.setOnPreferenceChangeListener((preference, newValue) -> {
                requireActivity().recreate();
                Toast.makeText(getContext(), getString(R.string.please_reload), Toast.LENGTH_LONG).show();
                return true;
            });
            assert dvc_password != null;
            dvc_password.setOnPreferenceChangeListener((preference, newValue) -> {
                String listValue = (String) newValue;
                View view = getView();
                NetworkUtils.checkPassword(getContext(), view, listValue);
                return true;
            });
            assert dvc_login != null;
            dvc_login.setOnPreferenceChangeListener((preference, newValue) -> {
                String listValue = (String) newValue;
                View view = getView();
                NetworkUtils.checkLogin(getContext(), view, listValue);
                return true;
            });
            assert dvc_pm != null;

            dvc_pm.setOnPreferenceChangeListener((preference, newValue) -> {
                SharedPreferences sharedPrefs = getDefaultSharedPreferences(requireContext());
                final String password = sharedPrefs.getString("dvc_password", "null");
                View view = getView();
                NetworkUtils.checkPassword(getContext(), view, password);
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

            btnLogin.setOnClickListener(view -> {

                EditText userName = (EditText) dialog.findViewById(R.id.txtName);
                EditText userEmail = (EditText) dialog.findViewById(R.id.txtEmail);
                EditText userPassword = (EditText) dialog.findViewById(R.id.txtPwd);

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
                output = new ObjectOutputStream(new FileOutputStream(dst));
                SharedPreferences pref = getDefaultSharedPreferences(requireContext());
                output.writeObject(pref.getAll());

            } catch (FileNotFoundException e) {
                e.printStackTrace();
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
                input = new ObjectInputStream(new FileInputStream(src));
                SharedPreferences.Editor prefEdit = getDefaultSharedPreferences(requireContext()).edit();
                prefEdit.clear();
                Map<String, ?> entries = (Map<String, ?>) input.readObject();
                for (Map.Entry<String, ?> entry : entries.entrySet()) {
                    Object v = entry.getValue();
                    String key = entry.getKey();

                    if (v instanceof Boolean)
                        prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                    else if (v instanceof Float)
                        prefEdit.putFloat(key, ((Float) v).floatValue());
                    else if (v instanceof Integer)
                        prefEdit.putInt(key, ((Integer) v).intValue());
                    else if (v instanceof Long)
                        prefEdit.putLong(key, ((Long) v).longValue());
                    else if (v instanceof String)
                        prefEdit.putString(key, ((String) v));
                }
                prefEdit.apply();
                res = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
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
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onKeyLongPress(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return super.onKeyLongPress(keycode, event);
    }


}
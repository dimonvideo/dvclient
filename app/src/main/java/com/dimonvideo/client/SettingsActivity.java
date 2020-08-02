package com.dimonvideo.client;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.util.GetToken;
import com.dimonvideo.client.util.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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
            assert dvc_theme != null;
            dvc_theme.setOnPreferenceClickListener(
                    arg0 -> {
                        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme",false);
                        if (is_dark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
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
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                final String password = sharedPrefs.getString("dvc_password","null");
                View view = getView();
                NetworkUtils.checkPassword(getContext(), view, password);
                return true;
            });

            dvc_clear_login.setOnPreferenceClickListener(preference -> {
                alertForClearData();

                return true;
            });

            dvc_register.setOnPreferenceClickListener(preference -> {
                loadReg(getContext());

                return true;
            });

        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }

        private void alertForClearData(){

            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(getString(R.string.clear_alert_title));
            alert.setMessage(getString(R.string.clear_alert_message));

            alert.setCancelable(false);
            alert.setPositiveButton(R.string.ok, (dialog, which) -> {
                SharedPreferences.Editor editor;
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                editor = sharedPrefs.edit();
                editor.putInt("auth_state", 0);
                editor.remove("dvc_password");
                editor.remove("dvc_login");
                editor.remove("dvc_pm");
                editor.remove("auth_foto");
                editor.apply();
                getActivity().onBackPressed();
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
                            Log.e("tag", state);

                            if (state.equals("2")) {

                                SharedPreferences.Editor editor;
                                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                                editor = sharedPrefs.edit();
                                editor.putInt("auth_state", 1);
                                editor.putString("dvc_password", userPassword.getText().toString());
                                editor.putString("dvc_login", userName.getText().toString());
                                editor.apply();

                                GetToken.getToken(mContext);

                                Toast.makeText(mContext, mContext.getString(R.string.success_auth), Toast.LENGTH_LONG).show();
                                requireActivity().onBackPressed();
                                dialog.dismiss();

                            } else Toast.makeText(mContext, mContext.getString(R.string.unsuccess_auth), Toast.LENGTH_LONG).show();

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
package com.dimonvideo.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;

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
            assert dvc_theme != null;
            dvc_theme.setOnPreferenceClickListener(
                    arg0 -> {
                        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme",false);
                        if (is_dark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        return true;
                    });

            dvc_password.setOnPreferenceChangeListener((preference, newValue) -> {
                String listValue = (String) newValue;
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                final String login = sharedPrefs.getString("dvc_login","null");
                if (listValue == null || listValue.length() < 5 || listValue.length() > 71) {
                    Toast.makeText(getContext(), getActivity().getString(R.string.password_invalid), Toast.LENGTH_LONG).show();
                } else {

                    RequestQueue queue = Volley.newRequestQueue(getContext());
                    String pass = listValue;
                    try {
                        pass = URLEncoder.encode(listValue, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    String url = Config.CHECK_AUTH_URL + "&login_name=" + login + "&login_password=" + pass;
                    Log.d("tag", url);
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                            response -> {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    int state = jsonObject.getInt(Config.TAG_STATE);
                                    if (state > 0) Toast.makeText(getContext(), "state", Toast.LENGTH_LONG).show();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }, error -> {
                                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                                    Toast.makeText(getContext(), getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
                                } else if (error instanceof AuthFailureError) {
                                    Toast.makeText(getContext(), getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
                                } else if (error instanceof ServerError) {
                                    Toast.makeText(getContext(), getString(R.string.error_server), Toast.LENGTH_LONG).show();
                                } else if (error instanceof NetworkError) {
                                    Toast.makeText(getContext(), getString(R.string.error_network), Toast.LENGTH_LONG).show();
                                } else if (error instanceof ParseError) {
                                    Toast.makeText(getContext(), getString(R.string.error_server), Toast.LENGTH_LONG).show();
                                }
                            });
                    queue.add(stringRequest);

                }

                return true;
            });

        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

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

    public static final String md5(final String toEncrypt) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(toEncrypt.getBytes());
            final byte[] bytes = digest.digest();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02X", bytes[i]));
            }
            return sb.toString().toLowerCase();
        } catch (Exception exc) {
            return "";
        }
    }
}
package com.dimonvideo.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

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

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference dvc_theme = findPreference("dvc_theme");
            Preference dvc_pm = findPreference("dvc_pm");
            assert dvc_theme != null;
            dvc_theme.setOnPreferenceClickListener(
                    arg0 -> {
                        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme",false);
                        if (is_dark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        return true;
                    });
            dvc_pm.setOnPreferenceChangeListener((preference, newValue) -> {
                String listValue = (String) newValue;
                Log.d("tag", listValue);
                return true;
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
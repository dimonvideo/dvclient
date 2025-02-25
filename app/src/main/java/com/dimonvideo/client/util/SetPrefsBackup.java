package com.dimonvideo.client.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.databinding.ActivitySetPrefsBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SetPrefsBackup extends AppCompatActivity {

    public ActivitySetPrefsBinding binding;
    private boolean doubleBackToExitPressedOnce = false;
    private int count = 0;
    private TextView info;
    private String status, date_create, json;
    private JSONArray ResultArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySetPrefsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        info = binding.info;
        Button save = binding.save;
        Button restore = binding.restore;

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        // save all prefs
        ResultArray = null;
        try {
            JSONObject infoObj = new JSONObject();
            ResultArray = new JSONArray();
            Map<String, ?> allEntries = AppController.getSharedPreferences().getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                infoObj.put(entry.getKey(), entry.getValue().toString());
                if (entry.getValue().toString().equals("true") || entry.getValue().toString().equals("false")) {
                    Log.e(Config.TAG, "NOW BOOLEAN key is: " + entry.getKey() + " " + entry.getValue().toString());
                }else if (entry.getValue().toString().contains("[")){
                    Log.e(Config.TAG, "NOW LIST key is: " + entry.getKey() + " " + entry.getValue().toString());
                } else if (!entry.getValue().toString().isEmpty()) {
                    Log.e(Config.TAG, "NOW STRING key is: " + entry.getKey() + " " + entry.getValue().toString());
                }

                count++;
            }

            ResultArray.put(infoObj);

        } catch (Exception ignored) {

        }

        Log.d(Config.TAG, String.valueOf(ResultArray));

        runOnUiThread(() -> {
            info.setText(AppController.getInstance().userName("---"));
            info.append("\n" + getString(R.string.prefs_count));
            info.append(" " + count);


            ProgressHelper.showDialog(this, getString(R.string.please_wait));
            StringRequest stringRequest = getStringRequest(75);
            stringRequest.setShouldCache(false);
            AppController.getInstance().addToRequestQueue(stringRequest);
        });

        // onBackPressed
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finish();
                    return;
                }
                doubleBackToExitPressedOnce = true;
                Toast.makeText(SetPrefsBackup.this, getString(R.string.press_twice), Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
            }
        };

        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback);

        // сохранение настроек
        save.setOnClickListener(view -> {
            Log.e(Config.TAG, "BACKUP started");
            ProgressHelper.showDialog(this, getString(R.string.please_wait));
            StringRequest stringRequest = getStringRequest(74);
            stringRequest.setShouldCache(false);
            AppController.getInstance().addToRequestQueue(stringRequest);
        });

        // восстановление настроек
        restore.setOnClickListener(view -> {
            Log.e(Config.TAG, "RESTORE started");
            ProgressHelper.showDialog(this, getString(R.string.please_wait));
            StringRequest stringRequest = getStringRequest(76);
            stringRequest.setShouldCache(false);
            AppController.getInstance().addToRequestQueue(stringRequest);
        });


    }

    @NonNull
    private StringRequest getStringRequest(int option) {

        String url = Config.RESTORE_SETTINGS_URL + option + "&u=" + AppController.getInstance().isUserId();
        if (option == 74) url = Config.SAVE_SETTINGS_URL + option + "&u=" + AppController.getInstance().isUserId();

        Log.e(Config.TAG, "RESPONSE URL: " + url);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {

            if (response != null) {

                Log.e(Config.TAG, "RESPONSE BACKUP: " + response);
                status = "error";
                date_create = "error";
                json = "error";

                JSONObject jsonResponse = null;
                try {
                    jsonResponse = new JSONObject(response);
                    status = jsonResponse.getString("status");
                    json = jsonResponse.getString("json");
                    date_create = jsonResponse.getString("date");
                } catch (Exception ignored) {

                }

                Log.e(Config.TAG, "STATUS BACKUP: " + status);

                if (status.equals("ok")) {

                    runOnUiThread(() -> {

                        if (option == 76) {
                            info.append("\n" + getString(R.string.prefs_date_restore));
                            info.append(" " + date_create);
                        } else {
                            info.append("\n" + getString(R.string.prefs_date_copy));
                            info.append(" " + date_create);
                        }
                    });

                    if (option == 76) {
                        // восстановление настроек
                        try {

                            json = json.replace("\\\"","'");
                            JSONObject jsonObj = new JSONObject(json.substring(1,json.length()-1));
                            Iterator<String> iter = jsonObj.keys();
                            while (iter.hasNext()) {
                                String key = iter.next();
                                try {
                                    Object value = jsonObj.get(key);
                                    if (key.equalsIgnoreCase("last_version_code")) continue;
                                    if (key.equalsIgnoreCase("auth_state")) continue;
                                    if (key.equalsIgnoreCase("user_id")) continue;
                                    if (key.equalsIgnoreCase("user_group")) continue;
                                    if (key.equalsIgnoreCase("pm_unread")) continue;
                                    if (key.equalsIgnoreCase("dvc_login")) continue;
                                    if (key.equalsIgnoreCase("dvc_password")) continue;
                                    if (key.equalsIgnoreCase("auth_posts")) continue;
                                    if (key.equalsIgnoreCase("auth_reg")) continue;
                                    if (key.equalsIgnoreCase("auth_rep")) continue;
                                    if (key.equalsIgnoreCase("auth_rang")) continue;
                                    if (key.equalsIgnoreCase("auth_last")) continue;
                                    if (key.equalsIgnoreCase("auth_foto")) continue;
                                    if (key.equalsIgnoreCase("current_token")) continue;


                                    if (value.equals("true") || value.equals("false")) {
                                      //  Log.e(Config.TAG, "BACKUP BOOLEAN key is: " + key + " " + value);
                                         AppController.putSharedPreferences().putBoolean(key, Boolean.parseBoolean(value.toString())).apply();
                                    }else if (value.toString().contains("[")){
                                        String[] clear = value.toString().replaceAll("[^A-Za-z0-9,]","").split(",");
                                        Log.e(Config.TAG, "BACKUP LIST key is: " + key + " " + Arrays.toString(clear));
                                        AppController.putSharedPreferences().putStringSet(key, Set.of(clear)).apply();
                                    } else if (value != "") {
                                     //   Log.e(Config.TAG, "BACKUP STRING key is: " + key + " " + value);
                                      AppController.putSharedPreferences().putString(key, (String) value).apply();
                                    }

                                    GetToken.getToken(this);

                                } catch (JSONException ignored) {

                                }
                            }

                            info.append("\n" + getString(R.string.restart_app));
                            finishAffinity();
                        } catch (Exception ignored) {

                        }
                    }

                } else {

                    runOnUiThread(() -> {
                        info.setText(getString(R.string.error_network));
                    });


                }

                if (ProgressHelper.isDialogVisible())
                    ProgressHelper.dismissDialog();
            }
        }, error -> {

            if (ProgressHelper.isDialogVisible())
                ProgressHelper.dismissDialog();
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> postMap = new HashMap<>();
                postMap.put("json", String.valueOf(ResultArray));
                return postMap;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        return stringRequest;
    }


    @Override
    public void onDestroy() {
        try {
            if (ProgressHelper.isDialogVisible()) ProgressHelper.dismissDialog();
        } catch (Exception ignored) {

        }

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
    public boolean onKeyLongPress(int keycode, KeyEvent event) {
        return super.onKeyLongPress(keycode, event);
    }

    @Override
    protected void onStop() {
        try {
            if (ProgressHelper.isDialogVisible()) ProgressHelper.dismissDialog();
        } catch (Exception ignored) {

        }
        super.onStop();
    }
}

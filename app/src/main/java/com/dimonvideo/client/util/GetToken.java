package com.dimonvideo.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class GetToken {

    public static void getToken(Context context){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String login = sharedPrefs.getString("dvc_login","null");
        String pass = sharedPrefs.getString("dvc_password","null");
        try {
            pass = URLEncoder.encode(pass, "utf-8");
            login = URLEncoder.encode(login, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {


            String finalLogin = login;
            String finalPass = pass;
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w("TAG", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        Log.w("TAG", "Fetching FCM registration token " + token);
            // Get new Instance ID token

                SharedPreferences.Editor editor;
                editor = sharedPrefs.edit();
                editor.putString("current_token", token);
                editor.apply();


            String url = Config.CHECK_AUTH_URL + "&login_name=" + finalLogin + "&login_password=" + finalPass;
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {

                Log.e("Get token Result", "curr token: " + token + response);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String state = jsonObject.getString("state");
                    Log.e("tag", state);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, Throwable::printStackTrace) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> postMap = new HashMap<>();
                    postMap.put("token", token);
                    return postMap;
                }
            };

            Volley.newRequestQueue(context).add(stringRequest);
                    });

        } catch (Exception ignored) {
        }


    }
}

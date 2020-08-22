package com.dimonvideo.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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


            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:771109015774:android:dfe7f6831106e74781324f")
                    .setProjectId("dv-offline")
                    .build();
            FirebaseApp.initializeApp(context.getApplicationContext(), options, "DVClient");
            FirebaseInstanceId.getInstance().getInstanceId();
            String finalPass = pass;
            String finalLogin = login;


            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                String token = instanceIdResult.getToken();
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

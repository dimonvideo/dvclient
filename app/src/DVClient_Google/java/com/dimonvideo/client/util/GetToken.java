package com.dimonvideo.client.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
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
        new Handler(Looper.getMainLooper()).post(() -> {

            String is_name = AppController.getInstance().userName("null");
            String password = AppController.getInstance().userPassword();
            try {
                password = URLEncoder.encode(password, "utf-8");
                is_name = URLEncoder.encode(is_name, "utf-8");
            } catch (UnsupportedEncodingException ignored) {

            }

            try {

                String finalIs_name = is_name;
                String finalPassword = password;
                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                return;
                            }
                            String token = task.getResult();
                            Log.w("---", "Fetching FCM registration token " + token);

                            AppController.getInstance().putToken(token);

                            String url = Config.CHECK_AUTH_URL + "&login_name=" + finalIs_name + "&login_password=" + finalPassword;
                            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    String state = jsonObject.getString("state");
                                    Log.e("---", state);

                                } catch (JSONException ignored) {

                                }
                            }, Throwable::printStackTrace) {

                                @Override
                                protected Map<String, String> getParams() {
                                    Map<String, String> postMap = new HashMap<>();
                                    postMap.put("token", token);
                                    return postMap;
                                }
                            };
                            stringRequest.setShouldCache(false);
                            AppController.getInstance().addToRequestQueue(stringRequest);
                        });

            } catch (Exception ignored) {
            }
            Log.e("---", "token");

        });
    }
}

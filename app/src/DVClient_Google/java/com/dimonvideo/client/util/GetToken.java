package com.dimonvideo.client.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.dimonvideo.client.Config;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GetToken {

    public static void getToken(Context context){

        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("FCM", "Google Play Services unavailable");
            // Показать пользователю диалог для обновления Play Services
            Objects.requireNonNull(GoogleApiAvailability.getInstance().getErrorDialog((Activity) context, resultCode, 1)).show();
        } else {


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
                                    Log.w("FCM", "Fetching FCM token failed", task.getException());
                                    Toast.makeText(context, "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                                    return;
                                }
                                String token = task.getResult();
                                Log.w("---", "Fetching FCM registration token " + token);

                                AppController.getInstance().putToken(token);

                                StringRequest stringRequest = getStringRequest(finalIs_name, finalPassword, token);
                                AppController.getInstance().addToRequestQueue(stringRequest);
                            });

                } catch (Exception e) {
                    Log.e("---", "token"+e);

                }
                Log.e("---", "token");

            });
        }
    }

    @NonNull
    private static StringRequest getStringRequest(String finalIs_name, String finalPassword, String token) {
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
        return stringRequest;
    }
}

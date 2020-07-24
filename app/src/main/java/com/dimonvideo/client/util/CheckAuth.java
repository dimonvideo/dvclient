package com.dimonvideo.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class CheckAuth {


    public static void checkPassword(Context context, View view, String password){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String login = sharedPrefs.getString("dvc_login","null");
        if (password == null || password.length() < 5 || password.length() > 71) {
            Snackbar.make(view, context.getString(R.string.password_invalid), Snackbar.LENGTH_LONG).show();
        } else {

            RequestQueue queue = Volley.newRequestQueue(context);
            String pass = password;
            try {
                pass = URLEncoder.encode(password, "utf-8");
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
                            if (state > 0) Snackbar.make(view, context.getString(R.string.success_auth), Snackbar.LENGTH_LONG).show(); else
                                Snackbar.make(view, context.getString(R.string.unsuccess_auth), Snackbar.LENGTH_LONG).show();

                            SharedPreferences.Editor editor;
                            editor = sharedPrefs.edit();
                            editor.putInt("auth_state", state);
                            editor.apply();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }, error -> {
                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    Toast.makeText(context, context.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
                } else if (error instanceof AuthFailureError) {
                    Toast.makeText(context, context.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
                } else if (error instanceof ServerError) {
                    Toast.makeText(context, context.getString(R.string.error_server), Toast.LENGTH_LONG).show();
                } else if (error instanceof NetworkError) {
                    Toast.makeText(context, context.getString(R.string.error_network), Toast.LENGTH_LONG).show();
                } else if (error instanceof ParseError) {
                    Toast.makeText(context, context.getString(R.string.error_server), Toast.LENGTH_LONG).show();
                }
            });
            queue.add(stringRequest);

        }

    }

    public static void checkLogin(Context context, View view, String login){

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = sharedPrefs.getString("dvc_password","null");
        if (login == null || login.length() < 2 || login.length() > 71) {
            Snackbar.make(view, context.getString(R.string.login_invalid), Snackbar.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {


                RequestQueue queue = Volley.newRequestQueue(context);
                String pass = password;
                try {
                    pass = URLEncoder.encode(password, "utf-8");
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
                                if (state > 0)
                                    Snackbar.make(view, context.getString(R.string.success_auth), Snackbar.LENGTH_LONG).show();
                                else
                                    Snackbar.make(view, context.getString(R.string.unsuccess_auth), Snackbar.LENGTH_LONG).show();

                                SharedPreferences.Editor editor;
                                editor = sharedPrefs.edit();
                                editor.putInt("auth_state", state);
                                editor.apply();


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }, error -> {
                    if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                        Toast.makeText(context, context.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
                    } else if (error instanceof AuthFailureError) {
                        Toast.makeText(context, context.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
                    } else if (error instanceof ServerError) {
                        Toast.makeText(context, context.getString(R.string.error_server), Toast.LENGTH_LONG).show();
                    } else if (error instanceof NetworkError) {
                        Toast.makeText(context, context.getString(R.string.error_network), Toast.LENGTH_LONG).show();
                    } else if (error instanceof ParseError) {
                        Toast.makeText(context, context.getString(R.string.error_server), Toast.LENGTH_LONG).show();
                    }
                });
                queue.add(stringRequest);
            }

        }

    }

}

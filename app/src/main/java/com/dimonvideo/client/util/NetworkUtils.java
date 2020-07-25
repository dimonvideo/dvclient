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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class NetworkUtils {


    public static void checkPassword(Context context, View view, String password) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String login = sharedPrefs.getString("dvc_login", "null");
        if (password == null || password.length() < 5 || password.length() > 71) {
            Snackbar.make(view, context.getString(R.string.password_invalid), Snackbar.LENGTH_LONG).show();
        } else {

            RequestQueue queue = Volley.newRequestQueue(context);
            String pass = password;
            try {
                pass = URLEncoder.encode(password, "utf-8");
                login = URLEncoder.encode(login, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String url = Config.CHECK_AUTH_URL + "&login_name=" + login + "&login_password=" + pass;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    response -> {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            int state = jsonObject.getInt(Config.TAG_STATE);
                            if (state > 0) {
                                Snackbar.make(view, context.getString(R.string.success_auth), Snackbar.LENGTH_LONG).show();
                            } else
                                Snackbar.make(view, context.getString(R.string.unsuccess_auth), Snackbar.LENGTH_LONG).show();

                            SharedPreferences.Editor editor;
                            editor = sharedPrefs.edit();
                            editor.putInt("auth_state", state);
                            editor.apply();
                            GetToken.getToken(context);

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

    public static void checkLogin(Context context, View view, String login) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = sharedPrefs.getString("dvc_password", "null");
        if (login == null || login.length() < 2 || login.length() > 71) {
            Snackbar.make(view, context.getString(R.string.login_invalid), Snackbar.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {

                RequestQueue queue = Volley.newRequestQueue(context);
                String pass = password;
                try {
                    pass = URLEncoder.encode(password, "utf-8");
                    login = URLEncoder.encode(login, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                String url = Config.CHECK_AUTH_URL + "&login_name=" + login + "&login_password=" + pass;
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
                                GetToken.getToken(context);


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

    public static void deletePm(Context context, int pm_id) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = sharedPrefs.getString("dvc_password", "null");
        String login = sharedPrefs.getString("dvc_login", "null");
        if (login == null || login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {

                RequestQueue queue = Volley.newRequestQueue(context);
                String pass = password;
                try {
                    pass = URLEncoder.encode(password, "utf-8");
                    login = URLEncoder.encode(login, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                String url = Config.PM_URL + 1 + "&login_name=" + login + "&login_password=" + pass + "&pm_id=" + pm_id + "&pm=10";
                Log.e("net", url);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        response -> {

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

    public static void readPm(Context context, int pm_id) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = sharedPrefs.getString("dvc_password", "null");
        String login = sharedPrefs.getString("dvc_login", "null");
        if (login == null || login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {

                RequestQueue queue = Volley.newRequestQueue(context);
                String pass = password;
                try {
                    pass = URLEncoder.encode(password, "utf-8");
                    login = URLEncoder.encode(login, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                String url = Config.PM_URL + 1 + "&login_name=" + login + "&login_password=" + pass + "&pm_id=" + pm_id + "&pm=11";
                Log.e("net", url);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        response -> {

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

    public static void sendPm(Context context, int pm_id, String text, int delete) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = sharedPrefs.getString("dvc_password", "null");
        String login = sharedPrefs.getString("dvc_login", "null");
        String pass = password;
        if (login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
        } else {
            if ((text != null) && (text.length() > 1) ){
                try {
                    pass = URLEncoder.encode(password, "utf-8");
                    login = URLEncoder.encode(login, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String url = Config.PM_URL + 1 + "&login_name=" + login + "&login_password=" + pass + "&pm_id=" + pm_id + "&pm=12&delete="+delete;
                StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {


                            Toast.makeText(context, context.getString(R.string.success_auth), Toast.LENGTH_LONG).show();

                }, Throwable::printStackTrace) {

                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> postMap = new HashMap<>();
                        postMap.put("pm_text", text);
                        return postMap;
                    }
                };

                Volley.newRequestQueue(context).add(stringRequest);

            }
        }
    }
}

package com.dimonvideo.client.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class NetworkUtils {
    private static final SharedPreferences sharedPrefs = AppController.getInstance().getSharedPreferences();

    public static void checkPassword(Context context, String password) {
        View view = MainActivity.binding.getRoot();
        String login = sharedPrefs.getString("dvc_login", "null");
        final int auth_state = sharedPrefs.getInt("auth_state", 0);
        String current_token = sharedPrefs.getString("current_token", "null");
        if (password == null || password.length() < 5 || password.length() > 71) {
            Snackbar.make(view, context.getString(R.string.password_invalid), Snackbar.LENGTH_LONG).show();
        } else {

            RequestQueue queue = AppController.getInstance().getRequestQueue();
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

                            String image = "0", status = "0", lastdate = "0", token = "0", rep = "0", reg = "0", rat = "0", posts = "0";
                            int pm_unread = 0, uid = 0;

                            if (state > 0) {
                                pm_unread = jsonObject.getInt(Config.TAG_PM_UNREAD);
                                image = jsonObject.getString(Config.TAG_IMAGE_URL);
                                status = jsonObject.getString(Config.TAG_HEADERS);
                                lastdate = jsonObject.getString(Config.TAG_TIME);
                                token = jsonObject.getString(Config.TAG_TOKEN);
                                rep = jsonObject.getString(Config.TAG_REP);
                                reg = jsonObject.getString(Config.TAG_REG);
                                rat = jsonObject.getString(Config.TAG_COMMENTS);
                                posts = jsonObject.getString(Config.TAG_COUNT);
                                uid = jsonObject.getInt(Config.TAG_UID);

                                try {
                                    if (auth_state == 0) Snackbar.make(view, context.getString(R.string.success_auth), Snackbar.LENGTH_LONG).show();
                                    Intent local = new Intent();
                                    local.setAction(Config.INTENT_AUTH);
                                    local.putExtra("pm_unread", String.valueOf(pm_unread));
                                    context.sendBroadcast(local);
                                } catch (Throwable ignored) {
                                }
                            } else {
                                try {
                                    Snackbar.make(view, context.getString(R.string.unsuccess_auth), Snackbar.LENGTH_LONG).show();
                                } catch (Throwable ignored) {
                                }
                            }

                            SharedPreferences.Editor editor;
                            editor = sharedPrefs.edit();
                            editor.putInt("auth_state", state);
                            if (state > 0) {

                                editor.putString("auth_foto", image);
                                editor.putString("auth_rang", status);
                                editor.putString("auth_last", lastdate);
                                editor.putString("auth_rep", rep);
                                editor.putString("auth_reg", reg);
                                editor.putString("auth_rat", rat);
                                editor.putString("auth_posts", posts);
                                editor.putInt("pm_unread", pm_unread);
                                editor.putInt("user_id", uid);
                            }
                            editor.apply();

                            if ((!token.equals(current_token)) && (state > 0)) GetToken.getToken(context);

                            // Log.e("auth", response);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }, error -> showErrorToast(context, error)
            );
            queue.add(stringRequest);

        }

    }

    public static void checkLogin(Context context, String login) {
        View view = MainActivity.binding.getRoot();
        final String password = sharedPrefs.getString("dvc_password", "null");
        if (login == null || login.length() < 2 || login.length() > 71) {
            Snackbar.make(view, context.getString(R.string.login_invalid), Snackbar.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {

                RequestQueue queue = AppController.getInstance().getRequestQueue();
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
                                    Intent local = new Intent();
                                    local.setAction(Config.INTENT_AUTH);
                                    context.sendBroadcast(local);
                                }
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
                        }, error -> showErrorToast(context, error)
                );

                queue.add(stringRequest);
            }

        }

    }
    static void showErrorToast(Context context, VolleyError error) {
        @StringRes int errorTextRes = getErrorTextResId(error);

        if (errorTextRes != 0) {
            Toast.makeText(context, context.getString(errorTextRes), Toast.LENGTH_LONG).show();
        }
    }

    @StringRes
    private static int getErrorTextResId(VolleyError error) {
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            return R.string.error_network_timeout;
        } else if (error instanceof AuthFailureError) {
            return R.string.unsuccess_auth;
        } else if (error instanceof ServerError) {
            return R.string.error_server;
        } else if (error instanceof NetworkError) {
            return R.string.error_network;
        } else if (error instanceof ParseError) {
            return R.string.error_server;
        }

        return 0;
    }

    public static void deletePm(Context context, int pm_id, int delete) {

        final String password = sharedPrefs.getString("dvc_password", "null");
        String login = sharedPrefs.getString("dvc_login", "null");
        final int pm_unread = sharedPrefs.getInt("pm_unread", 0);
        if (login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {

                RequestQueue queue = AppController.getInstance().getRequestQueue();
                String pass = password;
                try {
                    pass = URLEncoder.encode(password, "utf-8");
                    login = URLEncoder.encode(login, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                String url = Config.PM_URL + 1 + "&login_name=" + login + "&login_password=" + pass + "&pm_id=" + pm_id + "&pm=10&delete=" + delete;
                Log.e(Config.TAG, url);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        response -> {

                            if (delete == 0) {
                                SharedPreferences.Editor editor;
                                editor = sharedPrefs.edit();
                                if (pm_unread > 1) editor.putInt("pm_unread", pm_unread - 1);
                                else editor.putInt("pm_unread", 0);
                                editor.apply();
                                String count = "0";
                                if (pm_unread > 1) count = String.valueOf(pm_unread - 1);
                                if (Integer.parseInt(count) < 1) count = "0";
                                Intent local = new Intent();
                                local.setAction(Config.INTENT_DELETE_PM);
                                local.putExtra("pm_unread", count);
                                local.putExtra("action", "deletePm");
                                context.sendBroadcast(local);
                            }
                        }, error -> showErrorToast(context, error)
                );

                queue.add(stringRequest);
            }

        }

    }

    public static void readPm(Context context, int pm_id) {

        final String password = sharedPrefs.getString("dvc_password", "null");
        String login = sharedPrefs.getString("dvc_login", "null");
        final int pm_unread = sharedPrefs.getInt("pm_unread", 0);
        if (login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {

                RequestQueue queue = AppController.getInstance().getRequestQueue();
                String pass = password;
                try {
                    pass = URLEncoder.encode(password, "utf-8");
                    login = URLEncoder.encode(login, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                String url = Config.PM_URL + 1 + "&login_name=" + login + "&login_password=" + pass + "&pm_id=" + pm_id + "&pm=11";
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        response -> {
                            String count = "0";
                            if (pm_unread > 1) count = String.valueOf(pm_unread - 1);
                            if (Integer.parseInt(count) < 1) count = "0";
                            Intent local = new Intent();
                            local.setAction(Config.INTENT_READ_PM);
                            local.putExtra("pm_unread", count);
                            local.putExtra("action", "readPm");
                            context.sendBroadcast(local);
                        }, error -> showErrorToast(context, error)
                );

                queue.add(stringRequest);
            }

        }

    }

    public static void sendPm(Context context, int pm_id, String text, int delete, String razdel, int uid) {

        final String password = sharedPrefs.getString("dvc_password", "null");
        String login = sharedPrefs.getString("dvc_login", "null");
        String pass = password;
        if (login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
        } else {
            if ((text != null) && (text.length() > 1)) {
                try {
                    pass = URLEncoder.encode(password, "utf-8");
                    login = URLEncoder.encode(login, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                RequestQueue queue = AppController.getInstance().getRequestQueue();

                String url = Config.PM_URL + 1 + "&login_name=" + login + "&login_password=" + pass + "&pm_id=" + pm_id + "&pm=12&delete=" + delete + "&razdel=" + razdel + "&uid=" + uid;
                StringRequest stringRequest = new StringRequest(Request.Method.POST, url, response -> {

                    Toast.makeText(context, context.getString(R.string.success_send_pm), Toast.LENGTH_LONG).show();
                    GetToken.getToken(context);
                }, Throwable::printStackTrace) {

                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> postMap = new HashMap<>();
                        postMap.put("pm_text", text);
                        return postMap;
                    }
                };

                queue.add(stringRequest);

            }
        }

        GetToken.getToken(context);
    }

    public static void loadAvatar(Context context, Toolbar toolbar){

        final String image_url = sharedPrefs.getString("auth_foto", Config.BASE_URL + "/images/noavatar.png");
        final int auth_state = sharedPrefs.getInt("auth_state", 0);
        if (auth_state > 0) {
            Glide.with(context)
                    .asDrawable()
                    .load(image_url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerInside()
                    .apply(RequestOptions.circleCropTransform())
                    .into(new CustomTarget<Drawable>() {

                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable @org.jetbrains.annotations.Nullable com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                            try {
                                toolbar.setNavigationIcon(resource);
                            } catch (Throwable ignored) {
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
    }
}

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

    public static void checkPassword(Context context, String password) {
        View view = MainActivity.binding.getRoot();
        String login = AppController.getInstance().userName("null");
        final int auth_state = AppController.getInstance().isAuth();
        String current_token = AppController.getInstance().isToken();
        if (password == null || password.length() < 5 || password.length() > 71) {
            Snackbar.make(view, context.getString(R.string.password_invalid), Snackbar.LENGTH_LONG).show();
        } else {

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

                            AppController.getInstance().putAuthState(state);

                            if (state > 0) {

                                AppController.getInstance().putImage(image);
                                AppController.getInstance().putRang(status);
                                AppController.getInstance().putLastDate(lastdate);
                                AppController.getInstance().putReputation(rep);
                                AppController.getInstance().putRegDate(reg);
                                AppController.getInstance().putRating(rat);
                                AppController.getInstance().putPosts(posts);
                                AppController.getInstance().putUserId(uid);
                                AppController.getInstance().putPmUnread(pm_unread);
                            }

                            if ((!token.equals(current_token)) && (state > 0)) GetToken.getToken(context);

                            // Log.e("auth", response);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }, error -> showErrorToast(context, error)
            );
            AppController.getInstance().addToRequestQueue(stringRequest);

        }

    }

    public static void checkLogin(Context context, String login) {
        View view = MainActivity.binding.getRoot();
        final String password = AppController.getInstance().userPassword();
        if (login == null || login.length() < 2 || login.length() > 71) {
            Snackbar.make(view, context.getString(R.string.login_invalid), Snackbar.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {

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

                                AppController.getInstance().putAuthState(state);
                                GetToken.getToken(context);


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }, error -> showErrorToast(context, error)
                );

                AppController.getInstance().addToRequestQueue(stringRequest);
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

        final String password = AppController.getInstance().userPassword();
        String login = AppController.getInstance().userName("null");
        final int pm_unread = AppController.getInstance().isPmUnread();
        if (login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {

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
                                if (pm_unread > 1) AppController.getInstance().putPmUnread(pm_unread - 1); else AppController.getInstance().putPmUnread(0);
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

                AppController.getInstance().addToRequestQueue(stringRequest);
            }

        }

    }

    public static void readPm(Context context, int pm_id) {

        final String password = AppController.getInstance().userPassword();
        String login = AppController.getInstance().userName("null");
        final int pm_unread = AppController.getInstance().isPmUnread();
        if (login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
        } else {
            if (password.length() > 5) {

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

                AppController.getInstance().addToRequestQueue(stringRequest);
            }

        }

    }

    public static void sendPm(Context context, int pm_id, String text, int delete, String razdel, int uid) {

        final String password = AppController.getInstance().userPassword();
        String login = AppController.getInstance().userName("null");
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

                AppController.getInstance().addToRequestQueue(stringRequest);

            }
        }

        GetToken.getToken(context);
    }

    public static void loadAvatar(Context context, Toolbar toolbar){

        final String image_url = AppController.getInstance().imageUrl();
        final int auth_state = AppController.getInstance().isAuth();
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

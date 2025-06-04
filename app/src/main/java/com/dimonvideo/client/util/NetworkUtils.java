/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.ui.main.MainFragmentOpros;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NetworkUtils {

    private static final String UTF_8 = "utf-8";

    // Получение и кодирование данных авторизации
    private static boolean getEncodedAuthData(AppController appController, String[] authData, Context context) {
        String login = appController.userName("null");
        String password = appController.userPassword();

        if (login.length() < 2 || login.length() > 71) {
            Toast.makeText(context, context.getString(R.string.login_invalid), Toast.LENGTH_LONG).show();
            return true;
        }
        if (password.length() < 5) {
            Toast.makeText(context, context.getString(R.string.password_invalid), Toast.LENGTH_LONG).show();
            return true;
        }

        try {
            authData[0] = URLEncoder.encode(login, UTF_8);
            authData[1] = URLEncoder.encode(password, UTF_8);
            return false;
        } catch (UnsupportedEncodingException e) {
            Log.e(Config.TAG, "Encoding error: " + e.getMessage());
            return true;
        }
    }

    public static void checkPassword(Context context, String password, String razdel) {
        AppController appController = AppController.getInstance();
        View view = MainActivity.binding.getRoot();

        if (password == null || password.length() < 5 || password.length() > 71) {
            Snackbar.make(view, context.getString(R.string.password_invalid), Snackbar.LENGTH_LONG).show();
            return;
        }

        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context)) return;

        String url = Config.CHECK_AUTH_URL + "&login_name=" + authData[0] + "&login_password=" + authData[1];
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {

            Log.e(Config.TAG, "Check pass: "+response);

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int state = jsonObject.getInt(Config.TAG_STATE);
                        appController.putAuthState(state);

                        if (state > 0) {
                            appController.putImage(jsonObject.getString(Config.TAG_IMAGE_URL));
                            appController.putRang(jsonObject.getString(Config.TAG_HEADERS));
                            appController.putLastDate(jsonObject.getString(Config.TAG_TIME));
                            appController.putReputation(jsonObject.getString(Config.TAG_REP));
                            appController.putRegDate(jsonObject.getString(Config.TAG_REG));
                            appController.putRating(jsonObject.getString(Config.TAG_COMMENTS));
                            appController.putPosts(jsonObject.getString(Config.TAG_COUNT));
                            appController.putUserId(jsonObject.getInt(Config.TAG_UID));
                            appController.putUserGroup(jsonObject.getInt(Config.TAG_USER_GROUP));
                            appController.putPmUnread(jsonObject.getInt(Config.TAG_PM_UNREAD));

                            if (appController.isAuth() == 0) {
                                Snackbar.make(view, context.getString(R.string.success_auth), Snackbar.LENGTH_LONG).show();
                            }
                            EventBus.getDefault().post(new MessageEvent(razdel, null, null, String.valueOf(jsonObject.getInt(Config.TAG_PM_UNREAD)), null, null));

                            String token = jsonObject.getString(Config.TAG_TOKEN);
                            if (!token.equals(appController.isToken())) {
                                GetToken.getToken(context);
                            }
                        } else {
                            Snackbar.make(view, context.getString(R.string.unsuccess_auth), Snackbar.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                    }
                }, error -> showErrorToast(context, error));

        stringRequest.setShouldCache(false);
        appController.addToRequestQueue(stringRequest);
    }

    public static void checkLogin(Context context, String login) {
        AppController appController = AppController.getInstance();
        View view = MainActivity.binding.getRoot();

        if (login == null || login.length() < 2 || login.length() > 71) {
            Snackbar.make(view, context.getString(R.string.login_invalid), Snackbar.LENGTH_LONG).show();
            return;
        }

        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context)) return;

        String url = Config.CHECK_AUTH_URL + "&login_name=" + login + "&login_password=" + authData[1];
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.e(Config.TAG, "Check login: "+response);

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int state = jsonObject.getInt(Config.TAG_STATE);
                        appController.putAuthState(state);

                        if (state > 0) {
                            Snackbar.make(view, context.getString(R.string.success_auth), Snackbar.LENGTH_LONG).show();
                            context.sendBroadcast(new Intent(Config.INTENT_AUTH));
                            GetToken.getToken(context);
                        } else {
                            Snackbar.make(view, context.getString(R.string.unsuccess_auth), Snackbar.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                    }
                }, error -> showErrorToast(context, error));

        stringRequest.setShouldCache(false);
        appController.addToRequestQueue(stringRequest);
    }

    public static void deletePm(Context context, int pm_id, int delete) {
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context)) return;

        String url = Config.PM_URL + 1 + "&login_name=" + authData[0] + "&login_password=" + authData[1] + "&pm_id=" + pm_id + "&pm=10&delete=" + delete;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (delete == 0) {
                        int pm_unread = appController.isPmUnread();
                        String count = pm_unread > 1 ? String.valueOf(pm_unread - 1) : "0";
                        appController.putPmUnread(Integer.parseInt(count));
                        EventBus.getDefault().post(new MessageEvent(null, null, null, count, "deletePm", null));
                    }
                }, error -> showErrorToast(context, error));

        stringRequest.setShouldCache(false);
        appController.addToRequestQueue(stringRequest);
    }

    public static void readPm(Context context, int pm_id) {
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context)) return;

        String url = Config.PM_URL + 1 + "&login_name=" + authData[0] + "&login_password=" + authData[1] + "&pm_id=" + pm_id + "&pm=11";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    int pm_unread = appController.isPmUnread();
                    String count = pm_unread > 1 ? String.valueOf(pm_unread - 1) : "0";
                    appController.putPmUnread(Integer.parseInt(count));
                    EventBus.getDefault().post(new MessageEvent(null, null, null, count, "readPm", null));
                }, error -> showErrorToast(context, error));

        stringRequest.setShouldCache(false);
        appController.addToRequestQueue(stringRequest);
    }

    public static void sendPm(Context context, int pm_id, String text, int delete, String razdel, int uid) {
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, context)) return;

        if (text == null || text.length() <= 1) {
            Toast.makeText(context, context.getString(R.string.error_network), Toast.LENGTH_LONG).show();
            return;
        }

        String url = Config.PM_URL + 1 + "&login_name=" + authData[0] + "&login_password=" + authData[1] + "&pm_id=" + pm_id + "&pm=12&delete=" + delete + "&razdel=" + razdel + "&uid=" + uid;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(context, context.getString(R.string.success_send_pm), Toast.LENGTH_LONG).show();
                    GetToken.getToken(context);
                }, error -> showErrorToast(context, error)) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> postMap = new HashMap<>();
                postMap.put("pm_text", text);
                return postMap;
            }
        };

        stringRequest.setShouldCache(false);
        appController.addToRequestQueue(stringRequest);
        GetToken.getToken(context);
    }

    public static void loadAvatar(Context context, Toolbar toolbar) {
        AppController appController = AppController.getInstance();
        if (appController.isAuth() <= 0) return;

        String image_url = appController.imageUrl();
        Glide.with(context)
                .asDrawable()
                .load(image_url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerInside()
                .apply(RequestOptions.circleCropTransform())
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                        toolbar.setNavigationIcon(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    public static byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static String uploadBitmap(Bitmap bitmap, Context context, String razdel) {
        AppController controller = AppController.getInstance();
        String user_name = controller.userName("dvclient");

        ProgressHelper.showDialog(context, context.getString(R.string.please_wait));
        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, Config.UPLOAD_URL,
                response -> {
                    ProgressHelper.dismissDialog();
                    try {
                        JSONObject obj = new JSONObject(new String(response.data));
                        String msg = obj.getString(Config.TAG_LINK);
                        String err = obj.getString("error");
                        EventBus.getDefault().postSticky(new MessageEvent(razdel, null, msg, null, null, bitmap));
                        Toast.makeText(context, R.string.success_image, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                    }
                }, error -> {
            ProgressHelper.dismissDialog();
            showErrorToast(context, error);
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", user_name);
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                long imagename = System.currentTimeMillis();
                params.put("pic", new DataPart(imagename + ".png", getFileDataFromDrawable(bitmap)));
                return params;
            }
        };

        volleyMultipartRequest.setShouldCache(false);
        volleyMultipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                (int) TimeUnit.SECONDS.toMillis(10),
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        controller.addToRequestQueue(volleyMultipartRequest);
        return user_name;
    }

    public static void getOprosTitle(TextView opros, Context context) {
        AppController appController = AppController.getInstance();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Config.VOTE_URL,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        Feed jsonFeed = new Feed();
                        try {
                            JSONObject json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            opros.setText(jsonFeed.getTitle());
                            opros.setOnClickListener(v -> {
                                MainFragmentOpros fragment = new MainFragmentOpros();
                                Bundle bundle = new Bundle();
                                bundle.putString(Config.TAG_TITLE, jsonFeed.getTitle());
                                fragment.setArguments(bundle);
                                if (context instanceof AppCompatActivity) {
                                    fragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "MainFragmentOpros");
                                }
                            });
                        } catch (JSONException e) {
                            Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                        }
                    }
                }, error -> showErrorToast(context, error));

        jsonArrayRequest.setShouldCache(false);
        appController.addToRequestQueue(jsonArrayRequest);
    }

    public static void getOdob(String razdel, int lid) {
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, null)) return;

        View view = MainActivity.binding.getRoot();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Config.APPROVE_URL + razdel + "&u=" + authData[0] + "&p=" + authData[1] + "&lid=" + lid,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        Feed jsonFeed = new Feed();
                        try {
                            JSONObject json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            Snackbar.make(view, jsonFeed.getTitle(), Snackbar.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                        }
                    }
                }, error -> showErrorToast(null, error));

        jsonArrayRequest.setShouldCache(false);
        appController.addToRequestQueue(jsonArrayRequest);
    }

    public static void putToNews(String razdel, int lid) {
        AppController appController = AppController.getInstance();
        String[] authData = new String[2];
        if (getEncodedAuthData(appController, authData, null)) return;

        View view = MainActivity.binding.getRoot();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Config.PUTTONEWS_URL + razdel + "&u=" + authData[0] + "&p=" + authData[1] + "&lid=" + lid,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        Feed jsonFeed = new Feed();
                        try {
                            JSONObject json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            Snackbar.make(view, jsonFeed.getTitle(), Snackbar.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Log.e(Config.TAG, "JSON parsing error: " + e.getMessage());
                        }
                    }
                }, error -> showErrorToast(null, error));

        jsonArrayRequest.setShouldCache(false);
        appController.addToRequestQueue(jsonArrayRequest);
    }

    private static void showErrorToast(Context context, VolleyError error) {
        @StringRes int errorTextRes = getErrorTextResId(error);
        if (errorTextRes != 0 && context != null) {
            Toast.makeText(context, context.getString(errorTextRes), Toast.LENGTH_LONG).show();
        }
    }

    @StringRes
    private static int getErrorTextResId(VolleyError error) {
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            return R.string.error_network_timeout;
        } else if (error instanceof AuthFailureError) {
            return R.string.unsuccess_auth;
        } else if (error instanceof ServerError || error instanceof ParseError) {
            return R.string.error_server;
        } else if (error instanceof NetworkError) {
            return R.string.error_network;
        }
        return 0;
    }
}
package com.dimonvideo.client.util;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
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
import com.bumptech.glide.Glide;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.potyvideo.library.AndExoPlayerView;
import com.potyvideo.library.globalEnums.EnumAspectRatio;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;

public class ButtonsActions {

    // загрузить скриншот в окне
    public static void loadScreen(Context mContext, String image_url) {

        final Dialog dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.screen);
        ImageView image = dialog.findViewById(R.id.screenshot);
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        Glide.with(mContext).load(image_url).into(image);

        dialog.show();

        Button bt_close = dialog.findViewById(R.id.btn_close);

        bt_close.setOnClickListener(v -> dialog.dismiss());

    }

    // оценка плюс или отмена плюса
    public static void like_file(Context mContext, String razdel, int id, int type){
        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        android_id = "DVClient_" + android_id;
        final String is_name = sharedPrefs.getString("dvc_login",android_id);
        RequestQueue queue = Volley.newRequestQueue(mContext);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.LIKE_URL+ razdel + "&id="+id + "&u=" + is_name + "&t=" + type,
                response -> {

            Log.e("favR", response);

                }, error -> {
            if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
            } else if (error instanceof AuthFailureError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
            } else if (error instanceof ServerError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_server), Toast.LENGTH_LONG).show();
            } else if (error instanceof NetworkError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_network), Toast.LENGTH_LONG).show();
            } else if (error instanceof ParseError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_server), Toast.LENGTH_LONG).show();
            }
        });
        queue.add(stringRequest);

    }

    // оценка плюс или отмена плюса
    public static void like_forum_post(Context mContext, int id, int type){
        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        android_id = "DVClient_" + android_id;
        final String is_name = sharedPrefs.getString("dvc_login",android_id);
        RequestQueue queue = Volley.newRequestQueue(mContext);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.LIKE_POST_URL + "&id="+id + "&u=" + is_name + "&t=" + type,
                response -> {

                    Log.e("favR", response);

                }, error -> {
            if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
            } else if (error instanceof AuthFailureError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
            } else if (error instanceof ServerError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_server), Toast.LENGTH_LONG).show();
            } else if (error instanceof NetworkError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_network), Toast.LENGTH_LONG).show();
            } else if (error instanceof ParseError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_server), Toast.LENGTH_LONG).show();
            }
        });
        queue.add(stringRequest);

    }

    // добавление или удаление файла из избранного
    public static void add_to_fav_file(Context mContext, String razdel, int id, int type){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String login = sharedPrefs.getString("dvc_login", "null");
        final String password = sharedPrefs.getString("dvc_password", "null");
        String pass = password;
        try {
            pass = URLEncoder.encode(password, "utf-8");
            login = URLEncoder.encode(login, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(mContext);
        String url = Config.CHECK_AUTH_URL + "&login_name=" + login + "&login_password=" + pass + "&razdel=" + razdel + "&id=" + id + "&addfav=" + type;
        Log.d("tag", url);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int state = jsonObject.getInt(Config.TAG_STATE);

                        if (state == 1) {
                            if (type == 1)
                                Toast.makeText(mContext, mContext.getString(R.string.favorites_btn), Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(mContext, mContext.getString(R.string.unfavorites_btn), Toast.LENGTH_LONG).show();
                        } else Toast.makeText(mContext, mContext.getString(R.string.nav_header_title), Toast.LENGTH_LONG).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {
            if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
            } else if (error instanceof AuthFailureError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
            } else if (error instanceof ServerError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_server), Toast.LENGTH_LONG).show();
            } else if (error instanceof NetworkError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_network), Toast.LENGTH_LONG).show();
            } else if (error instanceof ParseError) {
                Toast.makeText(mContext, mContext.getString(R.string.error_server), Toast.LENGTH_LONG).show();
            }
        });
        queue.add(stringRequest);

    }

    // проиграть видео в окне диалога
    public static void PlayVideo(Context mContext, String link) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final boolean is_aspect = sharedPrefs.getBoolean("dvc_vuploader_aspect",false);
        link = link.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","https://");
        final Dialog dialog = new Dialog(mContext);
        Objects.requireNonNull(dialog.getWindow()).setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.video);
        AndExoPlayerView andExoPlayerView = dialog.findViewById(R.id.andExoPlayerView);
        if (is_aspect) andExoPlayerView.setAspectRatio(EnumAspectRatio.ASPECT_16_9); else andExoPlayerView.setAspectRatio(EnumAspectRatio.ASPECT_MATCH);
        andExoPlayerView.setSource(link);
        dialog.show();

    }
}

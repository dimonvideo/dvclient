package com.dimonvideo.client.util;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.ForumPostsAdapter;
import com.dimonvideo.client.model.FeedForum;
import com.google.android.material.snackbar.Snackbar;
import com.potyvideo.library.AndExoPlayerView;
import com.potyvideo.library.globalEnums.EnumAspectRatio;

import org.json.JSONException;
import org.json.JSONObject;
import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
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

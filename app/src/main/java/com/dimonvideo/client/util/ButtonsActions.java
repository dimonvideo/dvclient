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
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
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
import com.google.android.material.snackbar.Snackbar;
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

    // загрузить комментарии к файлу
    public static void loadComments(Context mContext, String comm_url, ProgressBar progressBar) {

        final Dialog dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.comments_list);
        WebView webView = dialog.findViewById(R.id.read_full_content);

        LoadWeb(mContext, webView, comm_url, progressBar);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y-100;
        int width = size.x-20;
        Button bt_close = dialog.findViewById(R.id.btn_close);
        Objects.requireNonNull(dialog.getWindow()).setLayout(width, height);
        dialog.show();
        bt_close.setOnClickListener(v -> dialog.dismiss());
    }

    // загрузить что нить в webview
    @SuppressLint("SetJavaScriptEnabled")
    public static void LoadWeb(Context mContext, final WebView webView, String full_url, ProgressBar progressBar) {

        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCachePath(mContext.getFilesDir().getPath() + mContext.getPackageName() + "/cache");
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme",false);
        if (is_dark) webView.setBackgroundColor(Color.BLACK); else webView.setBackgroundColor(0);

        webView.setWebViewClient(new WebViewClient() {


            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                view.getContext().startActivity(intent);
                return true;
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

                webView.loadUrl("file:///android_asset/error.html");
            }

            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                injectCSS(mContext, webView);
            }

        });

        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
            }
        });

        webView.loadUrl(full_url);

    }

    // применить темную тему к webview
    private static void injectCSS(Context mContext, WebView webView) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme",false);
        if (is_dark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (is_dark) webView.loadUrl(
                "javascript:document.body.style.setProperty(\"color\", \"white\");"
        );
    }
}

package com.dimonvideo.client.util;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.dimonvideo.client.AllContent;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.potyvideo.library.AndExoPlayerView;

import java.util.Objects;

public class ButtonsActions {

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
        @SuppressLint("HardwareIds") final String android_id = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        RequestQueue queue = Volley.newRequestQueue(mContext);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.LIKE_URL+ razdel + "&id="+id + "&u=" + android_id + "&t=" + type,
                response -> {

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

    public static void PlayVideo(Context mContext, String link) {
        link = link.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","https://");
        final Dialog dialog = new Dialog(mContext);
        Objects.requireNonNull(dialog.getWindow()).setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.video);
        AndExoPlayerView andExoPlayerView = dialog.findViewById(R.id.andExoPlayerView);
        andExoPlayerView.setSource(link);
        dialog.show();

    }
}

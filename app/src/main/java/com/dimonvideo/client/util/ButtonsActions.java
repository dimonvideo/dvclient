package com.dimonvideo.client.util;

import static com.dimonvideo.client.util.NetworkUtils.showErrorToast;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.potyvideo.library.AndExoPlayerView;
import com.potyvideo.library.globalEnums.EnumAspectRatio;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Objects;

public class ButtonsActions {

    // загрузить скриншот в окне
    public static void loadScreen(Context context, String image_url) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.screen);
        ImageView image = dialog.findViewById(R.id.screenshot);
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

        Glide.with(context).load(image_url).apply(RequestOptions.bitmapTransform(new RoundedCorners(24))).into(image);

        dialog.show();

        Button bt_close = dialog.findViewById(R.id.btn_close);

        bt_close.setOnClickListener(v -> dialog.dismiss());



    }

    // оценка плюс или отмена плюса
    public static void like_file(Context context, String razdel, int id, int type){
        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        android_id = "DVClient_" + android_id;
        final String is_name = sharedPrefs.getString("dvc_login",android_id);
        RequestQueue queue = AppController.getInstance().getRequestQueue();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.LIKE_URL+ razdel + "&id="+id + "&u=" + is_name + "&t=" + type,
                response -> {

                    Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_LONG).show();

                }, error -> showErrorToast(context, error)
        );

        queue.add(stringRequest);

    }

    // оценка плюс или отмена плюса
    public static void like_forum_post(Context context, int id, int type){
        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        android_id = "DVClient_" + android_id;
        final String is_name = sharedPrefs.getString("dvc_login",android_id);
        RequestQueue queue = AppController.getInstance().getRequestQueue();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.LIKE_POST_URL + "&id="+id + "&u=" + is_name + "&t=" + type,
                response -> {

                    Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_LONG).show();

                }, error -> showErrorToast(context, error)
        );

        queue.add(stringRequest);

    }

    // добавление или удаление файла из избранного
    public static void add_to_fav_file(Context context, String razdel, int id, int type){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String login = sharedPrefs.getString("dvc_login", "null");
        final String password = sharedPrefs.getString("dvc_password", "null");
        String pass = password;
        try {
            pass = URLEncoder.encode(password, "utf-8");
            login = URLEncoder.encode(login, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        RequestQueue queue = AppController.getInstance().getRequestQueue();
        String url = Config.CHECK_AUTH_URL + "&login_name=" + login + "&login_password=" + pass + "&razdel=" + razdel + "&id=" + id + "&addfav=" + type;
        Log.d("tag", url);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int state = jsonObject.getInt(Config.TAG_STATE);

                        if (state == 1) {
                            if (type == 1)
                                Toast.makeText(context, context.getString(R.string.favorites_btn), Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(context, context.getString(R.string.unfavorites_btn), Toast.LENGTH_LONG).show();
                        } else Toast.makeText(context, context.getString(R.string.nav_header_title), Toast.LENGTH_LONG).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> showErrorToast(context, error)
        );

        queue.add(stringRequest);

    }

    // проиграть видео в окне диалога
    public static void PlayVideo(Context context, String link) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean is_aspect = sharedPrefs.getBoolean("dvc_vuploader_aspect",false);
        final boolean is_external_video = sharedPrefs.getBoolean("dvc_external_video",false);
        try {
            link = link.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","https://");
        } catch (Throwable ignored) {
        }

        if ((is_external_video) && (!link.equals(""))) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(link), "video/*");
            try {
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_video)));
            } catch (Throwable ignored) {
            }
        } else {
            final Dialog dialog = new Dialog(context);
            Objects.requireNonNull(dialog.getWindow()).setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.video);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            AndExoPlayerView andExoPlayerView = dialog.findViewById(R.id.andExoPlayerView);

            if (is_aspect) andExoPlayerView.setAspectRatio(EnumAspectRatio.ASPECT_16_9);
            else andExoPlayerView.setAspectRatio(EnumAspectRatio.ASPECT_MATCH);

            if (!link.equals("")) {
                HashMap<String, String> map = new HashMap<>();
                map.put("link", link);
                andExoPlayerView.setSource(link, map);
                dialog.show();
                dialog.setOnKeyListener((arg0, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        andExoPlayerView.stopPlayer();
                        dialog.dismiss();
                    }
                    return true;
                });
            }
        }
    }
    public static void loadProfile(Context context, String login_name, String image_url) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String rang = sharedPrefs.getString("auth_rang","---");
        String last_date = sharedPrefs.getString("auth_last","---");
        String rep = sharedPrefs.getString("auth_rep","0");
        String reg = sharedPrefs.getString("auth_reg","0");
        String rat = sharedPrefs.getString("auth_rat","0");
        String posts = sharedPrefs.getString("auth_posts","0");

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.profile);
        ImageView image = dialog.findViewById(R.id.avatar);
        TextView login = dialog.findViewById(R.id.login_name);
        TextView status = dialog.findViewById(R.id.status);
        TextView last = dialog.findViewById(R.id.last_date);
        TextView reputation = dialog.findViewById(R.id.rep);
        TextView regdate = dialog.findViewById(R.id.reg);
        TextView rating = dialog.findViewById(R.id.rat);
        TextView post = dialog.findViewById(R.id.posts);
        status.setText(context.getString(R.string.user_rang));
        status.append(rang);
        login.setText(login_name);
        last.setText(context.getString(R.string.user_last));
        last.append(last_date);
        reputation.setText(context.getString(R.string.user_rep));
        reputation.append(rep);
        regdate.setText(context.getString(R.string.user_reg));
        regdate.append(reg);
        rating.setText(context.getString(R.string.user_rat));
        rating.append(rat);
        post.setText(context.getString(R.string.user_posts));
        post.append(posts);

        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        Glide.with(context).load(image_url).apply(RequestOptions.circleCropTransform()).into(image);

        dialog.show();

        Button bt_close = dialog.findViewById(R.id.btn_close);
        Button bt_go = dialog.findViewById(R.id.btn_go);
        Button bt_sett = dialog.findViewById(R.id.btn_setting);

        bt_close.setOnClickListener(v -> dialog.dismiss());

        // переход в профиль
        bt_go.setOnClickListener(view -> {
            String url = Config.BASE_URL + "/0/name/"+login_name;

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            try {
                context.startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        });

        // переход в настройки сайта
        bt_sett.setOnClickListener(view -> {
            String url = Config.BASE_URL + "/set";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            try {
                context.startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        });
    }
}

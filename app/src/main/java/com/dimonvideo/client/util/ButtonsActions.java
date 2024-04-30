package com.dimonvideo.client.util;

import static com.dimonvideo.client.util.NetworkUtils.showErrorToast;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
        new Handler(Looper.getMainLooper()).post(() -> {

            final Dialog dialog = new Dialog(context);

            dialog.setContentView(R.layout.screen);
            ImageView image = dialog.findViewById(R.id.screenshot);
            image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

            Glide.with(context)
                    .load(image_url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.baseline_image_20)
                    .apply(new RequestOptions().dontTransform())
                    .into(image);

            dialog.show();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            Button bt_close = dialog.findViewById(R.id.btn_close);
            bt_close.setOnClickListener(v -> dialog.dismiss());
        });
    }

    // оценка плюс или отмена плюса
    public static void like_file(Context context, String razdel, int id, int type){
        new Handler(Looper.getMainLooper()).post(() -> {

            @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            android_id = "DVClient_" + android_id;
            final String is_name = AppController.getInstance().userName(android_id);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.LIKE_URL + razdel + "&id=" + id + "&u=" + is_name + "&t=" + type,
                    response -> {
                        Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_LONG).show();
                    }, error -> showErrorToast(context, error)
            );

            AppController.getInstance().addToRequestQueue(stringRequest);
        });
    }

    public static void like_member(Context context, int to_uid, String to_name, int type){
        new Handler(Looper.getMainLooper()).post(() -> {

            final String is_name = AppController.getInstance().userName("dvclient");
            final int from_id = AppController.getInstance().isUserId();
            StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.LIKE_URL + "name" + "&id=" + to_uid + "&u=" + is_name + "&from_id=" + from_id + "&t=" + type,
                    response -> {
                        Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_LONG).show();
                    }, error -> showErrorToast(context, error)
            );
            AppController.getInstance().addToRequestQueue(stringRequest);
        });
    }

    // оценка плюс или отмена плюса
    public static void like_forum_post(Context context, int id, int type){
        new Handler(Looper.getMainLooper()).post(() -> {

            @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            android_id = "DVClient_" + android_id;
            final String is_name = AppController.getInstance().userName(android_id);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.LIKE_POST_URL + "&id=" + id + "&u=" + is_name + "&t=" + type,
                    response -> {
                        Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_LONG).show();
                    }, error -> showErrorToast(context, error)
            );
            AppController.getInstance().addToRequestQueue(stringRequest);
        });
    }

    // добавление или удаление файла из избранного
    public static void add_to_fav_file(Context context, String razdel, int id, int type){
        new Handler(Looper.getMainLooper()).post(() -> {
            String is_name = AppController.getInstance().userName("null");
            final String password = AppController.getInstance().userPassword();
            String pass = password;
            try {
                pass = URLEncoder.encode(password, "utf-8");
                is_name = URLEncoder.encode(is_name, "utf-8");
            } catch (UnsupportedEncodingException ignored) {

            }

            String url = Config.CHECK_AUTH_URL + "&login_name=" + is_name + "&login_password=" + pass + "&razdel=" + razdel + "&id=" + id + "&addfav=" + type;
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
                            } else
                                Toast.makeText(context, context.getString(R.string.nav_header_title), Toast.LENGTH_LONG).show();

                        } catch (JSONException ignored) {

                        }
                    }, error -> showErrorToast(context, error)
            );

            AppController.getInstance().addToRequestQueue(stringRequest);
        });
    }

    // добавление или удаление user из избранного
    public static void add_to_fav_user(Context context, int id, int type){
        new Handler(Looper.getMainLooper()).post(() -> {

            String is_name = AppController.getInstance().userName("null");
            final String password = AppController.getInstance().userPassword();
            String pass = password;
            try {
                pass = URLEncoder.encode(password, "utf-8");
                is_name = URLEncoder.encode(is_name, "utf-8");
            } catch (UnsupportedEncodingException ignored) {

            }

            String url = Config.CHECK_AUTH_URL + "&login_name=" + is_name + "&login_password=" + pass + "&razdel=members&id=" + id + "&addfav=" + type;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    response -> {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            int state = jsonObject.getInt(Config.TAG_STATE);

                            if (state == 1) {
                                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_LONG).show();
                            } else
                                Toast.makeText(context, context.getString(R.string.nav_header_title), Toast.LENGTH_LONG).show();

                        } catch (JSONException ignored) {

                        }
                    }, error -> showErrorToast(context, error)
            );

            AppController.getInstance().addToRequestQueue(stringRequest);
        });
    }


    // проиграть видео в окне диалога
    public static void PlayVideo(Context context, String url) {
        new Handler(Looper.getMainLooper()).post(() -> {

            final boolean is_aspect = AppController.getInstance().isAspectRatio();
            final boolean is_external_video = AppController.getInstance().isExternalPlayer();
            String link = url;
            try {
                link = link.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)", "https://");
            } catch (Throwable ignored) {
            }

            if ((is_external_video) && (!link.isEmpty())) {
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

                if (!link.isEmpty()) {
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
        });
    }
    public static void loadProfile(Context context, String login_name, String image_url) {
        new Handler(Looper.getMainLooper()).post(() -> {

            String rang = AppController.getInstance().isRang();
            String last_date = AppController.getInstance().isLastDate();
            String rep = AppController.getInstance().isReputation();
            String reg = AppController.getInstance().isRegDate();
            String rat = AppController.getInstance().isRating();
            String posts = AppController.getInstance().isPosts();

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
            Glide.with(context).load(image_url).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.circleCropTransform()).into(image);

            dialog.show();

            Button bt_close = dialog.findViewById(R.id.btn_close);
            Button bt_go = dialog.findViewById(R.id.btn_go);
            Button bt_sett = dialog.findViewById(R.id.btn_setting);

            bt_close.setOnClickListener(v -> dialog.dismiss());

            // переход в профиль
            bt_go.setOnClickListener(view -> {
                String url = Config.BASE_URL + "/0/name/" + login_name;

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
        });
    }
}

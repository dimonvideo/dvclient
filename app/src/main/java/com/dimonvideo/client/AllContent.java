package com.dimonvideo.client;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.dimonvideo.client.ui.main.MainFragmentHorizontal;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.potyvideo.library.AndExoPlayerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class AllContent extends AppCompatActivity  {
    WebView webView;
    ProgressBar progressBar;
    String title;
    String url;
    String headers;
    String category;
    String razdel;
    String image_url;
    String date;
    String user;
    String size;
    String id;
    String link;
    String mod;
    int comments, plus;
    Toolbar toolbar;
    Button downloadBtn, modBtn, commentsBtn, mp4Btn;
    LikeButton likeButton, starButton;
    Dialog dialog;
    TextView txt_plus;

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 10001;
    private static final String WRITE_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme",false);
        final boolean is_vuploader_play = sharedPrefs.getBoolean("dvc_vuploader_play",true);
        if (is_dark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        dialog = new Dialog(AllContent.this,android.R.style.Theme_Translucent_NoTitleBar);

        setContentView(R.layout.activity_collapse);
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        downloadBtn = findViewById(R.id.btn_download);
        modBtn = findViewById(R.id.btn_mod);
        commentsBtn = findViewById(R.id.btn_comment);
        mp4Btn = findViewById(R.id.btn_mp4);

        if (getIntent()!=null) {
            title = (String) getIntent().getSerializableExtra(Config.TAG_TITLE);
            headers = (String) getIntent().getSerializableExtra(Config.TAG_HEADERS);
            category = (String) getIntent().getSerializableExtra(Config.TAG_CATEGORY);
            razdel = (String) getIntent().getSerializableExtra(Config.TAG_RAZDEL);
            image_url = getIntent().getStringExtra(Config.TAG_IMAGE_URL);
            date = getIntent().getStringExtra(Config.TAG_DATE);
            user = getIntent().getStringExtra(Config.TAG_USER);
            size = getIntent().getStringExtra(Config.TAG_SIZE);
            link = getIntent().getStringExtra(Config.TAG_LINK);
            mod = getIntent().getStringExtra(Config.TAG_MOD);
            id = getIntent().getStringExtra(Config.TAG_ID);
            comments = Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra(Config.TAG_COMMENTS)));
            plus = Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra(Config.TAG_PLUS)));
        }

        downloadBtn.setVisibility(View.VISIBLE);
        modBtn.setVisibility(View.VISIBLE);
        url = Config.BASE_URL + "/" + razdel + "/" + id;

        if (razdel.equals(Config.COMMENTS_RAZDEL)) {
            url = Config.BASE_URL + "/" + id + "-news.html";
        }
        if (razdel.equals(Config.VUPLOADER_RAZDEL)) {
            mp4Btn.setVisibility(View.VISIBLE);
            mp4Btn.setOnClickListener(view -> ButtonsActions.PlayVideo(this, link));

        }
        // если нет размера файла
        if (size.startsWith("0")) {
            downloadBtn.setVisibility(View.GONE);
            modBtn.setVisibility(View.GONE);
        } else downloadBtn.setText(getString(R.string.download) + " " + size);

        // если нет mod
        if (mod.startsWith("null")) {
            modBtn.setVisibility(View.GONE);
        }

        downloadBtn.setOnClickListener(view -> {

            if (isPermissionGranted()) {
                DownloadFile.download(getApplicationContext(), link);
            } else {
                // иначе запрашиваем разрешение у пользователя
                requestPermission();
            }
        });

        modBtn.setOnClickListener(view -> {
            if (isPermissionGranted()) {
                DownloadFile.download(getApplicationContext(), mod);
                // иначе запрашиваем разрешение у пользователя
                requestPermission();
            }
        });

        if (comments > 0) {
            String comText = getResources().getString(R.string.Comments) + ": " + comments;
            commentsBtn.setText(comText);
        }

        commentsBtn.setOnClickListener(view -> {
            String comm_url = Config.COMMENTS_READS_URL + razdel + "&lid=" + id;
            Log.d("tag", comm_url);
            ButtonsActions.loadComments(this, comm_url, progressBar);
        });

        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        TextView titleHeaders = findViewById(R.id.headers_title);
        TextView titleSubHeaders = findViewById(R.id.sub_headers_title);
        titleHeaders.setText(headers);
        titleSubHeaders.setText(date);
        titleSubHeaders.append(" " + getString(R.string.by) + " " + user);
        assert razdel != null;
        if (!razdel.equals(Config.COMMENTS_RAZDEL)) titleHeaders.append(" - " + category);

        ImageView imageView = findViewById(R.id.main_imageview_placeholder);
        Glide.with(this).load(image_url).diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView);
        if (razdel.equals(Config.VUPLOADER_RAZDEL) && is_vuploader_play) imageView.setOnClickListener(v -> ButtonsActions.PlayVideo(this, link)); else imageView.setOnClickListener(v -> ButtonsActions.loadScreen(this, image_url));

        /*


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

         */
        progressBar = findViewById(R.id.progressBar);
        String full_url = Config.TEXT_URL + razdel + "&min=" + id;
        webView = findViewById(R.id.read_full_content);

        ButtonsActions.LoadWeb(this, webView, full_url, progressBar);

        progressBar.setMax(100);
        progressBar.setProgress(1);

        txt_plus = findViewById(R.id.txt_plus);
        txt_plus.setText(String.valueOf(plus));

        View view = findViewById(android.R.id.content);
        likeButton = findViewById(R.id.thumb_button);
        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Snackbar.make(view, getString(R.string.like), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(getApplicationContext(), razdel, Integer.parseInt(id), 1);
                txt_plus.setText(String.valueOf(plus+1));

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Snackbar.make(view, getString(R.string.unlike), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(getApplicationContext(), razdel, Integer.parseInt(id), 2);
                txt_plus.setText(String.valueOf(plus-1));
            }
        });

        starButton = findViewById(R.id.star_button);
        starButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton starButton) {
                Snackbar.make(view, getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(getApplicationContext(), razdel, Integer.parseInt(id), 1);

            }

            @Override
            public void unLiked(LikeButton starButton) {
                Snackbar.make(view, getString(R.string.unfavorites_btn), Snackbar.LENGTH_LONG).show();
            }
        });

     //   starButton.setLiked(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_all_content, menu);
        return true;
    }
    
    // toolbar main arrow
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        // main arrow
        if (id == android.R.id.home) {
            onBackPressed();
        }
        // settings
        if (id == R.id.action_settings) {
            Intent i = new Intent(AllContent.this, SettingsActivity.class);
            startActivityForResult(i, 1);
            return true;
        }
        // refresh
        if (id == R.id.action_refresh) {
            recreate();
        }

        // share
        if (id == R.id.menu_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, title);
            try {
                startActivity(shareIntent);
            } catch (Throwable ignored) {
            }
        }
        // open page
        if (id == R.id.action_open) {

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }
        // feedback
        if (id == R.id.action_screen) {

            ButtonsActions.loadScreen(this, image_url);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dialog.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        dialog.dismiss();
    }

    @Override
    public void onBackPressed() {
        if(getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStackImmediate();
        } else {
            super.onBackPressed();
            finish();
        }
    }

    @Override
    public boolean onKeyLongPress(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return super.onKeyLongPress(keycode, event);
    }


    // проверяем разрешение - есть ли оно у приложения
    public boolean isPermissionGranted() {
        int permissionCheck = ActivityCompat.checkSelfPermission(getApplicationContext(), AllContent.WRITE_EXTERNAL_STORAGE_PERMISSION);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(AllContent.this, "Разрешения получены", Toast.LENGTH_LONG).show();


            } else {
                Toast.makeText(AllContent.this, "Разрешения не получены", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void requestPermission() {
        // запрашиваем разрешение
        ActivityCompat.requestPermissions(AllContent.this, new String[]{AllContent.WRITE_EXTERNAL_STORAGE_PERMISSION}, AllContent.REQUEST_WRITE_EXTERNAL_STORAGE);
    }
}
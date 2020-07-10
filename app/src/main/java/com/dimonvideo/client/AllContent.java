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
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 10001;
    private static final String WRITE_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme",false);
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
            mp4Btn.setOnClickListener(view -> PlayVideo(link));

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
            loadComments(comm_url);
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
        if (!razdel.equals("comments")) titleHeaders.append(" - " + category);

        ImageView imageView = findViewById(R.id.main_imageview_placeholder);
        Glide.with(this).load(image_url).diskCacheStrategy(DiskCacheStrategy.ALL).into(imageView);
        imageView.setOnClickListener(v -> loadScreen());
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
        LoadWeb(webView, full_url);

        progressBar.setMax(100);
        progressBar.setProgress(1);

        View view = findViewById(android.R.id.content);
        likeButton = findViewById(R.id.thumb_button);
        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Snackbar.make(view, getString(R.string.like), Snackbar.LENGTH_LONG).show();
                like_file(getApplicationContext(), razdel, Integer.parseInt(id), 1);
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Snackbar.make(view, getString(R.string.unlike), Snackbar.LENGTH_LONG).show();
                like_file(getApplicationContext(), razdel, Integer.parseInt(id), 2);
            }
        });

        starButton = findViewById(R.id.star_button);
        starButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton starButton) {
                Snackbar.make(view, getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void unLiked(LikeButton starButton) {
                Snackbar.make(view, getString(R.string.unfavorites_btn), Snackbar.LENGTH_LONG).show();
            }
        });

     //   starButton.setLiked(true);

    }

    private void PlayVideo(String link) {
        link = link.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","https://");
        Objects.requireNonNull(dialog.getWindow()).setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.video);
        AndExoPlayerView andExoPlayerView = dialog.findViewById(R.id.andExoPlayerView);
        andExoPlayerView.setSource(link);
        dialog.show();

    }

    @SuppressLint("SetJavaScriptEnabled")
    public void LoadWeb(final WebView webView, String full_url) {

        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCachePath(this.getFilesDir().getPath() + getPackageName() + "/cache");
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setBackgroundColor(0);
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
                injectCSS();

                FragmentManager fragmentManager = getSupportFragmentManager();

                MainFragmentHorizontal homeFrag = new MainFragmentHorizontal();
                Bundle bundle = new Bundle();
                bundle.putString(Config.TAG_RAZDEL, razdel);
                homeFrag.setArguments(bundle);
                Handler handler = new Handler();
                handler.postDelayed(() -> fragmentManager.beginTransaction()
                        .add(R.id.container, homeFrag)
                        .addToBackStack(null)
                        .commit(), 500);

            }

        });

        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {


                progressBar.setProgress(progress);
            }
        });

        webView.loadUrl(full_url);

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

        // refresh
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
        // other apps
        if (id == R.id.action_open) {

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }
        // feedback
        if (id == R.id.action_screen) {

                loadScreen();

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

    private void injectCSS() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme",false);

        if (is_dark) webView.loadUrl(
                "javascript:document.body.style.setProperty(\"color\", \"white\");"
        );
    }
    private void loadScreen() {

        final Dialog dialog = new Dialog(AllContent.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.screen);
        ImageView image = dialog.findViewById(R.id.screenshot);
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        Glide.with(AllContent.this).load(image_url).into(image);

        dialog.show();

        Button bt_close = dialog.findViewById(R.id.btn_close);

        bt_close.setOnClickListener(v -> dialog.dismiss());

    }

    private void loadComments(String comm_url) {

        final Dialog dialog = new Dialog(AllContent.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.comments_list);
        webView = dialog.findViewById(R.id.read_full_content);

        LoadWeb(webView, comm_url);

        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int height = dm.heightPixels-100;
        int width = dm.widthPixels-20;
        Button bt_close = dialog.findViewById(R.id.btn_close);
        Objects.requireNonNull(dialog.getWindow()).setLayout(width, height);
        dialog.show();
        bt_close.setOnClickListener(v -> dialog.dismiss());

    }

    // проверяем разрешение - есть ли оно у приложения
    private boolean isPermissionGranted() {
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

    private void requestPermission() {
        // запрашиваем разрешение
        ActivityCompat.requestPermissions(AllContent.this, new String[]{AllContent.WRITE_EXTERNAL_STORAGE_PERMISSION}, AllContent.REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    private void like_file(Context mContext, String razdel,  int id, int type){
        @SuppressLint("HardwareIds") final String android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        RequestQueue queue = Volley.newRequestQueue(mContext);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.LIKE_URL+ razdel + "&id="+id + "&u=" + android_id + "&t=" + type,
                response -> {

                }, error -> {
                    if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                        Toast.makeText(mContext, getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
                    } else if (error instanceof AuthFailureError) {
                        Toast.makeText(mContext, getString(R.string.error_network_timeout), Toast.LENGTH_LONG).show();
                    } else if (error instanceof ServerError) {
                        Toast.makeText(mContext, getString(R.string.error_server), Toast.LENGTH_LONG).show();
                    } else if (error instanceof NetworkError) {
                        Toast.makeText(mContext, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                    } else if (error instanceof ParseError) {
                        Toast.makeText(mContext, getString(R.string.error_server), Toast.LENGTH_LONG).show();
                    }
                });
        queue.add(stringRequest);
    }
}
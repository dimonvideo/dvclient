package com.dimonvideo.client;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.dimonvideo.client.util.DownloadFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

public class AllContent extends AppCompatActivity {
    WebView webView;
    ProgressBar progressBar;
    String title, url, headers, category, razdel, image_url, date, user, size, id, link, mod;
    Toolbar toolbar;
    Button downloadBtn, modBtn, btnComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collapse);
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        downloadBtn = findViewById(R.id.btn_download);
        modBtn = findViewById(R.id.btn_mod);
        btnComments = findViewById(R.id.btn_comment);
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

        downloadBtn.setVisibility(View.VISIBLE);
        modBtn.setVisibility(View.VISIBLE);
        url = Config.BASE_URL + "/" + razdel + "/" + id;

        if (razdel.equals("comments")) {
            url = Config.BASE_URL + "/" + id + "-news.html";
        }

        // если нет размера файла
        if (size.startsWith("0")) {
            downloadBtn.setVisibility(View.GONE);
            modBtn.setVisibility(View.GONE);
        }

        // если нет mod
        if (mod.startsWith("null")) {
            modBtn.setVisibility(View.GONE);
        }

        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DownloadFile.download(getApplicationContext(), link);

            }
        });

        modBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DownloadFile.download(getApplicationContext(), link);

            }
        });

        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        TextView titleHeaders = (TextView) findViewById(R.id.headers_title);
        TextView titleSubHeaders = (TextView) findViewById(R.id.sub_headers_title);
        titleHeaders.setText(headers);
        titleSubHeaders.setText(date);
        titleSubHeaders.append(" " + getString(R.string.by) + " " + user);
        assert razdel != null;
        if (!razdel.equals("comments")) titleHeaders.append(" - " + category);

        ImageView imageView = findViewById(R.id.main_imageview_placeholder);
        Glide.with(this).load(image_url).into(imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadScreen();
            }
        });
        /*


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

         */
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        LoadWeb(razdel, id);

        progressBar.setMax(100);
        progressBar.setProgress(1);

        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {


                progressBar.setProgress(progress);
            }
        });

        webView.setWebViewClient(new WebViewClient() {


            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }


            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

                webView.loadUrl("file:///android_asset/error.html");
            }

            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                injectCSS();

            }

        });


    }

    @SuppressLint("SetJavaScriptEnabled")
    public void LoadWeb(String razdel, String id) {

        webView = findViewById(R.id.read_full_content);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCachePath(this.getFilesDir().getPath() + getPackageName() + "/cache");
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setBackgroundColor(0);
        webView.loadUrl(Config.TEXT_URL + razdel + "&min=" + id);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_all_content, menu);
        return true;
    }
    
    // toolbar home arrow
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        // home arrow
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
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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

        Button bt_close = (Button)dialog.findViewById(R.id.btn_close);

        bt_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

    }
}
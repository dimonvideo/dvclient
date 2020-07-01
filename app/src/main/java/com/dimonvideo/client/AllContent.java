package com.dimonvideo.client;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

import java.util.Objects;

public class AllContent extends AppCompatActivity {
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collapse);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        String title = (String) getIntent().getSerializableExtra(Config.TAG_TITLE);
        String headers = (String) getIntent().getSerializableExtra(Config.TAG_HEADERS);
        String category = (String) getIntent().getSerializableExtra(Config.TAG_CATEGORY);
        String razdel = (String) getIntent().getSerializableExtra(Config.TAG_RAZDEL);
        String image = (String) getIntent().getStringExtra(Config.TAG_IMAGE_URL);
        String date = (String) getIntent().getStringExtra(Config.TAG_DATE);
        String user = (String) getIntent().getStringExtra(Config.TAG_USER);
        String size = (String) getIntent().getStringExtra(Config.TAG_SIZE);
        String id = (String) getIntent().getStringExtra(Config.TAG_ID);
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
        Glide.with(this).load(image).into(imageView);

        webView=(WebView)findViewById(R.id.read_full_content);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAppCachePath(this.getFilesDir().getPath() + getPackageName() + "/cache");
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setBackgroundColor(0);
        webView.loadUrl(Config.TEXT_URL + razdel + "&min=" + id);



        Log.w("myApp", Config.TEXT_URL + razdel + "&min=" + id);

        /*


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    // toolbar home arrow
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(menuItem);
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
}
package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.SettingsActivity;
import com.dimonvideo.client.adater.CommentsAdapter;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.util.NetworkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.M)
public class Comments extends AppCompatActivity  implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener {
    private List<FeedForum> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;
    LinearLayout emptyLayout;
    String comm_url, file_title, razdel, lid;
    private RequestQueue requestQueue;
    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    SharedPreferences sharedPrefs;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final int auth_state = sharedPrefs.getInt("auth_state", 0);
        final String is_pm = sharedPrefs.getString("dvc_pm", "off");
        final String is_dark = sharedPrefs.getString("dvc_theme_list", "false");
        assert is_dark != null;
        if (is_dark.equals("true")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (is_dark.equals("system")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        adjustFontScale( getResources().getConfiguration());
        setContentView(R.layout.comments_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getIntent()!=null) {
            file_title = (String) getIntent().getSerializableExtra(Config.TAG_TITLE);
            comm_url = (String) getIntent().getSerializableExtra(Config.TAG_LINK);
            razdel = (String) getIntent().getSerializableExtra(Config.TAG_RAZDEL);
            lid = getIntent().getStringExtra(Config.TAG_ID);
        }

        Objects.requireNonNull(getSupportActionBar()).setTitle(file_title);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnScrollChangeListener(this);
        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(this);
        emptyLayout = findViewById(R.id.linearEmpty);

        progressBar = findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new CommentsAdapter(listFeed, this);
        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);

        recyclerView.setAdapter(adapter);
        // pull to refresh
        swipLayout = findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(this);

        LinearLayout post_layout = findViewById(R.id.post);
        if (auth_state > 0) post_layout.setVisibility(View.VISIBLE);
        // отправка ответа
        Button btnSend = findViewById(R.id.btnSend);
        EditText textInput = findViewById(R.id.textInput);

        btnSend.setOnClickListener(v -> {
            NetworkUtils.sendPm(this, Integer.parseInt(lid), textInput.getText().toString(), 20, razdel, 0);
            textInput.getText().clear();
            recreate();
        });

        // open PM
        FloatingActionButton fab = findViewById(R.id.fab);
        if ((is_pm.equals("off")) || (auth_state != 1)) fab.setVisibility(View.GONE);
        fab.setOnClickListener(view -> {
            Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);
            notificationIntent.putExtra("action", "PmFragment");
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(notificationIntent);

        });
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(receiver, new IntentFilter("com.dimonvideo.client.NEW_PM"));

        if (auth_state > 0) {

            // обновляем счетчик лс
            @SuppressLint("StaticFieldLeak")
            class AsyncCountPm extends AsyncTask<String, String, String> {
                SharedPreferences sharedPrefs;
                private WeakReference<Context> contextRef;

                public AsyncCountPm(Context context) {
                    this.contextRef = new WeakReference<>(context);
                }

                @Override
                protected String doInBackground(String... params) {
                    Context context = contextRef.get();
                    if (context != null) {
                        try {
                            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                            // check is logged
                            final String password = sharedPrefs.getString("dvc_password", "null");
                            View view = ((Activity) context).getWindow().getDecorView().getRootView();

                            NetworkUtils.checkPassword(context, view, password);

                            return null;
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    final int pm_unread = sharedPrefs.getInt("pm_unread", 0);
                    if (pm_unread > 0) {
                        TextView fab_badge = findViewById(R.id.fab_badge);
                        fab_badge.setVisibility(View.VISIBLE);
                        fab_badge.setText(String.valueOf(pm_unread));
                    }
                }
            }
            AsyncCountPm task = new AsyncCountPm(this);
            task.execute();

        }
    }
    // запрос к серверу апи
    private JsonArrayRequest getDataFromServer(int requestCount) {

        return new JsonArrayRequest(comm_url + requestCount,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                    for (int i = 0; i < response.length(); i++) {
                        FeedForum jsonFeed = new FeedForum();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setUser(json.getString(Config.TAG_USER));
                            jsonFeed.setCategory(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setState(json.getString(Config.TAG_RAZDEL));
                            jsonFeed.setTime(json.getLong(Config.TAG_TIME));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setMin(json.getInt(Config.TAG_MIN));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listFeed.add(jsonFeed);
                    }
                    adapter.notifyDataSetChanged();

                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                });
    }

    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String str = intent.getStringExtra("count");
                TextView fab_badge = findViewById(R.id.fab_badge);
                fab_badge.setVisibility(View.VISIBLE);
                fab_badge.setText(str);
                if ((str == null) || (str.equals("0"))) fab_badge.setVisibility(View.GONE);
            }
        }
    };

    // получение данных и увеличение номера страницы
    private void getData() {
        ProgressBarBottom.setVisibility(View.VISIBLE);
        requestQueue.add(getDataFromServer(requestCount));
        requestCount++;
    }

    // опредление последнего элемента
    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        if (Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() != 0) {
            int lastVisibleItemPosition = ((LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).findLastCompletelyVisibleItemPosition();
            return lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1;
        }
        return false;
    }

    // получение следующей страницы при скролле
    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (isLastItemDisplaying(recyclerView)) {
            getData();
        }
    }

    // обновление
    @Override
    public void onRefresh() {
        requestCount = 1;
        recreate();
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(receiver);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_comments, menu);
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
            Intent i = new Intent(Comments.this, SettingsActivity.class);
            startActivityForResult(i, 1);
            return true;
        }
        // refresh
        if (id == R.id.action_refresh) {
            recreate();
        }
        // open page
        if (id == R.id.action_open) {

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Config.BASE_URL+"/"+razdel+"/"+lid+"#comments"));
            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }

        return super.onOptionsItemSelected(item);
    }

    // scale font
    private void adjustFontScale(Configuration configuration) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        configuration.fontScale = Float.parseFloat(sharedPrefs.getString("dvc_scale","1.0f"));
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        assert wm != null;
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
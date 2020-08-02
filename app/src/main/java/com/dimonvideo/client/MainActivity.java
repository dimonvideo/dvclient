package com.dimonvideo.client;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.ui.forum.ForumFragmentTopics;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.ui.pm.PmFragment;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    Fragment homeFrag;
    SharedPreferences sharedPrefs;
    static int razdel = 10;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 10001;
    private static final String WRITE_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean is_uploader = sharedPrefs.getBoolean("dvc_uploader", true);
        final boolean is_vuploader = sharedPrefs.getBoolean("dvc_vuploader", true);
        final boolean is_news = sharedPrefs.getBoolean("dvc_news", true);
        final boolean is_gallery = sharedPrefs.getBoolean("dvc_gallery", true);
        final boolean is_muzon = sharedPrefs.getBoolean("dvc_muzon", true);
        final boolean is_books = sharedPrefs.getBoolean("dvc_books", true);
        final boolean is_articles = sharedPrefs.getBoolean("dvc_articles", true);
        final boolean is_forum = sharedPrefs.getBoolean("dvc_forum", true);
        final String is_pm = sharedPrefs.getString("dvc_pm", "off");
        final String login_name = sharedPrefs.getString("dvc_login", getString(R.string.nav_header_title));
        final String image_url = sharedPrefs.getString("auth_foto", Config.BASE_URL + "/images/noavatar.png");
        final int auth_state = sharedPrefs.getInt("auth_state", 0);
        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme", false);
        if (is_dark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_forum,
                R.id.nav_news,
                R.id.nav_gallery,
                R.id.nav_vuploader,
                R.id.nav_muzon,
                R.id.nav_books,
                R.id.nav_uploader,
                R.id.nav_cats,
                R.id.nav_android,
                R.id.nav_articles
        ).setOpenableLayout(drawer).build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        ImageView status = navigationView.getHeaderView(0).findViewById(R.id.status);
        status.setImageResource(R.drawable.ic_status_gray);
        TextView Login_Name = navigationView.getHeaderView(0).findViewById(R.id.login_string);
        ImageView avatar = navigationView.getHeaderView(0).findViewById(R.id.avatar);
        TextView app_version = navigationView.getHeaderView(0).findViewById(R.id.app_version);
        app_version.append(": " + BuildConfig.VERSION_NAME);

        Glide.with(this).load(image_url).apply(RequestOptions.circleCropTransform()).into(avatar);

        // открываем лс из уведомления
        Intent intent_pm = getIntent();
        if (intent_pm != null) {
            try {
                if (intent_pm.getStringExtra("action").equals("PmFragment")) {
                    FragmentManager fragmentManager = getSupportFragmentManager();

                    Fragment PmFragment = new PmFragment();

                    fragmentManager.beginTransaction()
                            .add(R.id.nav_host_fragment, PmFragment)
                            .addToBackStack(null)
                            .commit();
                }
            } catch (Throwable ignored) {
            }
        }

        if (auth_state > 0) {

            // обновляем счетчик лс
            status.setImageResource(R.drawable.ic_status_green);
            Login_Name.setText(getString(R.string.sign_as));
            Login_Name.append(login_name);
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
            task.execute(login_name);

        } else {
            Login_Name.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            });
        }

        // скрываем пункты бокового меню
        if (!is_uploader) navigationView.getMenu().removeItem(R.id.nav_uploader);
        if (!is_news) navigationView.getMenu().removeItem(R.id.nav_news);
        if (!is_gallery) navigationView.getMenu().removeItem(R.id.nav_gallery);
        if (!is_vuploader) navigationView.getMenu().removeItem(R.id.nav_vuploader);
        if (!is_muzon) navigationView.getMenu().removeItem(R.id.nav_muzon);
        if (!is_books) navigationView.getMenu().removeItem(R.id.nav_books);
        if (!is_articles) navigationView.getMenu().removeItem(R.id.nav_articles);
        if (!is_forum) navigationView.getMenu().removeItem(R.id.nav_forum);

        // open PM
        FloatingActionButton fab = findViewById(R.id.fab);
        if ((is_pm.equals("off")) || (auth_state != 1)) fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {

                FragmentManager fragmentManager = getSupportFragmentManager();

                Fragment PmFragment = new PmFragment();

                fragmentManager.beginTransaction()
                        .add(R.id.nav_host_fragment, PmFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(receiver, new IntentFilter("com.dimonvideo.client.NEW_PM"));

        if (!isPermissionGranted()) requestPermission();


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
                Log.e("pm", "recived");
            }
        }
    };


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        razdel = event.razdel;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        // search
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        final EditText searchEditText = searchView.findViewById(R.id.search_src_text);

        searchEditText.setHint(getString(R.string.search));

        searchEditText.setHintTextColor(getResources().getColor(R.color.list_row_end_color));
        assert searchManager != null;
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(500);
        Log.d("tagActivity", String.valueOf(razdel));

        searchEditText.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                //run query to the server
                FragmentManager fragmentManager = getSupportFragmentManager();

                homeFrag = new MainFragmentContent();

                if (razdel == 8) homeFrag = new ForumFragmentTopics(); // forum
                if (razdel == 13) homeFrag = new PmFragment(); // pm

                Bundle bundle = new Bundle();
                String story = searchEditText.getText().toString().trim();
                if (TextUtils.isEmpty(story)) story = null;
                bundle.putSerializable(Config.TAG_STORY, story);
                bundle.putInt(Config.TAG_CATEGORY, razdel);
                homeFrag.setArguments(bundle);

                fragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, homeFrag)
                        .addToBackStack(null)
                        .commit();
            }
            return false;
        });

        return true;
    }


    // menu
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        // settings
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(i, 1);
            return true;
        }
        // refresh
        if (id == R.id.action_refresh) {
            FragmentManager fragmentManager = getSupportFragmentManager();

            homeFrag = new MainFragmentContent();

            if (razdel == 8) homeFrag = new ForumFragmentTopics(); // forum
            if (razdel == 13) homeFrag = new PmFragment(); // pm

            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFrag)
                    .addToBackStack(null)
                    .commit();

        }

        // other apps
        if (id == R.id.action_others) {

            String url = "https://play.google.com/store/apps/dev?id=6091758746633814135";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));

            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }
        // feedback
        if (id == R.id.action_feedback) {

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.fromParts("mailto", getString(R.string.app_mail), null));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Feedback");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(intent);
            } catch (Throwable ignored) {
            }
        }

        // donate
        if (id == R.id.action_donate) {

            String url = Config.BASE_URL + "/reklama.php";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));

            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }

        // votes
        if (id == R.id.action_vote) {

            String url = Config.BASE_URL + "/votes";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));

            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        try {
            unregisterReceiver(receiver);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack(); // pop fragment here
        } else {
            super.onBackPressed(); // after nothing is there default behavior of android works.
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // проверяем разрешение - есть ли оно у приложения
    public boolean isPermissionGranted() {
        int permissionCheck = ActivityCompat.checkSelfPermission(getApplicationContext(), MainActivity.WRITE_EXTERNAL_STORAGE_PERMISSION);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(MainActivity.this, "Разрешения получены", Toast.LENGTH_LONG).show();


            } else {
                Toast.makeText(MainActivity.this, "Разрешения не получены", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void requestPermission() {
        // запрашиваем разрешение
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{MainActivity.WRITE_EXTERNAL_STORAGE_PERMISSION}, MainActivity.REQUEST_WRITE_EXTERNAL_STORAGE);
    }
}
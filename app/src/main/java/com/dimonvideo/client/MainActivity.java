package com.dimonvideo.client;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.adater.AdapterMainRazdel;
import com.dimonvideo.client.databinding.ActivityMainBinding;
import com.dimonvideo.client.db.Provider;
import com.dimonvideo.client.ui.forum.ForumFragmentTopics;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.ui.main.MainFragmentAddFile;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.ui.pm.PmMembersFragment;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.GetRazdelName;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.PurchaseHelper;
import com.dimonvideo.client.util.RequestPermissionHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private Fragment homeFrag;
    static String razdel = "10";
    private RequestPermissionHandler mRequestPermissionHandler;
    private AppBarConfiguration mAppBarConfiguration;
    public static ActivityMainBinding binding;
    public static NavigationView navigationView;
    @SuppressLint("StaticFieldLeak")
    public static NavController navController;
    private boolean doubleBackToExitPressedOnce = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        // очистка старых записей
        new Handler().postDelayed(Provider::clearDB_OLD, 3000);

        final String is_dark = AppController.getInstance().isDark();
        final String main_razdel = AppController.getInstance().mainRazdel();
        final String image_url = AppController.getInstance().imageUrl();
        final String password = AppController.getInstance().userPassword();
        final String login_name = AppController.getInstance().userName(getString(R.string.nav_header_title));
        final String is_pm = AppController.getInstance().isPm();
        final int auth_state = AppController.getInstance().isAuth();


        final boolean is_uploader = AppController.getInstance().isUploader();
        final boolean is_android = AppController.getInstance().isAndroid();
        final boolean is_vuploader = AppController.getInstance().isVuploader();
        final boolean is_news = AppController.getInstance().isUsernews();
        final boolean is_gallery = AppController.getInstance().isGallery();
        final boolean is_muzon = AppController.getInstance().isMuzon();
        final boolean is_books = AppController.getInstance().isBooks();
        final boolean is_articles = AppController.getInstance().isArticles();
        final boolean is_forum = AppController.getInstance().isForum();
        final boolean is_tracker = AppController.getInstance().isTracker();
        final boolean is_blog = AppController.getInstance().isBlog();
        final boolean is_suploader = AppController.getInstance().isSuploader();
        final boolean is_add_file = AppController.getInstance().isAddFile();

        if (is_dark.equals("true"))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (is_dark.equals("system"))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adjustFontScale(getResources().getConfiguration());

        setSupportActionBar(binding.appBarMain.toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // информация об обновлении
        try {

            if (appWasUpdated(this)) {

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.whats_new_title))
                        .setMessage(getString(R.string.whats_new_text))
                        .setNegativeButton(android.R.string.ok,
                                (dialog, which) -> dialog.dismiss()).setIcon(R.mipmap.ic_launcher_round).show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        DrawerLayout drawerLayout = binding.drawerLayout;
        navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_forum, R.id.nav_news,
                R.id.nav_vuploader, R.id.nav_muzon, R.id.nav_books, R.id.nav_uploader,
                R.id.nav_android, R.id.nav_articles, R.id.nav_tracker, R.id.nav_blog, R.id.nav_suploader)
                .setOpenableLayout(drawerLayout)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.post(() -> {

            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.mobile_navigation);

            // выбор главного раздела
            if (Integer.parseInt(main_razdel) == 10) navGraph.setStartDestination(R.id.nav_home);
            if (Integer.parseInt(main_razdel) == 4) navGraph.setStartDestination(R.id.nav_news);
            if (Integer.parseInt(main_razdel) == 1) navGraph.setStartDestination(R.id.nav_gallery);
            if (Integer.parseInt(main_razdel) == 3) navGraph.setStartDestination(R.id.nav_vuploader);
            if (Integer.parseInt(main_razdel) == 5) navGraph.setStartDestination(R.id.nav_muzon);
            if (Integer.parseInt(main_razdel) == 6) navGraph.setStartDestination(R.id.nav_books);
            if (Integer.parseInt(main_razdel) == 2) navGraph.setStartDestination(R.id.nav_uploader);
            if (Integer.parseInt(main_razdel) == 11) navGraph.setStartDestination(R.id.nav_android);
            if (Integer.parseInt(main_razdel) == 7) navGraph.setStartDestination(R.id.nav_articles);
            if (Integer.parseInt(main_razdel) == 14) navGraph.setStartDestination(R.id.nav_tracker);
            if (Integer.parseInt(main_razdel) == 15) navGraph.setStartDestination(R.id.nav_blog);
            if (Integer.parseInt(main_razdel) == 16) navGraph.setStartDestination(R.id.nav_suploader);

            navController.setGraph(navGraph);

        });

        ImageView status = navigationView.getHeaderView(0).findViewById(R.id.status);
        status.setImageResource(R.drawable.ic_status_gray);
        TextView Login_Name = navigationView.getHeaderView(0).findViewById(R.id.login_string);
        ImageView avatar = navigationView.getHeaderView(0).findViewById(R.id.avatar);
        ImageView setting_icon = navigationView.getHeaderView(0).findViewById(R.id.settings_icon);
        ImageView theme_icon = navigationView.getHeaderView(0).findViewById(R.id.theme_icon);
        TextView app_version = navigationView.getHeaderView(0).findViewById(R.id.app_version);
        app_version.append(": " + BuildConfig.VERSION_NAME);

        // иконка настроек
        setting_icon.setOnClickListener(view -> {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // иконка темы
        theme_icon.setOnClickListener(view -> {
            if (is_dark.equals("true")) AppController.getInstance().putThemeLight();
            else AppController.getInstance().putThemeDark();
            finishAffinity();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // иконка меню
        if (auth_state > 0) {
            // загрузка аватара пользователя
            Glide.with(getApplicationContext())
                    .load(image_url)
                    .apply(RequestOptions.circleCropTransform())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(avatar);

            avatar.setOnClickListener(v -> ButtonsActions.loadProfile(this, login_name, image_url));
        }

        // быстрые ярлыки
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {

            try {
                ShortcutManager shortcutManager = (ShortcutManager) getSystemService(SHORTCUT_SERVICE);
                Intent notificationIntent = new Intent(this, MainActivity.class);
                notificationIntent.putExtra("action", "PmFragment");
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                ShortcutInfo webShortcut = new ShortcutInfo.Builder(this, "shortcut_help")
                        .setShortLabel(getString(R.string.tab_pm))
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                        .setIntent(notificationIntent.setAction(Intent.ACTION_VIEW))
                        .build();

                Intent forumIntent = new Intent(this, MainActivity.class);
                forumIntent.putExtra("action", "ForumFragment");
                forumIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                ShortcutInfo forumShortcut = new ShortcutInfo.Builder(this, "shortcut_forum")
                        .setShortLabel(getString(R.string.tab_forums))
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                        .setIntent(forumIntent.setAction(Intent.ACTION_VIEW))
                        .build();

                ShortcutInfo logShortcut = new ShortcutInfo.Builder(this, "shortcut_visit")
                        .setShortLabel(getString(R.string.action_page))
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                        .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.BASE_URL)))
                        .build();

                ShortcutInfo opdsShortcut = new ShortcutInfo.Builder(this, "shortcut_opds")
                        .setShortLabel(getString(R.string.action_opds))
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                        .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.OPDS_URL)))
                        .build();

                if (auth_state > 0) {
                    new Thread(() -> shortcutManager.setDynamicShortcuts(Arrays.asList(webShortcut, forumShortcut, logShortcut, opdsShortcut))).start();

                } else {
                    new Thread(() -> shortcutManager.setDynamicShortcuts(Arrays.asList(forumShortcut, logShortcut, opdsShortcut))).start();

                }
            } catch (Throwable ignored) {
            }
        }

        if (auth_state > 0) {

            // обновляем счетчик лс
            status.setImageResource(R.drawable.ic_status_green);
            Login_Name.setText(getString(R.string.sign_as));
            Login_Name.append(login_name);
            View view = this.getWindow().getDecorView().getRootView();
            //  UpdatePm.update(this, view);

        } else {
            Login_Name.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        // обновление личных данных после авторизации
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.INTENT_AUTH);
        filter.addAction(Config.INTENT_NEW_PM);
        filter.addAction(Config.INTENT_READ_PM);
        filter.addAction(Config.INTENT_DELETE_PM);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((intent != null) && (Objects.equals(intent.getAction(), Config.INTENT_AUTH))) {
                    Log.e(Config.TAG, "Auth broadcast");
                    String auth_name = AppController.getInstance().userName(getString(R.string.nav_header_title));
                    String is_pm = AppController.getInstance().isPm();
                    String image_url = AppController.getInstance().imageUrl();
                    Glide.with(getApplicationContext())
                            .load(image_url)
                            .apply(RequestOptions.circleCropTransform())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(avatar);
                    status.setImageResource(R.drawable.ic_status_green);
                    Login_Name.setText(getString(R.string.sign_as));
                    Login_Name.append(auth_name);
                    avatar.setOnClickListener(v -> ButtonsActions.loadProfile(context, auth_name, image_url));
                    if ((!is_pm.equals("off")) && (binding != null))
                        binding.appBarMain.fab.setVisibility(View.VISIBLE);
                }
            }
        }, filter);

        // скрываем пункты бокового меню
        if (!is_uploader) navigationView.getMenu().removeItem(R.id.nav_uploader);
        if (!is_android) navigationView.getMenu().removeItem(R.id.nav_android);
        if (!is_news) navigationView.getMenu().removeItem(R.id.nav_news);
        if (!is_gallery) navigationView.getMenu().removeItem(R.id.nav_gallery);
        if (!is_vuploader) navigationView.getMenu().removeItem(R.id.nav_vuploader);
        if (!is_muzon) navigationView.getMenu().removeItem(R.id.nav_muzon);
        if (!is_books) navigationView.getMenu().removeItem(R.id.nav_books);
        if (!is_articles) navigationView.getMenu().removeItem(R.id.nav_articles);
        if (!is_forum) navigationView.getMenu().removeItem(R.id.nav_forum);
        if (!is_tracker) navigationView.getMenu().removeItem(R.id.nav_tracker);
        if (!is_blog) navigationView.getMenu().removeItem(R.id.nav_blog);
        if (!is_suploader) navigationView.getMenu().removeItem(R.id.nav_suploader);
        if ((is_pm.equals("off")) || (auth_state != 1))
            navigationView.getMenu().removeItem(R.id.nav_pm);
        if ((auth_state != 1) || (!is_add_file)) navigationView.getMenu().removeItem(R.id.nav_add);

        // открытие личных сообщений
        if ((is_pm.equals("off")) || (auth_state != 1))
            binding.appBarMain.fab.setVisibility(View.GONE);


        // billing init
        if (!getCurrentLanguage().equals("ru")) PurchaseHelper.init(this);

        // открываем лс из уведомления
        Intent intent_pm = getIntent();
        if (intent_pm != null) {

            String action = intent_pm.getStringExtra("action");

            Log.e(Config.TAG, "Main intent: " + action);

            if (action != null) {
                if (Objects.equals(action, "PmFragment")) {
                    navigationView.post(() -> navController.navigate(R.id.nav_pm));
                }
                if (Objects.equals(action, "ForumFragment")) {
                    navigationView.post(() -> navController.navigate(R.id.nav_forum));
                }
            }
        }
        // написание личных сообщений
        FloatingActionButton fab = MainActivity.binding.appBarMain.fab;
        TextView fabBage = MainActivity.binding.appBarMain.fabBadge;
        fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.baseline_mail_24));
        fab.setOnClickListener(view -> {
            fabClick();
        });
        fabBage.setOnClickListener(view -> {
            fabClick();
        });

        Toolbar toolbar = binding.appBarMain.toolbar;
        NetworkUtils.loadAvatar(this, toolbar);

        // проверка разрешений
        if ((is_pm.equals("on")) || (auth_state == 1)) {
            mRequestPermissionHandler = new RequestPermissionHandler();
            handlePerm();
        }

        // калбэк загрузки изображений
        pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                          //  Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 720, 720, false);
                            Matrix matrix = new Matrix();
                            matrix .setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, 720, 720), Matrix.ScaleToFit.CENTER);
                            Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            NetworkUtils.uploadBitmap(scaledBitmap, this, razdel);
                            Log.e("---", "Main pickMedia: "+razdel);
                        } catch (Exception ignored) {
                        }
                    }
                });
    }


    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // вызов личных сообщений по кнопке
    public void fabClick() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        navigationView.post(() -> navController.navigate(R.id.nav_pm));

    }

    // масштабирование шрифтов
    private void adjustFontScale(Configuration configuration) {
        final String scale = AppController.getInstance().scaleFont();
        configuration.fontScale = Float.parseFloat(scale);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        assert wm != null;
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {

        razdel = event.razdel;
        String count_pm = event.count_pm;
        TextView fab_badge;
        if ((count_pm != null)) {
            fab_badge = binding.appBarMain.fabBadge;
            fab_badge.setVisibility(View.VISIBLE);
            fab_badge.setText(count_pm);
        }
        if ((count_pm == null) || (count_pm.equals("0"))) {
            fab_badge = binding.appBarMain.fabBadge;
            fab_badge.setVisibility(View.GONE);
        }

        Log.e("---", "Main activity pm recieved: "+count_pm);
        Log.e("---", "Main activity razdel: "+razdel);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        // search
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        assert searchView != null;
        final EditText searchEditText = searchView.findViewById(R.id.search_src_text);

        searchEditText.setHint(getString(R.string.search));

        searchEditText.setHintTextColor(ContextCompat.getColor(this, R.color.list_row_hover_end_color));
        assert searchManager != null;
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(500);

        searchEditText.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                FragmentManager fragmentManager = getSupportFragmentManager();

                homeFrag = new MainFragmentContent();
                if (razdel.equals("8")) homeFrag = new ForumFragmentTopics(); // forum
                if (razdel.equals("13")) homeFrag = new PmMembersFragment(); // pm

                Bundle bundle = new Bundle();
                String story = searchEditText.getText().toString().trim();
                if (TextUtils.isEmpty(story)) story = null;
                bundle.putSerializable(Config.TAG_STORY, story);
                bundle.putString(Config.TAG_CATEGORY, razdel);
                homeFrag.setArguments(bundle);

                fragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, homeFrag)
                        .commit();
            }
            return false;
        });

        return true;
    }


    // menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        // settings
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return true;
        }

        // refresh
        if (id == R.id.action_refresh) {
            FragmentManager fragmentManager = getSupportFragmentManager();

            homeFrag = new MainFragment();

            EventBus.getDefault().postSticky(new MessageEvent(razdel, null, null, null, null, null));

            if (razdel != null) {
                if (razdel.equals("8")) homeFrag = new ForumFragmentTopics(); // forum
                if (razdel.equals("13")) homeFrag = new PmMembersFragment(); // pm
                if (razdel.equals("0")) homeFrag = new MainFragmentAddFile(); // pm
            }

            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFrag)
                    .addToBackStack(null)
                    .commit();



        }

        // mark all read
        if (id == R.id.action_mark) {

            String key = GetRazdelName.getRazdelName(razdel, 0);

            Provider.markAllRead(key);
            Toast.makeText(this, this.getString(R.string.success), Toast.LENGTH_LONG).show();


            FragmentManager fragmentManager = getSupportFragmentManager();

            homeFrag = new MainFragment();
            if (razdel != null) {
                if (razdel.equals("8")) homeFrag = new ForumFragmentTopics(); // forum
                if (razdel.equals("13")) homeFrag = new PmMembersFragment(); // pm
            }
            EventBus.getDefault().postSticky(new MessageEvent(razdel, null, null, null, null, null));

            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFrag)
                    .addToBackStack(null)
                    .commit();
        }

        // other apps
        if (id == R.id.action_others) {

            String url = "https://play.google.com/store/apps/dev?id=6091758746633814135";

            if (BuildConfig.FLAVOR.equals("DVClientSamsung"))
                url = "https://dimonvideo.ru/android.html";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));

            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }

        // rate app
        if (id == R.id.action_rate) {

            String url = Config.GOOGLE_PLAY_RATE_URL;
            if (BuildConfig.SAMSUNG) url = Config.SAMSUNG_RATE_URL;
            if (BuildConfig.HUAWEI) url = Config.HUAWEI_RATE_URL;
            if (BuildConfig.NASHSTORE) url = Config.NASHSTORE_RATE_URL;
            if (BuildConfig.RUSTORE) url = Config.RUSTORE_RATE_URL;

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));

            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }

        // feedback
        if (id == R.id.action_feedback) {

            Intent selectorIntent = new Intent(Intent.ACTION_SENDTO);
            selectorIntent.setData(Uri.parse("mailto:"));

            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.app_mail)});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Feedback");
            emailIntent.setSelector(selectorIntent);

            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.app_name)));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.share_no_email_handler_found), Toast.LENGTH_SHORT).show();
            }
        }

        // clear cache
        if (id == R.id.nav_clear_cache) {
            new Thread(() -> Glide.get(MainActivity.this).clearDiskCache()).start();
            Provider.clearDB();
            Toast.makeText(this, this.getString(R.string.clear_cache_success), Toast.LENGTH_LONG).show();
            return true;

        }

        // donate
        if (id == R.id.action_donate) {

            String url = Config.BASE_URL + "/reklama.php";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));


            if (!BuildConfig.GOOGLE) {
                try {
                    startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
            } else {
                if (getCurrentLanguage().equals("ru")) {
                    try {
                        startActivity(browserIntent);
                    } catch (Throwable ignored) {
                    }
                } else {
                    try {

                        PurchaseHelper.doPurchase(MainActivity.this);

                    } catch (Throwable ignored) {
                    }
                }
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

        // exit
        if (id == R.id.nav_exit) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        AdapterMainRazdel.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // is new version
    public boolean appWasUpdated(Context context) throws PackageManager.NameNotFoundException {
        PackageManager manager = context.getPackageManager();
        PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
        int versionCode = info.versionCode;
        if (AppController.getInstance().isVersionCode() != versionCode) {
            AppController.getInstance().putVersionCode(versionCode);
            return true;
        }
        return false;
    }

    private void handlePerm() {
        mRequestPermissionHandler.requestPermission(this, permissions(), 123, new RequestPermissionHandler.RequestPermissionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailed() {
                Toast.makeText(MainActivity.this, getString(R.string.perm_invalid), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getCurrentLanguage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return LocaleList.getDefault().get(0).getLanguage();
        } else {
            return Locale.getDefault().getLanguage();
        }
    }

    public static String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static String[] permissions_33 = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS
    };

    public static String[] permissions() {
        String[] p;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            p = permissions_33;
        } else {
            p = permissions;
        }
        return p;
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            finish();
            return;
        }
        navController.navigate(R.id.nav_home);
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, getString(R.string.press_twice), Toast.LENGTH_SHORT).show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce=false, 2000);
    }

}
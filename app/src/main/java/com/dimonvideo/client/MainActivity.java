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
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.dimonvideo.client.databinding.ActivityMainBinding;
import com.dimonvideo.client.ui.forum.ForumFragmentTopics;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.ui.main.MainFragmentAddFile;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.ui.pm.PmFragmentMembers;
import com.dimonvideo.client.ui.pm.PmFragmentTabs;
import com.dimonvideo.client.util.Analytics;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.GetRazdelName;
import com.dimonvideo.client.util.ImageUtils;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    public static ActivityMainBinding binding;
    private NavigationView navigationView;
    private NavController navController;
    private TextView fab_badge;
    private BroadcastReceiver authReceiver;
    private final String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private String razdel = "10";
    private Fragment homeFrag;
    private AppBarConfiguration mAppBarConfiguration;
    private AppController controller;
    private ActivityResultLauncher<String[]> permissionLauncher;


    private String[] permissions() {
        String[] p;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            p = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            p = permissions;
        }
        return p;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        controller = AppController.getInstance();
        Analytics.init(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        // очистка старых записей
        controller.getExecutor().execute(() -> controller.getDatabase().feedDao().clearDBOld());

        final String main_razdel = controller.mainRazdel();
        final String image_url = controller.imageUrl();
        final String login_name = controller.userName(getString(R.string.nav_header_title));
        final String is_pm = controller.isPm();
        final int auth_state = controller.isAuth();
        final boolean is_pm_notify = controller.isPmNotify();

        final boolean is_uploader = controller.isUploader();
        final boolean is_android = controller.isAndroid();
        final boolean is_vuploader = controller.isVuploader();
        final boolean is_news = controller.isUsernews();
        final boolean is_gallery = controller.isGallery();
        final boolean is_muzon = controller.isMuzon();
        final boolean is_books = controller.isBooks();
        final boolean is_articles = controller.isArticles();
        final boolean is_forum = controller.isForum();
        final boolean is_tracker = controller.isTracker();
        final boolean is_blog = controller.isBlog();
        final boolean is_suploader = controller.isSuploader();
        final boolean is_device = controller.isDevice();
        final boolean is_add_file = controller.isAddFile();
        final boolean is_new_files = controller.isNewFiles();

        fab_badge = binding.appBarMain.fabBadge;

        controller.adjustFontScale(this);

        setSupportActionBar(binding.appBarMain.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // информация об обновлении
        try {
            if (appWasUpdated(this)) {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.whats_new_title))
                        .setMessage(getString(R.string.whats_new_text))
                        .setNegativeButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                        .setIcon(R.mipmap.ic_launcher_round)
                        .show();
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        DrawerLayout drawerLayout = binding.drawerLayout;
        navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_new, R.id.nav_forum, R.id.nav_news,
                R.id.nav_vuploader, R.id.nav_muzon, R.id.nav_books, R.id.nav_uploader,
                R.id.nav_android, R.id.nav_articles, R.id.nav_tracker, R.id.nav_blog, R.id.nav_suploader, R.id.nav_device)
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
            if (Integer.parseInt(main_razdel) == 17) navGraph.setStartDestination(R.id.nav_device);
            if (Integer.parseInt(main_razdel) == 18) navGraph.setStartDestination(R.id.nav_new);
            navController.setGraph(navGraph);
        });

        ImageView status = navigationView.getHeaderView(0).findViewById(R.id.status);
        status.setImageResource(R.drawable.ic_status_gray);
        TextView Login_Name = navigationView.getHeaderView(0).findViewById(R.id.login_string);
        ImageView avatar = navigationView.getHeaderView(0).findViewById(R.id.avatar);
        ImageView setting_icon = navigationView.getHeaderView(0).findViewById(R.id.settings_icon);
        ImageView exit_icon = navigationView.getHeaderView(0).findViewById(R.id.exit_icon);
        ImageView theme_icon = navigationView.getHeaderView(0).findViewById(R.id.theme_icon);
        TextView app_version = navigationView.getHeaderView(0).findViewById(R.id.app_version);

        // иконка exit
        exit_icon.setOnClickListener(view -> finish());

        // иконка настроек
        setting_icon.setOnClickListener(view -> {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // иконка темы
        theme_icon.setOnClickListener(view -> {
            try {
                String currentTheme = controller.isDark();
                if (currentTheme.equals("yes")) {
                    controller.putThemeLight();
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else {
                    controller.putThemeDark();
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                recreate();
            } catch (Exception ignored) {}
        });

        // слушаем изменение темы в настройках
        controller.getSharedPreferences().registerOnSharedPreferenceChangeListener((prefs, key) -> {
            if ("dvc_theme_list".equals(key)) {
                controller.applyTheme();
                recreate();
            }
        });

        // иконка меню
        if (auth_state > 0) {
            // загрузка аватара пользователя
            Glide.with(this)
                    .load(image_url)
                    .apply(RequestOptions.circleCropTransform())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(avatar);
            avatar.setOnClickListener(v -> ButtonsActions.loadProfile(this, login_name, image_url));
        }

        // быстрые ярлыки
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcuts = new ArrayList<>();

        // Ярлык для личных сообщений (только для авторизованных с включенными уведомлениями)
        if (auth_state > 0 && is_pm_notify) {
            Intent pmIntent = new Intent(this, MainActivity.class);
            pmIntent.setAction(Intent.ACTION_MAIN);
            pmIntent.putExtra("action", "PmFragmentTabs");
            pmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            ShortcutInfo pmShortcut = new ShortcutInfo.Builder(this, "shortcut_pm")
                    .setShortLabel(getString(R.string.tab_pm))
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(pmIntent)
                    .build();
            shortcuts.add(pmShortcut);
        }

        // Ярлык для форума
        Intent forumIntent = new Intent(this, MainActivity.class);
        forumIntent.setAction(Intent.ACTION_MAIN);
        forumIntent.putExtra("action", "ForumFragment");
        forumIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        ShortcutInfo forumShortcut = new ShortcutInfo.Builder(this, "shortcut_forum")
                .setShortLabel(getString(R.string.tab_forums))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(forumIntent)
                .build();
        shortcuts.add(forumShortcut);

        // Ярлык для веб-страницы
        ShortcutInfo webShortcut = new ShortcutInfo.Builder(this, "shortcut_visit")
                .setShortLabel(getString(R.string.action_page))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.WRITE_URL)))
                .build();
        shortcuts.add(webShortcut);

        // Ярлык для OPDS
        ShortcutInfo opdsShortcut = new ShortcutInfo.Builder(this, "shortcut_opds")
                .setShortLabel(getString(R.string.action_opds))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.OPDS_URL)))
                .build();
        shortcuts.add(opdsShortcut);

        controller.getExecutor().execute(() -> shortcutManager.setDynamicShortcuts(shortcuts));

        if (auth_state > 0) {
            // обновляем счетчик лс
            status.setImageResource(R.drawable.ic_status_green);
            Login_Name.setText(getString(R.string.sign_as));
            Login_Name.append(login_name);
            View view = this.getWindow().getDecorView().getRootView();
            // UpdatePm.update(this, view);
        } else {
            Login_Name.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
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

        authReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;
                switch (intent.getAction()) {
                    case Config.INTENT_AUTH:
                        Log.d(Config.TAG, "Auth broadcast");
                        String auth_name = controller.userName(getString(R.string.nav_header_title));
                        String is_pm = controller.isPm();
                        String image_url = controller.imageUrl();
                        Glide.with(context)
                                .load(image_url)
                                .apply(RequestOptions.circleCropTransform())
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerCrop()
                                .into(avatar);
                        status.setImageResource(R.drawable.ic_status_green);
                        Login_Name.setText(getString(R.string.sign_as));
                        Login_Name.append(auth_name);
                        avatar.setOnClickListener(v -> ButtonsActions.loadProfile(context, auth_name, image_url));
                        if (!is_pm.equals("off") && binding != null) {
                            binding.appBarMain.fab.setVisibility(View.VISIBLE);
                        }
                        updateShortcuts();
                        break;
                    case Config.INTENT_NEW_PM:
                    case Config.INTENT_READ_PM:
                    case Config.INTENT_DELETE_PM:
                        int unread = controller.isPmUnread();
                        if (unread > 0) {
                            fab_badge.setVisibility(View.VISIBLE);
                            fab_badge.setText(String.valueOf(unread));
                        } else {
                            fab_badge.setVisibility(View.GONE);
                        }
                        break;
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(authReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(authReceiver, filter);
        }

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
        if (!is_device) navigationView.getMenu().removeItem(R.id.nav_device);
        if (is_pm.equals("off") || auth_state != 1) navigationView.getMenu().removeItem(R.id.nav_pm);
        if (auth_state != 1 || !is_add_file) navigationView.getMenu().removeItem(R.id.nav_add);
        if (!is_new_files) navigationView.getMenu().removeItem(R.id.nav_new);

        // открытие личных сообщений
        if (is_pm.equals("off") || auth_state != 1) binding.appBarMain.fab.setVisibility(View.GONE);

        // открываем лс из уведомления
        Intent intent_pm = getIntent();
        if (intent_pm != null) {
            String action = intent_pm.getStringExtra("action");
            Log.d(Config.TAG, "Main intent: " + action);
            if (action != null) {
                if (Objects.equals(action, "PmFragmentTabs")) {
                    navigationView.post(() -> navController.navigate(R.id.nav_pm));
                }
                if (Objects.equals(action, "ForumFragment")) {
                    navigationView.post(() -> navController.navigate(R.id.nav_forum));
                }
            }
        }

        // открываем раздел из dvadmin
        Intent intent_admin = getIntent();
        if (intent_admin != null) {
            String action_admin = intent_admin.getStringExtra("action_admin");
            if (action_admin != null) {
                if (action_admin.equalsIgnoreCase(Config.UPLOADER_RAZDEL))
                    navigationView.post(() -> navController.navigate(R.id.nav_uploader));
                if (action_admin.equalsIgnoreCase(Config.VUPLOADER_RAZDEL))
                    navigationView.post(() -> navController.navigate(R.id.nav_vuploader));
                if (action_admin.equalsIgnoreCase(Config.NEWS_RAZDEL))
                    navigationView.post(() -> navController.navigate(R.id.nav_news));
                if (action_admin.equalsIgnoreCase(Config.GALLERY_RAZDEL))
                    navigationView.post(() -> navController.navigate(R.id.nav_gallery));
                if (action_admin.equalsIgnoreCase(Config.MUZON_RAZDEL))
                    navigationView.post(() -> navController.navigate(R.id.nav_muzon));
                if (action_admin.equalsIgnoreCase(Config.DEVICE_RAZDEL))
                    navigationView.post(() -> navController.navigate(R.id.nav_device));
            }
        }

        // написание личных сообщений
        FloatingActionButton fab = binding.appBarMain.fab;
        TextView fabBadge = binding.appBarMain.fabBadge;
        fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.baseline_mail_24));
        fab.setOnClickListener(view -> fabClick());
        fabBadge.setOnClickListener(view -> fabClick());

        Toolbar toolbar = binding.appBarMain.toolbar;
        NetworkUtils.loadAvatar(this, toolbar);

        // инициализация лаунчера для разрешений
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allGranted = true;
            for (Boolean granted : result.values()) {
                if (!granted) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                if (shouldShowRequestPermissionRationale()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Разрешения необходимы")
                            .setMessage("Для получения уведомлений о личных сообщениях требуется разрешение. Хотите попробовать снова?")
                            .setPositiveButton("Да", (dialog, which) -> requestPermissions())
                            .setNegativeButton("Нет", (dialog, which) -> dialog.dismiss())
                            .show();
                }
            }
        });

        // проверка разрешений
        if (is_pm.equals("on") && !is_pm_notify) {
            requestPermissions();
        }

        // каллбэк загрузки изображений
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                try {

                    NetworkUtils.uploadBitmap(ImageUtils.loadAndProcessImage(this, uri), this, razdel);

                    Log.d("---", "Main pickMedia: " + razdel);
                } catch (Exception e) {
                    Log.e("MainActivity", "Error processing image", e);
                }
            }
        });

        controller.getExecutor().execute(() -> controller.getDatabase().feedDao().clearDBOld());
    }

    private void requestPermissions() {
        String[] perms = permissions();
        List<String> permsToRequest = new ArrayList<>();
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permsToRequest.add(perm);
            }
        }
        if (!permsToRequest.isEmpty()) {
            permissionLauncher.launch(permsToRequest.toArray(new String[0]));
        }
    }

    private boolean shouldShowRequestPermissionRationale() {
        for (String perm : permissions()) {
            if (shouldShowRequestPermissionRationale(perm)) {
                return true;
            }
        }
        return false;
    }

    private void updateShortcuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcuts = new ArrayList<>();

        // Проверяем авторизацию и уведомления
        int auth_state = controller.isAuth();
        boolean is_pm_notify = controller.isPmNotify();
        String is_pm = controller.isPm();

        if (auth_state > 0 && is_pm_notify && !is_pm.equals("off")) {
            Intent pmIntent = new Intent(this, MainActivity.class);
            pmIntent.setAction(Intent.ACTION_MAIN);
            pmIntent.putExtra("action", "PmFragmentTabs");
            pmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            ShortcutInfo pmShortcut = new ShortcutInfo.Builder(this, "shortcut_pm")
                    .setShortLabel(getString(R.string.tab_pm))
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(pmIntent)
                    .build();
            shortcuts.add(pmShortcut);
        }

        // Ярлык для форума
        Intent forumIntent = new Intent(this, MainActivity.class);
        forumIntent.setAction(Intent.ACTION_MAIN);
        forumIntent.putExtra("action", "ForumFragment");
        forumIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        ShortcutInfo forumShortcut = new ShortcutInfo.Builder(this, "shortcut_forum")
                .setShortLabel(getString(R.string.tab_forums))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(forumIntent)
                .build();
        shortcuts.add(forumShortcut);

        // Ярлык для веб-страницы
        ShortcutInfo webShortcut = new ShortcutInfo.Builder(this, "shortcut_visit")
                .setShortLabel(getString(R.string.action_page))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.WRITE_URL)))
                .build();
        shortcuts.add(webShortcut);

        // Ярлык для OPDS
        ShortcutInfo opdsShortcut = new ShortcutInfo.Builder(this, "shortcut_opds")
                .setShortLabel(getString(R.string.action_opds))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.OPDS_URL)))
                .build();
        shortcuts.add(opdsShortcut);

        shortcutManager.setDynamicShortcuts(shortcuts);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    // вызов личных сообщений по кнопке
    public void fabClick() {
        razdel = "13";
        navigationView.post(() -> navController.navigate(R.id.nav_pm));
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        razdel = event.razdel;
        String count_pm = event.count_pm;
        if (count_pm != null) {
            fab_badge.setVisibility(View.VISIBLE);
            fab_badge.setText(count_pm);
        }
        if (count_pm != null && count_pm.equals("0")) {
            fab_badge.setVisibility(View.GONE);
        }
        Log.d("MainActivity", "Main activity razdel: " + razdel);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        // search
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        if (searchView != null) {
            final EditText searchEditText = searchView.findViewById(R.id.search_src_text);
            searchEditText.setHint(getString(R.string.search));
            searchEditText.setHintTextColor(ContextCompat.getColor(this, R.color.cardview_dark_background));
            if (searchManager != null) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            }
            searchView.setMaxWidth(Integer.MAX_VALUE);

            searchEditText.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    homeFrag = new MainFragmentContent();
                    if (razdel.equals("8")) homeFrag = new ForumFragmentTopics();
                    if (razdel.equals("13")) homeFrag = new PmFragmentMembers();
                    if (razdel.equals("0")) homeFrag = new MainFragmentAddFile();

                    Bundle bundle = new Bundle();
                    String story = searchEditText.getText().toString().trim();
                    if (TextUtils.isEmpty(story)) story = null;
                    bundle.putSerializable(Config.TAG_STORY, story);
                    bundle.putString(Config.TAG_CATEGORY, razdel);
                    homeFrag.setArguments(bundle);

                    fragmentManager.beginTransaction()
                            .add(R.id.nav_host_fragment, homeFrag)
                            .commit();
                }
                return true;
            });
        }
        return true;
    }

    // menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        if (id == R.id.action_refresh) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            homeFrag = new MainFragment();
            EventBus.getDefault().postSticky(new MessageEvent(razdel, null, null, null, null, null));
            if (razdel != null) {
                if (razdel.equals("8")) homeFrag = new ForumFragmentTopics();
                if (razdel.equals("13")) homeFrag = new PmFragmentTabs();
                if (razdel.equals("0")) homeFrag = new MainFragmentAddFile();
            }
            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFrag)
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        if (id == R.id.action_mark) {
            String key = GetRazdelName.getRazdelName(razdel, 0);
            try {
                controller.getExecutor().execute(() -> controller.getDatabase().readMarkDao().markAllRead(key));
            } catch (Throwable ignored) {
            }
            Toast.makeText(this, R.string.success, Toast.LENGTH_LONG).show();
            recreate();
            return true;
        }
        if (id == R.id.action_others) {
            String url = "https://play.google.com/store/apps/dev?id=6091758746633814135";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
            return true;
        }
        if (id == R.id.action_rate) {
            String url = Config.GOOGLE_PLAY_RATE_URL;
            if (BuildConfig.SAMSUNG) url = Config.SAMSUNG_RATE_URL;
            if (BuildConfig.HUAWEI) url = Config.HUAWEI_RATE_URL;
            if (BuildConfig.NASHSTORE) url = Config.NASHSTORE_RATE_URL;
            if (BuildConfig.RUSTORE) url = Config.RUSTORE_RATE_URL;
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
            return true;
        }
        if (id == R.id.action_feedback) {
            String s = getString(R.string.is_info_feedback);
            try {
                s += "\n\nAPI: " + Build.VERSION.SDK_INT;
                s += "\nDevice: " + Build.DEVICE;
                s += "\nModel: " + Build.MODEL + " (" + Build.PRODUCT + ")";
            } catch (Throwable ignored) {
            }
            Intent i = new Intent(Intent.ACTION_SENDTO);
            i.setData(Uri.parse("mailto:"));
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.app_mail)});
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Feedback");
            i.putExtra(Intent.EXTRA_TEXT, "\n\n" + s);
            try {
                startActivity(i);
            } catch (Throwable ignored) {
                Toast.makeText(this, R.string.share_no_email_handler_found, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (id == R.id.nav_clear_cache) {
            new Thread(() -> Glide.get(MainActivity.this).clearDiskCache()).start();
            controller.getExecutor().execute(() -> controller.getDatabase().feedDao().clearDB());
            Toast.makeText(this, R.string.clear_cache_success, Toast.LENGTH_LONG).show();
            return true;
        }
        if (id == R.id.action_donate) {
            String url = Config.WRITE_URL + "/reklama.php";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
            return true;
        }
        if (id == R.id.nav_exit) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        if (authReceiver != null) {
            unregisterReceiver(authReceiver);
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String is_pm = controller.isPm();
        boolean is_pm_notify = controller.isPmNotify();
        if (is_pm.equals("on") && is_pm_notify) {
            requestPermissions();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // Проверка, обновлено ли приложение
    public boolean appWasUpdated(Context context) throws PackageManager.NameNotFoundException {
        PackageManager manager = context.getPackageManager();
        PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
        int versionCode = info.versionCode;
        if (controller.isVersionCode() != versionCode) {
            controller.putVersionCode(versionCode);
            return true;
        }
        return false;
    }
}
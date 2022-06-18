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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.os.StrictMode;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.transition.Transition;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.dimonvideo.client.ui.forum.ForumFragment;
import com.dimonvideo.client.ui.forum.ForumFragmentTopics;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.ui.pm.PmFragment;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.RequestPermissionHandler;
import com.dimonvideo.client.util.Security;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    private AppBarConfiguration mAppBarConfiguration;
    Fragment homeFrag;
    SharedPreferences sharedPrefs;
    static int razdel = 10;
    private RequestPermissionHandler mRequestPermissionHandler;
    private int backpress = 0;


    // ---------- billing ----------------
    BillingClient billingClient;
    List<String> skuList = new ArrayList<>();
    String product = "com.dimonvideo.client_1";
    // ------------------------------------

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean is_uploader = sharedPrefs.getBoolean("dvc_uploader", true);
        final boolean is_android = sharedPrefs.getBoolean("dvc_android", true);
        final boolean is_vuploader = sharedPrefs.getBoolean("dvc_vuploader", true);
        final boolean is_news = sharedPrefs.getBoolean("dvc_news", true);
        final boolean is_gallery = sharedPrefs.getBoolean("dvc_gallery", true);
        final boolean is_muzon = sharedPrefs.getBoolean("dvc_muzon", true);
        final boolean is_books = sharedPrefs.getBoolean("dvc_books", false);
        final boolean is_articles = sharedPrefs.getBoolean("dvc_articles", true);
        final boolean is_forum = sharedPrefs.getBoolean("dvc_forum", true);
        final boolean is_tracker = sharedPrefs.getBoolean("dvc_tracker", true);
        final boolean is_blog = sharedPrefs.getBoolean("dvc_blog", true);
        final String is_pm = sharedPrefs.getString("dvc_pm", "off");
        final String login_name = sharedPrefs.getString("dvc_login", getString(R.string.nav_header_title));
        final String image_url = sharedPrefs.getString("auth_foto", Config.BASE_URL + "/images/noavatar.png");
        final int auth_state = sharedPrefs.getInt("auth_state", 0);
        String main_razdel = sharedPrefs.getString("dvc_main_razdel", "10");
        final String is_dark = sharedPrefs.getString("dvc_theme_list", "false");
        if (is_dark.equals("true")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (is_dark.equals("system")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);
        adjustFontScale(getResources().getConfiguration());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_forum,
                R.id.nav_news,
                R.id.nav_gallery,
                R.id.nav_vuploader,
                R.id.nav_muzon,
                R.id.nav_books,
                R.id.nav_uploader,
                R.id.nav_tracker,
                R.id.nav_cats,
                R.id.nav_android,
                R.id.nav_pminbox,
                R.id.nav_topics,
                R.id.nav_topics_fav,
                R.id.nav_topics_no_posts,
                R.id.nav_fav,
                R.id.nav_articles,
                R.id.nav_blog
        ).setOpenableLayout(drawer).build();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = null;
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            navController.popBackStack(R.id.home, true);

        }

        assert navController != null;
        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.mobile_navigation);

        NavigationUI.setupActionBarWithNavController(MainActivity.this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // main razdel
        assert main_razdel != null;
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

        navController.setGraph(navGraph);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ImageView status = navigationView.getHeaderView(0).findViewById(R.id.status);
        status.setImageResource(R.drawable.ic_status_gray);
        TextView Login_Name = navigationView.getHeaderView(0).findViewById(R.id.login_string);
        ImageView avatar = navigationView.getHeaderView(0).findViewById(R.id.avatar);
        TextView app_version = navigationView.getHeaderView(0).findViewById(R.id.app_version);
        app_version.append(": " + BuildConfig.VERSION_NAME);

        Glide.with(this)
                .load(image_url)
                .apply(RequestOptions.circleCropTransform())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(avatar);

        // home icon
        if (auth_state > 0) {
            avatar.setOnClickListener(v -> ButtonsActions.loadProfile(this, login_name, image_url));
        }

        // быстрые ярлыки
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

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

            assert shortcutManager != null;

            if (auth_state > 0)
                new Thread(() -> shortcutManager.setDynamicShortcuts(Arrays.asList(webShortcut, forumShortcut, logShortcut))).start();
            else
                new Thread(() -> shortcutManager.setDynamicShortcuts(Arrays.asList(forumShortcut, logShortcut))).start();

            // открываем лс из уведомления
            Intent intent_pm = getIntent();
            if (intent_pm != null) {
                Log.e("mainContent", ""+intent_pm.getStringExtra("action"));

                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment PmFragment = new MainFragment();

                try {
                    if (Objects.equals(intent_pm.getStringExtra("action"), "PmFragment")) {
                        PmFragment = new PmFragment();
                    }
                    if (Objects.equals(intent_pm.getStringExtra("action"), "ForumFragment")) {
                        PmFragment = new ForumFragment();
                    }
                    fragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, PmFragment)
                            .addToBackStack(null)
                            .commit();
                } catch (Throwable ignored) {
                }
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
                private final WeakReference<Context> contextRef;

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

        } else {
            Login_Name.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            });
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


        // открытие личных сообщений
        FloatingActionButton fab = findViewById(R.id.fab);
        if ((is_pm.equals("off")) || (auth_state != 1)) fab.setVisibility(View.GONE);
        fab.setOnClickListener(view -> {

            FragmentManager fragmentManager = getSupportFragmentManager();

            Fragment PmFragment = new PmFragment();

            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, PmFragment)
                    .addToBackStack(null)
                    .commit();
        });


        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(receiver, new IntentFilter("com.dimonvideo.client.NEW_PM"));


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




        // ---- billing --------
        billingClient = BillingClient.newBuilder(MainActivity.this).enablePendingPurchases().setListener((billingResult, list) -> {
            if(list != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
            {
                for(Purchase purchase : list)
                {
                    handlePurchase(purchase);
                }
            }
        }).build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
                {
                    Toast.makeText(MainActivity.this, "Successfully connected to Billing Client", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Failed to connect to Google Services", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Toast.makeText(MainActivity.this, "Disconnected from the Billing Client", Toast.LENGTH_SHORT).show();
            }
        });

        // -------------------
        mRequestPermissionHandler = new RequestPermissionHandler();
        handlePerm();

    }

    private boolean isSignatureValid(Purchase purchase) {
        return Security.verifyPurchase(Security.BASE_64_ENCODED_PUBLIC_KEY, purchase.getOriginalJson(), purchase.getSignature());
    }

    public void handlePurchase(Purchase purchase) {
        try {
            if ((purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) && isSignatureValid(purchase)) {
                if (purchase.getSkus().get(0).equals(product)) {

                    ConsumeParams.Builder param = ConsumeParams.newBuilder();
                    param.setPurchaseToken(purchase.getPurchaseToken());
                    billingClient.consumeAsync(param.build(), (billingResult1, s) ->
                            (new Handler(Looper.getMainLooper())).post(()->
                                    Toast.makeText(this, getString(R.string.thanks), Toast.LENGTH_SHORT).show()));
                    if (billingClient!= null) {
                        billingClient.endConnection();
                    }
                }
            }
        }
        catch (Exception e)
        {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();


        }
    }
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null)
        {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
    }

    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases!=null && purchases.size()>0) {
            for(Purchase purchase: purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    ConsumeParams.Builder param = ConsumeParams.newBuilder();
                    param.setPurchaseToken(purchase.getPurchaseToken());
                    billingClient.consumeAsync(param.build(), new ConsumeResponseListener() {
                        @Override
                        public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {

                            (new Handler(Looper.getMainLooper())).post(()->
                                    Toast.makeText(MainActivity.this, getString(R.string.thanks), Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            }
        }
    };


    // масштабирование шрифтов
    private void adjustFontScale(Configuration configuration) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        configuration.fontScale = Float.parseFloat(sharedPrefs.getString("dvc_scale", "1.0f"));
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        assert wm != null;
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    // счетчик сообщений
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
            startActivity(i);
            return true;
        }
        // refresh
        if (id == R.id.action_refresh) {
            FragmentManager fragmentManager = getSupportFragmentManager();

            homeFrag = new MainFragment();

            if (razdel == 8) homeFrag = new ForumFragment(); // forum
            if (razdel == 13) homeFrag = new PmFragment(); // pm

            EventBus.getDefault().postSticky(new MessageEvent(razdel, null));

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
        // feedback
        if (id == R.id.action_feedback) {

            Intent selectorIntent = new Intent(Intent.ACTION_SENDTO);
            selectorIntent.setData(Uri.parse("mailto:"));

            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.putExtra(Intent.EXTRA_EMAIL,  new String[]{getString(R.string.app_mail)});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Feedback");
            emailIntent.setSelector(selectorIntent);

            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.app_name)));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.share_no_email_handler_found), Toast.LENGTH_SHORT).show();
            }
        }

        // donate
        if (id == R.id.action_donate) {

            String url = Config.BASE_URL + "/reklama.php";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));


            if (BuildConfig.FLAVOR.equals("DVClientSamsung")) {
                try {
                    startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
            } else
            {
                if (getCurrentLanguage().equals("ru")) {
                    try {
                        startActivity(browserIntent);
                    } catch (Throwable ignored) {
                    }
                } else {
                    try {

                        skuList.add(product);
                        final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                        billingClient.querySkuDetailsAsync(params.build(), (billingResult, list) -> {
                            if (list != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                for (final SkuDetails skuDetails : list) {
                                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                            .setSkuDetails(skuDetails)
                                            .build();
                                    billingClient.launchBillingFlow(MainActivity.this, flowParams);
                                }
                            }
                        });

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
        EventBus.getDefault().unregister(this);
        try {
            unregisterReceiver(receiver);
        } catch (Throwable ignored) {
        }
        if(billingClient!=null){
            billingClient.endConnection();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.finishActivity(0);
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
    public void onBackPressed(){
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        toolbar.setSubtitle(null);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            finish();
        }
    }

    // is new version
    public boolean appWasUpdated(Context context) throws PackageManager.NameNotFoundException {
        PackageManager manager = context.getPackageManager();
        PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
        int versionCode = info.versionCode;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getInt("last_version_code", 1) != versionCode) {
            prefs.edit().putInt("last_version_code", versionCode).apply();
               return true;
        }
        return false;
    }

    private void handlePerm() {
        mRequestPermissionHandler.requestPermission(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        }, 123, new RequestPermissionHandler.RequestPermissionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailed() {
                Toast.makeText(MainActivity.this, getString(R.string.perm_invalid), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getCurrentLanguage(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return LocaleList.getDefault().get(0).getLanguage();
        } else{
            return Locale.getDefault().getLanguage();
        }
    }
}
package com.dimonvideo.client.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.db.AppDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AppController extends Application {

    private static AppController sInstance;
    private RequestQueue mRequestQueue;
    private static SharedPreferences sharedPrefs;
    private static SharedPreferences.Editor editor;
    public static final String TAG = AppController.class.getSimpleName();
    /** Экземпляр базы данных приложения */
    private AppDatabase db;
    /** Пул потоков для фоновых задач */
    private ExecutorService executor;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        // Инициализация базы данных
        db = AppDatabase.getInstance(this);
        applyTheme();

    }

    public RequestQueue getRequestQueueV() {

        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this);
            mRequestQueue.getCache().clear();
        }

        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(Config.TAG);
        req.setRetryPolicy(new DefaultRetryPolicy(
                6000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        req.setShouldCache(false);
        getRequestQueueV().add(req);
    }

    /**
     * Настраивает масштаб шрифта приложения, пересоздавая активность с новой конфигурацией.
     *
     * @param context Контекст активности или приложения, должен быть экземпляром {@link Activity}.
     */
    public void adjustFontScale(Context context) {
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Context is not an Activity, cannot adjust font scale");
            return;
        }

        Activity activity = (Activity) context;
        Resources resources = context.getResources();
        Configuration currentConfig = resources.getConfiguration();

        try {
            float newFontScale = Float.parseFloat(scaleFont());
            if (newFontScale != currentConfig.fontScale) {
                Log.d(TAG, "Applying new font scale: " + newFontScale);

                // Создаём новую конфигурацию
                Configuration newConfig = new Configuration(currentConfig);
                newConfig.fontScale = newFontScale;

                // Применяем новую конфигурацию через пересоздание активности
                // Сохраняем масштаб в SharedPreferences или другом хранилище
                SharedPreferences prefs = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                prefs.edit().putFloat("font_scale", newFontScale).apply();

                // Пересоздаём активность для применения изменений
                activity.recreate();
            } else {
                Log.d(TAG, "Font scale is already set to: " + newFontScale);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid font scale value: " + scaleFont(), e);
            // Устанавливаем значение по умолчанию
            SharedPreferences prefs = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            prefs.edit().putFloat("font_scale", 1.0f).apply();
            activity.recreate();
        }
    }

    /**
     * Получить экземпляр базы данных приложения.
     * @return Экземпляр AppDatabase
     */
    public synchronized AppDatabase getDatabase() {
        if (db == null) {
            db = AppDatabase.getInstance(this);
            Log.w(Config.TAG, "Database initialized in getDatabase");
        }
        return db;
    }

    /**
     * Получить экземпляр ExecutorService для выполнения фоновых задач.
     * @return Экземпляр ExecutorService
     */
    public ExecutorService getExecutor() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }
        return executor;
    }

    /**
     * Закрыть базу данных приложения.
     * После вызова повторное получение через getDatabase создаст новый экземпляр.
     */
    public void closeDatabase() {
        if (db != null && db.isOpen()) {
            db.close();
            Log.d(TAG, "Database closed");
            db = null; // Устанавливаем в null, чтобы избежать дальнейших обращений
        }
    }

    /**
     * Применить выбранную пользователем тему приложения (тёмная/светлая/системная).
     */
    public void applyTheme() {
        try {
            String darkMode = isDark();
            Log.d(Config.TAG, "Applying theme: " + darkMode);
            switch (darkMode) {
                case "yes":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case "no":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case "system":
                default:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
            }
        } catch (Exception ignored) {}

    }

    // Получение всех настроек
    public Map<String, Object> getAllPreferences() {
        return new HashMap<>(getSharedPreferences().getAll());
    }

    /**
     * Получить глобальный экземпляр AppController.
     * @return Экземпляр AppController
     */
    public static synchronized AppController getInstance() {
        return sInstance;
    }

    /**
     * Получить объект SharedPreferences для хранения настроек приложения.
     * @return Экземпляр SharedPreferences
     */
    public SharedPreferences getSharedPreferences() {
        if (sharedPrefs == null) {
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        }
        return sharedPrefs;
    }

    public static SharedPreferences.Editor putSharedPreferences() {

        if (editor == null) {
            editor = sharedPrefs.edit();
        }

        return editor;
    }


    // ===================================== preferences ========================================================= //
    public String isDark() {
        return getSharedPreferences().getString("dvc_theme_new", "no");
    }

    public String mainRazdel() {
        return getSharedPreferences().getString("dvc_main_razdel", "10");
    }

    public String imageUrl() {
        return getSharedPreferences().getString("auth_foto", Config.WRITE_URL + "/images/noavatar.png");
    }

    public String userPassword() {
        return getSharedPreferences().getString("dvc_password", "null");
    }

    public String userName(String default_name) {
        return getSharedPreferences().getString("dvc_login", default_name);
    }

    public String isPm() {
        return getSharedPreferences().getString("dvc_pm", "off");
    }

    public String scaleFont() {
        return getSharedPreferences().getString("dvc_scale", "1.0f");
    }

    public String isRang() {
        return getSharedPreferences().getString("auth_rang", "---");
    }

    public String isLastDate() {
        return getSharedPreferences().getString("auth_last", "---");
    }

    public String isReputation() {
        return getSharedPreferences().getString("auth_rep", "0");
    }

    public String isRegDate() {
        return getSharedPreferences().getString("auth_reg", "0");
    }

    public String isRating() {
        return getSharedPreferences().getString("auth_rat", "0");
    }

    public String isPosts() {
        return getSharedPreferences().getString("auth_posts", "0");
    }

    public String isToken() {
        return getSharedPreferences().getString("current_token", "null");
    }

    public int isAuth() {
        return getSharedPreferences().getInt("auth_state", 0);
    }

    public int isUserId() {
        return getSharedPreferences().getInt("user_id", 0);
    }
    public int isUserGroup() {
        return getSharedPreferences().getInt("user_group", 4);
    }

    public int isPmUnread() {
        return getSharedPreferences().getInt("pm_unread", 0);
    }

    public int isVersionCode() {
        return getSharedPreferences().getInt("last_version_code", 1);
    }

    public boolean isUploader() {
        return getSharedPreferences().getBoolean("dvc_uploader", true);
    }

    public boolean isAndroid() {
        return getSharedPreferences().getBoolean("dvc_android", false);
    }
    public boolean isNewFiles() {
        return getSharedPreferences().getBoolean("dvc_new_files", false);
    }

    public boolean isVuploader() {
        return getSharedPreferences().getBoolean("dvc_vuploader", true);
    }

    public boolean isUsernews() {
        return getSharedPreferences().getBoolean("dvc_news", true);
    }

    public boolean isGallery() {
        return getSharedPreferences().getBoolean("dvc_gallery", true);
    }

    public boolean isMuzon() {
        return getSharedPreferences().getBoolean("dvc_muzon", true);
    }

    public boolean isBooks() {
        return getSharedPreferences().getBoolean("dvc_books", false);
    }

    public boolean isArticles() {
        return getSharedPreferences().getBoolean("dvc_articles", true);
    }

    public boolean isForum() {
        return getSharedPreferences().getBoolean("dvc_forum", true);
    }

    public boolean isTracker() {
        return getSharedPreferences().getBoolean("dvc_tracker", false);
    }

    public boolean isBlog() {
        return getSharedPreferences().getBoolean("dvc_blog", true);
    }

    public boolean isSuploader() {
        return getSharedPreferences().getBoolean("dvc_suploader", false);
    }

    public boolean isDevice() {
        return getSharedPreferences().getBoolean("dvc_device", false);
    }

    public boolean isAspectRatio() {
        return getSharedPreferences().getBoolean("dvc_vuploader_aspect", false);
    }

    public boolean isExternalPlayer() {
        return getSharedPreferences().getBoolean("dvc_external_video", false);
    }

    public boolean isDVGET() {
        return getSharedPreferences().getBoolean("dvc_dvget", false);
    }

    public boolean isIDM() {
        return getSharedPreferences().getBoolean("dvc_idm", false);
    }

    public boolean isPmNotify() {
        return getSharedPreferences().getBoolean("dvc_pm_notify", false);
    }

    public boolean isOpenLinks() {
        return getSharedPreferences().getBoolean("dvc_open_link", false);
    }

    public boolean isOnTop() {
        return getSharedPreferences().getBoolean("dvc_ontop", true);
    }

    public boolean isOnTopMark() {
        return getSharedPreferences().getBoolean("dvc_ontop_mark", false);
    }

    public boolean isVuploaderPlayListtext() {
        return getSharedPreferences().getBoolean("dvc_vuploader_play_listtext", false);
    }

    public boolean isVuploaderPlay() {
        return getSharedPreferences().getBoolean("dvc_vuploader_play", true);
    }

    public boolean isMuzonPlay() {
        return getSharedPreferences().getBoolean("dvc_muzon_play", true);
    }

    public boolean isShareBtn() {
        return getSharedPreferences().getBoolean("dvc_btn_share", false);
    }

    public boolean isTabsInline() {
        return getSharedPreferences().getBoolean("dvc_tab_inline", false);
    }

    public boolean isTopicsNoPosts() {
        return getSharedPreferences().getBoolean("dvc_tab_topics_no_posts", false);
    }

    public boolean isTabFavor() {
        return getSharedPreferences().getBoolean("dvc_favor", false);
    }

    public boolean isTabIcons() {
        return getSharedPreferences().getBoolean("dvc_tab_icons", true);
    }

    public boolean isMore() {
        return getSharedPreferences().getBoolean("dvc_more", false);
    }
    public boolean isMoreOdob() {
        return getSharedPreferences().getBoolean("dvc_more_odob", false);
    }

    public boolean isCommentTab() {
        return getSharedPreferences().getBoolean("dvc_comment", false);
    }

    public boolean isPmOutbox() {
        return getSharedPreferences().getBoolean("dvc_pm_outbox", true);
    }

    public boolean isPmArchive() {
        return getSharedPreferences().getBoolean("dvc_pm_arc", false);
    }

    public boolean isAddFile() {
        return getSharedPreferences().getBoolean("dvc_add_file", true);
    }
    public boolean isOpros() {
        return getSharedPreferences().getBoolean("dvc_add_vote", true);
    }

    // =============================================== put preferences ================================================================== //


    public void putThemeLight() {
        putSharedPreferences().putString("dvc_theme_new", "no").apply();
    }

    public void putThemeDark() {
        putSharedPreferences().putString("dvc_theme_new", "yes").apply();
    }

    public void putToken(String token) {
        putSharedPreferences().putString("current_token", token).apply();
    }

    public void putImage(String image) {
        putSharedPreferences().putString("auth_foto", image).apply();
    }

    public void putRang(String status) {
        putSharedPreferences().putString("auth_rang", status).apply();
    }

    public void putLastDate(String lastdate) {
        putSharedPreferences().putString("auth_last", lastdate).apply();
    }

    public void putReputation(String rep) {
        putSharedPreferences().putString("auth_rep", rep).apply();
    }

    public void putRegDate(String reg) {
        putSharedPreferences().putString("auth_reg", reg).apply();
    }

    public void putRating(String rat) {
        putSharedPreferences().putString("auth_rat", rat).apply();
    }

    public void putPosts(String posts) {
        putSharedPreferences().putString("auth_posts", posts).apply();
    }

    public void putVersionCode(int versionCode) {
        putSharedPreferences().putInt("last_version_code", versionCode).apply();
    }

    public void putAuthState(int state) {
        putSharedPreferences().putInt("auth_state", state).apply();
    }

    public void putUserId(int uid) {
        putSharedPreferences().putInt("user_id", uid).apply();
    }

    public void putUserGroup(int user_group) {
        putSharedPreferences().putInt("user_group", user_group).apply();
    }

    public void putPmUnread(int pm_unread) {
        putSharedPreferences().putInt("pm_unread", pm_unread).apply();
    }

    /**
     * Метод жизненного цикла приложения. Вызывается при завершении работы приложения.
     * Закрывает базу данных, завершает ExecutorService и останавливает TTS.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        closeDatabase(); // Закрываем базу данных при завершении приложения

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        Log.w(Config.TAG, "AppController terminated");
    }

}

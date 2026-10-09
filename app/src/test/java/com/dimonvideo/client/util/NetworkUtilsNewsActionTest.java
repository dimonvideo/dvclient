package com.dimonvideo.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.FrameLayout;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterMainRazdel;
import com.dimonvideo.client.db.AppDatabase;
import com.dimonvideo.client.model.Feed;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Reproduces the Play Android 16 menu crash and detached-screen moderation callbacks without a server. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, application = NetworkUtilsNewsActionTest.TestApp.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class NetworkUtilsNewsActionTest {
    private TestApp application;
    private SharedPreferences preferences;
    private ActivityController<AppCompatActivity> activity;
    private AppDatabase database;
    private AdapterMainRazdel adapter;
    private boolean hostDestroyed;

    /** Installs isolated account storage and a themed host while leaving the legacy screen binding absent. */
    @Before
    public void setUp() {
        application = (TestApp) RuntimeEnvironment.getApplication();
        preferences = application.getSharedPreferences();
        preferences.edit().clear().commit();
        MainActivity.binding = null;
        ShadowToast.reset();
        activity = Robolectric.buildActivity(AppCompatActivity.class);
        activity.get().setTheme(R.style.AppTheme);
        activity.setup();
        database = Room.inMemoryDatabaseBuilder(application, AppDatabase.class)
                .allowMainThreadQueries().build();
    }

    /** Releases adapter workers before its real SQLite store and destroys each test's window once. */
    @After
    public void tearDown() throws InterruptedException {
        if (adapter != null) adapter.cleanup();
        application.executor.shutdown();
        assertTrue(application.executor.awaitTermination(5, TimeUnit.SECONDS));
        database.close();
        if (application.latestRequest != null) application.latestRequest.cancel();
        MainActivity.binding = null;
        if (!hostDestroyed) activity.pause().stop().destroy();
    }

    /** Tapping the actual last card action while signed out reports invalid login instead of the Play crash. */
    @Test
    public void anonymousNewsMenuTapRejectsWithoutRequestOrNullContextCrash() {
        selectNewsAction("vuploader", 49);

        assertNull(application.latestRequest);
        assertEquals(application.getString(R.string.login_invalid), ShadowToast.getTextOfLatestToast());
    }

    /** Existing callers of the old news API also receive a safe rejection when no Activity binding exists. */
    @Test
    public void legacyNewsCallRejectsMissingLoginWithoutScreenBinding() {
        NetworkUtils.putToNews("comments", 82);

        assertNull(application.latestRequest);
        assertEquals(application.getString(R.string.login_invalid), ShadowToast.getTextOfLatestToast());
    }

    /** The approval endpoint shared the same null validation context and must reject missing passwords safely. */
    @Test
    public void legacyApprovalCallRejectsMissingPasswordWithoutScreenBinding() {
        preferences.edit().putString("dvc_login", "Alice").commit();
        NetworkUtils.getOdob("articles", 73);

        assertNull(application.latestRequest);
        assertEquals(application.getString(R.string.password_invalid), ShadowToast.getTextOfLatestToast());
    }

    /** An explicit screen context preserves password validation and never queues a rejected menu action. */
    @Test
    public void newsMenuTapRejectsShortPassword() {
        saveAccount("Alice", "1234");
        selectNewsAction("vuploader", 49);

        assertNull(application.latestRequest);
        assertEquals(application.getString(R.string.password_invalid), ShadowToast.getTextOfLatestToast());
    }

    /** A valid card action keeps its exact section and ID, with URL-safe credentials and an Activity-free reply. */
    @Test
    public void validNewsMenuRequestAndLateSuccessDoNotDependOnActivityBinding() throws JSONException {
        saveAccount("Alice Admin", "p+ass&word");
        selectNewsAction("vuploader", 49);
        JsonArrayRequest request = latestRequest();
        assertRequest(request, "22", "vuploader", "49", "Alice Admin", "p+ass&word");

        destroyHost();
        deliver(request, "Moved to news");

        assertEquals("Moved to news", ShadowToast.getTextOfLatestToast());
    }

    /** A queued approval remains safe when its screen is gone and the server reports a network timeout. */
    @Test
    public void validApprovalLateNetworkErrorReportsWithoutScreenBinding() {
        saveAccount("Alice", "valid-password");
        NetworkUtils.getOdob(activity.get(), "articles", 73);
        JsonArrayRequest request = latestRequest();
        assertRequest(request, "19", "articles", "73", "Alice", "valid-password");

        destroyHost();
        request.deliverError(new TimeoutError());

        assertEquals(application.getString(R.string.error_network_timeout),
                ShadowToast.getTextOfLatestToast());
    }

    /** The compatibility news entry point still submits and handles a valid result without any live screen. */
    @Test
    public void legacyNewsValidAccountWorksWithoutScreenBinding() throws JSONException {
        saveAccount("Alice", "valid-password");
        NetworkUtils.putToNews("comments", 82);
        JsonArrayRequest request = latestRequest();
        assertRequest(request, "22", "comments", "82", "Alice", "valid-password");

        destroyHost();
        deliver(request, "Accepted");

        assertEquals("Accepted", ShadowToast.getTextOfLatestToast());
    }

    /** The compatibility approval entry point also submits and handles a result without the static binding. */
    @Test
    public void legacyApprovalValidAccountWorksWithoutScreenBinding() throws JSONException {
        saveAccount("Alice", "valid-password");
        NetworkUtils.getOdob("articles", 73);
        JsonArrayRequest request = latestRequest();
        assertRequest(request, "19", "articles", "73", "Alice", "valid-password");

        destroyHost();
        deliver(request, "Approved");

        assertEquals("Approved", ShadowToast.getTextOfLatestToast());
    }

    /** Opens the production adapter menu, then invokes its actual ListView news item listener. */
    private void selectNewsAction(String section, int id) {
        Feed feed = new Feed();
        feed.setRazdel(section);
        feed.setId(id);
        feed.setTitle("Test file");
        adapter = new AdapterMainRazdel(Collections.singletonList(feed), activity.get(), activity.get(), database);
        AdapterMainRazdel.ViewHolder holder = adapter.onCreateViewHolder(new FrameLayout(activity.get()), 0);
        ReflectionHelpers.callInstanceMethod(adapter, "show_dialog",
                ReflectionHelpers.ClassParameter.from(AdapterMainRazdel.ViewHolder.class, holder),
                ReflectionHelpers.ClassParameter.from(int.class, 0));
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        ListView list = dialog.getListView();
        int newsPosition = list.getAdapter().getCount() - 1;
        assertEquals(activity.get().getString(R.string.put_to_news), list.getAdapter().getItem(newsPosition));
        list.performItemClick(list.getAdapter().getView(newsPosition, null, list), newsPosition,
                list.getAdapter().getItemId(newsPosition));
    }

    /** Stores the account values read by the production legacy request encoder. */
    private void saveAccount(String login, String password) {
        preferences.edit().putString("dvc_login", login).putString("dvc_password", password).commit();
    }

    /** Returns the real queued Volley request with its production listeners intact. */
    private JsonArrayRequest latestRequest() {
        assertNotNull(application.latestRequest);
        assertTrue(application.latestRequest instanceof JsonArrayRequest);
        return (JsonArrayRequest) application.latestRequest;
    }

    /** Verifies endpoint routing and decoded credentials rather than mirroring URL concatenation details. */
    private static void assertRequest(JsonArrayRequest request, String operation, String section,
                                      String id, String login, String password) {
        Uri url = Uri.parse(request.getUrl());
        assertEquals(operation, url.getQueryParameter("op"));
        assertEquals(section, url.getQueryParameter("razdel"));
        assertEquals(id, url.getQueryParameter("lid"));
        assertEquals(login, url.getQueryParameter("u"));
        assertEquals(password, url.getQueryParameter("p"));
    }

    /** Delivers the documented moderation response through Volley's protected production callback. */
    private static void deliver(JsonArrayRequest request, String title) throws JSONException {
        JSONArray response = new JSONArray().put(new JSONObject().put("title", title));
        ReflectionHelpers.callInstanceMethod(request, "deliverResponse",
                ReflectionHelpers.ClassParameter.from(Object.class, response));
    }

    /** Destroys the screen before network completion to expose any retained static view dependency. */
    private void destroyHost() {
        activity.pause().stop().destroy();
        hostDestroyed = true;
        MainActivity.binding = null;
    }

    /** Supplies real isolated preferences, a bounded adapter worker and captured requests without startup side effects. */
    public static class TestApp extends AppController {
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private Request<?> latestRequest;

        /** Installs the singleton required by the adapter and public network APIs without Firebase initialization. */
        @Override
        public void onCreate() { ReflectionHelpers.setStaticField(AppController.class, "sInstance", this); }

        /** Uses this test application's preference file instead of a previous run's cached instance. */
        @Override
        public SharedPreferences getSharedPreferences() {
            return PreferenceManager.getDefaultSharedPreferences(this);
        }

        /** Supplies a test-owned executor whose pending Room reads are joined before the database closes. */
        @Override
        public ExecutorService getExecutor() { return executor; }

        /** Captures the production request without contacting the server or replacing its callbacks. */
        @Override
        public <T> void addToRequestQueue(Request<T> request) { latestRequest = request; }
    }
}

package com.dimonvideo.client.ui.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterMainRazdel;
import com.dimonvideo.client.db.AppDatabase;
import com.dimonvideo.client.db.FeedEntity;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.feed.FeedQuery;

import org.greenrobot.eventbus.EventBus;
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
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/** Exercises the real feed, Room cache and Volley callbacks with deterministic disk/network ordering. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 35, application = MainFragmentContentTest.TestApp.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class MainFragmentContentTest {
    private TestApp app;
    private ActivityController<AppCompatActivity> host;
    private MainFragmentContent fragment;

    /** Creates an isolated database and a themed fragment host without starting application services. */
    @Before
    public void setUp() {
        app = (TestApp) RuntimeEnvironment.getApplication();
        app.getSharedPreferences().edit().clear().commit();
        MainActivity.binding = null;
        EventBus.getDefault().removeAllStickyEvents();
        host = Robolectric.buildActivity(AppCompatActivity.class);
        host.get().setTheme(R.style.AppTheme);
        host.setup();
    }

    /** Invalidates the view before finishing queued work and closes every test-owned database. */
    @After
    public void tearDown() {
        host.pause().stop().destroy();
        drainDisk();
        app.getDatabase().close();
        app.executor.shutdownNow();
        EventBus.getDefault().removeAllStickyEvents();
        MainActivity.binding = null;
    }

    /** A failed launch restores the requested legacy comments section instead of leaving an empty adapter. */
    @Test
    public void volleyFailureRestoresLegacyCommentsAndShowsSavedNotice() {
        legacy("comments", 71, "<b>Saved comment</b>");
        legacy("usernews", 72, "Wrong section");
        open("10", null);
        latestRequest().deliverError(new VolleyError());
        drainDisk();
        assertItems(71);
        assertEquals("<b>Saved comment</b>", items().get(0).getText());
        assertEquals(View.VISIBLE, view(R.id.saved_notice).getVisibility());
        assertEquals(View.GONE, view(R.id.offline_empty).getVisibility());
        assertEquals(View.GONE, view(R.id.progressbar).getVisibility());
    }

    /** Exact saved video pages restore HTML and metadata without borrowing another section's cache. */
    @Test
    public void failedVideoLaunchRestoresOnlyMatchingScopedCache() throws JSONException {
        save("3", FeedQuery.LATEST, "", page("vuploader", 81, 1));
        save("4", FeedQuery.LATEST, "", page("usernews", 91, 1));
        open("3", null);
        latestRequest().deliverError(new VolleyError());
        drainDisk();
        assertItems(81);
        assertEquals("vuploader", items().get(0).getRazdel());
        assertEquals("<b>description 81</b>", items().get(0).getText());
        assertEquals("https://localhost/files/81.mp4", items().get(0).getLink());
    }

    /** A different section's saved rows cannot suppress the actionable no-network empty state. */
    @Test
    public void wrongSectionCacheLeavesIconTextAndRetryInsteadOfBlankScreen() throws JSONException {
        save("4", FeedQuery.LATEST, "", page("usernews", 91, 1));
        open("3", null);
        latestRequest().deliverError(new VolleyError());
        drainDisk();
        assertItems();
        View empty = view(R.id.offline_empty);
        assertEquals(View.VISIBLE, empty.getVisibility());
        assertTrue(containsVisibleIcon(empty));
        assertTrue(containsVisibleText(empty));
        assertEquals(View.GONE, view(R.id.empty_view).getVisibility());
        assertEquals(View.GONE, view(R.id.progressbar).getVisibility());
        assertTrue(view(R.id.retry_button).performClick());
        assertEquals(2, app.requests.size());
        assertTrue(latestRequest().getUrl().contains("min=1"));
        assertTrue(latestRequest().getUrl().contains("razdel=vuploader"));
    }

    /** A live page arriving first stays authoritative after the slower saved-data callback arrives. */
    @Test
    public void networkFirstCannotBeOverwrittenOrDuplicatedByLateDatabase() throws JSONException {
        save("10", FeedQuery.LATEST, "", page("comments", 11, 1));
        open("10", null);
        succeed(latestRequest(), page("comments", 21, 1));
        assertItems(21);
        drainDisk();
        assertItems(21);
        assertEquals(View.GONE, view(R.id.saved_notice).getVisibility());
    }

    /** Saved cards are replaced by a successful live first page rather than appended to it. */
    @Test
    public void cacheFirstIsReplacedByLiveResponse() throws JSONException {
        save("10", FeedQuery.LATEST, "", page("comments", 11, 1));
        open("10", null);
        drainDisk();
        assertItems(11);
        succeed(latestRequest(), page("comments", 21, 1));
        assertItems(21);
        drainDisk();
        assertEquals(page("comments", 21, 1).toString(),
                app.getDatabase().feedPageDao().loadPages(query("10", FeedQuery.LATEST, "").scopeKey())
                        .get(0).payload);
    }

    /** A failed manual refresh keeps visible cards and ends both loading indicators. */
    @Test
    public void refreshFailureDoesNotDiscardAlreadyVisibleCards() throws JSONException {
        open("10", null);
        succeed(latestRequest(), page("comments", 31, 1));
        drainDisk();
        ReflectionHelpers.callInstanceMethod(fragment, "refresh");
        assertItems(31);
        latestRequest().deliverError(new VolleyError());
        assertItems(31);
        assertEquals(View.VISIBLE, view(R.id.saved_notice).getVisibility());
        assertEquals(View.GONE, view(R.id.progress_bottom).getVisibility());
        assertEquals(View.GONE, view(R.id.offline_empty).getVisibility());
    }

    /** Retry resubmits a failed later page, preserving the existing first page and its item order. */
    @Test
    public void failedSecondPageRetryDoesNotSkipToThirdPage() throws JSONException {
        open("10", null);
        succeed(latestRequest(), page("comments", 1, 10));
        drainDisk();
        ReflectionHelpers.callInstanceMethod(fragment, "requestPage",
                ReflectionHelpers.ClassParameter.from(int.class, 2));
        latestRequest().deliverError(new VolleyError());
        assertEquals(10, adapter().getItemCount());
        assertTrue(view(R.id.retry_saved_button).performClick());
        assertEquals(3, app.requests.size());
        assertTrue(latestRequest().getUrl().contains("min=2"));
        succeed(latestRequest(), page("comments", 11, 1));
        assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
    }

    /** A short nonempty page can be followed by more server rows after category filtering. */
    @Test
    public void shortNonemptyLivePageStillAllowsNextPage() throws JSONException {
        open("4", null);
        succeed(latestRequest(), page("usernews", 11, 1));
        drainDisk();
        assertFalse("A filtered short page does not establish the server's end",
                ReflectionHelpers.<Boolean>getField(fragment, "endReached"));
        ReflectionHelpers.callInstanceMethod(fragment, "requestPage",
                ReflectionHelpers.ClassParameter.from(int.class, 2));
        assertEquals(2, app.requests.size());
        assertTrue(latestRequest().getUrl().contains("min=2"));
        succeed(latestRequest(), page("usernews", 12, 1));
        assertItems(11, 12);
    }

    /** Offline restoration keeps contiguous saved pages even when an earlier page contains fewer than ten rows. */
    @Test
    public void contiguousShortCachedPagesRestoreAllTheirRows() throws JSONException {
        save("4", FeedQuery.LATEST, "", page("usernews", 21, 1));
        app.getDatabase().feedPageDao().savePage(query("4", FeedQuery.LATEST, "").scopeKey(),
                2, page("usernews", 22, 1).toString(), 1001);
        open("4", null);
        latestRequest().deliverError(new VolleyError());
        drainDisk();
        assertItems(21, 22);
        assertEquals(View.VISIBLE, view(R.id.saved_notice).getVisibility());
    }

    /** Category-filtered latest rows remain available to mark-all-read without polluting the unfiltered snapshot. */
    @Test
    public void filteredLatestResponseRetainsReadMarkerIndexAndExactCacheScope() throws JSONException {
        app.getSharedPreferences().edit().putStringSet("dvc_vuploader_cat", Set.of("video")).commit();
        open("3", null);
        JSONArray response = page("vuploader", 25, 1);
        succeed(latestRequest(), response);
        drainDisk();
        assertItems(25);
        assertEquals(1, app.getDatabase().feedDao().getAllRows("vuploader", 20).size());
        assertEquals(25, app.getDatabase().feedDao().getAllRows("vuploader", 20).get(0).lid);
        app.getDatabase().readMarkDao().markAllRead("vuploader", app.getDatabase().feedDao());
        assertEquals(1, app.getDatabase().readMarkDao().getStatus(25, "vuploader"));
        FeedQuery filtered = new FeedQuery("3", 0, null, FeedQuery.LATEST,
                false, Set.of("video"), "");
        assertEquals(response.toString(), app.getDatabase().feedPageDao()
                .loadPages(filtered.scopeKey()).get(0).payload);
        assertTrue(app.getDatabase().feedPageDao()
                .loadPages(query("3", FeedQuery.LATEST, "").scopeKey()).isEmpty());
    }

    /** An invalid live response reports failure and cannot erase a valid persisted page. */
    @Test
    public void malformedResponsePreservesCachedCardsAndStoredPayload() throws JSONException {
        JSONArray saved = page("comments", 41, 1);
        save("10", FeedQuery.LATEST, "", saved);
        open("10", null);
        drainDisk();
        succeed(latestRequest(), new JSONArray().put(new JSONObject().put("lid", 99)));
        drainDisk();
        assertItems(41);
        assertEquals(View.VISIBLE, view(R.id.saved_notice).getVisibility());
        assertEquals(saved.toString(), app.getDatabase().feedPageDao()
                .loadPages(query("10", FeedQuery.LATEST, "").scopeKey()).get(0).payload);
    }

    /** An accepted empty result must not resurrect stale legacy rows on the next offline launch. */
    @Test
    public void authoritativeEmptySnapshotSuppressesLegacyFallback() throws JSONException {
        legacy("comments", 51, "Stale comment");
        save("10", FeedQuery.LATEST, "", new JSONArray());
        open("10", null);
        latestRequest().deliverError(new VolleyError());
        drainDisk();
        assertItems();
        assertEquals(View.VISIBLE, view(R.id.offline_empty).getVisibility());
    }

    /** A newly successful empty first page wins even if the old legacy read finishes afterward. */
    @Test
    public void liveEmptyResponseStaysEmptyAfterLateLegacyRead() throws JSONException {
        legacy("comments", 51, "Stale comment");
        open("10", null);
        succeed(latestRequest(), new JSONArray());
        drainDisk();
        assertItems();
        assertEquals(View.VISIBLE, view(R.id.empty_view).getVisibility());
        assertEquals(View.GONE, view(R.id.offline_empty).getVisibility());
    }

    /** Fragment arguments identify its own section even when a conflicting navigation event is sticky. */
    @Test
    public void explicitSectionArgumentWinsOverStickyNavigation() throws JSONException {
        EventBus.getDefault().postSticky(new MessageEvent("4", "unrelated sticky search", null, null, null, null));
        save("3", FeedQuery.LATEST, "", page("vuploader", 61, 1));
        open("3", null);
        assertTrue(latestRequest().getUrl().contains("razdel=vuploader"));
        assertFalse(latestRequest().getUrl().contains("story="));
        latestRequest().deliverError(new VolleyError());
        drainDisk();
        assertItems(61);
    }

    /** Switching favorites accounts discards old cards and rejects both obsolete disk and network callbacks. */
    @Test
    public void favoritesAccountChangeInvalidatesOldCacheAndResponse() throws JSONException {
        app.account = "first-user";
        save("3", FeedQuery.FAVORITES, app.account, page("vuploader", 71, 1));
        open("3", host.get().getString(R.string.tab_favorites));
        JsonArrayRequest oldRequest = latestRequest();
        drainDisk();
        assertItems(71);
        host.pause();
        app.account = "second-user";
        host.resume();
        assertTrue(oldRequest.isCanceled());
        assertEquals(2, app.requests.size());
        assertItems();
        succeed(oldRequest, page("vuploader", 72, 1));
        latestRequest().deliverError(new VolleyError());
        drainDisk();
        assertItems();
        assertEquals(View.VISIBLE, view(R.id.offline_empty).getVisibility());
    }

    /** Destroying the feed view cancels its request and prevents late results from being persisted. */
    @Test
    public void destroyedViewIgnoresLateNetworkAndDatabaseCallbacks() throws JSONException {
        save("10", FeedQuery.LATEST, "", page("comments", 81, 1));
        open("10", null);
        JsonArrayRequest request = latestRequest();
        host.get().getSupportFragmentManager().beginTransaction().remove(fragment).commitNow();
        assertTrue(request.isCanceled());
        succeed(request, page("comments", 91, 1));
        request.deliverError(new VolleyError());
        drainDisk();
        assertEquals(page("comments", 81, 1).toString(), app.getDatabase().feedPageDao()
                .loadPages(query("10", FeedQuery.LATEST, "").scopeKey()).get(0).payload);
        assertFalse(fragment.isAdded());
    }

    /** Adds the production fragment with explicit navigation arguments. */
    private void open(String section, String tab) {
        fragment = new MainFragmentContent();
        Bundle arguments = new Bundle();
        arguments.putString(Config.TAG_CATEGORY, section);
        arguments.putString("tab", tab == null ? host.get().getString(R.string.tab_last) : tab);
        fragment.setArguments(arguments);
        host.get().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment).commitNow();
    }

    /** Stores an exact query response using the real transactional cache DAO. */
    private void save(String section, String mode, String account, JSONArray payload) {
        app.getDatabase().feedPageDao().savePage(query(section, mode, account).scopeKey(),
                1, payload.toString(), 1000);
    }

    /** Creates the same immutable scope used for an unfiltered feed fixture. */
    private FeedQuery query(String section, String mode, String account) {
        return new FeedQuery(section, 0, null, mode, false, null, account);
    }

    /** Adds a pre-migration section row with the HTML and identifiers used by actual legacy storage. */
    private void legacy(String section, int id, String description) {
        FeedEntity entity = new FeedEntity();
        entity.lid = id;
        entity.razdel = section;
        entity.title = "Saved item " + id;
        entity.description = description;
        entity.fullText = "<p>full saved item</p>";
        entity.date = "09 October 2026";
        entity.timestamp = 1791532800;
        entity.category = "Saved category";
        entity.img = "https://localhost/offline-test.png";
        entity.size = "1 MB";
        entity.url = "https://localhost/files/" + id + ".mp4";
        entity.state = 1;
        app.getDatabase().feedDao().insert(entity);
    }

    /** Builds complete API rows so malformed-response tests omit fields intentionally. */
    private JSONArray page(String section, int firstId, int count) throws JSONException {
        JSONArray response = new JSONArray();
        for (int id = firstId; id < firstId + count; id++) {
            response.put(new JSONObject().put(Config.TAG_ID, id)
                    .put(Config.TAG_IMAGE_URL, "https://localhost/offline-test.png")
                    .put(Config.TAG_TITLE, "Item " + id)
                    .put(Config.TAG_TEXT, "<b>description " + id + "</b>")
                    .put(Config.TAG_FULL_TEXT, "<p>full " + id + "</p>")
                    .put(Config.TAG_DATE, "09 October 2026")
                    .put(Config.TAG_COMMENTS, 0).put(Config.TAG_HITS, 2)
                    .put(Config.TAG_RAZDEL, section)
                    .put(Config.TAG_LINK, "https://localhost/files/" + id + ".mp4")
                    .put(Config.TAG_MOD, "mp4").put(Config.TAG_CATEGORY, "Category")
                    .put(Config.TAG_HEADERS, "header").put(Config.TAG_USER, "Uploader")
                    .put(Config.TAG_SIZE, "1 MB").put(Config.TAG_TIME, 1791532800)
                    .put(Config.TAG_MIN, 0).put(Config.TAG_PLUS, 0)
                    .put(Config.TAG_FAV, 0).put(Config.TAG_STATUS, 1));
        }
        return response;
    }

    /** Delivers the actual Volley success listener without opening a network connection. */
    private void succeed(JsonArrayRequest request, JSONArray response) {
        ReflectionHelpers.callInstanceMethod(request, "deliverResponse",
                ReflectionHelpers.ClassParameter.from(Object.class, response));
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Executes bounded queued disk work and all of its main-thread callbacks in deterministic order. */
    private void drainDisk() {
        int tasks = 0;
        while (!app.executor.tasks.isEmpty()) {
            assertTrue("Unexpected unbounded background work", tasks++ < 200);
            app.executor.tasks.remove().run();
            ShadowLooper.shadowMainLooper().idle();
        }
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Returns the most recently submitted real request for precise page and callback assertions. */
    private JsonArrayRequest latestRequest() {
        return app.requests.get(app.requests.size() - 1);
    }

    /** Reads rendered adapter identifiers rather than the fragment's private working list. */
    private void assertItems(int... ids) {
        assertEquals(ids.length, adapter().getItemCount());
        for (int index = 0; index < ids.length; index++) assertEquals(ids[index], items().get(index).getId());
    }

    /** Returns the adapter installed on the actual RecyclerView. */
    private AdapterMainRazdel adapter() {
        return (AdapterMainRazdel) ((RecyclerView) view(R.id.recycler_view)).getAdapter();
    }

    /** Inspects the adapter's independent snapshot to verify content and section isolation. */
    private List<Feed> items() {
        return ReflectionHelpers.getField(adapter(), "jsonFeed");
    }

    /** Finds a real view in the fragment's inflated production layout. */
    private View view(int id) {
        View found = fragment.requireView().findViewById(id);
        assertNotNull(found);
        return found;
    }

    /** Checks that the offline state contains a visible icon with an actual drawable. */
    private boolean containsVisibleIcon(View candidate) {
        if (candidate.getVisibility() != View.VISIBLE) return false;
        if (candidate instanceof ImageView) return ((ImageView) candidate).getDrawable() != null;
        if (candidate instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) candidate;
            for (int index = 0; index < group.getChildCount(); index++) {
                if (containsVisibleIcon(group.getChildAt(index))) return true;
            }
        }
        return false;
    }

    /** Checks that the offline state explains the recovery action using visible text. */
    private boolean containsVisibleText(View candidate) {
        if (candidate.getVisibility() != View.VISIBLE) return false;
        if (candidate instanceof TextView && ((TextView) candidate).getText().length() > 0) return true;
        if (candidate instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) candidate;
            for (int index = 0; index < group.getChildCount(); index++) {
                if (containsVisibleText(group.getChildAt(index))) return true;
            }
        }
        return false;
    }

    /** Avoids real services while retaining the production Application API and database implementation. */
    public static class TestApp extends AppController {
        private final ControlledExecutor executor = new ControlledExecutor();
        private final List<JsonArrayRequest> requests = new ArrayList<>();
        private AppDatabase database;
        private String account = "";

        /** Installs the singleton without starting WorkManager, networking or the persistent user database. */
        @Override
        public void onCreate() {
            ReflectionHelpers.setStaticField(AppController.class, "sInstance", this);
        }

        /** Supplies one isolated real Room database to both fragment and adapter. */
        @Override
        public synchronized AppDatabase getDatabase() {
            if (database == null) database = Room.inMemoryDatabaseBuilder(this, AppDatabase.class)
                    .allowMainThreadQueries().build();
            return database;
        }

        /** Holds disk tasks until each test chooses when their results may arrive. */
        @Override
        public ControlledExecutor getExecutor() { return executor; }

        /** Captures production Volley requests rather than executing HTTP. */
        @Override
        public <T> void addToRequestQueue(Request<T> request) { requests.add((JsonArrayRequest) request); }

        /** Supplies isolated preference state even when the base Application's static field survives a test. */
        @Override
        public SharedPreferences getSharedPreferences() {
            return PreferenceManager.getDefaultSharedPreferences(this);
        }

        /** Models an account switch without storing real credentials. */
        @Override
        public String userName(String defaultName) { return account; }
    }

    /** A manually advanced ExecutorService makes saved-data races reproducible without sleeps. */
    public static class ControlledExecutor extends AbstractExecutorService {
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private boolean shutdown;

        /** Enqueues background work in the same FIFO order as the production single-thread executor. */
        @Override
        public void execute(Runnable command) { tasks.add(command); }

        /** Stops accepting work after the owning test has finished. */
        @Override
        public void shutdown() { shutdown = true; }

        /** Cancels test-owned work without retaining callbacks across test cases. */
        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> pending = new ArrayList<>(tasks);
            tasks.clear();
            return pending;
        }

        /** Reports the deterministic executor's shutdown state. */
        @Override
        public boolean isShutdown() { return shutdown; }

        /** Reports completion when no queued callback remains after shutdown. */
        @Override
        public boolean isTerminated() { return shutdown && tasks.isEmpty(); }

        /** Requires no blocking because tasks are run explicitly on the test thread. */
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) { return isTerminated(); }
    }
}

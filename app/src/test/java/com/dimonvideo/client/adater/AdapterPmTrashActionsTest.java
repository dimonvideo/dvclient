package com.dimonvideo.client.adater;

import android.net.Uri;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.pm.PmHttpTransport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Exercises actual row menus and folder actions without starting a deletion worker or HTTP request. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = AdapterPmTrashActionsTest.TestApp.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class AdapterPmTrashActionsTest {
    private ActivityController<AppCompatActivity> activity;
    private AdapterPm adapter;
    private RequestQueue previousQueue;
    private final List<Request<?>> requests = new ArrayList<>();
    private int removed;

    /** Opens a themed host and installs a capture-only queue so a valid restoration can be inspected. */
    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity.class);
        activity.get().setTheme(R.style.AppTheme);
        activity.setup();
        previousQueue = ReflectionHelpers.getStaticField(PmHttpTransport.class, "requestQueue");
        RequestQueue captureQueue = new RequestQueue(new NoCache(), request -> {
            throw new AssertionError("Tests must not execute HTTP requests");
        }) {
            /** Captures the real production request without starting any dispatcher thread. */
            @Override public <T> Request<T> add(Request<T> request) {
                requests.add(request);
                return request;
            }
        };
        ReflectionHelpers.setStaticField(PmHttpTransport.class, "requestQueue", captureQueue);
    }

    /** Releases renderer resources, the real menu window and the capture-only transport after each test. */
    @After
    public void tearDown() {
        if (adapter != null) adapter.onDetachedFromRecyclerView(new RecyclerView(activity.get()));
        if (ShadowDialog.getLatestDialog() != null) ShadowDialog.getLatestDialog().dismiss();
        ReflectionHelpers.setStaticField(PmHttpTransport.class, "requestQueue", previousQueue);
        activity.pause().stop().destroy();
    }

    /** The real Trash menu must offer opening and copying while omitting its ineffective delete action. */
    @Test
    public void trashMenuOmitsDeletionAction() {
        FeedPm message = createAdapter(5);
        ListView actions = openMenu(message);
        assertEquals(2, actions.getAdapter().getCount());
        assertEquals(activity.get().getString(R.string.action_open), actions.getAdapter().getItem(0));
        assertEquals(activity.get().getString(R.string.copy_listtext), actions.getAdapter().getItem(1));
        assertTrue(requests.isEmpty());
        assertEquals(0, removed);
    }

    /** A source row in an active folder must retain the existing context-menu deletion action. */
    @Test
    public void activeMenuStillOffersDeletionAction() {
        FeedPm message = createAdapter(2);
        ListView actions = openMenu(message);
        assertEquals(3, actions.getAdapter().getCount());
        assertEquals(activity.get().getString(R.string.pm_delete), actions.getAdapter().getItem(2));
        assertTrue(requests.isEmpty());
    }

    /** Programmatic trash deletion is rejected while its existing restore operation remains routable. */
    @Test
    public void trashRemoveCannotQueueDeletionButRestoreStillTargetsTheSourceMessage() {
        createAdapter(5);
        adapter.removeItem(0);
        assertEquals(1, adapter.getItemCount());
        assertEquals(0, removed);
        assertTrue(requests.isEmpty());

        adapter.restoreItem(0);
        assertEquals(1, requests.size());
        Uri address = Uri.parse(requests.get(0).getUrl());
        assertEquals("10", address.getQueryParameter("pm"));
        assertEquals("1", address.getQueryParameter("delete"));
        assertEquals("42", address.getQueryParameter("pm_id"));
        assertEquals(1, adapter.getItemCount());
        assertEquals(0, removed);
    }

    /** A row's own trash metadata protects it even before the fragment supplies the adapter folder hint. */
    @Test
    public void trashRowGuardsDeletionWithoutAnAdapterFolderHint() {
        FeedPm message = createAdapter(5);
        adapter.setDeletionSourceFolder(0);
        assertEquals(2, openMenu(message).getAdapter().getCount());
        adapter.removeItem(0);
        assertEquals(1, adapter.getItemCount());
        assertEquals(0, removed);
        assertTrue(requests.isEmpty());
    }

    /** Builds one real committed adapter row with distinct stable identity and server folder metadata. */
    private FeedPm createAdapter(int folder) {
        FeedPm message = new FeedPm();
        message.setId(42);
        message.setTitle("private message");
        message.setSourceFolder(folder);
        message.setSourcePage(3);
        adapter = new AdapterPm(Collections.singletonList(message), activity.get());
        adapter.setDeletionSourceFolder(folder);
        adapter.setOnMessageRemovedListener(messageId -> removed++);
        assertEquals(1, adapter.getItemCount());
        return message;
    }

    /** Opens production menu code and returns the actual AlertDialog list without mocking its actions. */
    private ListView openMenu(FeedPm message) {
        ReflectionHelpers.callInstanceMethod(adapter, "showActions",
                ReflectionHelpers.ClassParameter.from(FeedPm.class, message));
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        return dialog.getListView();
    }

    /** Supplies a fixed authenticated identity without application services, a database or real credentials. */
    public static class TestApp extends AppController {
        /** Installs only the singleton required by account-bound adapter and network guards. */
        @Override public void onCreate() {
            ReflectionHelpers.setStaticField(AppController.class, "sInstance", this);
        }
        /** Treats these synthetic rows as belonging to the test's authenticated account. */
        @Override public int isAuth() { return 1; }
        /** Returns a stable, positive account identity for adapter routing. */
        @Override public int isUserId() { return 12; }
        /** Supplies a non-secret login accepted by the existing credential validator. */
        @Override public String userName(String defaultName) { return "Alice"; }
        /** Supplies a synthetic password solely so restoration reaches the capture-only request queue. */
        @Override public String userPassword() { return "fake-test-password"; }
        /** Fails if an ineffective trash deletion reaches the queue's storage executor. */
        @Override public ExecutorService getExecutor() {
            throw new AssertionError("Trash deletion must not enter the durable queue");
        }
    }
}

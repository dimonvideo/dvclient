package com.dimonvideo.client.util.pm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.dimonvideo.client.util.AppController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Verifies exact PM cancellation and durable suppression of notifications arriving after deletion. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = PmNotificationsTest.TestApp.class)
public class PmNotificationsTest {
    private static final int MESSAGE_ID = 55;
    private static final String DOWNLOAD_CHANNEL = "download-test";
    private final String accountKey = PmDeletionAccount.key("Alice", 12);
    private TestApp context;
    private PmDeletionStore store;
    private NotificationManager notifications;

    /** Installs independent channels and an authenticated application without network startup. */
    @Before
    public void prepareNotifications() {
        context = (TestApp) RuntimeEnvironment.getApplication();
        store = PmDeletionStore.get(context);
        store.getWritableDatabase().delete("pm_deletions", null, null);
        store.getWritableDatabase().delete("pm_composer_checkpoints", null, null);
        notifications = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifications.createNotificationChannel(new NotificationChannel(PmNotifications.CHANNEL_ID,
                "PM test", NotificationManager.IMPORTANCE_DEFAULT));
        notifications.createNotificationChannel(new NotificationChannel(DOWNLOAD_CHANNEL,
                "Download test", NotificationManager.IMPORTANCE_DEFAULT));
    }

    /** An ordinary new PM is still shown when no accepted deletion belongs to that message. */
    @Test
    public void publishesCurrentAccountsUndeletedMessage() throws Exception {
        PmNotifications.publish(context, accountKey, MESSAGE_ID, pmNotification());
        awaitLookups();

        assertNotNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID));
    }

    /** An avatar finishing after a durable local deletion cannot recreate the removed notification. */
    @Test
    public void delayedPublicationSuppressesPendingDeletion() throws Exception {
        Notification delayedAvatarResult = pmNotification();
        store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis());

        PmNotifications.publish(context, accountKey, MESSAGE_ID, delayedAvatarResult);
        awaitLookups();

        assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID));
    }

    /** A delayed duplicate push is suppressed after the worker has durably confirmed deletion. */
    @Test
    public void delayedPublicationSuppressesConfirmedDeletion() throws Exception {
        PmDeletionStore.Entry entry = store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis());
        assertTrue(store.complete(entry));
        store.close();

        PmNotifications.publish(context, accountKey, MESSAGE_ID, pmNotification());
        awaitLookups();

        assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID));
    }

    /** A callback belonging to another account is never labelled as the account currently signed in. */
    @Test
    public void rejectsAnotherAccountsDelayedPublication() throws Exception {
        PmNotifications.publish(context, PmDeletionAccount.key("Bob", 12), MESSAGE_ID, pmNotification());
        awaitLookups();

        assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID));
    }

    /** Switching accounts before a queued lookup callback prevents an old avatar from appearing. */
    @Test
    public void accountChangeBeforePublicationCallbackDropsOldResult() throws Exception {
        PmNotifications.publish(context, accountKey, MESSAGE_ID, pmNotification());
        context.login = "Bob";
        awaitLookups();

        assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID));
    }

    /** An unreadable queue never permits a late callback to restore a possibly deleted notification. */
    @Test
    public void unavailableDurableStateSuppressesPublication() throws Exception {
        SQLiteDatabase database = store.getWritableDatabase();
        database.execSQL("DROP TABLE pm_deletions");
        try {
            PmNotifications.publish(context, accountKey, MESSAGE_ID, pmNotification());
            awaitLookups();
            assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID));
        } finally {
            database.execSQL("DROP TABLE pm_composer_checkpoints");
            store.onCreate(database);
        }
    }

    /** Exact cancellation preserves a second PM and a download whose numeric ID matches the deleted PM. */
    @Test
    public void dismissTargetsOnlyTheSelectedMessage() {
        notifications.notify(MESSAGE_ID, pmNotification());
        notifications.notify(MESSAGE_ID + 1, pmNotification());
        notifications.notify("download:55", MESSAGE_ID, downloadNotification());

        PmNotifications.dismiss(context, MESSAGE_ID);

        assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID));
        assertNotNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID + 1));
        assertNotNull(Shadows.shadowOf(notifications).getNotification("download:55", MESSAGE_ID));
    }

    /** Entering the mailbox clears only its channel instead of cancelling active download notifications. */
    @Test
    public void dismissAllPreservesNotificationsOutsideThePmChannel() {
        notifications.notify(MESSAGE_ID, pmNotification());
        notifications.notify(MESSAGE_ID + 1, pmNotification());
        notifications.notify("download:55", MESSAGE_ID, downloadNotification());

        PmNotifications.dismissAll(context);

        assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID));
        assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID + 1));
        assertNotNull(Shadows.shadowOf(notifications).getNotification("download:55", MESSAGE_ID));
    }

    /** Startup repairs lost cancellation callbacks for pending and confirmed intents without hiding live PMs. */
    @Test
    public void reconcileRestoresCancellationAfterLostCallbacks() throws Exception {
        long now = System.currentTimeMillis();
        store.insert(accountKey, 12, MESSAGE_ID, now);
        assertTrue(store.complete(store.insert(accountKey, 12, MESSAGE_ID + 1, now)));
        store.insert(accountKey, 12, MESSAGE_ID + 3, now - PmDeletionRetryPolicy.LIFETIME_MS);
        for (int id = MESSAGE_ID; id <= MESSAGE_ID + 3; id++) {
            notifications.notify(id, pmNotification());
        }
        notifications.notify("download:55", MESSAGE_ID, downloadNotification());
        store.close();

        PmNotifications.reconcile(context);
        awaitLookups();

        assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID));
        assertNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID + 1));
        assertNotNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID + 2));
        assertNotNull(Shadows.shadowOf(notifications).getNotification(MESSAGE_ID + 3));
        assertNotNull(Shadows.shadowOf(notifications).getNotification("download:55", MESSAGE_ID));
    }

    /** Builds an ordinary PM notification without involving Firebase or image decoding. */
    private Notification pmNotification() {
        return new Notification.Builder(context, PmNotifications.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle("PM").build();
    }

    /** Builds a notification from an unrelated channel for cancellation isolation checks. */
    private Notification downloadNotification() {
        return new Notification.Builder(context, DOWNLOAD_CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("Download").build();
    }

    /** Waits behind earlier queue lookups so both their I/O and main-thread callbacks have completed. */
    private void awaitLookups() throws Exception {
        AtomicBoolean completed = new AtomicBoolean();
        PmDeletionQueue.getDeletionState(context, PmDeletionQueue.currentAccountKey(), MESSAGE_ID,
                state -> completed.set(true));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!completed.get() && System.nanoTime() < deadline) {
            Thread.sleep(10);
            ShadowLooper.idleMainLooper();
        }
        assertTrue("Notification lookup did not complete", completed.get());
        ShadowLooper.idleMainLooper();
    }

    /** Supplies a mutable account identity without initializing networking, credentials, or Room. */
    public static class TestApp extends AppController {
        private String login = "Alice";

        /** Installs only the application singleton used by the queue's account guard. */
        @Override public void onCreate() {
            ReflectionHelpers.setStaticField(AppController.class, "sInstance", this);
        }

        /** Keeps authentication enabled while the tests explicitly switch account names. */
        @Override public int isAuth() { return 1; }

        /** Returns a synthetic server user ID without real account data. */
        @Override public int isUserId() { return 12; }

        /** Returns the account name currently selected by the lifecycle test. */
        @Override public String userName(String defaultName) { return login; }
    }
}

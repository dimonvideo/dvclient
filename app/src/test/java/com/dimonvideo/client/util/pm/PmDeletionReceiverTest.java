package com.dimonvideo.client.util.pm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Configuration;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.dimonvideo.client.util.ActionReceiver;
import com.dimonvideo.client.util.AppController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowBroadcastReceiver;
import org.robolectric.shadows.ShadowBroadcastPendingResult;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Verifies notification removal and receiver lifetime around the real durable enqueue boundary. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 28, application = PmDeletionReceiverTest.TestApp.class)
public class PmDeletionReceiverTest {
    private TestApp context;
    private NotificationManager notifications;
    private ActionReceiver receiver;
    private PmDeletionStore store;

    /** Installs no-network workers and posts the notification targeted by the receiver action. */
    @Before
    public void prepareNotification() {
        context = (TestApp) RuntimeEnvironment.getApplication();
        WorkManagerTestInitHelper.initializeTestWorkManager(context,
                new Configuration.Builder().setExecutor(new SynchronousExecutor())
                        .setTaskExecutor(new SynchronousExecutor())
                        .setWorkerFactory(new WorkerFactory() {
                            /** Substitutes a no-network worker while exercising the real WorkManager enqueue. */
                            @Override
                            public ListenableWorker createWorker(@NonNull Context appContext,
                                                                 @NonNull String className,
                                                                 @NonNull WorkerParameters parameters) {
                                return new NoNetworkWorker(appContext, parameters);
                            }
                        }).build());
        store = PmDeletionStore.get(context);
        for (PmDeletionStore.Entry entry : store.entries()) store.remove(entry);
        notifications = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifications.createNotificationChannel(new NotificationChannel("pm-test", "PM test",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifications.notify(55, new Notification.Builder(context, "pm-test")
                .setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle("Message").build());
        receiver = new ActionReceiver();
        // AndroidX contributes this signature permission to the merged APK manifest;
        // Robolectric's application permission shadow requires the grant explicitly.
        Shadows.shadowOf(context).grantPermissions(
                context.getPackageName() + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
        ContextCompat.registerReceiver(context, receiver, new IntentFilter("pm-test-delete"),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    /** A blocked database commit leaves the notification visible and the receiver alive. */
    @Test
    public void notificationDismissesOnlyAfterDurableAcceptance() throws Exception {
        store.getWritableDatabase().beginTransaction();
        try {
            sendDeleteAction();
            ShadowBroadcastReceiver shadow = Shadows.shadowOf(receiver);
            assertTrue(shadow.wentAsync());
            assertNotNull(Shadows.shadowOf(notifications).getNotification(55));
            assertFalse(receiverFinished());
        } finally {
            store.getWritableDatabase().endTransaction();
        }
        awaitFinished();
        assertNull(Shadows.shadowOf(notifications).getNotification(55));
        assertNotNull(store.find(PmDeletionAccount.key("Alice", 12), 55));
    }

    /** Rejected authentication finishes the receiver and keeps the unqueued notification available. */
    @Test
    public void queueRejectionKeepsNotificationAndFinishesReceiver() throws Exception {
        context.signedIn = false;
        sendDeleteAction();
        awaitFinished();
        assertNotNull(Shadows.shadowOf(notifications).getNotification(55));
        assertTrue(store.entries().isEmpty());
    }

    /** Delivers the action through Android's broadcast machinery so goAsync has a real pending result. */
    private void sendDeleteAction() {
        context.sendBroadcast(new Intent("pm-test-delete").setPackage(context.getPackageName())
                .putExtra("action", "deletePm").putExtra("id", "55"));
        ShadowLooper.idleMainLooper();
    }

    /** Allows the queue's background commit and main callback to finish with a bounded test deadline. */
    private void awaitFinished() throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (!receiverFinished() && System.nanoTime() < deadline) {
            Thread.sleep(10);
            ShadowLooper.idleMainLooper();
        }
        assertTrue(receiverFinished());
    }

    /** Reads Robolectric's supported completion future for the actual goAsync pending result. */
    private boolean receiverFinished() {
        ShadowBroadcastPendingResult pending = Shadow.extract(
                Shadows.shadowOf(receiver).getOriginalPendingResult());
        return pending.getFuture().isDone();
    }

    /** Supplies an in-memory authenticated app without database startup or real credentials. */
    public static class TestApp extends AppController {
        private boolean signedIn = true;

        /** Installs only the application singleton required by the queue boundary. */
        @Override public void onCreate() {
            ReflectionHelpers.setStaticField(AppController.class, "sInstance", this);
        }
        /** Returns the test's selected authentication state. */
        @Override public int isAuth() { return signedIn ? 1 : 0; }
        /** Supplies the original account identity without writing preferences. */
        @Override public int isUserId() { return 12; }
        /** Supplies a fake account name for hashing. */
        @Override public String userName(String defaultName) { return "Alice"; }
    }

    /** Completes WorkManager requests locally without using the production PM transport. */
    private static final class NoNetworkWorker extends Worker {
        /** Creates the test-only worker from the actual persisted work parameters. */
        NoNetworkWorker(Context context, WorkerParameters parameters) { super(context, parameters); }
        /** Completes without acknowledging or deleting the queue's server message intent. */
        @NonNull @Override public Result doWork() { return Result.success(); }
    }
}

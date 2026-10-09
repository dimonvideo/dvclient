package com.dimonvideo.client.util.pm;

import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.dimonvideo.client.R;
import com.dimonvideo.client.util.AppController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Exercises UI acceptance and recovery against the real SQLite queue with a failing scheduler. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = PmDeletionQueueTest.TestApp.class)
public class PmDeletionQueueTest {
    private static final int MESSAGE_ID = 55;
    private final String accountKey = PmDeletionAccount.key("Alice", 12);
    private final AtomicInteger accepted = new AtomicInteger();
    private final AtomicInteger rejected = new AtomicInteger();
    private TestApp context;
    private PmDeletionStore store;

    /** Starts with an empty durable queue and a fake signed-in application without network startup. */
    @Before
    public void prepareQueue() {
        context = (TestApp) RuntimeEnvironment.getApplication();
        store = PmDeletionStore.get(context);
        for (PmDeletionStore.Entry entry : store.entries()) store.remove(entry);
        ShadowToast.reset();
    }

    /** A saved intent stays accepted and hidden locally even if WorkManager cannot save its request. */
    @Test
    public void schedulingFailureAcceptsPersistedIntentAndResumeRecoversIt() throws Exception {
        PmDeletionQueue.enqueue(context, MESSAGE_ID, 2, 37,
                accepted::incrementAndGet, rejected::incrementAndGet,
                (appContext, entry, policy) -> {
                    throw new IOException("Controlled scheduling failure");
                });

        await(() -> accepted.get() + rejected.get() == 1);
        assertEquals(1, accepted.get());
        assertEquals(0, rejected.get());
        assertEquals(context.getString(R.string.pm_delete_queued), ShadowToast.getTextOfLatestToast());
        PmDeletionStore.Entry saved = store.find(accountKey, MESSAGE_ID);
        assertNotNull(saved);
        assertEquals(Collections.singleton(MESSAGE_ID), pendingIds());

        List<PmDeletionStore.Entry> recovered = new CopyOnWriteArrayList<>();
        PmDeletionQueue.resume(context,
                (appContext, entry, policy) -> recovered.add(entry));
        await(() -> recovered.size() == 1);
        assertEquals(saved.createdAt, recovered.get(0).createdAt);
        assertEquals(accountKey, recovered.get(0).accountKey);
        assertEquals(2, recovered.get(0).sourceFolder);
        assertEquals(37, recovered.get(0).sourcePage);
        assertEquals(1, accepted.get());
        assertEquals(0, rejected.get());
    }

    /** A reply's own confirmation remains visible instead of being replaced by the ordinary queue toast. */
    @Test
    public void callerConfirmationSurvivesPersistedIntentAndSchedulingFailure() throws Exception {
        PmDeletionQueue.enqueue(context, MESSAGE_ID, 0, 1,
                () -> {
                    accepted.incrementAndGet();
                    Toast.makeText(context, R.string.pm_sent_and_deleted, Toast.LENGTH_LONG).show();
                }, rejected::incrementAndGet,
                (appContext, entry, policy) -> {
                    throw new IOException("Controlled scheduling failure");
                }, false);
        await(() -> accepted.get() + rejected.get() == 1);
        assertEquals(1, accepted.get());
        assertEquals(0, rejected.get());
        assertNotNull(store.find(accountKey, MESSAGE_ID));
        assertEquals(context.getString(R.string.pm_sent_and_deleted), ShadowToast.getTextOfLatestToast());
    }

    /** Disabling the ordinary success toast never hides a real database failure or accepts its deletion. */
    @Test
    public void callerConfirmationModeStillReportsFailedInsertion() throws Exception {
        SQLiteDatabase database = store.getWritableDatabase();
        database.execSQL("CREATE TRIGGER queue_test_quiet_reject BEFORE INSERT ON pm_deletions "
                + "BEGIN SELECT RAISE(ABORT, 'Controlled queue write failure'); END");
        try {
            PmDeletionQueue.enqueue(context, MESSAGE_ID, 0, 1,
                    accepted::incrementAndGet, rejected::incrementAndGet,
                    (appContext, entry, policy) -> { }, false);
            await(() -> accepted.get() + rejected.get() == 1);
            assertEquals(0, accepted.get());
            assertEquals(1, rejected.get());
            assertTrue(store.entries().isEmpty());
            assertEquals(context.getString(R.string.pm_delete_queue_error), ShadowToast.getTextOfLatestToast());
        } finally {
            database.execSQL("DROP TRIGGER queue_test_quiet_reject");
        }
    }

    /** A genuinely failed SQLite write rejects removal and leaves nothing that recovery can delete. */
    @Test
    public void failedInsertionRejectsWithoutLeavingRecoverableIntent() throws Exception {
        SQLiteDatabase database = store.getWritableDatabase();
        database.execSQL("CREATE TRIGGER queue_test_reject BEFORE INSERT ON pm_deletions "
                + "BEGIN SELECT RAISE(ABORT, 'Controlled queue write failure'); END");
        AtomicInteger scheduled = new AtomicInteger();
        try {
            PmDeletionQueue.enqueue(context, MESSAGE_ID, 0, 1,
                    accepted::incrementAndGet, rejected::incrementAndGet,
                    (appContext, entry, policy) -> scheduled.incrementAndGet());
            await(() -> accepted.get() + rejected.get() == 1);
            assertEquals(0, accepted.get());
            assertEquals(1, rejected.get());
            assertEquals(context.getString(R.string.pm_delete_queue_error),
                    ShadowToast.getTextOfLatestToast());
            assertTrue(store.entries().isEmpty());
        } finally {
            database.execSQL("DROP TRIGGER queue_test_reject");
        }

        PmDeletionQueue.resume(context,
                (appContext, entry, policy) -> scheduled.incrementAndGet());
        assertTrue(pendingIds().isEmpty());
        assertEquals(0, scheduled.get());
    }

    /** Recovery continues with other accepted messages when scheduling one saved intent still fails. */
    @Test
    public void oneRecoverySchedulingFailureCannotBlockOtherIntents() throws Exception {
        long now = System.currentTimeMillis();
        store.insert(accountKey, 12, MESSAGE_ID, now - 2);
        store.insert(accountKey, 12, MESSAGE_ID + 1, now - 1);
        AtomicInteger attempts = new AtomicInteger();
        List<Integer> scheduled = new CopyOnWriteArrayList<>();

        PmDeletionQueue.resume(context, (appContext, entry, policy) -> {
            attempts.incrementAndGet();
            if (entry.messageId == MESSAGE_ID) {
                throw new IOException("Controlled scheduling failure");
            }
            scheduled.add(entry.messageId);
        });

        assertEquals(2, pendingIds().size());
        assertEquals(2, attempts.get());
        assertEquals(Collections.singletonList(MESSAGE_ID + 1), scheduled);
        assertEquals(2, store.entries().size());
    }

    /** Recovery expires an old accepted intent without scheduling a deletion beyond six hours. */
    @Test
    public void expiredIntentCannotBeScheduledDuringRecovery() throws Exception {
        store.insert(accountKey, 12, MESSAGE_ID,
                System.currentTimeMillis() - PmDeletionRetryPolicy.LIFETIME_MS);
        AtomicInteger scheduled = new AtomicInteger();

        PmDeletionQueue.resume(context,
                (appContext, entry, policy) -> scheduled.incrementAndGet());

        assertTrue(pendingIds().isEmpty());
        assertTrue(store.entries().isEmpty());
        assertEquals(0, scheduled.get());
    }

    /** Reads the real queue's filtering result after previously queued background operations finish. */
    private Set<Integer> pendingIds() throws Exception {
        AtomicReference<Set<Integer>> result = new AtomicReference<>();
        PmDeletionQueue.getPendingIds(context, result::set);
        await(() -> result.get() != null);
        return result.get();
    }

    /** Waits for queue I/O and main-thread callbacks with a bounded deadline and no server requests. */
    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
            ShadowLooper.idleMainLooper();
        }
        assertTrue("Queue callback did not complete", condition.getAsBoolean());
    }

    /** Supplies an authenticated account without initializing Room, networking, or production workers. */
    public static class TestApp extends AppController {
        /** Installs only the application singleton needed by the queue's account guard. */
        @Override public void onCreate() {
            ReflectionHelpers.setStaticField(AppController.class, "sInstance", this);
        }
        /** Keeps these queue tests signed in to the original account. */
        @Override public int isAuth() { return 1; }
        /** Supplies the stable server user identity for durable work metadata. */
        @Override public int isUserId() { return 12; }
        /** Supplies a fake login only for computing the non-secret account hash. */
        @Override public String userName(String defaultName) { return "Alice"; }
    }
}

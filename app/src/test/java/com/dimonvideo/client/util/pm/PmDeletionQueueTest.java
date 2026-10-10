package com.dimonvideo.client.util.pm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
    private NotificationManager notifications;

    /** Starts with an empty durable queue and a fake signed-in application without network startup. */
    @Before
    public void prepareQueue() {
        context = (TestApp) RuntimeEnvironment.getApplication();
        context.login = "Alice";
        store = PmDeletionStore.get(context);
        store.getWritableDatabase().delete("pm_deletions", null, null);
        store.getWritableDatabase().delete("pm_composer_checkpoints", null, null);
        notifications = context.getSystemService(NotificationManager.class);
        notifications.cancelAll();
        notifications.createNotificationChannel(new NotificationChannel(PmNotifications.CHANNEL_ID,
                "Private messages", NotificationManager.IMPORTANCE_DEFAULT));
        ShadowToast.reset();
    }

    /** Acceptance dismisses exactly its durable PM before the UI callback, even when scheduling fails. */
    @Test
    public void persistedEnqueueDismissesOnlyTargetBeforeAcceptanceWhenSchedulingFails() throws Exception {
        postNotification(null, MESSAGE_ID);
        postNotification(null, MESSAGE_ID + 1);
        postNotification("download", MESSAGE_ID);
        AtomicReference<Boolean> targetVisibleAtAcceptance = new AtomicReference<>();

        PmDeletionQueue.enqueue(context, MESSAGE_ID, 0, 1, () -> {
            targetVisibleAtAcceptance.set(hasNotification(null, MESSAGE_ID));
            accepted.incrementAndGet();
        }, rejected::incrementAndGet, (appContext, entry, policy) -> {
            throw new IOException("Controlled scheduling failure");
        });

        await(() -> accepted.get() + rejected.get() == 1);
        assertEquals(1, accepted.get());
        assertEquals(0, rejected.get());
        assertEquals(Boolean.FALSE, targetVisibleAtAcceptance.get());
        assertNotNull(store.find(accountKey, MESSAGE_ID));
        assertFalse(hasNotification(null, MESSAGE_ID));
        assertTrue(hasNotification(null, MESSAGE_ID + 1));
        assertTrue(hasNotification("download", MESSAGE_ID));
        assertEquals(2, notifications.getActiveNotifications().length);
    }

    /** Rejecting an invalid server identity does not dismiss even a notification with that numeric ID. */
    @Test
    public void invalidMessageIdentityKeepsNotifications() throws Exception {
        postNotification(null, 0);
        postNotification(null, MESSAGE_ID);
        AtomicInteger scheduled = new AtomicInteger();

        PmDeletionQueue.enqueue(context, 0, 0, 1, accepted::incrementAndGet,
                rejected::incrementAndGet, (appContext, entry, policy) -> scheduled.incrementAndGet());

        await(() -> accepted.get() + rejected.get() == 1);
        assertEquals(0, accepted.get());
        assertEquals(1, rejected.get());
        assertEquals(0, scheduled.get());
        assertTrue(store.entries().isEmpty());
        assertTrue(hasNotification(null, 0));
        assertTrue(hasNotification(null, MESSAGE_ID));
    }

    /** Switching accounts during scheduling preserves a replacement notification belonging to the new account. */
    @Test
    public void accountChangeBeforeAcceptanceCannotDismissNewAccountsNotification() throws Exception {
        postNotification(null, MESSAGE_ID);

        PmDeletionQueue.enqueue(context, MESSAGE_ID, 0, 1, accepted::incrementAndGet,
                rejected::incrementAndGet, (appContext, entry, policy) -> {
                    context.login = "Bob";
                    postNotification(null, MESSAGE_ID);
                });

        await(() -> accepted.get() + rejected.get() == 1);
        assertEquals(0, accepted.get());
        assertEquals(1, rejected.get());
        assertNotNull(store.find(accountKey, MESSAGE_ID));
        assertTrue(hasNotification(null, MESSAGE_ID));
    }

    /** Confirmed worker completion repairs a notification left behind without removing other PMs or downloads. */
    @Test
    public void confirmedCompletionDismissesOnlyItsMessage() throws Exception {
        PmDeletionStore.Entry entry = store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis());
        postNotification(null, MESSAGE_ID);
        postNotification(null, MESSAGE_ID + 1);
        postNotification("download", MESSAGE_ID);

        PmDeletionQueue.finish(context, entry, PmDeletionEvent.Outcome.CONFIRMED);
        assertTrue(pendingIds().isEmpty());

        assertFalse(hasNotification(null, MESSAGE_ID));
        assertTrue(hasNotification(null, MESSAGE_ID + 1));
        assertTrue(hasNotification("download", MESSAGE_ID));
        assertTrue(store.findIncludingCompleted(accountKey, MESSAGE_ID).confirmed);
    }

    /** An expired deletion leaves the actual message's notification because server deletion was not confirmed. */
    @Test
    public void expiredCompletionKeepsMessageNotification() throws Exception {
        PmDeletionStore.Entry entry = store.insert(accountKey, 12, MESSAGE_ID,
                System.currentTimeMillis() - PmDeletionRetryPolicy.LIFETIME_MS);
        postNotification(null, MESSAGE_ID);

        PmDeletionQueue.finish(context, entry, PmDeletionEvent.Outcome.EXPIRED);
        assertTrue(pendingIds().isEmpty());

        assertTrue(hasNotification(null, MESSAGE_ID));
        assertNull(store.findIncludingCompleted(accountKey, MESSAGE_ID));
    }

    /** A stale worker generation cannot dismiss the notification of a newly accepted deletion for the same ID. */
    @Test
    public void staleWorkerCompletionKeepsReplacementNotification() throws Exception {
        long now = System.currentTimeMillis();
        PmDeletionStore.Entry oldEntry = store.insert(accountKey, 12, MESSAGE_ID, now - 1);
        assertTrue(store.complete(oldEntry));
        PmDeletionStore.Entry replacement = store.insert(accountKey, 12, MESSAGE_ID, now);
        postNotification(null, MESSAGE_ID);

        PmDeletionQueue.finish(context, oldEntry, PmDeletionEvent.Outcome.CONFIRMED);
        assertEquals(Collections.singleton(MESSAGE_ID), pendingIds());

        assertEquals(replacement.createdAt, store.find(accountKey, MESSAGE_ID).createdAt);
        assertTrue(hasNotification(null, MESSAGE_ID));
    }

    /** Recovery repairs lost acceptance callbacks while preserving expired work and other-account notifications. */
    @Test
    public void recoveryDismissesAcceptedAndConfirmedNotificationsWithoutRequiringScheduling() throws Exception {
        long now = System.currentTimeMillis();
        store.insert(accountKey, 12, MESSAGE_ID, now);
        assertTrue(store.complete(store.insert(accountKey, 12, MESSAGE_ID + 1, now)));
        store.insert(accountKey, 12, MESSAGE_ID + 2, now - PmDeletionRetryPolicy.LIFETIME_MS);
        store.insert(PmDeletionAccount.key("Bob", 25), 25, MESSAGE_ID + 3, now);
        postNotification(null, MESSAGE_ID);
        postNotification(null, MESSAGE_ID + 1);
        postNotification(null, MESSAGE_ID + 2);
        postNotification(null, MESSAGE_ID + 3);
        postNotification("download", MESSAGE_ID);
        AtomicInteger attempts = new AtomicInteger();

        PmDeletionQueue.resume(context, (appContext, entry, policy) -> {
            attempts.incrementAndGet();
            throw new IOException("Controlled scheduling failure");
        });
        assertEquals(Collections.singleton(MESSAGE_ID), pendingIds());

        assertEquals(2, attempts.get());
        assertFalse(hasNotification(null, MESSAGE_ID));
        assertFalse(hasNotification(null, MESSAGE_ID + 1));
        assertTrue(hasNotification(null, MESSAGE_ID + 2));
        assertTrue(hasNotification(null, MESSAGE_ID + 3));
        assertTrue(hasNotification("download", MESSAGE_ID));
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
        postNotification(null, MESSAGE_ID);
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
            assertTrue(hasNotification(null, MESSAGE_ID));
        } finally {
            database.execSQL("DROP TRIGGER queue_test_reject");
        }

        PmDeletionQueue.resume(context,
                (appContext, entry, policy) -> scheduled.incrementAndGet());
        assertTrue(pendingIds().isEmpty());
        assertEquals(0, scheduled.get());
    }

    /** Posts through Android's real notification service using the PM channel and an optional unrelated-work tag. */
    private void postNotification(String tag, int messageId) {
        notifications.notify(tag, messageId, new Notification.Builder(context, PmNotifications.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Message " + messageId).build());
    }

    /** Checks both tag and numeric ID so a download cannot be mistaken for a PM with the same server ID. */
    private boolean hasNotification(String tag, int messageId) {
        for (StatusBarNotification notification : notifications.getActiveNotifications()) {
            if (notification.getId() == messageId && Objects.equals(tag, notification.getTag())) return true;
        }
        return false;
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
        assertNull(store.findIncludingCompleted(accountKey, MESSAGE_ID));
    }

    /** A worker result remains available after database reopening even when its EventBus callback was lost. */
    @Test
    public void confirmedDeletionSurvivesColdLookupBeyondRetryLifetime() throws Exception {
        long createdAt = System.currentTimeMillis() - PmDeletionRetryPolicy.LIFETIME_MS - 1;
        PmDeletionStore.Entry entry = store.insert(accountKey, 12, MESSAGE_ID, createdAt);
        PmDeletionQueue.finish(context, entry, PmDeletionEvent.Outcome.CONFIRMED);
        store.close();

        assertEquals(PmDeletionQueue.DeletionState.COMPLETED, deletionState(accountKey, MESSAGE_ID));
        assertNull(store.find(accountKey, MESSAGE_ID));
        assertTrue(store.findIncludingCompleted(accountKey, MESSAGE_ID).confirmed);
    }

    /** A durable empty checkpoint detects completion after a clock rollback and survives a lost capture callback. */
    @Test
    public void completionLookupUsesDurableCausalCheckpoint() throws Exception {
        long createdAt = System.currentTimeMillis() - 1000;
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState(accountKey, MESSAGE_ID, "sheet", true, createdAt));
        PmDeletionStore.Entry entry = store.insert(accountKey, 12, MESSAGE_ID, createdAt);
        assertTrue(store.complete(entry, createdAt - TimeUnit.DAYS.toMillis(7)));
        store.close();

        assertEquals(PmDeletionQueue.DeletionState.COMPLETED,
                deletionState(accountKey, MESSAGE_ID, "sheet", false));
    }

    /** A fresh externally restored composer ignores the same existing receipt even if its timestamp is in the future. */
    @Test
    public void freshCheckpointIgnoresOldReceiptIndependentOfClock() throws Exception {
        PmDeletionStore.Entry entry = store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis());
        assertTrue(store.complete(entry, Long.MAX_VALUE));

        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                deletionState(accountKey, MESSAGE_ID, "new-sheet", true));
        store.close();
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                deletionState(accountKey, MESSAGE_ID, "new-sheet", false));
    }

    /** Legacy snapshots with existing receipts remain uncertain without replacing their missing historical baseline. */
    @Test
    public void missingCheckpointWithReceiptReturnsUntracked() throws Exception {
        assertTrue(store.complete(store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis())));

        assertEquals(PmDeletionQueue.DeletionState.UNTRACKED,
                deletionState(accountKey, MESSAGE_ID, "legacy-sheet", false));
        assertEquals(PmDeletionQueue.DeletionState.UNTRACKED,
                deletionState(accountKey, MESSAGE_ID, "legacy-sheet", false));
    }

    /** A snapshot lacking both checkpoint and receipt safely establishes its baseline for future completions. */
    @Test
    public void missingCheckpointWithoutReceiptCanBeEstablished() throws Exception {
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                deletionState(accountKey, MESSAGE_ID, "sheet", false));
        assertTrue(store.complete(store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis())));

        assertEquals(PmDeletionQueue.DeletionState.COMPLETED,
                deletionState(accountKey, MESSAGE_ID, "sheet", false));
    }

    /** A checkpoint permanently belongs to its captured message identity rather than the current adapter position. */
    @Test
    public void checkpointLookupRejectsAnotherMessageIdentity() throws Exception {
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                deletionState(accountKey, MESSAGE_ID, "sheet", true));

        assertEquals(PmDeletionQueue.DeletionState.UNAVAILABLE,
                deletionState(accountKey, MESSAGE_ID + 1, "sheet", false));
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                deletionState(accountKey, MESSAGE_ID, "sheet", false));
    }

    /** Expired pending work does not fabricate a receipt or prevent safe creation of an empty baseline. */
    @Test
    public void expiredIntentCanEstablishAnEmptyCheckpoint() throws Exception {
        store.insert(accountKey, 12, MESSAGE_ID,
                System.currentTimeMillis() - PmDeletionRetryPolicy.LIFETIME_MS);

        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                deletionState(accountKey, MESSAGE_ID, "sheet", false));
        assertNull(store.findIncludingCompleted(accountKey, MESSAGE_ID).receiptToken);
    }

    /** A failed checkpoint commit cannot report success or later treat an uncaptured receipt as an older baseline. */
    @Test
    public void failedCheckpointWriteRemainsUnavailableThenUntracked() throws Exception {
        SQLiteDatabase database = store.getWritableDatabase();
        database.execSQL("CREATE TRIGGER queue_test_reject_checkpoint BEFORE INSERT ON pm_composer_checkpoints "
                + "BEGIN SELECT RAISE(ABORT, 'Controlled checkpoint write failure'); END");
        try {
            assertEquals(PmDeletionQueue.DeletionState.UNAVAILABLE,
                    deletionState(accountKey, MESSAGE_ID, "sheet", true));
        } finally {
            database.execSQL("DROP TRIGGER queue_test_reject_checkpoint");
        }
        assertTrue(store.complete(store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis())));

        assertEquals(PmDeletionQueue.DeletionState.UNTRACKED,
                deletionState(accountKey, MESSAGE_ID, "sheet", false));
    }

    /** An old completion for another account or another message cannot close this restored draft. */
    @Test
    public void completionLookupIsScopedToAccountAndMessage() throws Exception {
        long now = System.currentTimeMillis();
        String otherAccount = PmDeletionAccount.key("Bob", 25);
        assertTrue(store.complete(store.insert(otherAccount, 25, MESSAGE_ID, now)));
        assertTrue(store.complete(store.insert(accountKey, 12, MESSAGE_ID + 1, now)));

        assertEquals(PmDeletionQueue.DeletionState.ABSENT, deletionState(accountKey, MESSAGE_ID));
    }

    /** Receipts never hide active server rows or cause app-start recovery to repeat an acknowledged mutation. */
    @Test
    public void completedIntentIsNeitherFilteredNorRescheduled() throws Exception {
        assertTrue(store.complete(store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis())));
        AtomicInteger scheduled = new AtomicInteger();

        PmDeletionQueue.resume(context,
                (appContext, entry, policy) -> scheduled.incrementAndGet());

        assertTrue(pendingIds().isEmpty());
        assertEquals(0, scheduled.get());
        assertEquals(PmDeletionQueue.DeletionState.COMPLETED, deletionState(accountKey, MESSAGE_ID));
    }

    /** An old worker's expiry cannot erase the persisted confirmation of a successful deletion. */
    @Test
    public void lateExpiryCannotEraseCompletedIntent() throws Exception {
        PmDeletionStore.Entry entry = store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis());
        PmDeletionQueue.finish(context, entry, PmDeletionEvent.Outcome.CONFIRMED);

        PmDeletionQueue.finish(context, entry, PmDeletionEvent.Outcome.EXPIRED);

        assertEquals(PmDeletionQueue.DeletionState.COMPLETED, deletionState(accountKey, MESSAGE_ID));
    }

    /** A restored composer sees its exact durable intent and receives the result on the UI thread. */
    @Test
    public void deletionLookupFindsExactAccountAndMessageOnMainThread() throws Exception {
        store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis());
        AtomicReference<PmDeletionQueue.DeletionState> result = new AtomicReference<>();
        AtomicReference<Looper> callbackLooper = new AtomicReference<>();

        PmDeletionQueue.getDeletionState(context, accountKey, MESSAGE_ID, state -> {
            callbackLooper.set(Looper.myLooper());
            result.set(state);
        });

        await(() -> result.get() != null);
        assertEquals(PmDeletionQueue.DeletionState.PENDING, result.get());
        assertEquals(Looper.getMainLooper(), callbackLooper.get());
    }

    /** A queued deletion for a different message never closes the currently restored composer. */
    @Test
    public void deletionLookupDoesNotMatchAnotherMessage() throws Exception {
        store.insert(accountKey, 12, MESSAGE_ID + 1, System.currentTimeMillis());

        assertEquals(PmDeletionQueue.DeletionState.ABSENT, deletionState(accountKey, MESSAGE_ID));
    }

    /** Server message IDs reused by another account do not make this account's draft look deleted. */
    @Test
    public void deletionLookupDoesNotMatchAnotherAccountsMessage() throws Exception {
        store.insert(PmDeletionAccount.key("Bob", 25), 25, MESSAGE_ID, System.currentTimeMillis());

        assertEquals(PmDeletionQueue.DeletionState.ABSENT, deletionState(accountKey, MESSAGE_ID));
    }

    /** A restored form stays usable after the six-hour intent deadline without renewing that intent. */
    @Test
    public void deletionLookupTreatsExpiredIntentAsAbsent() throws Exception {
        long createdAt = System.currentTimeMillis() - PmDeletionRetryPolicy.LIFETIME_MS;
        store.insert(accountKey, 12, MESSAGE_ID, createdAt);

        assertEquals(PmDeletionQueue.DeletionState.ABSENT, deletionState(accountKey, MESSAGE_ID));
        assertEquals(createdAt, store.find(accountKey, MESSAGE_ID).createdAt);
    }

    /** An account change invalidates a lookup before its result can enable another account's form. */
    @Test
    public void deletionLookupRejectsAccountChangeBeforeCallback() throws Exception {
        store.insert(accountKey, 12, MESSAGE_ID, System.currentTimeMillis());
        AtomicReference<PmDeletionQueue.DeletionState> result = new AtomicReference<>();

        PmDeletionQueue.getDeletionState(context, accountKey, MESSAGE_ID, "sheet", true, result::set);
        context.login = "Bob";

        await(() -> result.get() != null);
        assertEquals(PmDeletionQueue.DeletionState.UNAVAILABLE, result.get());
    }

    /** Missing account metadata or an invalid server message ID never permits a restored action. */
    @Test
    public void deletionLookupRejectsInvalidIdentity() throws Exception {
        assertEquals(PmDeletionQueue.DeletionState.UNAVAILABLE, deletionState(null, MESSAGE_ID));
        assertEquals(PmDeletionQueue.DeletionState.UNAVAILABLE, deletionState(accountKey, 0));
        assertEquals(PmDeletionQueue.DeletionState.UNAVAILABLE,
                deletionState(PmDeletionAccount.key("Bob", 25), MESSAGE_ID));
    }

    /** A database read failure cannot be mistaken for permission to reply to a possibly deleted message. */
    @Test
    public void deletionLookupReportsUnreadableStore() throws Exception {
        SQLiteDatabase database = store.getWritableDatabase();
        database.execSQL("DROP TABLE pm_deletions");
        try {
            assertEquals(PmDeletionQueue.DeletionState.UNAVAILABLE, deletionState(accountKey, MESSAGE_ID));
        } finally {
            database.execSQL("DROP TABLE pm_composer_checkpoints");
            store.onCreate(database);
        }
    }

    /** Reads one durable deletion state after its queue I/O and main-thread account guard complete. */
    private PmDeletionQueue.DeletionState deletionState(String account, int messageId) throws Exception {
        AtomicReference<PmDeletionQueue.DeletionState> result = new AtomicReference<>();
        PmDeletionQueue.getDeletionState(context, account, messageId, result::set);
        await(() -> result.get() != null);
        return result.get();
    }

    /** Reads account-scoped causal state through the queue's background checkpoint transaction and UI callback. */
    private PmDeletionQueue.DeletionState deletionState(String account, int messageId,
                                                       String checkpoint, boolean create) throws Exception {
        AtomicReference<PmDeletionQueue.DeletionState> result = new AtomicReference<>();
        PmDeletionQueue.getDeletionState(context, account, messageId, checkpoint, create, result::set);
        await(() -> result.get() != null);
        return result.get();
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
        private String login = "Alice";

        /** Installs only the application singleton needed by the queue's account guard. */
        @Override public void onCreate() {
            ReflectionHelpers.setStaticField(AppController.class, "sInstance", this);
        }
        /** Keeps these queue tests signed in to the original account. */
        @Override public int isAuth() { return 1; }
        /** Supplies the stable server user identity for durable work metadata. */
        @Override public int isUserId() { return 12; }
        /** Supplies a controllable fake login only for computing the non-secret account hash. */
        @Override public String userName(String defaultName) { return login; }
    }
}

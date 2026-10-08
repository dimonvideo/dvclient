package com.dimonvideo.client.util.pm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** Exercises real SQLite persistence, deduplication, and account isolation for offline deletions. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class PmDeletionStoreTest {
    private PmDeletionStore store;

    /** Starts each test with an empty queue without touching the application's feed database. */
    @Before
    public void clearQueue() {
        store = PmDeletionStore.get(RuntimeEnvironment.getApplication());
        for (PmDeletionStore.Entry entry : store.entries()) store.remove(entry);
    }

    /** Duplicate taps keep one persisted intent and do not reset its six-hour deadline. */
    @Test
    public void duplicateIntentPreservesItsOriginalDeadline() {
        store.insert("account", 12, 55, 1000);
        store.insert("account", 12, 55, 9999);
        assertEquals(1, store.entries().size());
        assertEquals(1000, store.find("account", 55).createdAt);
    }

    /** Removing one account's intent cannot acknowledge a different account's queued work. */
    @Test
    public void completionIsScopedToTheOriginalAccount() {
        store.insert("one", 12, 55, 1000);
        store.insert("two", 13, 55, 1000);
        store.remove(store.find("one", 55));
        assertNull(store.find("one", 55));
        assertNotNull(store.find("two", 55));
    }

    /** Retry state can be read back independently of the in-memory entry that started a worker. */
    @Test
    public void retryMetadataIsPersistedWithoutReplacingIntent() {
        PmDeletionStore.Entry original = store.insert("account", 12, 55, 1000);
        store.retry(original, 601000);
        PmDeletionStore.Entry recovered = store.find("account", 55);
        assertEquals(1, recovered.failures);
        assertEquals(601000, recovered.nextAttemptAt);
        assertEquals(1000, recovered.createdAt);
        assertEquals(12, recovered.userId);
    }

    /** Closing and reopening the SQLite connection recovers the same pending offline intent. */
    @Test
    public void intentSurvivesDatabaseReopening() {
        store.insert("account", 12, 55, 1000);
        store.close();
        PmDeletionStore.Entry recovered = store.find("account", 55);
        assertNotNull(recovered);
        assertEquals(1000, recovered.createdAt);
        assertEquals(12, recovered.userId);
    }

    /** Location hints remain available after reopening without altering the original retry deadline. */
    @Test
    public void sourcePageHintIsDurableAndDoesNotResetOnDuplicateTap() {
        store.insert("account", 12, 55, 1000, 2, 37);
        store.close();
        store.insert("account", 12, 55, 9000, 0, 1);
        PmDeletionStore.Entry recovered = store.find("account", 55);
        assertEquals(2, recovered.sourceFolder);
        assertEquals(37, recovered.sourcePage);
        assertEquals(1000, recovered.createdAt);
    }

    /** A fresh explicit deletion after expiry starts a new six-hour window instead of silently expiring again. */
    @Test
    public void explicitDeletionAfterExpiryRenewsTheIntent() {
        PmDeletionStore.Entry previous = store.insert("account", 12, 55, 1000);
        store.retry(previous, 601000);
        long renewedAt = 1000 + PmDeletionRetryPolicy.LIFETIME_MS;
        PmDeletionStore.Entry renewed = store.insert("account", 12, 55, renewedAt, 2, 37);
        assertEquals(renewedAt, renewed.createdAt);
        assertEquals(renewedAt, renewed.nextAttemptAt);
        assertEquals(0, renewed.failures);
        assertEquals(2, renewed.sourceFolder);
        assertEquals(37, renewed.sourcePage);
    }

    /** A stale worker cannot remove or overwrite a new user intent saved after its original expiry. */
    @Test
    public void staleWorkerCannotAcknowledgeOrRescheduleRenewedIntent() {
        PmDeletionStore.Entry previous = store.insert("account", 12, 55, 1000);
        long renewedAt = 1000 + PmDeletionRetryPolicy.LIFETIME_MS;
        store.insert("account", 12, 55, renewedAt);
        assertFalse(store.remove(previous));
        store.retry(previous, 601000);
        PmDeletionStore.Entry renewed = store.find("account", 55);
        assertNotNull(renewed);
        assertEquals(renewedAt, renewed.createdAt);
        assertEquals(renewedAt, renewed.nextAttemptAt);
        assertEquals(0, renewed.failures);
    }
}

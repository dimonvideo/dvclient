package com.dimonvideo.client.util.pm;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Exercises real SQLite persistence, deduplication, and account isolation for offline deletions. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class PmDeletionStoreTest {
    private PmDeletionStore store;

    /** Removes pending and confirmed fixture rows without touching the application's feed database. */
    @Before
    public void clearQueue() {
        store = PmDeletionStore.get(RuntimeEnvironment.getApplication());
        store.getWritableDatabase().delete("pm_deletions", null, null);
        store.getWritableDatabase().delete("pm_composer_checkpoints", null, null);
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
        assertFalse(store.complete(previous));
        assertFalse(store.findIncludingCompleted("account", 55).confirmed);
    }

    /** A terminal marker survives reopening while disappearing from the worker's pending-only queries. */
    @Test
    public void completionSurvivesReopeningWithoutRemainingQueued() {
        PmDeletionStore.Entry original = store.insert("account", 12, 55, 1000, 2, 37);

        assertTrue(store.complete(original));
        store.close();

        assertNull(store.find("account", 55));
        assertTrue(store.entries().isEmpty());
        PmDeletionStore.Entry completed = store.findIncludingCompleted("account", 55);
        assertNotNull(completed);
        assertTrue(completed.confirmed);
        assertEquals(original.createdAt, completed.createdAt);
        assertEquals(2, completed.sourceFolder);
        assertEquals(37, completed.sourcePage);
        assertTrue(completed.completedAt >= completed.createdAt);
        assertNotNull(completed.receiptToken);
        String receipt = completed.receiptToken;
        store.close();
        assertEquals(receipt, store.findIncludingCompleted("account", 55).receiptToken);
    }

    /** Marking an account's intent complete cannot change the same message ID in another account. */
    @Test
    public void confirmedMarkerIsScopedToExactAccount() {
        PmDeletionStore.Entry original = store.insert("one", 12, 55, 1000);
        store.insert("two", 13, 55, 1000);

        assertTrue(store.complete(original));

        assertTrue(store.findIncludingCompleted("one", 55).confirmed);
        assertFalse(store.findIncludingCompleted("two", 55).confirmed);
        assertNotNull(store.find("two", 55));
        assertEquals(1, store.entries().size());
    }

    /** Retried callbacks neither erase a completion receipt nor modify its immutable intent generation. */
    @Test
    public void completedMarkerCannotBeRemovedOrRetriedAsPending() {
        PmDeletionStore.Entry original = store.insert("account", 12, 55, 1000);
        assertTrue(store.complete(original));
        String receipt = store.findIncludingCompleted("account", 55).receiptToken;

        assertFalse(store.remove(original));
        assertFalse(store.complete(original));
        store.retry(original, 601000);

        PmDeletionStore.Entry completed = store.findIncludingCompleted("account", 55);
        assertTrue(completed.confirmed);
        assertEquals(0, completed.failures);
        assertEquals(1000, completed.nextAttemptAt);
        assertEquals(receipt, completed.receiptToken);
    }

    /** SQLite failure while recording completion preserves the pending intent for a safe retry. */
    @Test
    public void completionWriteFailurePreservesOriginalPendingIntent() {
        PmDeletionStore.Entry original = store.insert("account", 12, 55, 1000);
        SQLiteDatabase database = store.getWritableDatabase();
        database.execSQL("CREATE TRIGGER store_test_reject_completion BEFORE UPDATE OF confirmed "
                + "ON pm_deletions WHEN NEW.confirmed=1 "
                + "BEGIN SELECT RAISE(ABORT, 'Controlled completion write failure'); END");
        try {
            assertThrows(SQLiteException.class, () -> store.complete(original));
            assertNotNull(store.find("account", 55));
            assertFalse(store.findIncludingCompleted("account", 55).confirmed);
            assertEquals(1000, store.find("account", 55).createdAt);
            assertNull(store.find("account", 55).receiptToken);
        } finally {
            database.execSQL("DROP TRIGGER store_test_reject_completion");
        }
    }

    /** Explicit deletion after a server restore replaces a receipt with fresh pending retry metadata. */
    @Test
    public void explicitDeletionRenewsCompletedIntent() {
        PmDeletionStore.Entry original = store.insert("account", 12, 55, 1000, 0, 1);
        store.retry(original, 601000);
        assertTrue(store.complete(original));

        PmDeletionStore.Entry renewed = store.insert("account", 12, 55, 2000, 2, 37);

        assertFalse(renewed.confirmed);
        assertEquals(2000, renewed.createdAt);
        assertEquals(2000, renewed.nextAttemptAt);
        assertEquals(0, renewed.failures);
        assertEquals(2, renewed.sourceFolder);
        assertEquals(37, renewed.sourcePage);
        assertEquals(0, renewed.completedAt);
        assertNull(renewed.receiptToken);
        assertEquals(1, store.entries().size());
        assertFalse(store.complete(original));
    }

    /** Two explicit intents created in one millisecond still have different worker generations. */
    @Test
    public void sameTimestampRenewalCannotReuseCompletedGeneration() {
        PmDeletionStore.Entry original = store.insert("account", 12, 55, 1000);
        assertTrue(store.complete(original));

        PmDeletionStore.Entry renewed = store.insert("account", 12, 55, 1000);

        assertTrue(renewed.createdAt > original.createdAt);
        assertFalse(store.complete(original));
        assertFalse(store.findIncludingCompleted("account", 55).confirmed);
    }

    /** Upgrading the original queue schema preserves retry metadata and supplies both later additions. */
    @Test
    public void migrationFromVersionOnePreservesIntent() {
        prepareLegacyDatabase(1);

        PmDeletionStore.Entry migrated = store.find("account", 55);

        assertMigratedIntent(migrated);
        assertEquals(0, migrated.sourceFolder);
        assertEquals(1, migrated.sourcePage);
        assertTrue(store.complete(migrated));
        assertTrue(store.findIncludingCompleted("account", 55).confirmed);
    }

    /** Upgrading the source-hint schema keeps hints intact and initializes its pending completion state. */
    @Test
    public void migrationFromVersionTwoPreservesIntentAndHints() {
        prepareLegacyDatabase(2);

        PmDeletionStore.Entry migrated = store.find("account", 55);

        assertMigratedIntent(migrated);
        assertEquals(2, migrated.sourceFolder);
        assertEquals(37, migrated.sourcePage);
        assertTrue(store.complete(migrated));
        assertTrue(store.findIncludingCompleted("account", 55).confirmed);
    }

    /** Version three pending rows retain their retry generation while gaining checkpoint support. */
    @Test
    public void migrationFromVersionThreePreservesPendingIntent() {
        prepareLegacyDatabase(3);

        PmDeletionStore.Entry migrated = store.find("account", 55);

        assertMigratedIntent(migrated);
        assertEquals(2, migrated.sourceFolder);
        assertEquals(37, migrated.sourcePage);
        assertNull(migrated.receiptToken);
    }

    /** Existing confirmed rows receive one stable identity rather than losing their durable acknowledgement. */
    @Test
    public void migrationFromVersionThreeAssignsStableReceiptToken() {
        prepareLegacyDatabase(3, true);

        PmDeletionStore.Entry completed = store.findIncludingCompleted("account", 55);

        assertEquals(4, store.getReadableDatabase().getVersion());
        assertTrue(completed.confirmed);
        assertEquals(1000, completed.createdAt);
        assertEquals(9000, completed.completedAt);
        assertNotNull(completed.receiptToken);
        assertFalse(completed.receiptToken.isEmpty());
        String receipt = completed.receiptToken;
        store.close();
        assertEquals(receipt, store.findIncludingCompleted("account", 55).receiptToken);
        assertNull(store.find("account", 55));
    }

    /** A committed checkpoint is recoverable even if process death loses the callback that reported its capture. */
    @Test
    public void checkpointSurvivesLostCaptureCallbackAndDatabaseReopening() {
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState("account", 55, "sheet", true, 1000));
        PmDeletionStore.Entry entry = store.insert("account", 12, 55, 2000);
        assertTrue(store.complete(entry, 500));
        store.close();

        assertEquals(PmDeletionQueue.DeletionState.COMPLETED,
                store.composerState("account", 55, "sheet", false, 3000));
    }

    /** A fresh composer for an externally restored message recognizes the receipt it captured before becoming usable. */
    @Test
    public void freshCheckpointIgnoresMatchingOlderReceiptAcrossReopening() {
        assertTrue(store.complete(store.insert("account", 12, 55, 1000), Long.MAX_VALUE));
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState("account", 55, "fresh-sheet", true, 2000));
        store.close();

        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState("account", 55, "fresh-sheet", false, 3000));
    }

    /** Unknown legacy snapshots cannot consume an existing receipt and erase an unrelated preserved draft. */
    @Test
    public void missingCheckpointWithReceiptRemainsUntracked() {
        assertTrue(store.complete(store.insert("account", 12, 55, 1000), 500));

        assertEquals(PmDeletionQueue.DeletionState.UNTRACKED,
                store.composerState("account", 55, "legacy-sheet", false, 2000));
        assertEquals(PmDeletionQueue.DeletionState.UNTRACKED,
                store.composerState("account", 55, "legacy-sheet", false, 3000));
    }

    /** An absent receipt permits a missing checkpoint to establish a known empty baseline for later completion. */
    @Test
    public void missingCheckpointWithoutReceiptEstablishesEmptyBaseline() {
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState("account", 55, "restored-sheet", false, 1000));
        assertTrue(store.complete(store.insert("account", 12, 55, 2000), 9000));

        assertEquals(PmDeletionQueue.DeletionState.COMPLETED,
                store.composerState("account", 55, "restored-sheet", false, 3000));
    }

    /** Receipt renewal changes causal identity even if completion timestamps repeat or move backwards. */
    @Test
    public void renewedReceiptCannotMatchOlderCheckpointAfterClockChange() {
        PmDeletionStore.Entry original = store.insert("account", 12, 55, 1000);
        assertTrue(store.complete(original, 9000));
        String previousReceipt = store.findIncludingCompleted("account", 55).receiptToken;
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState("account", 55, "sheet", true, 10000));
        PmDeletionStore.Entry renewed = store.insert("account", 12, 55, 500);
        assertNull(renewed.receiptToken);
        assertTrue(store.complete(renewed, 400));

        assertNotEquals(previousReceipt, store.findIncludingCompleted("account", 55).receiptToken);
        assertEquals(PmDeletionQueue.DeletionState.COMPLETED,
                store.composerState("account", 55, "sheet", false, 2000));
    }

    /** Reusing another account's or message's checkpoint cannot authorize the newly supplied identity. */
    @Test
    public void checkpointCannotBeReboundToDifferentAccountOrMessage() {
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState("account", 55, "sheet", true, 1000));

        assertEquals(PmDeletionQueue.DeletionState.UNAVAILABLE,
                store.composerState("other-account", 55, "sheet", false, 1000));
        assertEquals(PmDeletionQueue.DeletionState.UNAVAILABLE,
                store.composerState("account", 56, "sheet", true, 1000));
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState("account", 55, "sheet", false, 1000));
    }

    /** A pruned snapshot preserves uncertain draft state while the latest live checkpoint still recognizes its receipt. */
    @Test
    public void prunedCheckpointWithReceiptBecomesUntracked() {
        assertTrue(store.complete(store.insert("account", 12, 55, 1000), 9000));
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState("account", 55, "old-sheet", true, 10000));
        for (int index = 0; index < 257; index++) {
            assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                    store.composerState("account", 55, "new-sheet-" + index, true, 10000));
        }

        assertEquals(PmDeletionQueue.DeletionState.UNTRACKED,
                store.composerState("account", 55, "old-sheet", false, 10000));
        assertEquals(PmDeletionQueue.DeletionState.ABSENT,
                store.composerState("account", 55, "new-sheet-256", false, 10000));
    }

    /** Reopens an actual previous-version SQLite file so the production helper performs its migration. */
    private void prepareLegacyDatabase(int version) {
        prepareLegacyDatabase(version, false);
    }

    /** Builds an old pending or confirmed fixture without running migration until the next helper read. */
    private void prepareLegacyDatabase(int version, boolean confirmed) {
        String path = store.getWritableDatabase().getPath();
        store.close();
        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(path, null,
                SQLiteDatabase.OPEN_READWRITE)) {
            database.execSQL("DROP TABLE pm_deletions");
            database.execSQL("DROP TABLE pm_composer_checkpoints");
            String hints = version >= 2
                    ? ", source_folder INTEGER NOT NULL DEFAULT 0, source_page INTEGER NOT NULL DEFAULT 1" : "";
            String completion = version == 3
                    ? ", confirmed INTEGER NOT NULL DEFAULT 0, completed_at INTEGER NOT NULL DEFAULT 0" : "";
            database.execSQL("CREATE TABLE pm_deletions (account_key TEXT NOT NULL, "
                    + "message_id INTEGER NOT NULL, user_id INTEGER NOT NULL, created_at INTEGER NOT NULL, "
                    + "next_attempt_at INTEGER NOT NULL, failures INTEGER NOT NULL DEFAULT 0"
                    + hints + completion + ", PRIMARY KEY(account_key, message_id))");
            String values = "'account',55,12,1000,601000,2";
            if (version >= 2) values += ",2,37";
            if (version == 3) values += confirmed ? ",1,9000" : ",0,0";
            database.execSQL("INSERT INTO pm_deletions VALUES (" + values + ")");
            if (version == 3) {
                database.execSQL("CREATE INDEX pm_deletions_pending ON pm_deletions(created_at) WHERE confirmed=0");
            }
            database.setVersion(version);
        }
    }

    /** Checks the identity, generation, and retry state common to all supported old schema versions. */
    private void assertMigratedIntent(PmDeletionStore.Entry migrated) {
        assertNotNull(migrated);
        assertEquals(4, store.getReadableDatabase().getVersion());
        assertEquals("account", migrated.accountKey);
        assertEquals(55, migrated.messageId);
        assertEquals(12, migrated.userId);
        assertEquals(1000, migrated.createdAt);
        assertEquals(601000, migrated.nextAttemptAt);
        assertEquals(2, migrated.failures);
        assertFalse(migrated.confirmed);
        assertEquals(0, migrated.completedAt);
        assertNull(migrated.receiptToken);
    }
}

package com.dimonvideo.client.util.pm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.robolectric.util.ReflectionHelpers;

import java.io.File;

/** Exposes the real package-private durable queue boundary to full Android fragment-restoration tests. */
public final class PmDeletionDurableFixture {
    /** Prevents creation of a stateless test-only SQLite fixture. */
    private PmDeletionDurableFixture() { }

    /** Removes all intents and completion receipts by recreating this test application's no-backup database. */
    public static void reset(Context context) {
        forgetHelper();
        SQLiteDatabase.deleteDatabase(new File(context.getNoBackupFilesDir(), "pm_deletions.db"));
    }

    /** Completes a real persisted intent through the worker's production acknowledgement path without invoking lost UI callbacks. */
    public static void confirm(Context context, String account, int userId, int messageId,
                               int folder, long createdAt) {
        persist(context, account, userId, messageId, folder, createdAt);
        completeConfirmed(context, account, messageId);
    }

    /** Completes a persisted generation with an explicit clock value to exercise corrections in either direction. */
    public static void confirmAt(Context context, String account, int userId, int messageId,
                                 int folder, long createdAt, long completedAt) {
        PmDeletionStore store = PmDeletionStore.get(context);
        PmDeletionStore.Entry entry = store.insert(account, userId, messageId, createdAt, folder, 1);
        if (!store.complete(entry, completedAt)) {
            throw new IllegalStateException("The test completion was not persisted");
        }
    }

    /** Persists a real opening checkpoint before its asynchronous UI callback is deliberately lost. */
    public static void capture(Context context, String account, int messageId, String checkpointId) {
        PmDeletionQueue.DeletionState state = PmDeletionStore.get(context).composerState(
                account, messageId, checkpointId, true, System.currentTimeMillis());
        if (state != PmDeletionQueue.DeletionState.ABSENT) {
            throw new IllegalStateException("The test checkpoint did not capture a usable source");
        }
    }

    /** Persists the same intent that exists before process death while leaving its acceptance callback undelivered. */
    public static void persist(Context context, String account, int userId, int messageId,
                               int folder, long createdAt) {
        PmDeletionStore.get(context).insert(account, userId, messageId, createdAt, folder, 1);
    }

    /** Completes the already persisted generation after the original Android host has been destroyed. */
    public static void completeConfirmed(Context context, String account, int messageId) {
        PmDeletionStore.Entry entry = PmDeletionStore.get(context).find(account, messageId);
        if (entry == null) throw new IllegalStateException("The test intent was not persisted");
        PmDeletionQueue.finish(context, entry, PmDeletionEvent.Outcome.CONFIRMED);
    }

    /** Distinguishes a finished deletion from a surviving pending row before the restored dialog reads its receipt. */
    public static boolean hasPending(Context context, String account, int messageId) {
        return PmDeletionStore.get(context).find(account, messageId) != null;
    }

    /** Reopens SQLite using a fresh helper so process-restoration tests cannot pass through in-memory store state. */
    public static void reopen(Context context) {
        forgetHelper();
        PmDeletionStore.get(context).getReadableDatabase();
    }

    /** Releases the singleton before deleting or reopening its actual SQLite file. */
    private static void forgetHelper() {
        PmDeletionStore previous = ReflectionHelpers.getStaticField(PmDeletionStore.class, "instance");
        if (previous != null) previous.close();
        ReflectionHelpers.setStaticField(PmDeletionStore.class, "instance", null);
    }
}

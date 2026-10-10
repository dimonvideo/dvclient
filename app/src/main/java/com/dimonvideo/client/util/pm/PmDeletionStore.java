package com.dimonvideo.client.util.pm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Durable queue and completion receipts kept outside the application cache and excluded from backup. */
final class PmDeletionStore extends SQLiteOpenHelper {
    private static PmDeletionStore instance;

    /** Stores only message identity and retry metadata; passwords never enter this database. */
    static final class Entry {
        final String accountKey;
        final int messageId;
        final int userId;
        final long createdAt;
        final long nextAttemptAt;
        final int failures;
        final int sourceFolder;
        final int sourcePage;
        final boolean confirmed;
        final long completedAt;

        /** Copies immutable intent and completion metadata from the queue cursor. */
        Entry(Cursor cursor) {
            accountKey = cursor.getString(cursor.getColumnIndexOrThrow("account_key"));
            messageId = cursor.getInt(cursor.getColumnIndexOrThrow("message_id"));
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
            nextAttemptAt = cursor.getLong(cursor.getColumnIndexOrThrow("next_attempt_at"));
            failures = cursor.getInt(cursor.getColumnIndexOrThrow("failures"));
            sourceFolder = cursor.getInt(cursor.getColumnIndexOrThrow("source_folder"));
            sourcePage = cursor.getInt(cursor.getColumnIndexOrThrow("source_page"));
            confirmed = cursor.getInt(cursor.getColumnIndexOrThrow("confirmed")) != 0;
            completedAt = cursor.getLong(cursor.getColumnIndexOrThrow("completed_at"));
        }
    }

    /** Opens the persistent no-backup database without changing the app's Room schema. */
    private PmDeletionStore(Context context) {
        super(context, new File(context.getNoBackupFilesDir(), "pm_deletions.db").getPath(),
                null, 3);
        setWriteAheadLoggingEnabled(true);
    }

    /** Returns the process-wide database helper; callers perform I/O on background threads. */
    static synchronized PmDeletionStore get(Context context) {
        if (instance == null) instance = new PmDeletionStore(context.getApplicationContext());
        return instance;
    }

    /** Creates the queue's account/message uniqueness constraint and retry columns. */
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE pm_deletions (account_key TEXT NOT NULL, "
                + "message_id INTEGER NOT NULL, user_id INTEGER NOT NULL, "
                + "created_at INTEGER NOT NULL, next_attempt_at INTEGER NOT NULL, "
                + "failures INTEGER NOT NULL DEFAULT 0, source_folder INTEGER NOT NULL DEFAULT 0, "
                + "source_page INTEGER NOT NULL DEFAULT 1, confirmed INTEGER NOT NULL DEFAULT 0, "
                + "completed_at INTEGER NOT NULL DEFAULT 0, "
                + "PRIMARY KEY(account_key, message_id))");
        createPendingIndex(database);
    }

    /** Preserves queued generations while adding source hints and durable completion acknowledgements. */
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 1 || oldVersion > 2 || newVersion != 3) {
            throw new IllegalStateException("A deletion queue migration is required");
        }
        if (oldVersion < 2) {
            database.execSQL("ALTER TABLE pm_deletions ADD COLUMN source_folder INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE pm_deletions ADD COLUMN source_page INTEGER NOT NULL DEFAULT 1");
        }
        database.execSQL("ALTER TABLE pm_deletions ADD COLUMN confirmed INTEGER NOT NULL DEFAULT 0");
        database.execSQL("ALTER TABLE pm_deletions ADD COLUMN completed_at INTEGER NOT NULL DEFAULT 0");
        createPendingIndex(database);
    }

    /** Keeps recovery scans proportional to pending work as completed receipts accumulate. */
    private void createPendingIndex(SQLiteDatabase database) {
        database.execSQL("CREATE INDEX pm_deletions_pending ON pm_deletions(created_at) WHERE confirmed=0");
    }

    /** Commits a new intent, preserving the original deadline when it was already queued. */
    Entry insert(String accountKey, int userId, int messageId, long now) {
        return insert(accountKey, userId, messageId, now, 0, 1);
    }

    /**
     * Saves location hints and renews an expired intent only after a new explicit user deletion.
     * Duplicate taps within the original lifetime preserve its original six-hour deadline.
     * A confirmed generation is replaced only by a new explicit deletion, never by recovery.
     */
    Entry insert(String accountKey, int userId, int messageId, long now, int folder, int page) {
        ContentValues values = new ContentValues();
        values.put("account_key", accountKey);
        values.put("user_id", userId);
        values.put("message_id", messageId);
        values.put("created_at", now);
        values.put("next_attempt_at", now);
        values.put("failures", 0);
        values.put("source_folder", folder == 1 || folder == 2 || folder == 3 || folder == 5 ? folder : 0);
        values.put("source_page", Math.max(1, page));
        values.put("confirmed", 0);
        values.put("completed_at", 0);
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        Entry entry;
        try {
            Entry previous = findIncludingCompleted(accountKey, messageId);
            if (previous != null && (previous.confirmed
                    || PmDeletionRetryPolicy.isExpired(previous.createdAt, now))) {
                // Preserve generation uniqueness even when a completed row is re-deleted in the same millisecond.
                if (previous.confirmed) values.put("created_at", Math.max(now, previous.createdAt + 1));
                database.update("pm_deletions", values, "account_key=? AND message_id=?",
                        new String[]{accountKey, String.valueOf(messageId)});
            } else {
                database.insertWithOnConflict("pm_deletions", null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
            entry = find(accountKey, messageId);
            if (entry == null) throw new IllegalStateException("Deletion intent was not saved");
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return entry;
    }

    /** Reads one pending account/message intent; completed receipts never authorize another worker. */
    Entry find(String accountKey, int messageId) {
        Entry entry = findIncludingCompleted(accountKey, messageId);
        return entry != null && !entry.confirmed ? entry : null;
    }

    /** Reads pending or confirmed state in one query so completion cannot create an absent-record gap. */
    Entry findIncludingCompleted(String accountKey, int messageId) {
        try (Cursor cursor = getReadableDatabase().query("pm_deletions", null,
                "account_key=? AND message_id=?", new String[]{accountKey,
                        String.valueOf(messageId)}, null, null, null)) {
            return cursor.moveToFirst() ? new Entry(cursor) : null;
        }
    }

    /** Reads queued intents for recovery and for filtering a refreshed message list. */
    List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("pm_deletions", null,
                "confirmed=0", null, null, null, "created_at ASC")) {
            while (cursor.moveToNext()) entries.add(new Entry(cursor));
        }
        return entries;
    }

    /** Saves a failed attempt's next wake-up before scheduling its successor worker. */
    void retry(Entry entry, long nextAttemptAt) {
        ContentValues values = new ContentValues();
        values.put("failures", entry.failures + 1);
        values.put("next_attempt_at", nextAttemptAt);
        getWritableDatabase().update("pm_deletions", values,
                "account_key=? AND message_id=? AND created_at=? AND confirmed=0", new String[]{entry.accountKey,
                        String.valueOf(entry.messageId), String.valueOf(entry.createdAt)});
    }

    /**
     * Atomically retains a server-confirmed generation instead of deleting its only durable evidence.
     * Receipts outlive the retry deadline because Android may restore a much older dialog snapshot.
     * The exact generation guard prevents a stale worker from acknowledging a newer deletion.
     */
    boolean complete(Entry entry) {
        return complete(entry, System.currentTimeMillis());
    }

    /** Records the completion instant so only snapshots predating that result are invalidated. */
    boolean complete(Entry entry, long completedAt) {
        ContentValues values = new ContentValues();
        values.put("confirmed", 1);
        values.put("completed_at", completedAt);
        return getWritableDatabase().update("pm_deletions", values,
                "account_key=? AND message_id=? AND created_at=? AND confirmed=0",
                new String[]{entry.accountKey, String.valueOf(entry.messageId),
                        String.valueOf(entry.createdAt)}) > 0;
    }

    /** Removes an expired pending generation without erasing a server-confirmed receipt. */
    boolean remove(Entry entry) {
        return getWritableDatabase().delete("pm_deletions",
                "account_key=? AND message_id=? AND created_at=? AND confirmed=0",
                new String[]{entry.accountKey, String.valueOf(entry.messageId),
                        String.valueOf(entry.createdAt)}) > 0;
    }
}

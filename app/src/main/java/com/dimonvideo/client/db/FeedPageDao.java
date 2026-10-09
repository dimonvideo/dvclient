package com.dimonvideo.client.db;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/** Keeps bounded offline list snapshots while preserving the exact server page order. */
@Dao
public abstract class FeedPageDao {
    /** Returns only the requested section, category, search, and account scope in page order. */
    @Query("SELECT * FROM feed_pages WHERE scope_key = :scopeKey ORDER BY page ASC")
    public abstract List<FeedPageEntity> loadPages(@NonNull String scopeKey);

    /**
     * Saves the first five successful pages and retains the forty most recently refreshed scopes.
     * A new first page starts a new snapshot, including an authoritative empty response, so old
     * later pages cannot reappear after a refresh. Pages outside the offline window are ignored.
     */
    @Transaction
    public void savePage(@NonNull String scopeKey, int page, @NonNull String payload, long savedAt) {
        if (scopeKey == null || scopeKey.isEmpty() || payload == null) {
            throw new IllegalArgumentException("A list snapshot requires a scope and response");
        }
        if (page < 1 || page > 5) return;
        if (page == 1) deleteScope(scopeKey);
        insert(new FeedPageEntity(scopeKey, page, payload, savedAt));
        pruneScopes();
    }

    /** Clears offline list snapshots when the user explicitly clears downloaded data. */
    @Query("DELETE FROM feed_pages")
    public abstract void clearAll();

    /** Replaces exactly one successful page within its request scope. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void insert(FeedPageEntity page);

    /** Removes an obsolete snapshot before storing its newly refreshed first page. */
    @Query("DELETE FROM feed_pages WHERE scope_key = :scopeKey")
    protected abstract void deleteScope(String scopeKey);

    /** Evicts every page of older scopes together rather than leaving partial orphan snapshots. */
    @Query("DELETE FROM feed_pages WHERE scope_key NOT IN "
            + "(SELECT scope_key FROM feed_pages GROUP BY scope_key "
            + "ORDER BY MAX(saved_at) DESC, scope_key ASC LIMIT 40)")
    protected abstract void pruneScopes();
}

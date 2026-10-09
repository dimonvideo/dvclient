package com.dimonvideo.client.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

/** Stores an exact successful list response without losing HTML or mixing sections and filters. */
@Entity(tableName = "feed_pages", primaryKeys = {"scope_key", "page"})
public class FeedPageEntity {
    @NonNull
    @ColumnInfo(name = "scope_key")
    public String scopeKey;

    public int page;

    @NonNull
    public String payload;

    @ColumnInfo(name = "saved_at")
    public long savedAt;

    /** Creates a persisted response for one page of one exact list request. */
    public FeedPageEntity(@NonNull String scopeKey, int page, @NonNull String payload, long savedAt) {
        this.scopeKey = scopeKey;
        this.page = page;
        this.payload = payload;
        this.savedAt = savedAt;
    }
}

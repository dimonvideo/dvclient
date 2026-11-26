/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Database(entities = {FeedEntity.class, ReadMarkEntity.class, FeedPageEntity.class}, version = 9, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public static long feedCutoffSeconds() {
        return Instant.now().getEpochSecond() - TimeUnit.DAYS.toSeconds(30);
    }

    /** Provides the existing downloaded-item cache. */
    public abstract FeedDao feedDao();

    /** Provides read markers independently of downloaded content and list snapshots. */
    public abstract ReadMarkDao readMarkDao();

    /** Provides exact successful list pages for section-aware offline restoration. */
    public abstract FeedPageDao feedPageDao();

    /** Adds durable list snapshots without discarding previously downloaded items or read marks. */
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        /** Creates the new cache table while preserving every existing version-seven table. */
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS feed_pages ("
                    + "scope_key TEXT NOT NULL, page INTEGER NOT NULL, payload TEXT NOT NULL, "
                    + "saved_at INTEGER NOT NULL, PRIMARY KEY(scope_key, page))");
        }
    };

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE read_marks ADD COLUMN marked_at INTEGER NOT NULL DEFAULT 0");
            // The old table has no dates. Keep its marks for one full retention window.
            database.execSQL("UPDATE read_marks SET marked_at = CAST(strftime('%s', 'now') AS INTEGER)");
            database.execSQL("CREATE INDEX index_read_marks_marked_at ON read_marks(marked_at)");
            database.execSQL("CREATE INDEX index_data_time ON data(time)");
            database.execSQL("CREATE INDEX index_data_razdel_time_lid ON data(razdel, time, lid)");
            database.execSQL("DROP INDEX index_data_razdel");
            database.execSQL("DROP INDEX index_data_lid");
        }
    };

    private static volatile AppDatabase INSTANCE;

    /** Opens the application database once and upgrades older snapshots without destructive fallback. */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "client_db")
                            .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

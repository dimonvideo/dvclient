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

@Database(entities = {FeedEntity.class, ReadMarkEntity.class, FeedPageEntity.class}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
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

    private static volatile AppDatabase INSTANCE;

    /** Opens the application database once and upgrades older snapshots without destructive fallback. */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "client_db")
                            .addMigrations(MIGRATION_7_8)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

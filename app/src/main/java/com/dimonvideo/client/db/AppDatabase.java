package com.dimonvideo.client.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {FeedEntity.class, ReadMarkEntity.class}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract FeedDao feedDao();
    public abstract ReadMarkDao readMarkDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "client_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface FeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FeedEntity feed);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    void insertAll(List<FeedEntity> feeds);

    @Query("SELECT * FROM data WHERE razdel = :razdel AND time > :cutoffSeconds "
            + "ORDER BY time DESC, lid DESC LIMIT :limit")
    List<FeedEntity> getRecentRows(String razdel, long cutoffSeconds, int limit);

    @Query("DELETE FROM data")
    void clearDB();

    /** Deletes a bounded batch across all sections using publication times in Unix seconds. */
    @Query("DELETE FROM data WHERE id IN (SELECT id FROM data "
            + "WHERE time <= :cutoffSeconds ORDER BY time LIMIT :limit)")
    int deleteExpired(long cutoffSeconds, int limit);
}

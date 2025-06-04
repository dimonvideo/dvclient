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

    @Query("SELECT * FROM data WHERE razdel = :razdel_name ORDER BY time DESC LIMIT :limit")
    List<FeedEntity> getAllRows(String razdel_name, int limit);

    @Query("DELETE FROM data")
    void clearDB();

    @Query("DELETE FROM data WHERE time <= date('now','-30 day')")
    void clearDBOld();
}
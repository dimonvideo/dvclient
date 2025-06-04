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

import java.util.List;

@Dao
public interface ReadMarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReadMarkEntity readMark);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ReadMarkEntity> readMarks);

    @Query("SELECT status FROM read_marks WHERE lid = :lid AND razdel = :razdel LIMIT 1")
    int getStatus(int lid, String razdel);

    @Query("DELETE FROM read_marks WHERE lid = :lid AND razdel = :razdel")
    void delete(int lid, String razdel);

    @Query("DELETE FROM read_marks")
    void clearAll();

    @Query("UPDATE read_marks SET status = 1 WHERE razdel = :razdel")
    void markAllRead(String razdel);
}
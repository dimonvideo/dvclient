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

import java.util.ArrayList;
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

    @Query("DELETE FROM read_marks WHERE id NOT IN (SELECT id FROM read_marks ORDER BY id DESC LIMIT 200)")
    void clearAll();

    @Transaction
    default void markAllRead(String razdel, FeedDao feedDao) {
        // Получаем все записи из таблицы data для указанного razdel
        List<FeedEntity> feedEntities = feedDao.getAllRows(razdel, Integer.MAX_VALUE);

        // Создаем список ReadMarkEntity для вставки
        List<ReadMarkEntity> readMarks = new ArrayList<>();
        for (FeedEntity feed : feedEntities) {
            ReadMarkEntity readMark = new ReadMarkEntity();
            readMark.lid = feed.lid;
            readMark.razdel = feed.razdel;
            readMark.status = 1;
            readMarks.add(readMark);
        }

        // Вставляем или обновляем записи в read_marks
        if (!readMarks.isEmpty()) {
            insertAll(readMarks);
        }
    }
}
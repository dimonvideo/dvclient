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

    /** Keeps read history independently of the cache, including items opened from a link. */
    @Query("DELETE FROM read_marks WHERE id IN (SELECT id FROM read_marks "
            + "WHERE marked_at <= :cutoffSeconds ORDER BY marked_at LIMIT :limit)")
    int deleteExpired(long cutoffSeconds, int limit);

    /** Marks the latest cached articles that still fall within the retention window. */
    @Query("INSERT OR REPLACE INTO read_marks (lid, razdel, status, marked_at) "
            + "SELECT lid, razdel, 1, CAST(strftime('%s', 'now') AS INTEGER) FROM data "
            + "WHERE razdel = :razdel AND lid > 0 AND time > :cutoffSeconds "
            + "ORDER BY time DESC, lid DESC LIMIT :limit")
    void markRecentRead(String razdel, int limit, long cutoffSeconds);
}

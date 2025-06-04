/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "read_marks", indices = {
        @Index(value = {"lid", "razdel"}, unique = true)
})
public class ReadMarkEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "lid")
    public int lid;

    @ColumnInfo(name = "razdel")
    public String razdel;

    @ColumnInfo(name = "status")
    public int status;
}
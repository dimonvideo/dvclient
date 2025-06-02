package com.dimonvideo.client.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "data", indices = {
        @Index(value = {"lid"}),
        @Index(value = {"razdel"}),
        @Index(value = {"lid", "razdel"}, unique = true)
})
public class FeedEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "lid")
    public int lid;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "text")
    public String description;

    @ColumnInfo(name = "full_text")
    public String fullText;

    @ColumnInfo(name = "date")
    public String date;

    @ColumnInfo(name = "time")
    public long timestamp;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "img")
    public String img;

    @ColumnInfo(name = "razdel")
    public String razdel;

    @ColumnInfo(name = "size")
    public String size;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "state")
    public int state;
}
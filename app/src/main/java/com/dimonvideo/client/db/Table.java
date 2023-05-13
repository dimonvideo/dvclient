package com.dimonvideo.client.db;

import android.database.sqlite.SQLiteDatabase;

public class Table {
    public static final String TABLE_NAME = "data";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_LID = "lid";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_FULL_TEXT = "full_text";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TIMESTAMP = "time";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_IMG = "img";
    public static final String COLUMN_RAZDEL = "razdel";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_STATE = "state";

    public static final String[] ALL_KEYS = new String[] {COLUMN_ID, COLUMN_LID, COLUMN_STATUS, COLUMN_TITLE, COLUMN_TEXT, COLUMN_FULL_TEXT,
            COLUMN_DATE, COLUMN_TIMESTAMP, COLUMN_CATEGORY, COLUMN_IMG, COLUMN_RAZDEL, COLUMN_SIZE, COLUMN_URL, COLUMN_STATE};

    private static final String DATABASE_CREATE = "create table "
            + TABLE_NAME
            + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_LID + " integer,"
            + COLUMN_STATUS + " integer,"
            + COLUMN_TITLE + " text,"
            + COLUMN_TEXT + " text,"
            + COLUMN_FULL_TEXT + " text,"
            + COLUMN_DATE + " text,"
            + COLUMN_TIMESTAMP + " text,"
            + COLUMN_CATEGORY + " text,"
            + COLUMN_IMG + " text,"
            + COLUMN_RAZDEL + " text,"
            + COLUMN_SIZE + " text,"
            + COLUMN_URL + " text,"
            + COLUMN_STATE + " integer"
            + ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(database);
    }
}

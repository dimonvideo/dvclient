package com.dimonvideo.client.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

public class Provider extends ContentProvider {
    private static SQLiteOpenHelper database;

    private static final int MESSAGES = 10;
    private static final int MESSAGE_ID = 20;

    private static final String AUTHORITY = "com.dimonvideo.client";

    private static final String BASE_PATH = "dvclient";
    public static final Uri CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, MESSAGES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", MESSAGE_ID);
    }

    @Override
    public boolean onCreate() {
        database = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        checkColumns(projection);

        queryBuilder.setTables(Table.TABLE_NAME);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case MESSAGES:
                break;
            case MESSAGE_ID:
                queryBuilder.appendWhere(Table.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id = 0;
        if (uriType == MESSAGES) {
            id = sqlDB.insertWithOnConflict(Table.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    // обновляем флаг status - прочитано или нет
    public static void updateStatus(int lid, String razdel, int status)
    {
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        ContentValues contentValues=new ContentValues();
        contentValues.put(Table.COLUMN_STATUS,status);

        Log.i("---", "put: "+lid+" - "+status+ " "+razdel);

        sqlDB.update(Table.TABLE_NAME, contentValues,
                Table.COLUMN_LID + " = ? AND " + Table.COLUMN_RAZDEL + " = ?",
                new String[]{String.valueOf(lid), razdel});
        sqlDB.close();
    }

    private void checkColumns(String[] projection) {
        String[] available = {
                Table.COLUMN_ID,
                Table.COLUMN_LID,
                Table.COLUMN_STATUS,
                Table.COLUMN_TITLE,
                Table.COLUMN_TEXT,
                Table.COLUMN_FULL_TEXT,
                Table.COLUMN_DATE,
                Table.COLUMN_TIMESTAMP,
                Table.COLUMN_CATEGORY,
                Table.COLUMN_IMG,
                Table.COLUMN_RAZDEL,
                Table.COLUMN_SIZE,
                Table.COLUMN_URL
        };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(available));
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

    // возвращаем последние 10 строк для быстрого запуска.
    public static Cursor getAllRows(String razdel) {
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        String WHERE = Table.COLUMN_RAZDEL + " = ?";

        return sqlDB.query(Table.TABLE_NAME, Table.ALL_KEYS,
                WHERE, new String[]{razdel}, null, null, Table.COLUMN_TIMESTAMP+" DESC", "10");
    }

    // очистка базы
    public static void clearDB(){
        SQLiteDatabase sqlDB = database.getWritableDatabase();

        String selectQuery = "DELETE FROM "+ Table.TABLE_NAME;
        Cursor c = sqlDB.rawQuery(selectQuery, null);
        c.moveToFirst();
        c.close();
    }

    // очистка базы старше полугода
    public static void clearDB_OLD(){
        SQLiteDatabase sqlDB = database.getWritableDatabase();

        String selectQuery = "DELETE FROM "+Table.TABLE_NAME+" WHERE "+Table.COLUMN_TIMESTAMP+" >= date('now','-100 day')";
        sqlDB.execSQL(selectQuery);
    }

    // находим одну запись
    public static Cursor getOneData(String lid, String razdel){
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        String WHERE = Table.COLUMN_LID + " = ? AND " + Table.COLUMN_RAZDEL + " = ?";
        Cursor c = sqlDB.query(Table.TABLE_NAME, Table.ALL_KEYS, WHERE, new String[]{String.valueOf(lid), razdel},
                null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }
}
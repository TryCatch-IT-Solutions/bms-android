package com.example.bms.time_entry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bms.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeRepository {

    private final DatabaseHelper dbHelper;

    public TimeRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long insertTimeEntry(long userId, String type) {
        return insertTimeEntry(userId, type, dbHelper.getCurrentDateTime(), null, false);
    }

    public long insertTimeEntry(long userId, String type, String datetime, String metadata, boolean isSynced) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_TYPE, type);
        values.put(DatabaseHelper.COLUMN_DATETIME, datetime);
        values.put(DatabaseHelper.COLUMN_METADATA, metadata);
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, isSynced);
        values.put(DatabaseHelper.COLUMN_CREATED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());

        long id = db.insert(DatabaseHelper.TABLE_TIME_ENTRIES, null, values);
        db.close();
        return id;
    }

    public int updateTimeEntry(long id, long userId, String type, String datetime, String metadata, boolean isSynced) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_TYPE, type);
        values.put(DatabaseHelper.COLUMN_DATETIME, datetime);
        values.put(DatabaseHelper.COLUMN_METADATA, metadata);
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, isSynced);
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());

        int rowsAffected = db.update(DatabaseHelper.TABLE_TIME_ENTRIES, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected;
    }

    public Cursor getTimeEntryById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_TIME_ENTRIES + " WHERE " + DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteTimeEntry(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_TIME_ENTRIES, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

}

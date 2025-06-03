package com.example.bms.time_entry;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.bms.DatabaseHelper;
import com.example.bms.GroupActivity;

public class TimeRepository {

    private final DatabaseHelper dbHelper;
    private final Context context;

    public TimeRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
        this.context = context;
    }

    public long insertTimeEntry(long userId, String type,String snapshot, String serialNo) {
        return insertTimeEntry(userId, type, dbHelper.getCurrentDateTime(), null, false, snapshot, serialNo);
    }

    public void updateTimeEntrySnapshot(long id, String snapshot) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_SNAPSHOT, snapshot);
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());
        db.update(DatabaseHelper.TABLE_TIME_ENTRIES, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public boolean hasTimeEntry(long userId, String datetime) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_TIME_ENTRIES + " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " + DatabaseHelper.COLUMN_DATETIME + " = ?", new String[]{String.valueOf(userId), datetime});
        boolean hasTimeEntry = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return hasTimeEntry;
    }

    private double[] getLatAndLong() {
        SharedPreferences sharedPreferences = this.context.getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        double latitude = Double.parseDouble(sharedPreferences.getString("latitude", "0"));
        double longitude = Double.parseDouble(sharedPreferences.getString("longitude", "0"));
//        Log.d("Location", "Lat: " + latitude + ", Lon: " + longitude);
        return new double[]{latitude, longitude};
    }



    public long insertTimeEntry(long userId, String type, String datetime, String metadata, boolean isSynced, String snapshot, String serialNo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_TYPE, nullToEmptyString(type));
        values.put(DatabaseHelper.COLUMN_DATETIME, datetime);
        values.put(DatabaseHelper.COLUMN_METADATA, nullToEmptyString(metadata));
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, isSynced);
        values.put(DatabaseHelper.COLUMN_CREATED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_SNAPSHOT, snapshot);
        values.put(DatabaseHelper.COLUMN_SERIAL_NO, serialNo);

        double[] latLong = getLatAndLong();
        Log.d("TimeRepository", "Lat: " + latLong[0] + ", Lon: " + latLong[1]);
        values.put(DatabaseHelper.COLUMN_LATITUDE, latLong[0]);
        values.put(DatabaseHelper.COLUMN_LONGITUDE, latLong[1]);

        long id = db.insert(DatabaseHelper.TABLE_TIME_ENTRIES, null, values);
        db.close();
        return id;
    }

    private String nullToEmptyString(String val) {
        if (val == null || val.equals("null")) {
            return null;
        } else {
            return val;
        }
    }

    public int updateTimeEntry(long id, long userId, String type, String datetime, String metadata, boolean isSynced) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_TYPE, nullToEmptyString(type));
        values.put(DatabaseHelper.COLUMN_DATETIME, datetime);
        values.put(DatabaseHelper.COLUMN_METADATA, nullToEmptyString(metadata));
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

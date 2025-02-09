package com.example.bms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DeviceRepository {

    private DatabaseHelper databaseHelper;

    public DeviceRepository(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public void insertOrUpdateDevice(long groupId, String model, String serialNo, double lat, double lon, String registeredAt, boolean isOnline, String lastSync, String lastActivity, String logoUrl, boolean manualTimeEntry, boolean checkIn, boolean checkOut, boolean breakIn, boolean breakOut, boolean overtimeIn, boolean overtimeOut) {
        databaseHelper.insertOrUpdateDevice(groupId, model, serialNo, lat, lon, registeredAt, isOnline, lastSync, lastActivity, logoUrl, manualTimeEntry, checkIn, checkOut, breakIn, breakOut, overtimeIn, overtimeOut);
    }

    public void newDeviceRecord(long groupId, String model, String serialNo, double lat, double lon, String registeredAt, boolean isOnline, String lastSync, String lastActivity, String logoUrl, boolean manualTimeEntry, boolean checkIn, boolean checkOut, boolean breakIn, boolean breakOut, boolean overtimeIn, boolean overtimeOut) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Check if serialNo already exists
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DEVICES + " WHERE " + DatabaseHelper.COLUMN_SERIAL_NO + " = ?", new String[]{serialNo});

        values.put(DatabaseHelper.COLUMN_GROUP_ID, groupId);
        values.put(DatabaseHelper.COLUMN_MODEL, model);
        values.put(DatabaseHelper.COLUMN_SERIAL_NO, serialNo);
        values.put(DatabaseHelper.COLUMN_LAT, lat);
        values.put(DatabaseHelper.COLUMN_LON, lon);
        values.put(DatabaseHelper.COLUMN_REGISTERED_AT, registeredAt);
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, databaseHelper.getCurrentDateTime());
        values.put("is_online", isOnline);
        values.put("last_sync", lastSync);
        values.put("last_activity", lastActivity);
        values.put("logo_url", logoUrl);
        values.put("manual_time_entry", manualTimeEntry);
        values.put("check_in", checkIn);
        values.put("check_out", checkOut);
        values.put("break_in", breakIn);
        values.put("break_out", breakOut);
        values.put("overtime_in", overtimeIn);
        values.put("overtime_out", overtimeOut);

        if (cursor.moveToFirst()) {
            db.update(DatabaseHelper.TABLE_DEVICES, values, DatabaseHelper.COLUMN_SERIAL_NO + " = ?", new String[]{serialNo});
        } else {
            values.put(DatabaseHelper.COLUMN_CREATED_AT, databaseHelper.getCurrentDateTime());
            db.insertWithOnConflict(DatabaseHelper.TABLE_DEVICES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
        cursor.close();
        db.close();
    }

   public void updateDeviceTimeConfig(String serialNo, boolean manualTimeEntry, boolean checkIn, boolean checkOut, boolean breakIn, boolean breakOut, boolean overtimeIn, boolean overtimeOut) {
       SQLiteDatabase db = databaseHelper.getWritableDatabase();
       ContentValues values = new ContentValues();

       values.put("manual_time_entry", manualTimeEntry);
       values.put("check_in", checkIn);
       values.put("check_out", checkOut);
       values.put("break_in", breakIn);
       values.put("break_out", breakOut);
       values.put("overtime_in", overtimeIn);
       values.put("overtime_out", overtimeOut);
       values.put("is_synced", 0);

       values.put(DatabaseHelper.COLUMN_UPDATED_AT, databaseHelper.getCurrentDateTime());

       db.update(DatabaseHelper.TABLE_DEVICES, values, DatabaseHelper.COLUMN_SERIAL_NO + " = ?", new String[]{serialNo});
       db.close();
   }

   public void updateUnsyncDevice(String serialNo) {
       SQLiteDatabase db = databaseHelper.getWritableDatabase();
       ContentValues values = new ContentValues();

       values.put("is_synced", 0);

       values.put(DatabaseHelper.COLUMN_UPDATED_AT, databaseHelper.getCurrentDateTime());

       db.update(DatabaseHelper.TABLE_DEVICES, values, DatabaseHelper.COLUMN_SERIAL_NO + " = ?", new String[]{serialNo});
       db.close();
   }

   public void updateSyncedDevice(String serialNo) {
       SQLiteDatabase db = databaseHelper.getWritableDatabase();
       ContentValues values = new ContentValues();

       values.put("is_synced", 1);

       values.put(DatabaseHelper.COLUMN_UPDATED_AT, databaseHelper.getCurrentDateTime());

       db.update(DatabaseHelper.TABLE_DEVICES, values, DatabaseHelper.COLUMN_SERIAL_NO + " = ?", new String[]{serialNo});
       db.close();

    }

    public DeviceModel getDevice(String serial) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_DEVICES + " WHERE " + DatabaseHelper.COLUMN_SERIAL_NO + " = ?", new String[]{serial});

        if (cursor.moveToFirst()) {
            DeviceModel device = new DeviceModel(
                    cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GROUP_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MODEL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SERIAL_NO)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAT)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LON)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REGISTERED_AT)),
                    cursor.getInt(cursor.getColumnIndexOrThrow("is_online")) > 0,
                    cursor.getString(cursor.getColumnIndexOrThrow("last_sync")),
                    cursor.getString(cursor.getColumnIndexOrThrow("last_activity")),
                    cursor.getString(cursor.getColumnIndexOrThrow("logo_url")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("manual_time_entry")) > 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("check_in")) > 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("check_out")) > 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("break_in")) > 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("break_out")) > 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("overtime_in")) > 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("overtime_out")) > 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("is_synced")) > 0
            );
            cursor.close();
            db.close();
            return device;
        } else {
            cursor.close();
            db.close();
            return null;
        }
    }

    public void removeMissingDevices(String[] serials) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < serials.length; i++) {
            placeholders.append("?");
            if (i < serials.length - 1) {
                placeholders.append(",");
            }
        }

        String whereClause = DatabaseHelper.COLUMN_SERIAL_NO + " NOT IN (" + placeholders.toString() + ")";
        db.delete(DatabaseHelper.TABLE_DEVICES, whereClause, serials);
        db.close();
    }

    public String getDeviceGroupModel(long groupId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_MODEL + " FROM " + DatabaseHelper.TABLE_DEVICES + " WHERE " + DatabaseHelper.COLUMN_GROUP_ID + " = ? LIMIT 1", new String[]{String.valueOf(groupId)});

        if (cursor.moveToFirst()) {
            String model = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MODEL));
            cursor.close();
            db.close();
            return model;
        } else {
            cursor.close();
            db.close();
            return null;
        }
    }

    public long getModelGroupId(String model) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_GROUP_ID + " FROM " + DatabaseHelper.TABLE_DEVICES + " WHERE " + DatabaseHelper.COLUMN_MODEL + " = ? LIMIT 1", new String[]{model});

        if (cursor.moveToFirst()) {
            long groupId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GROUP_ID));
            cursor.close();
            db.close();
            return groupId;
        } else {
            cursor.close();
            db.close();
            return -1;
        }
    }

}

package com.example.bms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.List;

public class BiometricRepository {

    private final DatabaseHelper dbHelper;

    public BiometricRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long insertBiometric(String key, long userId, String type) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_KEY, key);
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_TYPE, type);
        values.put(DatabaseHelper.COLUMN_IS_SYNCED,0);

        long id = db.insert(DatabaseHelper.TABLE_BIOMETRICS, null, values);
        db.close();
        return id;
    }

    public long insertOrUpdateBiometric(String key, long userId, String type) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_KEY, key);
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_TYPE, type);
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, 0);

        long result;
        String query = "SELECT " + DatabaseHelper.COLUMN_ID + " FROM " + DatabaseHelper.TABLE_BIOMETRICS +
                " WHERE " + DatabaseHelper.COLUMN_KEY + " = ? AND " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " + DatabaseHelper.COLUMN_TYPE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{key, String.valueOf(userId), type});

        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            db.update(DatabaseHelper.TABLE_BIOMETRICS, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            result = id;
        } else {
            result = db.insert(DatabaseHelper.TABLE_BIOMETRICS, null, values);
        }

        cursor.close();
        db.close();
        return result;
    }

    public Biometric findFaceBiometricByKey(String key) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Biometric biometric = null;

        String query = "SELECT * FROM " + DatabaseHelper.TABLE_BIOMETRICS + " WHERE " + DatabaseHelper.COLUMN_KEY + " = ? AND " + DatabaseHelper.COLUMN_TYPE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{key, "face"});

        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            long userId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT));
            String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UPDATED_AT));
            Integer isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_SYNCED));
            biometric = new Biometric(id, userId, key, type, createdAt, updatedAt, isSynced);
        }

        cursor.close();
        db.close();
        return biometric;
    }

    public void deleteBiometric(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_BIOMETRICS, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Biometric getBiometricById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Biometric biometric = null;

        String query = "SELECT * FROM " + DatabaseHelper.TABLE_BIOMETRICS + " WHERE " + DatabaseHelper.COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            String key = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KEY));
            long userId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT));
            String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UPDATED_AT));
            Integer isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_SYNCED));
            biometric = new Biometric(id, userId, key, type, createdAt, updatedAt, isSynced);
        }

        cursor.close();
        db.close();
        return biometric;
    }

    public Biometric getFingerPrintBiometricByUserId(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Biometric biometric = null;

        String query = "SELECT * FROM " + DatabaseHelper.TABLE_BIOMETRICS + " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " + DatabaseHelper.COLUMN_TYPE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), "fingerprint"});

        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            String key = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KEY));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT));
            String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UPDATED_AT));
            Integer isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_SYNCED));
            biometric = new Biometric(id, userId, key, type, createdAt, updatedAt, isSynced);
        }

        cursor.close();
        db.close();
        return biometric;
    }

    public long updateBiometric(long id, String key, long userId, String type) {
        List<Biometric> biometrics = dbHelper.getBiometricsByUserId(userId);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_KEY, key);
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_TYPE, type);
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, 0);

        // Check first if key is already in the database
        boolean isKeyExist = false;
        for (Biometric biometric : biometrics) {
            if (biometric.getType().equals("rfid") && biometric.getKey().equals(key)) {
                isKeyExist = true;
                break;
            }
        }

        if (isKeyExist) {
            db.update(DatabaseHelper.TABLE_BIOMETRICS, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        } else {
            id = this.insertBiometric(key, userId, type);
        }

        db.close();
        return id;
    }

    public long updateFaceBiometric(long id, String key, long userId, String type) {
        List<Biometric> biometrics = dbHelper.getBiometricsByUserId(userId);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_KEY, key);
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_TYPE, type);
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, 0);

        db.update(DatabaseHelper.TABLE_BIOMETRICS, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});

        db.close();
        return id;
    }
}

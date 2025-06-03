package com.example.bms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

public class FingerprintRepository {

    private DatabaseHelper dbHelper;

    public FingerprintRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public List<Fingerprint> getFingerprintsByBiometricId(long id) {
        return dbHelper.getFingerprintsByBiometricId(id);
    }

    public void updateFingerprint(long id, long biometricId, String key) {
        dbHelper.updateFingerprint(id, biometricId, key);
    }

    public void insertFingerprint(long biometricId, String key) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_BIOMETRIC_ID, biometricId);
        values.put(DatabaseHelper.COLUMN_KEY, key);
        values.put(DatabaseHelper.COLUMN_CREATED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());

        db.insert(DatabaseHelper.TABLE_FINGERPRINTS, null, values);
        db.close();
    }

    public void insertOrUpdateFingerprint(long biometricId, String key) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_BIOMETRIC_ID, biometricId);
        values.put(DatabaseHelper.COLUMN_KEY, key);
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());

        String query = "SELECT " + DatabaseHelper.COLUMN_ID + " FROM " + DatabaseHelper.TABLE_FINGERPRINTS +
                " WHERE " + DatabaseHelper.COLUMN_BIOMETRIC_ID + " = ? AND " + DatabaseHelper.COLUMN_KEY + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(biometricId), key});

        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            db.update(DatabaseHelper.TABLE_FINGERPRINTS, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        } else {
            values.put(DatabaseHelper.COLUMN_CREATED_AT, dbHelper.getCurrentDateTime());
            db.insert(DatabaseHelper.TABLE_FINGERPRINTS, null, values);
        }

        cursor.close();
//        db.close();
    }

    public List<Fingerprint> getFingerprintsByGroupId(long groupId) {
        return dbHelper.getFingerprintsByGroupId(groupId);
    }

    public List<Fingerprint> getAllFingerprints() {
        return dbHelper.getAllFingerprints();
    }

    public void deleteFingerprintsByBiometricId(long biometricId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_FINGERPRINTS, DatabaseHelper.COLUMN_BIOMETRIC_ID + " = ?", new String[]{String.valueOf(biometricId)});
        db.close();
    }
}
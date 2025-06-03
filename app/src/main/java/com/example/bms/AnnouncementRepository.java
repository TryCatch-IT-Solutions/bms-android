package com.example.bms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class AnnouncementRepository {

    private final DatabaseHelper dbHelper;

    public AnnouncementRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }


    public boolean hasAnnouncement(Long userId, String title, String message, String expiration) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query;
        String[] selectionArgs;
        if (userId == null) {
            query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ANNOUNCEMENTS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " IS NULL AND " +
                    DatabaseHelper.COLUMN_TITLE + " = ? AND " +
                    DatabaseHelper.COLUMN_MESSAGE + " = ? AND " +
                    DatabaseHelper.COLUMN_EXPIRATION + " = ?";
            selectionArgs = new String[] { title, message, expiration };
        } else {
            query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ANNOUNCEMENTS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_TITLE + " = ? AND " +
                    DatabaseHelper.COLUMN_MESSAGE + " = ? AND " +
                    DatabaseHelper.COLUMN_EXPIRATION + " = ?";
            selectionArgs = new String[] { String.valueOf(userId), title, message, expiration };
        }

        Cursor cursor = db.rawQuery(query, selectionArgs);
        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }
        cursor.close();
        //db.close();
        return exists;
    }

    public long insertAnnouncement(Long userId, String title, String message, String expiration) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (userId == null) {
            values.putNull(DatabaseHelper.COLUMN_USER_ID);
        } else {
            values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        }
        values.put(DatabaseHelper.COLUMN_TITLE, title);
        values.put(DatabaseHelper.COLUMN_MESSAGE, message);
        values.put(DatabaseHelper.COLUMN_EXPIRATION, expiration);
        values.put(DatabaseHelper.COLUMN_CREATED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());

        long id = db.insert(DatabaseHelper.TABLE_ANNOUNCEMENTS, null, values);
        //db.close();
        return id;
    }


    public AnnouncementModel[] getAnnouncements(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_TITLE + ", " +
                DatabaseHelper.COLUMN_MESSAGE + ", " +
                DatabaseHelper.COLUMN_USER_ID + ", " +
                DatabaseHelper.COLUMN_EXPIRATION +
                " FROM " + DatabaseHelper.TABLE_ANNOUNCEMENTS +
                " WHERE (" + DatabaseHelper.COLUMN_USER_ID + " = ? OR " + DatabaseHelper.COLUMN_USER_ID + " IS NULL) AND " +
                DatabaseHelper.COLUMN_EXPIRATION + " > datetime('now')" +
                " ORDER BY " + DatabaseHelper.COLUMN_USER_ID + " IS NULL DESC";
        String[] selectionArgs = { String.valueOf(userId) };

        Cursor cursor = db.rawQuery(query, selectionArgs);
        AnnouncementModel[] announcements = new AnnouncementModel[cursor.getCount()];
        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE));
                String message = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MESSAGE));
                String expiration = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EXPIRATION));
                long user_id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID));
                announcements[i++] = new AnnouncementModel(title, message, expiration, String.valueOf(user_id));
            } while (cursor.moveToNext());
        }
        cursor.close();
        //db.close();
        return announcements;
    }



}

package com.example.bms;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

public class GroupRepository {
    private DatabaseHelper databaseHelper;

    public GroupRepository(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public List<Group> getAllGroups() {
        return databaseHelper.getAllGroups();
    }

    public List<Group> getGroup(long id) {
        return databaseHelper.getGroup(id);
    }

    public void resetTable() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_GROUPS, null, null);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DatabaseHelper.TABLE_GROUPS + "'");
        db.close();
    }

    public void insertGroup(long id, String name, String createdAt, String updatedAt) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("name", name);
        values.put("created_at", createdAt);
        values.put("updated_at", updatedAt);
        db.insert(DatabaseHelper.TABLE_GROUPS, null, values);
        db.close();
    }

 /*   public void updateGroup(Group group) {
        databaseHelper.updateGroup(group);
    }*/
}
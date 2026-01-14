package com.example.bms;

import android.app.Application;

public class BmsApplication extends Application {

    private DatabaseHelper dbHelper;


    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DatabaseHelper(this);
        // Open the database connection
        dbHelper.getWritableDatabase();
    }

    public DatabaseHelper getDatabaseHelper() {
        return dbHelper;
    }
}
package com.example.bms;

import android.content.Context;

public class DeviceRepository {

    private DatabaseHelper databaseHelper;

    public DeviceRepository(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public void insertOrUpdateDevice(long groupId, String model, String serialNo, double lat, double lon, String registeredAt) {
        databaseHelper.insertOrUpdateDevice(groupId, model, serialNo, lat, lon, registeredAt);
    }
}

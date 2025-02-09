package com.example.bms;

public class DeviceModel {

    private long id;
    private String model;
    private String serialNo;
    private double lat;
    private double lon;
    private String registeredAt;
    private boolean isOnline;
    private String lastSync;
    private String lastActivity;
    private String logoUrl;
    private boolean manualTimeEntry;
    private boolean checkIn;
    private boolean checkOut;
    private boolean breakIn;
    private boolean breakOut;
    private boolean overtimeIn;
    private boolean overtimeOut;
    private boolean isSynced;

    public DeviceModel(long id, String model, String serialNo, double lat, double lon, String registeredAt, boolean isOnline, String lastSync, String lastActivity, String logoUrl, boolean manualTimeEntry, boolean checkIn, boolean checkOut, boolean breakIn, boolean breakOut, boolean overtimeIn, boolean overtimeOut, boolean isSynced) {
        this.id = id;
        this.model = model;
        this.serialNo = serialNo;
        this.lat = lat;
        this.lon = lon;
        this.registeredAt = registeredAt;
        this.isOnline = isOnline;
        this.lastSync = lastSync;
        this.lastActivity = lastActivity;
        this.logoUrl = logoUrl;
        this.manualTimeEntry = manualTimeEntry;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.breakIn = breakIn;
        this.breakOut = breakOut;
        this.overtimeIn = overtimeIn;
        this.overtimeOut = overtimeOut;
        this.isSynced = isSynced;
    }

    public boolean isSynced() {
        return isSynced;
    }

    // Getters for all fields
    public long getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getLastSync() {
        return lastSync;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isManualTimeEntry() {
        return manualTimeEntry;
    }

    public boolean isCheckIn() {
        return checkIn;
    }

    public boolean isCheckOut() {
        return checkOut;
    }

    public boolean isBreakIn() {
        return breakIn;
    }

    public boolean isBreakOut() {
        return breakOut;
    }

    public boolean isOvertimeIn() {
        return overtimeIn;
    }

    public boolean isOvertimeOut() {
        return overtimeOut;
    }
}
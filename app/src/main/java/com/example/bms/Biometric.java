package com.example.bms;

public class Biometric {
    private long id;
    private long userId;
    private String key;
    private String type;
    private Integer isSynced;
    private String createdAt;
    private String updatedAt;

    public Biometric(long id, long userId, String key, String type, String createdAt, String updatedAt, Integer isSynced) {
        this.id = id;
        this.userId = userId;
        this.key = key;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isSynced = isSynced;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getIsSynced() {
        return isSynced;
    }
}
package com.example.bms;

public class AnnouncementModel {
    private String title;
    private String message;
    private String expiration;
    private String userId;

    public AnnouncementModel(String title, String message, String expiration, String userId) {
        this.title = title;
        this.message = message;
        this.expiration = expiration;
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getExpiration() {
        return expiration;
    }

    public String getUserId() {
        return userId;
    }
}

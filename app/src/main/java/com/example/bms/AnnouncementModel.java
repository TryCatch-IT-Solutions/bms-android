package com.example.bms;

public class AnnouncementModel {
    private String title;
    private String message;
    private String expiration;

    public AnnouncementModel(String title, String message, String expiration) {
        this.title = title;
        this.message = message;
        this.expiration = expiration;
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
}

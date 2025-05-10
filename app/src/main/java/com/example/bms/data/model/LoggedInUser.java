package com.example.bms.data.model;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private String userId;
    private String role;
    private String displayName;
    private String email;
    private String password;
    private long groupId;
    private String token;

    public LoggedInUser(String userId, String displayName, String email, String password, long groupId, String role) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.password = password;
        this.groupId = groupId;

        this.role = role;
        this.token = null;
    }

    public LoggedInUser(String userId, String displayName, String email, String password, long groupId, String role, String token) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.password = password;
        this.groupId = groupId;
        this.role = role;
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public long getGroupId() {
        return groupId;
    }

    public String getPassword() {
        return password;
    }
    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }

}
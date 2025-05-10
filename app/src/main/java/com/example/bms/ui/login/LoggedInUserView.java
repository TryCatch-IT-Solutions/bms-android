package com.example.bms.ui.login;

/**
 * Class exposing authenticated user details to the UI.
 */
public class LoggedInUserView {
    private String displayName;
    private String email;
    private String password;
    private long groupId;
    private String role;
    private String token;
    //... other data fields that may be accessible to the UI

    public LoggedInUserView(String displayName, String email, String password, long groupId, String role, String token) {
        this.displayName = displayName;
        this.email = email;
        this.password = password;
        this.groupId = groupId;
        this.role = role;
        this.token = token;
    }

    String getDisplayName() {
        return displayName;
    }

    String getEmail() {
        return email;
    }

    String getPassword() {
        return password;
    }

    long getGroupId() {
        return groupId;
    }

    String getRole() {
        return role;
    }

    String getToken() {
        return token;
    }

}
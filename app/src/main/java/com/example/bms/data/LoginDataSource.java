package com.example.bms.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.example.bms.EncryptionUtil;
import com.example.bms.GroupActivity;
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.DatabaseHelper;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.IOException;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    private final DatabaseHelper dbHelper;

    public LoginDataSource(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public Result<LoggedInUser> login(Context context, String username, String password) {
        try {
            LoggedInUser user = dbHelper.getUserByEmail(username);
            Log.d("LoginDataSource", "trying: " + username + ", " + password);

            if (user != null) {
                if (BCrypt.checkpw(password, user.getPassword())) {
                    Log.d("LoginDataSource", "login: " + username + ", " + password);

                    return new Result.Success<>(user);
                } else {
                    return new Result.Error(new IOException("Invalid password"));
                }
            } else {
                return new Result.Error(new IOException("User not found"));
            }
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public LoggedInUser getUserData(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String encryptedData = sharedPreferences.getString("user_data", null);
            if (encryptedData != null) {
                byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
                String decryptedData = EncryptionUtil.decrypt(decodedData);
                String[] userData = decryptedData.split(",");
                Log.d("LoginDataSource", "getUserData: " + userData[0] + ", " + userData[1] + ", " + userData[2]);
                return new LoggedInUser(userData[0], userData[0], userData[1], userData[2], Long.parseLong(userData[3]), userData[4]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void logout(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        SharedPreferences groupSharedPrefs = context.getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor groupEditor = groupSharedPrefs.edit();
        groupEditor.clear();
        groupEditor.apply();
    }
}
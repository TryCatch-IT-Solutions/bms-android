package com.example.bms;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bms.data.model.User;
import com.example.bms.databinding.ActivityConfigurationBinding;
import com.example.bms.databinding.ActivityGroupBinding;
import com.example.bms.time_entry.TimeEntryRegister;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class Configuration extends AppCompatActivity {

    public static final String PREFS_NAME = "com.example.bms.PREFERENCES";
    public static final String KEY_TIME_REGISTER = "time_register";
    private ActivityConfigurationBinding binding;


    private String token;

    private SweetAlertDialog dialog;


    @SuppressLint("Range")
    private void syncTimeEntries() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT te.*, u." + DatabaseHelper.COLUMN_EMAIL + " FROM " + DatabaseHelper.TABLE_TIME_ENTRIES + " te " +
                        "JOIN " + DatabaseHelper.TABLE_USERS + " u ON te." + DatabaseHelper.COLUMN_USER_ID + " = u." + DatabaseHelper.COLUMN_ID +
                        " WHERE te." + DatabaseHelper.COLUMN_IS_SYNCED + " = 0", null);

        if (cursor.getCount() == 0) {
            cursor.close();
            db.close();
            new SweetAlertDialog(Configuration.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("All time entries has been synced already.")
                    .show();
            return;
        }

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        while (cursor.moveToNext()) {
            jsonBuilder.append("{");
            jsonBuilder.append("\"id\":").append(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID))).append(",");
            jsonBuilder.append("\"user_id\":").append(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID))).append(",");
            jsonBuilder.append("\"email\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL))).append("\",");
            jsonBuilder.append("\"type\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE))).append("\",");
            jsonBuilder.append("\"datetime\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATETIME))).append("\",");
            jsonBuilder.append("\"metadata\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_METADATA))).append("\",");
            jsonBuilder.append("\"is_synced\":").append(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_SYNCED))).append(",");
            jsonBuilder.append("\"created_at\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT))).append("\",");
            jsonBuilder.append("\"updated_at\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_UPDATED_AT))).append("\",");
            jsonBuilder.append("\"deleted_at\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_AT))).append("\",");
            jsonBuilder.append("\"deleted_by\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_BY))).append("\"");
            jsonBuilder.append("},");
        }

        if (jsonBuilder.length() > 1) {
            jsonBuilder.setLength(jsonBuilder.length() - 1); // Remove the last comma
        }
        jsonBuilder.append("]");

        cursor.close();
        db.close();

        String timeEntriesJson = jsonBuilder.toString();
        String jsonData = "{\"time_entries\":" + timeEntriesJson + "}";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            String errorMessage = null;

            try {

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        dialog = new SweetAlertDialog(Configuration.this, SweetAlertDialog.PROGRESS_TYPE)
                                .setTitleText("Loading");
                        dialog.show();
                    }
                });

                URL url = new URL(App.BASE_URL + "/sync/time_entries");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonData.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    success = true;
                } else {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.has("message")) {
                            errorMessage = jsonResponse.getString("message");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SyncTimeEntriesTask", e.getMessage());
                errorMessage = e.getMessage();
                e.printStackTrace();
            }
            String finalErrorMessage = errorMessage;
            boolean finalSuccess = success;
            handler.post(() -> {
                dialog.hide();
                if (finalSuccess) {

                    SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_IS_SYNCED, 1);
                    writableDb.update(DatabaseHelper.TABLE_TIME_ENTRIES, values, null, null);
                    writableDb.close();

                    new SweetAlertDialog(Configuration.this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Sync successful")
                            .show();
                } else {
                    new SweetAlertDialog(Configuration.this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Failed to sync.")
                            .setContentText(finalErrorMessage)
                            .show();
                }
            });
        });
    }

    private String getRole() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String encryptedData = sharedPreferences.getString("user_data", null);
            if (encryptedData != null) {
                byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
                String decryptedData = EncryptionUtil.decrypt(decodedData);
                String[] userData = decryptedData.split(",");
                return userData[4];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getGroupId(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String encryptedData = sharedPreferences.getString("user_data", null);
            if (encryptedData != null) {
                byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
                String decryptedData = EncryptionUtil.decrypt(decodedData);
                String[] userData = decryptedData.split(",");
                return userData[3];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("Range")
    private void exportUsersToCSV() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String role = getRole(); // Assume this method retrieves the current user's role
        String groupId = getGroupId(this); // Assume this method retrieves the current user's group ID

        String query;
        String[] queryArgs;

        if ("superadmin".equals(role)) {
            query = "SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE " + DatabaseHelper.COLUMN_STATUS + " = 'active'";
            queryArgs = new String[]{};
        } else if ("groupadmin".equals(role)) {
            query = "SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE " + DatabaseHelper.COLUMN_STATUS + " = 'active' AND " + DatabaseHelper.COLUMN_GROUP_ID + " = ?";
            queryArgs = new String[]{groupId};
        } else {
            // Handle other roles if necessary
            query = "SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE 1 = 0"; // No results
            queryArgs = new String[]{};
        }

        Cursor cursor = db.rawQuery(query, queryArgs);

        File exportDir = new File(Environment.getExternalStorageDirectory(), "BMSExports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        // Get current date and time
        String currentDateTime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File file = new File(exportDir, "users_" + currentDateTime + ".csv");

        try {
            file.createNewFile();
            FileWriter writer = new FileWriter(file);

            // Write CSV header
            writer.append("ID,Group ID,Role,First Name,Middle Name,Last Name,Lat,Lon,Address1,Address2,Barangay,Municipality,Province,Birth Date,Gender,Zip Code,Email,Phone Number,Emergency Contact Name,Emergency Contact No,Status,Created At,Updated At,Deleted At,Deleted By\n");

            // Write CSV rows
            while (cursor.moveToNext()) {
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GROUP_ID))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FIRST_NAME))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_MIDDLE_NAME))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LAST_NAME))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LAT))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LON))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ADDRESS1))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ADDRESS2))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BARANGAY))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_MUNICIPALITY))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PROVINCE))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BIRTH_DATE))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GENDER))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ZIP_CODE))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PHONE_NUMBER))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NAME))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NO))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_UPDATED_AT))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_AT))).append(",");
                writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_BY))).append("\n");
            }

            writer.flush();
            writer.close();
            cursor.close();
            db.close();

            Toast.makeText(this, "Exported to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("Range")
    private void exportTimeEntriesToCSV() {
    DatabaseHelper dbHelper = new DatabaseHelper(this);
    SQLiteDatabase db = dbHelper.getReadableDatabase();

    String groupId = getGroupId(this); // Assume this method retrieves the current user's group ID
    Cursor cursor = db.rawQuery(
                "SELECT te.*, u." + DatabaseHelper.COLUMN_FIRST_NAME + ", u." + DatabaseHelper.COLUMN_LAST_NAME + ", u." + DatabaseHelper.COLUMN_EMAIL +
                        " FROM " + DatabaseHelper.TABLE_TIME_ENTRIES + " te " +
                        "JOIN " + DatabaseHelper.TABLE_USERS + " u ON te." + DatabaseHelper.COLUMN_USER_ID + " = u." + DatabaseHelper.COLUMN_ID +
                        " WHERE u." + DatabaseHelper.COLUMN_GROUP_ID + " = ?", new String[]{groupId});

    File exportDir = new File(Environment.getExternalStorageDirectory(), "BMSExports");
    if (!exportDir.exists()) {
        exportDir.mkdirs();
    }

    // Get current date and time
    String currentDateTime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    File file = new File(exportDir, "time_entries_" + currentDateTime + ".csv");

    try {
        file.createNewFile();
        FileWriter writer = new FileWriter(file);

        // Write CSV header
        writer.append("ID,User ID,First Name,Last Name,Email,Type,Datetime,Metadata,Is Synced,Created At,Updated At,Deleted At,Deleted By\n");

        // Write CSV rows
        while (cursor.moveToNext()) {
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FIRST_NAME))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LAST_NAME))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATETIME))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_METADATA))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_SYNCED))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_UPDATED_AT))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_AT))).append(",");
            writer.append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_BY))).append("\n");
        }

        writer.flush();
        writer.close();
        cursor.close();
        db.close();

        Toast.makeText(this, "Exported to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
    } catch (IOException e) {
        e.printStackTrace();
        Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
    }
}

    public String getToken(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String encryptedData = sharedPreferences.getString("user_data", null);
            if (encryptedData != null) {
                byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
                String decryptedData = EncryptionUtil.decrypt(decodedData);
                String[] userData = decryptedData.split(",");
                return userData[5]; // Assuming the token is the 6th element in the array
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String escapeJson(String input) {
        if (input == null) {
            return ""; // or "null" if you want to explicitly represent null in JSON
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void writeResponseToFile(String response) {
        File file = new File(getFilesDir(), "response.json");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String nullToEmptyString(String val) {
        if (val == null || val.equals("null")) {
            return null;
        } else {
            return val;
        }
    }

    @SuppressLint("Range")
    public void syncUsers() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE is_synced = 0", null);

        if (cursor.getCount() < 1) {
            cursor.close();
            db.close();
            new SweetAlertDialog(Configuration.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("All users has been synced already.")
                    .show();
            return;
        }


        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        while (cursor.moveToNext()) {
            jsonBuilder.append("{");
            jsonBuilder.append("\"id\":").append(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID))).append(",");
            jsonBuilder.append("\"group_id\":").append(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_GROUP_ID))).append(",");
            jsonBuilder.append("\"role\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE))).append("\",");
            jsonBuilder.append("\"first_name\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FIRST_NAME))).append("\",");
            jsonBuilder.append("\"middle_name\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_MIDDLE_NAME))).append("\",");
            jsonBuilder.append("\"last_name\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LAST_NAME))).append("\",");
//            jsonBuilder.append("\"lat\":").append(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LAT))).append(",");
//            jsonBuilder.append("\"lon\":").append(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LON))).append(",");
            jsonBuilder.append("\"address1\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ADDRESS1))).append("\",");
            jsonBuilder.append("\"address2\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ADDRESS2))).append("\",");
            jsonBuilder.append("\"barangay\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BARANGAY))).append("\",");
            jsonBuilder.append("\"municipality\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_MUNICIPALITY))).append("\",");
            jsonBuilder.append("\"province\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PROVINCE))).append("\",");
            jsonBuilder.append("\"birth_date\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BIRTH_DATE))).append("\",");
            jsonBuilder.append("\"gender\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GENDER))).append("\",");
            jsonBuilder.append("\"zip_code\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ZIP_CODE))).append("\",");
            jsonBuilder.append("\"email\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL))).append("\",");
            jsonBuilder.append("\"phone_number\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PHONE_NUMBER))).append("\",");
            jsonBuilder.append("\"emergency_contact_name\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NAME))).append("\",");
            jsonBuilder.append("\"emergency_contact_no\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NO))).append("\",");
//            jsonBuilder.append("\"password\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD))).append("\",");
            jsonBuilder.append("\"status\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS))).append("\",");
//            jsonBuilder.append("\"is_synced\":").append(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_SYNCED))).append(",");
            jsonBuilder.append("\"created_at\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT))).append("\",");
            jsonBuilder.append("\"updated_at\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_UPDATED_AT))).append("\",");
//            jsonBuilder.append("\"deleted_at\":\"").append(nullToEmptyString(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_AT)))).append("\",");
            jsonBuilder.append("\"deleted_by\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_BY))).append("\",");

            // Fetch biometrics for the user

            long userId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
            List<Biometric> biometrics = dbHelper.getBiometricsByUserId(userId);
            jsonBuilder.append("\"biometrics\":[");

            for (Biometric biometric : biometrics) {
                jsonBuilder.append("{");
                jsonBuilder.append("\"id\":").append(biometric.getId()).append(",");
                jsonBuilder.append("\"key\":\"").append(escapeJson(biometric.getKey())).append("\",");
                jsonBuilder.append("\"is_synced\":\"").append(escapeJson(biometric.getIsSynced().toString())).append("\",");
                jsonBuilder.append("\"type\":\"").append(escapeJson(biometric.getType())).append("\"");

                // Fetch fingerprints for the biometric
                List<Fingerprint> fingerprints = dbHelper.getFingerprintsByBiometricId(biometric.getId());
                jsonBuilder.append(",\"fingerprints\":[");

                for (Fingerprint fingerprint : fingerprints) {
                    String key = Base64.encodeToString(fingerprint.getKey().getBytes(), Base64.DEFAULT);
                    jsonBuilder.append("{");
//                    jsonBuilder.append("\"id\":").append(fingerprint.getId()).append(",");
                    jsonBuilder.append("\"key\":\"").append(escapeJson(key)).append("\"");
//                    jsonBuilder.append("\"created_at\":\"").append(fingerprint.getCreatedAt()).append("\",");
//                    jsonBuilder.append("\"updated_at\":\"").append(fingerprint.getUpdatedAt()).append("\"");
                    jsonBuilder.append("},");
                }
                // Remove the last comma from fingerprints array
                if (jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
                    jsonBuilder.setLength(jsonBuilder.length() - 1);
                }
                jsonBuilder.append("]"); // Close fingerprints array
                jsonBuilder.append("},");
            }
// Remove the last comma from biometrics array
            if (jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
                jsonBuilder.setLength(jsonBuilder.length() - 1);
            }
            jsonBuilder.append("]"); // Close biometrics array
            jsonBuilder.append("},");
        }

        if (jsonBuilder.length() > 1) {
            jsonBuilder.setLength(jsonBuilder.length() - 1); // Remove the last comma
        }
        jsonBuilder.append("]");

        cursor.close();
        db.close();

        String usersJson = jsonBuilder.toString();
//        Log.d("SyncUsersTask", usersJson);
        System.out.println("Response: "+usersJson);
        String jsonData = "{\"users\":" + usersJson + "}";

        writeResponseToFile(jsonData);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            String errorMessage = null;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    dialog = new SweetAlertDialog(Configuration.this, SweetAlertDialog.PROGRESS_TYPE)
                            .setTitleText("Loading");
                    dialog.show();
                }
            });

            try {
                URL url = new URL(App.BASE_URL + "/sync/users");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonData.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    success = true;
                } else {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        Log.d("SyncUsersTask", response.toString());
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.has("message")) {
                            errorMessage = jsonResponse.getString("message");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SyncUsersTask", e.getMessage());
                errorMessage = e.getMessage();
                e.printStackTrace();
            }

            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;
            handler.post(() -> {
                dialog.hide();

                if (finalSuccess) {
                    new SweetAlertDialog(Configuration.this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Sync successful")
                            .show();

                    SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_IS_SYNCED, 1);
                    writableDb.update(DatabaseHelper.TABLE_USERS, values, null, null);
                    writableDb.close();
                } else {
                    new SweetAlertDialog(Configuration.this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Failed to sync.")
                            .setContentText(finalErrorMessage)
                            .show();
                }
            });
        });
    }

    private void getApiEndpoint()  {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String apiEndpoint = sharedPreferences.getString("API_ENDPOINT", "");

        Log.d("Configuration", "API Endpoint: " + apiEndpoint);
        TextInputEditText editTextApiEndpoint = findViewById(R.id.api_endpoint);
        editTextApiEndpoint.setText(apiEndpoint);
    }

    private void saveApiEndpoint(String apiEndpoint) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("API_ENDPOINT", apiEndpoint);
        editor.apply();
        Log.d("Configuration", "API Endpoint: " + apiEndpoint);
        App.BASE_URL = apiEndpoint;
    }

    private void getTrackerSwitches() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SwitchMaterial checkInSwitch = findViewById(R.id.check_in_switch);
        SwitchMaterial checkOutSwitch = findViewById(R.id.check_out_switch);
        SwitchMaterial breakInSwitch = findViewById(R.id.break_in_switch);
        SwitchMaterial breakOutSwitch = findViewById(R.id.break_out_switch);
        SwitchMaterial overtimeInSwitch = findViewById(R.id.overtime_in_switch);
        SwitchMaterial overtimeOutSwitch = findViewById(R.id.overtime_out_switch);

        checkInSwitch.setChecked(sharedPreferences.getBoolean(KEY_TIME_REGISTER + "_check_in", false));
        checkOutSwitch.setChecked(sharedPreferences.getBoolean(KEY_TIME_REGISTER + "_check_out", false));
        breakInSwitch.setChecked(sharedPreferences.getBoolean(KEY_TIME_REGISTER + "_break_in", false));
        breakOutSwitch.setChecked(sharedPreferences.getBoolean(KEY_TIME_REGISTER + "_break_out", false));
        overtimeInSwitch.setChecked(sharedPreferences.getBoolean(KEY_TIME_REGISTER + "_overtime_in", false));
        overtimeOutSwitch.setChecked(sharedPreferences.getBoolean(KEY_TIME_REGISTER + "_overtime_out", false));
    }

    private void disableTimeSwitches(){
        SwitchMaterial checkInSwitch = findViewById(R.id.check_in_switch);
        SwitchMaterial checkOutSwitch = findViewById(R.id.check_out_switch);
        SwitchMaterial breakInSwitch = findViewById(R.id.break_in_switch);
        SwitchMaterial breakOutSwitch = findViewById(R.id.break_out_switch);
        SwitchMaterial overtimeInSwitch = findViewById(R.id.overtime_in_switch);
        SwitchMaterial overtimeOutSwitch = findViewById(R.id.overtime_out_switch);

        checkInSwitch.setEnabled(false);
        checkOutSwitch.setEnabled(false);
        breakInSwitch.setEnabled(false);
        breakOutSwitch.setEnabled(false);
        overtimeInSwitch.setEnabled(false);
        overtimeOutSwitch.setEnabled(false);

        checkInSwitch.setChecked(false);
        checkOutSwitch.setChecked(false);
        breakInSwitch.setChecked(false);
        breakOutSwitch.setChecked(false);
        overtimeInSwitch.setChecked(false);
        overtimeOutSwitch.setChecked(false);
    }

    private void enableTimeSwitches(){
        SwitchMaterial checkInSwitch = findViewById(R.id.check_in_switch);
        SwitchMaterial checkOutSwitch = findViewById(R.id.check_out_switch);
        SwitchMaterial breakInSwitch = findViewById(R.id.break_in_switch);
        SwitchMaterial breakOutSwitch = findViewById(R.id.break_out_switch);
        SwitchMaterial overtimeInSwitch = findViewById(R.id.overtime_in_switch);
        SwitchMaterial overtimeOutSwitch = findViewById(R.id.overtime_out_switch);

        checkInSwitch.setEnabled(true);
        checkOutSwitch.setEnabled(true);
        breakInSwitch.setEnabled(true);
        breakOutSwitch.setEnabled(true);
        overtimeInSwitch.setEnabled(true);
        overtimeOutSwitch.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityConfigurationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Override the back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing to disable the back button
                //save the api endpoint
                TextInputEditText editTextApiEndpoint = findViewById(R.id.api_endpoint);
                String apiEndpoint = Objects.requireNonNull(editTextApiEndpoint.getText()).toString();
                saveApiEndpoint(apiEndpoint);
                Log.d("Configuration", "API Endpoint: " + apiEndpoint);
                finish();
            }
        });

        Toolbar toolbarHead = findViewById(R.id.toolbar_header);
        toolbarHead.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Get reference to the SwitchMaterial
        SwitchMaterial switchTimeRegister = findViewById(R.id.switch_time_register);

        disableTimeSwitches();

        // Load the saved state
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isTimeRegisterOn = sharedPreferences.getBoolean(KEY_TIME_REGISTER, false);
        switchTimeRegister.setChecked(isTimeRegisterOn);

        if(isTimeRegisterOn){
            enableTimeSwitches();
        }

        token = getToken(this);
        Log.d("Configuration", "Token: " + token);

        getApiEndpoint();

        getTrackerSwitches();

        findViewById(R.id.button_export_time_entries).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportTimeEntriesToCSV();
            }
        });

        findViewById(R.id.button_export_users).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportUsersToCSV();
            }
        });

        findViewById(R.id.button_sync_time_entries).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncTimeEntries();
            }
        });

        findViewById(R.id.button_sync_users).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncUsers();
            }
        });

        // Set an OnCheckedChangeListener
        switchTimeRegister.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER, isChecked);
            editor.apply();
            // Show a toast message
            if (isChecked) {
                enableTimeSwitches();
                Toast.makeText(Configuration.this, "Time Register is ON", Toast.LENGTH_SHORT).show();
            } else {
                disableTimeSwitches();
                Toast.makeText(Configuration.this, "Time Register is OFF", Toast.LENGTH_SHORT).show();
            }
        });

        // Check In Switch
        SwitchMaterial checkInSwitch = findViewById(R.id.check_in_switch);
        checkInSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_check_in", isChecked);
            editor.apply();
        });

// Check Out Switch
        SwitchMaterial checkOutSwitch = findViewById(R.id.check_out_switch);
        checkOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_check_out", isChecked);
            editor.apply();
        });

// Break In Switch
        SwitchMaterial breakInSwitch = findViewById(R.id.break_in_switch);
        breakInSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_break_in", isChecked);
            editor.apply();
        });

// Break Out Switch
        SwitchMaterial breakOutSwitch = findViewById(R.id.break_out_switch);
        breakOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_break_out", isChecked);
            editor.apply();
        });

// Overtime In Switch
        SwitchMaterial overtimeInSwitch = findViewById(R.id.overtime_in_switch);
        overtimeInSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_overtime_in", isChecked);
            editor.apply();
        });

// Overtime Out Switch
        SwitchMaterial overtimeOutSwitch = findViewById(R.id.overtime_out_switch);
        overtimeOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_overtime_out", isChecked);
            editor.apply();
        });
    }
}
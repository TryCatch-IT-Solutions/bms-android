package com.example.bms;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.example.bms.data.LoginDataSource;
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.data.model.User;
import com.example.bms.time_entry.TimeRepository;
import com.example.bms.ui.login.LoginActivity;
import com.github.yuweiguocn.library.greendao.MigrationHelper;
import com.google.android.material.textfield.TextInputEditText;

import cn.pedant.SweetAlert.SweetAlertDialog;
import facex.greendao.gen.DaoMaster;
import facex.greendao.gen.DaoSession;
import facex.greendao.gen.UserDao;

import org.greenrobot.greendao.database.Database;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author tx
 * @date 2024/3/19 21:21
 * @target this class will do...
 */
public class App extends Application {
    DaoSession daoSession;

    private DatabaseHelper dbHelper;

    public static String BASE_URL = "";
    public static String TOKEN = "client-123456";

    private SweetAlertDialog dialog;

    private Handler handler;
    private Runnable runnable;


    @Override
    public void onTerminate() {
        super.onTerminate();
        // Remove callbacks to prevent memory leaks
        handler.removeCallbacks(runnable);
    }

    private void getApiEndpoint()  {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        String apiEndpoint = sharedPreferences.getString("API_ENDPOINT", "http://115.147.32.2:9001/api");

        BASE_URL = apiEndpoint;
        Log.d("Configuration", "API Endpoint: " + apiEndpoint);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        getApiEndpoint();

        dbHelper = new DatabaseHelper(this);
        // Open the database connection
        dbHelper.getWritableDatabase();

        daoSession = getDaoSession();

        Log.d("Device", "DeviceGroupId Here: " + getDeviceGroupId());

        String access = getAccess();
        if(access.equals("online")) {
            // Initialize the handler and runnable
            handler = new Handler(Looper.getMainLooper());
            runnable = new Runnable() {
                @Override
                public void run() {
                    // Call the getTimeEntries method
                    getTimeEntries();
                    getAnnouncements();

                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(()-> syncUsersFromWeb());
                    // Schedule the runnable to run again after 1 minute (60000 milliseconds)
                    handler.postDelayed(this, 60000);
                }
            };

            // Start the initial runnable task by posting it to the handler
            handler.post(runnable);
        }
    }

    private void syncUsersFromWeb(){

        String access = getAccess();
        if(access.equals("offline")) {
            return;
        }

        Log.d("SyncingUsers","Syncing users from web");

        syncUsersOnLogout(App.this, new SyncCallback() {
            @Override
            public void onSuccess() {

                try {
                    Log.d("SyncingUsers",App.BASE_URL + "/sync/users/login");

                    URL url = new URL(App.BASE_URL + "/sync/users/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + App.TOKEN);

                    if (conn.getResponseCode() != 200) {
                        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

                    StringBuilder response = new StringBuilder();
                    String output;
                    while ((output = br.readLine()) != null) {
                        response.append(output);
                    }

                    conn.disconnect();

                    Log.d("Response 1:", response.toString());
                    JSONArray users = new JSONArray(response.toString());
                    UserRepository userRepository = new UserRepository(App.this);

                    BiometricRepository biometricRepository = new BiometricRepository(App.this);
                    FingerprintRepository fingerprintRepository = new FingerprintRepository(App.this);

                    for (int i = 0; i < users.length(); i++) {
                        JSONObject user = users.getJSONObject(i);
                        System.out.println("User: " + user.toString());

                        long groupId = user.isNull("group_id") ? 0 : user.getLong("group_id");
                        System.out.println("GGroup ID: " + groupId);
                        long userId = userRepository.insertOrUpdate(
                                groupId,
                                user.getString("first_name"),
                                user.getString("middle_name"),
                                user.getString("last_name"),
                                user.getString("address1"),
                                user.getString("address2"),
                                user.getString("barangay"),
                                user.getString("municipality"),
                                user.getString("province"),
                                user.getString("birth_date"),
                                user.getString("gender"),
                                user.getInt("zip_code"),
                                0,
                                0,
                                user.getString("email"),
                                user.getString("phone_number"),
                                user.getString("emergency_contact_no"),
                                user.getString("emergency_contact_name"),
                                user.getString("role"),
                                user.getString("password"),
                                user.getString("created_at")
                                );

                        JSONArray biometrics = user.getJSONArray("biometrics");
                        for (int j = 0; j < biometrics.length(); j++) {
                            JSONObject biometric = biometrics.getJSONObject(j);
                            long biometricId = biometricRepository.insertOrUpdateBiometric(
                                    biometric.getString("key"),
                                    userId,
                                    biometric.getString("type"));

                            JSONArray fingerprints = biometric.getJSONArray("fingerprints");
                            for (int k = 0; k < fingerprints.length(); k++) {
                                JSONObject fingerprint = fingerprints.getJSONObject(k);

                                byte[] decodedBytes = Base64.decode(fingerprint.getString("key"), Base64.DEFAULT);
                                String decodedKey = new String(decodedBytes, StandardCharsets.UTF_8);
                                fingerprintRepository.insertOrUpdateFingerprint(
                                        biometricId,
                                        decodedKey
                                );
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("LoginActivity", "Error during user sync: " + e.getMessage(), e);
                }

            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("SyncUsers", "Error syncing users: " + errorMessage);
            }
        });


    }

    public UserDao getUserDao() {
        if (daoSession != null) {
            return daoSession.getUserDao();
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

    private String getDeviceGroupId(){
        SharedPreferences sharedPreferences = getSharedPreferences("DEVICE_GROUP", Context.MODE_PRIVATE);
        return sharedPreferences.getString(GroupActivity.KEY_SELECTED_GROUP, null);
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


    public interface SyncCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public void syncUsers(Context context, SyncCallback callback) {
        syncUsers(context, callback, false);
    }

    public void getAnnouncements() {

        String access = getAccess();
        System.out.println("Access is: " + access);

        if(access.equals("offline")) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();

        LoginDataSource loginDataSource = new LoginDataSource(App.this);
        LoggedInUser userData = loginDataSource.getUserData(App.this);

        if (userData == null) {
            Log.e("App", "User data is null. Cannot fetch announcements.");
            return;
        }

        executor.execute(() -> {
            try {
                URL url = new URL(App.BASE_URL + "/sync/announcements?group_id=" + userData.getGroupId());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " +getToken(App.this));

                System.out.println("Token is real: " + getToken(App.this));

                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                }

                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                StringBuilder response = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    response.append(output);
                }

                conn.disconnect();

                Log.d("Announcement:", response.toString());
                JSONArray announcements = new JSONArray(response.toString());

                AnnouncementRepository repository = new AnnouncementRepository(App.this);
                UserRepository userRepository = new UserRepository(App.this);
                // Process the announcements as needed
                for (int i = 0; i < announcements.length(); i++) {
                    JSONObject announcement = announcements.getJSONObject(i);

                    LoggedInUser user = userRepository.getUserByEmail(announcement.getString("email"));

                    if(user == null) {
                        System.out.println("User not found: " + announcement.getString("email"));
                        continue;
                    }

                    if(repository.hasAnnouncement(announcement.getLong("user_id"), announcement.getString("title"), announcement.getString("message"), announcement.getString("expiration"))) {
                        System.out.println("Announcement already exists: " + announcement.toString());
                        continue;
                    }

                    repository.insertAnnouncement(Long.parseLong(user.getUserId()), announcement.getString("title"), announcement.getString("message"), announcement.getString("expiration"));
                    // Example: Log the announcement details
                    Log.d("Announcement", "Title: " + announcement.getString("title") + ", Message: " + announcement.getString("message"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("App", "Error fetching announcements: " + e.getMessage(), e);
            }
        });
    }

    private String getAccess()  {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("ACCESS", "offline");
    }

    public void getTimeEntries() {

        String access = getAccess();
        System.out.println("Access is: " + access);

        if(access.equals("offline")) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();

        LoginDataSource loginDataSource = new LoginDataSource(App.this);
        LoggedInUser userData = loginDataSource.getUserData(this);

        if(userData == null) {
            return;
        }

        executor.execute(() -> {
            try {

                URL url = new URL(App.BASE_URL + "/sync/time-entries/"+userData.getGroupId());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + App.TOKEN);

                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                }

                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

                StringBuilder response = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    response.append(output);
                }

                conn.disconnect();

                Log.d("Response 1:", response.toString());
                JSONArray timeEntries = new JSONArray(response.toString());

                TimeRepository repository = new TimeRepository(App.this);
                for (int i = 0; i < timeEntries.length(); i++) {
                    JSONObject entry = timeEntries.getJSONObject(i);
                    if(repository.hasTimeEntry(entry.getLong("user_id"), entry.getString("datetime"))) {
                        System.out.println("Time entry already exists: " + entry.toString());
                        continue;
                    }
                    repository.insertTimeEntry(entry.getLong("user_id"), entry.getString("type"), entry.getString("datetime"), entry.getString("metadata"), true);

                    System.out.println("Time: " + entry.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("LoginActivity", "Error during time sync: " + e.getMessage(), e);
            }
        });
    }

    private String nullToEmptyString(String val) {
        if (val == null || val.equals("null")) {
            return null;
        } else {
            return val;
        }
    }

    @SuppressLint("Range")
    public void syncTimeEntriesOnLogout(Context context, SyncCallback callback) {

        String access = getAccess();
        System.out.println("Access is: " + access);

        if(access.equals("offline")) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                DatabaseHelper dbHelper = new DatabaseHelper(context);

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery(
                        "SELECT te.*, u." + DatabaseHelper.COLUMN_EMAIL + " FROM " + DatabaseHelper.TABLE_TIME_ENTRIES + " te " +
                                "JOIN " + DatabaseHelper.TABLE_USERS + " u ON te." + DatabaseHelper.COLUMN_USER_ID + " = u." + DatabaseHelper.COLUMN_ID +
                                " WHERE te." + DatabaseHelper.COLUMN_IS_SYNCED + " = 0", null);
                String token = getToken(context);

                if (cursor.getCount() == 0) {
                    cursor.close();
                    db.close();
                    if (callback != null) {
                        callback.onSuccess();
                    }
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
                        if (finalSuccess) {

                            SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
                            ContentValues values = new ContentValues();
                            values.put(DatabaseHelper.COLUMN_IS_SYNCED, 1);
                            writableDb.update(DatabaseHelper.TABLE_TIME_ENTRIES, values, null, null);
                            writableDb.close();

                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            if (callback != null) {
                                callback.onFailure(finalErrorMessage);
                            }
                        }
                    });
                });
            }
        }, 3000);

    }

    @SuppressLint("Range")
    public void syncUsersOnLogout(Context context, SyncCallback callback) {

        String access = getAccess();
        System.out.println("Access is: " + access);

        if(access.equals("offline")) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        String token = getToken(context);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE is_synced = 0", null);

        Log.d("SyncUsersTask", "Cursor count: " + cursor.getCount());
        if (cursor.getCount() < 1) {
            cursor.close();
            db.close();

            if (callback != null) {
                callback.onSuccess();
            }
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
            jsonBuilder.append("\"lat\":").append(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LAT))).append(",");
            jsonBuilder.append("\"lon\":").append(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LON))).append(",");
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
            jsonBuilder.append("\"status\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS))).append("\",");
            jsonBuilder.append("\"created_at\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT))).append("\",");
            jsonBuilder.append("\"updated_at\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_UPDATED_AT))).append("\",");
//            jsonBuilder.append("\"deleted_at\":\"").append(nullToEmptyString(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_AT)))).append("\",");
            jsonBuilder.append("\"deleted_by\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_BY))).append("\",");

            long userId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
            List<Biometric> biometrics = dbHelper.getBiometricsByUserId(userId);
            jsonBuilder.append("\"biometrics\":[");

            for (Biometric biometric : biometrics) {
                jsonBuilder.append("{");
                jsonBuilder.append("\"id\":").append(biometric.getId()).append(",");
                jsonBuilder.append("\"key\":\"").append(escapeJson(biometric.getKey())).append("\",");
                jsonBuilder.append("\"is_synced\":\"").append(escapeJson(biometric.getIsSynced().toString())).append("\",");
                jsonBuilder.append("\"type\":\"").append(escapeJson(biometric.getType())).append("\"");

                List<Fingerprint> fingerprints = dbHelper.getFingerprintsByBiometricId(biometric.getId());
                jsonBuilder.append(",\"fingerprints\":[");

                for (Fingerprint fingerprint : fingerprints) {
                    String key = Base64.encodeToString(fingerprint.getKey().getBytes(), Base64.DEFAULT);
                    jsonBuilder.append("{");
                    jsonBuilder.append("\"key\":\"").append(escapeJson(key)).append("\"");
                    jsonBuilder.append("},");
                }
                if (jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
                    jsonBuilder.setLength(jsonBuilder.length() - 1);
                }
                jsonBuilder.append("]");
                jsonBuilder.append("},");
            }
            if (jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
                jsonBuilder.setLength(jsonBuilder.length() - 1);
            }
            jsonBuilder.append("]");
            jsonBuilder.append("},");
        }

        if (jsonBuilder.length() > 1) {
            jsonBuilder.setLength(jsonBuilder.length() - 1);
        }
        jsonBuilder.append("]");

        cursor.close();
        db.close();

        String usersJson = jsonBuilder.toString();
        String jsonData = "{\"users\":" + usersJson + "}";

        System.out.println("UserRe:"+jsonData);

        writeResponseToFile(jsonData);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            String errorMessage = null;

            try {
                URL url = new URL(BASE_URL + "/sync/users");
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
                        System.out.println("The Response: " + response.toString());
                        System.out.println("Token: " + token);
                        if(response.toString().equals("false")) {
                            errorMessage = "Unauthorized, please login again.";
                        }else {
                            JSONObject jsonResponse = new JSONObject(response.toString());
                            if (jsonResponse.has("message")) {
                                errorMessage = jsonResponse.getString("message");
                            }
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

                if (finalSuccess) {
                    SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_IS_SYNCED, 1);
                    writableDb.update(DatabaseHelper.TABLE_USERS, values, null, null);
                    writableDb.close();

                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(finalErrorMessage);
                    }
                }
            });
        });
    }

    @SuppressLint("Range")
    public void syncUsers(Context context, SyncCallback callback, boolean silent) {
        String access = getAccess();
        System.out.println("Access is: " + access);

        if(access.equals("offline")) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        String token = getToken(context);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE is_synced = 0", null);

        Log.d("SyncUsersTask", "Cursor count: " + cursor.getCount());
        if (cursor.getCount() < 1) {
            cursor.close();
            db.close();
           if(!silent){
               new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                       .setTitleText("All users have been synced already.")
                       .show();
           }
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
            jsonBuilder.append("\"lat\":").append(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LAT))).append(",");
            jsonBuilder.append("\"lon\":").append(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LON))).append(",");
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
            jsonBuilder.append("\"status\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS))).append("\",");
            jsonBuilder.append("\"created_at\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT))).append("\",");
            jsonBuilder.append("\"updated_at\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_UPDATED_AT))).append("\",");
            jsonBuilder.append("\"deleted_at\":\"").append("").append("\",");
            jsonBuilder.append("\"deleted_by\":\"").append(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_BY))).append("\",");

            long userId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
            List<Biometric> biometrics = dbHelper.getBiometricsByUserId(userId);
            jsonBuilder.append("\"biometrics\":[");

            for (Biometric biometric : biometrics) {
                jsonBuilder.append("{");
                jsonBuilder.append("\"id\":").append(biometric.getId()).append(",");
                jsonBuilder.append("\"key\":\"").append(escapeJson(biometric.getKey())).append("\",");
                jsonBuilder.append("\"is_synced\":\"").append(escapeJson(biometric.getIsSynced().toString())).append("\",");
                jsonBuilder.append("\"type\":\"").append(escapeJson(biometric.getType())).append("\"");

                List<Fingerprint> fingerprints = dbHelper.getFingerprintsByBiometricId(biometric.getId());
                jsonBuilder.append(",\"fingerprints\":[");

                for (Fingerprint fingerprint : fingerprints) {
                    String key = Base64.encodeToString(fingerprint.getKey().getBytes(), Base64.DEFAULT);
                    jsonBuilder.append("{");
                    jsonBuilder.append("\"key\":\"").append(escapeJson(key)).append("\"");
                    jsonBuilder.append("},");
                }
                if (jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
                    jsonBuilder.setLength(jsonBuilder.length() - 1);
                }
                jsonBuilder.append("]");
                jsonBuilder.append("},");
            }
            if (jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
                jsonBuilder.setLength(jsonBuilder.length() - 1);
            }
            jsonBuilder.append("]");
            jsonBuilder.append("},");
        }

        if (jsonBuilder.length() > 1) {
            jsonBuilder.setLength(jsonBuilder.length() - 1);
        }
        jsonBuilder.append("]");

        cursor.close();
        db.close();

        String usersJson = jsonBuilder.toString();
        String jsonData = "{\"users\":" + usersJson + "}";

        System.out.println("UserRe:"+jsonData);

        writeResponseToFile(jsonData);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            String errorMessage = null;
            new Handler(Looper.getMainLooper()).post(() -> {
                if(!silent) {
                    dialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE)
                            .setTitleText("Loading");
                    dialog.show();
                }
            });

            try {
                URL url = new URL(BASE_URL + "/sync/users");
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
                        System.out.println("The Response: " + response.toString());
                        System.out.println("Token: " + token);
                        if(response.toString().equals("false")) {
                            errorMessage = "Unauthorized, please login again.";
                        }else {
                            JSONObject jsonResponse = new JSONObject(response.toString());
                            if (jsonResponse.has("message")) {
                                errorMessage = jsonResponse.getString("message");
                            }
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
                if(!silent) {
                    dialog.dismiss();
                }

                if (finalSuccess) {

                    if(!silent) {
                        new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Sync successful")
                                .show();
                    }

                    SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_IS_SYNCED, 1);
                    writableDb.update(DatabaseHelper.TABLE_USERS, values, null, null);
                    writableDb.close();

                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {

                    new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Failed to sync.")
                            .setContentText(finalErrorMessage)
                            .show();

                    if (callback != null) {
                        callback.onFailure(finalErrorMessage);
                    }
                }
            });
        });
    }

    public DaoSession getDaoSession() {
        OnepassOpenHelper helper = new OnepassOpenHelper(this, "facex_db", null);
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        return daoSession;
    }

    public static class OnepassOpenHelper extends DaoMaster.OpenHelper {

        public OnepassOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory);
        }

        @Override
        public void onUpgrade(Database db, int oldVersion, int newVersion) {
            MigrationHelper.migrate(db, new MigrationHelper.ReCreateAllTableListener() {

                @Override
                public void onCreateAllTables(Database db, boolean ifNotExists) {
                    DaoMaster.createAllTables(db, ifNotExists);
                }

                @Override
                public void onDropAllTables(Database db, boolean ifExists) {
                    DaoMaster.dropAllTables(db, ifExists);
                }
            }, UserDao.class);// 修改beanDao对象
        }
    }


}

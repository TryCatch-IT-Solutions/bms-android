package com.example.bms;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.example.bms.utils.Logger;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.bms.data.LoginDataSource;
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.data.model.User;
import com.example.bms.time_entry.TimeRepository;
import com.github.yuweiguocn.library.greendao.MigrationHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import cn.pedant.SweetAlert.SweetAlertDialog;
import facex.greendao.gen.DaoMaster;
import facex.greendao.gen.DaoSession;
import facex.greendao.gen.UserDao;

import org.greenrobot.greendao.database.Database;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private ExecutorService syncExecutor; // Dedicated thread pool for sync operations
    private final Object tokenRefreshLock = new Object(); // Lock for token refresh
    private volatile boolean isRefreshingToken = false; // Flag to prevent concurrent refreshes

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    double latitude = 0, longitude = 0;

//    private BatteryLevelReceiver batteryLevelReceiver;

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
//                googleApiAvailability.getErrorDialog(this, status, 2404).show();
                Log.e("Location", "Google Play Services not available");
            }
            return false;
        }
        return true;
    }

    private void initLocation(){

        if (isGooglePlayServicesAvailable()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
// Create a location request
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
//                    .setMinUpdateIntervalMillis(1)
//                    .setMinUpdateDistanceMeters(1)
                    .build();
        }else{
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(this::syncMyDevice);
        }

        // Define the location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    latitude = locationResult.getLastLocation().getLatitude();
                    longitude = locationResult.getLastLocation().getLongitude();

                    SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("latitude",String.valueOf(latitude));
                    editor.putString("longitude",String.valueOf(longitude));
                    editor.apply();

                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> syncMyDevice());
                }

            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> syncMyDevice());
            }

        };

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if(fusedLocationClient != null) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                    .addOnSuccessListener(aVoid -> Log.d("Location", "Successfully requested location updates"))
                    .addOnCompleteListener(task -> Log.d("Location", "Completed location updates"))
                    .addOnFailureListener(e -> {
                        Log.e("AppLocation", "Failed to request location updates", e);
                        Toast.makeText(this, "Failed to request location updates", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Remove callbacks to prevent memory leaks
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        // Cleanup executor services
        if (syncExecutor != null && !syncExecutor.isShutdown()) {
            syncExecutor.shutdownNow();
        }
    }

    private void getApiEndpoint()  {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        String apiEndpoint = sharedPreferences.getString("API_ENDPOINT", "http://115.147.32.2:9001/api");

        BASE_URL = apiEndpoint;
        Logger.d("Configuration", "API Endpoint: " + apiEndpoint);
    }

    private long getSnapshotRetention() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String snapshotRetention = sharedPreferences.getString("SNAPSHOT_RETENTION", "43000");
        return Long.parseLong(snapshotRetention);
    }

    private String getStraingerDetection() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String strangerDetection = sharedPreferences.getString("STRANGER_DETECTION", "on");
        return strangerDetection;
    }

    private long getDeviceSyncInterval() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String syncInterval = sharedPreferences.getString("DEVICE_SYNC_INTERVAL", "60000");
        return Long.parseLong(syncInterval);
    }


    @Override
    public void onCreate() {
        super.onCreate();

        getApiEndpoint();

//        batteryLevelReceiver = new BatteryLevelReceiver();
//        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//        registerReceiver(batteryLevelReceiver, filter);

        dbHelper = new DatabaseHelper(this);
        // Open the database connection
        dbHelper.getWritableDatabase();

//        daoSession = getDaoSession();


        String access = getAccess();

        handler = new Handler(Looper.getMainLooper());
        // Initialize dedicated thread pool for sync operations (reusable to prevent thread leak)
        syncExecutor = Executors.newFixedThreadPool(3);

        long snapshotRetention = getSnapshotRetention();



        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                try {
                    // Run file cleanup on background thread to avoid blocking main thread
                    syncExecutor.execute(() -> {
                        try {
                            FileCleanupUtil fileCleanupUtil = new FileCleanupUtil();
                            fileCleanupUtil.deleteOldFiles(snapshotRetention);
                        } catch (Exception e) {
                            Log.e("App", "Error during file cleanup: " + e.getMessage(), e);
                        }
                    });
                    //every 30minutes
                    handler.postDelayed(this, 1800000);
                } catch (Exception e) {
                    Log.e("App", "Error scheduling cleanup task: " + e.getMessage(), e);
                }
            }
        };
        handler.post(cleanupTask);

        if(access.equals("online")) {
            // Initialize the handler and runnable
            runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        // Use shared thread pool instead of creating new executors every time
                        syncExecutor.execute(() -> {
                            try {
                                getSimilarDevices();
                            } catch (Exception e) {
                                Log.e("App", "Error syncing devices: " + e.getMessage(), e);
                            }
                        });
                        syncExecutor.execute(() -> {
                            try {
                                syncUsersFromWeb();
                            } catch (Exception e) {
                                Log.e("App", "Error syncing users: " + e.getMessage(), e);
                            }
                        });
                        syncExecutor.execute(() -> {
                            try {
                                getTimeEntries();
                            } catch (Exception e) {
                                Log.e("App", "Error syncing time entries: " + e.getMessage(), e);
                            }
                        });
                        syncExecutor.execute(() -> {
                            try {
                                getAnnouncements();
                            } catch (Exception e) {
                                Log.e("App", "Error syncing announcements: " + e.getMessage(), e);
                            }
                        });
                        syncExecutor.execute(() -> {
                            try {
                                syncMyDevice();
                            } catch (Exception e) {
                                Log.e("App", "Error syncing device: " + e.getMessage(), e);
                            }
                        });

                        // Run device settings on background thread as well
                        syncExecutor.execute(() -> {
                            try {
                                getDeviceSettings();
                            } catch (Exception e) {
                                Log.e("App", "Error getting device settings: " + e.getMessage(), e);
                            }
                        });

                        handler.postDelayed(this, getDeviceSyncInterval());
                    } catch (Exception e) {
                        Log.e("App", "Error in sync runnable: " + e.getMessage(), e);
                        // Retry after 30 seconds if there's an error
                        handler.postDelayed(this, 30000);
                    }
                }
            };

            // Start the initial runnable task by posting it to the handler
            handler.post(runnable);

            initLocation();
        }
    }

    private void updateDeviceSyncStatus(String serialNo, String status, String value) {

        LoginDataSource loginDataSource = new LoginDataSource(App.this);
        LoggedInUser userData = loginDataSource.getUserData(App.this);

        if (userData == null) {
            Log.e("App", "User data is null");
            return;
        }

        JSONObject deviceDetails = new JSONObject();


        try {
            deviceDetails.put("serial_no", serialNo);
            deviceDetails.put("status", status);
            deviceDetails.put("value", value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try {
            DeviceRepository deviceRepository = new DeviceRepository(this);
            String token = getToken(this);

            URL url = new URL(App.BASE_URL + "/sync/devices/status");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = deviceDetails.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                Log.d("App", "Status: " + status + " Value: " + value + " Serial No: " + serialNo);
                Log.d("App", "Response 1: " + response.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DeviceRegistration", "Error registering device: " + e.getMessage() + getToken(this));
        }
    }

    private void uploadData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String serialNo = getSerial();
                ExecutorService executor = Executors.newSingleThreadExecutor();

                executor.execute(()-> updateDeviceSyncStatus(serialNo,"pull_status","pending"));
                executor.execute(()-> syncUsersOnLogout(App.this,null));
                executor.execute(()-> syncTimeEntriesPaginated(App.this,null));

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        executor.execute(()-> updateDeviceSyncStatus(serialNo,"pull_status","off"));
                    }
                }, 5000);
            }
        }).start();
    }

    private void downloadData(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                String serialNo = getSerial();
                ExecutorService executor = Executors.newSingleThreadExecutor();

                executor.execute(()-> updateDeviceSyncStatus(serialNo,"push_status","pending"));
                executor.execute(()-> syncUsersFromWeb());
                executor.execute(()-> getTimeEntries());
                executor.execute(()-> getAnnouncements());

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        executor.execute(()-> updateDeviceSyncStatus(serialNo,"push_status","off"));
                    }
                }, 5000);
            }
        }).start();

    }

    private String getDeviceModel(){
        return android.os.Build.MODEL;
    }

    private boolean intToBoolean(int intValue) {
        return intValue == 1;
    }

    private boolean getBooleanValue(JSONObject json, String key) {
        try {
            // First try to get it as a boolean
            return json.getBoolean(key);
        } catch (JSONException e) {
            try {
                // If that fails, try to get it as an int
                return intToBoolean(json.getInt(key));
            } catch (JSONException e2) {
                // Default to false if neither works
                e2.printStackTrace();
                return false;
            }
        }
    }

    private boolean isValidImageUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            return (responseCode == HttpURLConnection.HTTP_OK && contentType.startsWith("image/"));
        } catch (Exception e) {
            return false;
        }
    }

    private String saveImage(String urlString, String fileName) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            File file = new File(getFilesDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    private void getDeviceSettings() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                URL url = new URL(App.BASE_URL + "/sync/settings");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + App.TOKEN);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String appToken = jsonResponse.getString("APP_TOKEN");
                    String fingerprintScoreThreshold = jsonResponse.getString("FINGERPRINT_SCORE_THRESHOLD");
                    String primaryLogo = jsonResponse.getString("PRIMARY_LOGO");
                    String secondaryLogo = jsonResponse.getString("SECONDARY_LOGO");
                    String snapshotRetention = jsonResponse.getString("SNAPSHOT_RETENTION");
                    String strangerDetection = jsonResponse.getString("STRANGER_DETECTION");
                    String screenTimeout = jsonResponse.getString("SCREEN_TIMEOUT");
                    String syncInterval = jsonResponse.getString("DEVICE_SYNC_INTERVAL");

                    String primaryLogoPath = null, secondaryLogoPath = null;

                    SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
                    String previousPrimaryLogoUrl = sharedPreferences.getString("PRIMARY_LOGO_URL", null);
                    String previousSecondaryLogoUrl = sharedPreferences.getString("SECONDARY_LOGO_URL", null);

                    // Save the logos if they are valid image URLs and have changed
                    if (isValidImageUrl(primaryLogo) && !primaryLogo.equals(previousPrimaryLogoUrl)) {
                        primaryLogoPath = saveImage(primaryLogo, "primary_logo.png");
                    } else {
                        primaryLogoPath = sharedPreferences.getString("PRIMARY_LOGO", null);
                    }

                    //check if primaryLogo is null
                    if (primaryLogo.equals("null")) {
                        primaryLogoPath = null;
                    }

                    if (isValidImageUrl(secondaryLogo) && !secondaryLogo.equals(previousSecondaryLogoUrl)) {
                        secondaryLogoPath = saveImage(secondaryLogo, "secondary_logo.png");
                    } else {
                        secondaryLogoPath = sharedPreferences.getString("SECONDARY_LOGO", null);
                    }

                    if(secondaryLogo.equals("null")){
                        secondaryLogoPath = null;
                    }

                    // Store the settings in SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("APP_TOKEN", appToken);
                    editor.putString("FINGERPRINT_SCORE_THRESHOLD", fingerprintScoreThreshold);
                    editor.putString("PRIMARY_LOGO", primaryLogoPath);
                    editor.putString("PRIMARY_LOGO_URL", primaryLogo);
                    editor.putString("SECONDARY_LOGO", secondaryLogoPath);
                    editor.putString("SECONDARY_LOGO_URL", secondaryLogo);
                    editor.putString("SNAPSHOT_RETENTION", snapshotRetention);
                    editor.putString("STRANGER_DETECTION", strangerDetection);
                    editor.putString("SCREEN_TIMEOUT", screenTimeout);
                    editor.putString("DEVICE_SYNC_INTERVAL", syncInterval);
                    editor.apply();

                    handler.post(() -> {
//                        Toast.makeText(this, "Device settings updated", Toast.LENGTH_SHORT).show();
                        Log.d("App", "Device settings updated");
                    });
                } else {
                    handler.post(() -> {
//                        Toast.makeText(this, "Failed to fetch device settings", Toast.LENGTH_SHORT).show();
                        Log.e("App", "Failed to fetch device settings");
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                handler.post(() -> {
                    Log.e("App", "Error: " + e.getMessage());
                });
                e.printStackTrace();
            }
        });
    }

    private String getUserGroupId() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
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


    private int getDeviceBattery() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager != null) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return -1; // Return -1 if the battery level cannot be retrieved
    }

    private String getPrimaryLogo() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        // Return the default logo resource name
        return sharedPreferences.getString("PRIMARY_LOGO", null);
    }

    private String getSecondaryLogo() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        // Return the default logo resource name
        return sharedPreferences.getString("SECONDARY_LOGO", null);
    }

    private void uploadImage(String key, File imageFile) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                URL url = new URL(App.BASE_URL + "/settings");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****");
                conn.setRequestProperty("Authorization", "Bearer " + getToken(this));
                conn.setDoOutput(true);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes("--*****\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"key\"\r\n");
                dos.writeBytes("\r\n");
                dos.writeBytes(key + "\r\n");
                dos.writeBytes("--*****\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"value\"; filename=\"" + imageFile.getName() + "\"\r\n");
                dos.writeBytes("\r\n");

                FileInputStream fis = new FileInputStream(imageFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                fis.close();

                dos.writeBytes("\r\n");
                dos.writeBytes("--*****--\r\n");
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    handler.post(() -> {
//                        Toast.makeText(App.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    });
                } else {
                    handler.post(() -> {
//                        Toast.makeText(App.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                        Log.e("FAILED_UPLOAD","Failed to upload image");
                    });
                }
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(App.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        });
    }

    public void syncMyDevice() {

        if(getAccess().equals("offline")) {
            return;
        }

        LoginDataSource loginDataSource = new LoginDataSource(App.this);
        LoggedInUser userData = loginDataSource.getUserData(App.this);

        if (userData == null) {
            Log.e("App", "User data is null");
            return;
        }


        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {

            String model = Build.MODEL;

            String serialNo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    serialNo = Build.getSerial();
                } catch (SecurityException e) {
                    serialNo = Build.SERIAL;
//                        serialNo = "Permission not granted";
                }
            } else {
                serialNo = Build.SERIAL;
            }

            String groupId = getUserGroupId();

            DeviceRepository deviceRepository = new DeviceRepository(this);
            DeviceModel device = deviceRepository.getDevice(serialNo);

//            if(device != null && device.isSynced()) {
//                return;
//            }

            // Create a JSON object with the device details
            JSONObject deviceDetails = new JSONObject();
            try {
                double[] latLong = getLatAndLong();

                deviceDetails.put("model", model);
                deviceDetails.put("serial_no", serialNo);
                deviceDetails.put("group_id", groupId);

                deviceDetails.put("lat", latLong[0]);
                deviceDetails.put("lon", latLong[1]);

                JSONObject metadata = new JSONObject();
                metadata.put("battery", getDeviceBattery());
                deviceDetails.put("metadata", metadata);

                deviceDetails.put("is_online", true);
                deviceDetails.put("last_sync", dbHelper.getCurrentDateTime());
                deviceDetails.put("last_activity", dbHelper.getCurrentDateTime());

                if(device != null) {
                    if(!device.isSynced()) {
                        deviceDetails.put("manual_time_entry", device.isManualTimeEntry());
                        deviceDetails.put("check_in", device.isCheckIn());
                        deviceDetails.put("check_out", device.isCheckOut());
                        deviceDetails.put("break_in", device.isBreakIn());
                        deviceDetails.put("break_out", device.isBreakOut());
                        deviceDetails.put("overtime_in", device.isOvertimeIn());
                        deviceDetails.put("overtime_out", device.isOvertimeOut());

                        //sync settings
                        if(getPrimaryLogo() != null) {
                            File primaryLogoFile = new File(getPrimaryLogo());
                            if (primaryLogoFile.exists()) {
                                uploadImage("PRIMARY_LOGO", primaryLogoFile);
                            }
                        }
                        if(getSecondaryLogo() != null) {
                            File secondaryLogoFile = new File(getSecondaryLogo());
                            if (secondaryLogoFile.exists()) {
                                uploadImage("SECONDARY_LOGO", secondaryLogoFile);
                            }
                        }
                    }
                }else{
                    deviceDetails.put("manual_time_entry", false);
                    deviceDetails.put("check_in", false);
                    deviceDetails.put("check_out", false);
                    deviceDetails.put("break_in", false);
                    deviceDetails.put("break_out", false);
                    deviceDetails.put("overtime_in", false);
                    deviceDetails.put("overtime_out", false);

                    deviceRepository.insertOrUpdateDevice(
                            Long.parseLong(groupId),
                            model,
                            serialNo,
                            latLong[0],
                            latLong[1],
                            dbHelper.getCurrentDateTime(),
                            true,
                            dbHelper.getCurrentDateTime(),
                            dbHelper.getCurrentDateTime(),
                            JSONObject.NULL.toString(),
                            false,
                            false,
                            false,
                            false,
                            false,
                            false,
                            false
                    );
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                String token = getToken(this);

                URL url = new URL(App.BASE_URL + "/sync/devices");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = deviceDetails.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    deviceRepository.updateSyncedDevice(serialNo);
                }





            } catch (Exception e) {
                e.printStackTrace();
                Log.e("DeviceRegistration", "Error registering device: " + e.getMessage() + getToken(this));
            }
        });

    }

    private String getSerial(){
        String serialNo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                serialNo = Build.getSerial();
            } catch (SecurityException e) {
                serialNo = Build.SERIAL;
//                        serialNo = "Permission not granted";
            }
        } else {
            serialNo = Build.SERIAL;
        }
        return serialNo;
    }

    private double[] getLatAndLong() {
        SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        double latitude = 0, longitude = 0;
        try{
            latitude = Double.parseDouble(sharedPreferences.getString("latitude", "0"));
            longitude = Double.parseDouble(sharedPreferences.getString("longitude", "0"));
        }catch (Exception e){
            e.printStackTrace();
        }

        return new double[]{latitude, longitude};
    }

    public void getSimilarDevices(){

        String mySerial = getSerial();
        try {
            String model = getDeviceModel();
            URL url = new URL(App.BASE_URL + "/all/devices?model=" + model + "&serial_no=" + mySerial);
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

            JSONArray devices = new JSONArray(response.toString());

            DeviceRepository deviceRepository = new DeviceRepository(App.this);

            String[] serials = new String[devices.length()];

            for (int i = 0; i < devices.length(); i++) {
                JSONObject device = devices.getJSONObject(i);

                DeviceModel storedDevice = deviceRepository.getDevice(device.getString("serial_no"));

                if(mySerial.equals(device.getString("serial_no"))){
                    String pushStatus = device.getString("push_status");
                    String pullStatus = device.getString("pull_status");
                    if(pushStatus.equals("on")) {
                        downloadData();
                    }
                    if (pullStatus.equals("on")) {
                        uploadData();
                    }

                }

                if(storedDevice != null && device.getString("serial_no").equals(storedDevice.getSerialNo()) && !storedDevice.isSynced() ) {
                    continue;
                }

                deviceRepository.insertOrUpdateDevice(
                        device.getLong("group_id"),
                        device.getString("model"),
                        device.getString("serial_no"),
                        device.getDouble("lat"),
                        device.getDouble("lon"),
                        device.getString("created_at"),
                        getBooleanValue(device,"is_online"),
                        device.getString("last_sync"),
                        device.getString("last_activity"),
                        device.getString("logo_url"),
                        device.getBoolean("manual_time_entry"),
                        device.getBoolean("check_in"),
                        device.getBoolean("check_out"),
                        device.getBoolean("break_in"),
                        device.getBoolean("break_out"),
                        device.getBoolean("overtime_in"),
                        device.getBoolean("overtime_out")
                );
                serials[i] = device.getString("serial_no");
            }

            deviceRepository.removeMissingDevices(serials);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("LoginActivity", "Error during device sync: " + e.getMessage(), e);
        }
    }

    private void syncUsersFromWeb(){

        String access = getAccess();
        if(access.equals("offline")) {
            return;
        }

        syncUsersOnLogout(App.this, new SyncCallback() {
            @Override
            public void onSuccess() {

                try {

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

                    JSONArray users = new JSONArray(response.toString());
                    UserRepository userRepository = new UserRepository(App.this);

                    BiometricRepository biometricRepository = new BiometricRepository(App.this);
                    FingerprintRepository fingerprintRepository = new FingerprintRepository(App.this);

                    for (int i = 0; i < users.length(); i++) {
                        JSONObject user = users.getJSONObject(i);
                        long groupId = user.isNull("group_id") ? 0 : user.getLong("group_id");
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

//                                Log.d("Fingerprint", "Key: " + fingerprint.getString("key"));
//                                byte[] decodedBytes = Base64.decode(fingerprint.getString("key"), Base64.DEFAULT);
//                                String decodedKey = new String(decodedBytes, StandardCharsets.ISO_8859_1);
                                fingerprintRepository.insertOrUpdateFingerprint(
                                        biometricId,
                                        fingerprint.getString("key")
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

    private String reverseEscapeJson(String input) {
        if (input == null) {
            return ""; // or "null" if you want to explicitly represent null in JSON
        }
        return input.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\\", "\\");
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


    public void getAnnouncements() {

        String access = getAccess();

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

        if(Objects.equals(userData.getGroupId() ,null)) {
            Log.e("App", "Group ID is null. Cannot fetch announcements.");
            return;
        }


        executor.execute(() -> {
            try {
                URL url = new URL(App.BASE_URL + "/sync/announcements?group_id=" + userData.getGroupId());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " +getToken(App.this));


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

                JSONArray announcements = new JSONArray(response.toString());

                AnnouncementRepository repository = new AnnouncementRepository(App.this);
                UserRepository userRepository = new UserRepository(App.this);
                // Process the announcements as needed
                for (int i = 0; i < announcements.length(); i++) {
                    JSONObject announcement = announcements.getJSONObject(i);

                    LoggedInUser user = userRepository.getUserByEmail(announcement.getString("email"));

                    if(user == null) {
                        continue;
                    }

                    if(repository.hasAnnouncement(announcement.getLong("user_id"), announcement.getString("title"), announcement.getString("message"), announcement.getString("expiration"))) {
                        continue;
                    }

                    repository.insertAnnouncement(Long.parseLong(user.getUserId()), announcement.getString("title"), announcement.getString("message"), announcement.getString("expiration"));
                    // Example: Log the announcement details
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

                JSONArray timeEntries = new JSONArray(response.toString());

                UserRepository userRepository = new UserRepository(App.this);
                TimeRepository repository = new TimeRepository(App.this);
                for (int i = 0; i < timeEntries.length(); i++) {
                    JSONObject entry = timeEntries.getJSONObject(i);

                    long userId = userRepository.findUserIdByEmail(entry.getJSONObject("employee").getString("email"));

                    if(repository.hasTimeEntry(userId, entry.getString("datetime"))) {
                        continue;
                    }

                    repository.insertTimeEntry(
                            userId,
                            entry.getString("type"),
                            entry.getString("datetime"),
                            entry.getString("metadata"),
                            true,
                            entry.getString("snapshot_path"),
                            entry.getString("serial_no")
                    );

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

    class BooleanWrapper {
        public boolean value;
        public BooleanWrapper(boolean value) {
            this.value = value;
        }
    }

    public void syncTimeEntriesPaginated(Context context,SyncCallback callback) {
        syncTimeEntriesPaginated(context, callback, 10);
    }

    @SuppressLint("Range")
    public void syncTimeEntriesPaginated(Context context,SyncCallback callback, int _limit) {
        String access = getAccess();

        if (access.equals("offline")) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Runnable syncTask = new Runnable() {
            @Override
            public void run() {
                DatabaseHelper dbHelper = new DatabaseHelper(context);
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String token = getToken(context);

                BooleanWrapper hasMoreData = new BooleanWrapper(true);
                int offset = 0;
                int limit = _limit;

                List<Integer> ids = new ArrayList<>();

                while (hasMoreData.value) {
                    Cursor cursor = db.rawQuery(
                            "SELECT te.*, u." + DatabaseHelper.COLUMN_EMAIL + " FROM " + DatabaseHelper.TABLE_TIME_ENTRIES + " te " +
                                    "JOIN " + DatabaseHelper.TABLE_USERS + " u ON te." + DatabaseHelper.COLUMN_USER_ID + " = u." + DatabaseHelper.COLUMN_ID +
                                    " WHERE te." + DatabaseHelper.COLUMN_IS_SYNCED + " = 0 LIMIT " + limit + " OFFSET " + offset, null);

                    if (cursor.getCount() == 0) {
                        cursor.close();
                        hasMoreData.value = false;
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        break;
                    }


                    JSONArray timeEntriesArray = new JSONArray();
                    List<File> imageFiles = new ArrayList<>();


                    while (cursor.moveToNext()) {
                        JSONObject timeEntry = new JSONObject();
                        try {
                            timeEntry.put("id", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                            timeEntry.put("user_id", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID)));
                            timeEntry.put("serial_no", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SERIAL_NO)));
                            timeEntry.put("type", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE)));
                            timeEntry.put("email", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL)));
                            timeEntry.put("type", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE)));
                            timeEntry.put("datetime", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATETIME)));
                            timeEntry.put("metadata", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_METADATA)));
                            timeEntry.put("is_synced", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_SYNCED)));
                            timeEntry.put("lat", cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDE)));
                            timeEntry.put("lon", cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDE)));
                            timeEntry.put("created_at", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT)));
                            timeEntry.put("updated_at", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_UPDATED_AT)));
                            timeEntry.put("deleted_at", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_AT)));
                            timeEntry.put("deleted_by", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_BY)));


                            String snapshotPath = cursor.getString(cursor.getColumnIndex("snapshot"));
                            if (snapshotPath != null && !snapshotPath.isEmpty()) {
                                File imageFile = new File(snapshotPath);
                                if (imageFile.exists()) {
                                    imageFiles.add(imageFile);
                                    timeEntry.put("snapshot", imageFile.getName());
                                } else {
                                    timeEntry.put("snapshot", JSONObject.NULL);
                                }
                            } else {
                                timeEntry.put("snapshot", JSONObject.NULL);
                            }

                            ids.add(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        timeEntriesArray.put(timeEntry);
                    }
                    cursor.close();

                    JSONObject jsonData = new JSONObject();
                    try {
                        jsonData.put("time_entries", timeEntriesArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    boolean success = false;
                    String errorMessage = null;
                    String boundary = "*****";

                    try {
                        URL url = new URL(App.BASE_URL + "/sync/time_entries");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setDoOutput(true);

                        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                        // Write JSON data
                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"time_entries\"\r\n\r\n");
                        dos.writeBytes(jsonData.getJSONArray("time_entries").toString());
                        dos.writeBytes("\r\n");

                        // Attach image files
                        for (int i = 0; i < imageFiles.size(); i++) {
                            File imageFile = imageFiles.get(i);
                            FileInputStream fis = new FileInputStream(imageFile);

                            dos.writeBytes("--" + boundary + "\r\n");
                            dos.writeBytes("Content-Disposition: form-data; name=\"snapshots[" + i + "]\"; filename=\"" + imageFile.getName() + "\"\r\n");
                            dos.writeBytes("Content-Type: " + URLConnection.guessContentTypeFromName(imageFile.getName()) + "\r\n\r\n");
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                dos.write(buffer, 0, bytesRead);
                            }
                            dos.writeBytes("\r\n");
                            fis.close();
                        }

                        dos.writeBytes("--" + boundary + "--\r\n");
                        dos.flush();
                        dos.close();

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
                                Log.e("SyncTimeEntriesTask", "Response: " + response.toString());
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
                            writableDb.update(DatabaseHelper.TABLE_TIME_ENTRIES, values, DatabaseHelper.COLUMN_ID + " IN (" + TextUtils.join(",", ids) + ")", null);
                            writableDb.close();

                            if (callback != null) {
                                callback.onSuccess();
                            }

                        } else {
                            hasMoreData.value = false;

                            if (callback != null) {
                                callback.onFailure(finalErrorMessage);
                            }
                        }
                    });

                    offset += limit;
                }
            }
        };

        executor.execute(syncTask);
    }

    public void syncTimeEntriesOnLogout(Context context, SyncCallback callback) {
        syncTimeEntriesOnLogout(context, callback, 10);
    }

    @SuppressLint("Range")
    public void syncTimeEntriesOnLogout(Context context, SyncCallback callback,int limit) {
        String access = getAccess();

        if (access.equals("offline")) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT te.*, u." + DatabaseHelper.COLUMN_EMAIL + " FROM " + DatabaseHelper.TABLE_TIME_ENTRIES + " te " +
                            "JOIN " + DatabaseHelper.TABLE_USERS + " u ON te." + DatabaseHelper.COLUMN_USER_ID + " = u." + DatabaseHelper.COLUMN_ID +
                            " WHERE te." + DatabaseHelper.COLUMN_IS_SYNCED + " = 0 LIMIT 10", null);
            String token = getToken(context);

            if (cursor.getCount() == 0) {
                cursor.close();
                if (callback != null) {
                    callback.onSuccess();
                }
                return;
            }

            JSONArray timeEntriesArray = new JSONArray();
            List<File> imageFiles = new ArrayList<>();

            List<Integer> ids = new ArrayList<>();
            while (cursor.moveToNext()) {
                JSONObject timeEntry = new JSONObject();
                try {
                    timeEntry.put("id", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                    timeEntry.put("user_id", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID)));
                    timeEntry.put("serial_no", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SERIAL_NO)));
                    timeEntry.put("type", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE)));
                    timeEntry.put("email", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL)));
                    timeEntry.put("type", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE)));
                    timeEntry.put("datetime", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATETIME)));
                    timeEntry.put("metadata", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_METADATA)));
                    timeEntry.put("is_synced", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_SYNCED)));
                    timeEntry.put("lat", cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDE)));
                    timeEntry.put("lon", cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDE)));
                    timeEntry.put("created_at", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT)));
                    timeEntry.put("updated_at", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_UPDATED_AT)));
                    timeEntry.put("deleted_at", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_AT)));
                    timeEntry.put("deleted_by", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_BY)));

                    String snapshotPath = cursor.getString(cursor.getColumnIndex("snapshot"));
                    if (snapshotPath != null && !snapshotPath.isEmpty()) {
                        File imageFile = new File(snapshotPath);
                        if (imageFile.exists()) {
                            imageFiles.add(imageFile);
                            timeEntry.put("snapshot", imageFile.getName());
                        } else {
                            timeEntry.put("snapshot", JSONObject.NULL);
                        }
                    } else {
                        timeEntry.put("snapshot", JSONObject.NULL);
                    }

                    ids.add(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                timeEntriesArray.put(timeEntry);
            }
            cursor.close();

            JSONObject jsonData = new JSONObject();
            try {
                jsonData.put("time_entries", timeEntriesArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                boolean success = false;
                String errorMessage = null;
                String boundary = "*****";

                try {
                    URL url = new URL(App.BASE_URL + "/sync/time_entries");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);

                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                    // Write JSON data
                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"time_entries\"\r\n\r\n");
                    dos.writeBytes(jsonData.getJSONArray("time_entries").toString());
                    dos.writeBytes("\r\n");

                    // Attach image files
                    for (int i = 0; i < imageFiles.size(); i++) {
                        File imageFile = imageFiles.get(i);
                        FileInputStream fis = new FileInputStream(imageFile);

                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"snapshots[" + i + "]\"; filename=\"" + imageFile.getName() + "\"\r\n");
                        dos.writeBytes("Content-Type: " + URLConnection.guessContentTypeFromName(imageFile.getName()) + "\r\n\r\n");
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                        }
                        dos.writeBytes("\r\n");
                        fis.close();
                    }

                    dos.writeBytes("--" + boundary + "--\r\n");
                    dos.flush();
                    dos.close();

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
                            Log.e("SyncTimeEntriesTask", "Response: " + response.toString());
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
                        writableDb.update(DatabaseHelper.TABLE_TIME_ENTRIES, values, DatabaseHelper.COLUMN_ID + " IN (" + TextUtils.join(",", ids) + ")", null);
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
        }, 3000);
    }

    @SuppressLint("Range")
    public void syncUsersOnLogout(Context context, SyncCallback callback)  {
        String access = getAccess();

        if (access.equals("offline")) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }


        syncMyDevice();
        String token = getToken(context);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE is_synced = 0", null);

        if (cursor.getCount() < 1) {
            cursor.close();
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        JSONArray usersArray = new JSONArray();

        while (cursor.moveToNext()) {
            try {
                JSONObject userObject = new JSONObject();
                userObject.put("id", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                userObject.put("group_id", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_GROUP_ID)));
                userObject.put("role", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE)));
                userObject.put("first_name", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FIRST_NAME)));
                userObject.put("middle_name", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_MIDDLE_NAME)));
                userObject.put("last_name", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LAST_NAME)));
                userObject.put("lat", cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LAT)));
                userObject.put("lon", cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LON)));
                userObject.put("address1", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ADDRESS1)));
                userObject.put("address2", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ADDRESS2)));
                userObject.put("barangay", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BARANGAY)));
                userObject.put("municipality", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_MUNICIPALITY)));
                userObject.put("province", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PROVINCE)));
                userObject.put("birth_date", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BIRTH_DATE)));
                userObject.put("gender", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GENDER)));
                userObject.put("zip_code", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ZIP_CODE)));
                userObject.put("email", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL)));
                userObject.put("phone_number", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PHONE_NUMBER)));
                userObject.put("emergency_contact_name", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NAME)));
                userObject.put("emergency_contact_no", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NO)));
                userObject.put("status", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS)));
                userObject.put("created_at", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT)));
                userObject.put("updated_at", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_UPDATED_AT)));
                userObject.put("deleted_by", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DELETED_BY)));
                userObject.put("source", cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SOURCE)));

                long userId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
                List<Biometric> biometrics = dbHelper.getBiometricsByUserId(userId);
                JSONArray biometricsArray = new JSONArray();

                for (Biometric biometric : biometrics) {
                    JSONObject biometricObject = new JSONObject();
                    biometricObject.put("id", biometric.getId());
                    biometricObject.put("key", escapeJson(biometric.getKey()));
                    biometricObject.put("is_synced", escapeJson(biometric.getIsSynced().toString()));
                    biometricObject.put("type", escapeJson(biometric.getType()));

                    List<Fingerprint> fingerprints = dbHelper.getFingerprintsByBiometricId(biometric.getId());
                    JSONArray fingerprintsArray = new JSONArray();

                    for (Fingerprint fingerprint : fingerprints) {
                        JSONObject fingerprintObject = new JSONObject();
//                        String key = Base64.encodeToString(fingerprint.getKey().getBytes(), Base64.DEFAULT);
                        fingerprintObject.put("key", fingerprint.getKey());
                        fingerprintsArray.put(fingerprintObject);
                    }
                    biometricObject.put("fingerprints", fingerprintsArray);
                    biometricsArray.put(biometricObject);
                }
                userObject.put("biometrics", biometricsArray);
                usersArray.put(userObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        cursor.close();

        String jsonData = null;
        try {
            jsonData = new JSONObject().put("users", usersArray).toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        writeResponseToFile(jsonData);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        String finalJsonData = jsonData;
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
                    byte[] input = finalJsonData.getBytes("utf-8");
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
                        if (response.toString().equals("false")) {
                            errorMessage = "Unauthorized, please login again.";
                        } else {
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

                        if (finalErrorMessage.contains("Unauthorized")) {
                            refreshToken();
                        }

                        callback.onFailure(finalErrorMessage);
                    }
                }
            });
        });
    }

    private String getCurrentEmail() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String encryptedData = sharedPreferences.getString("user_data", null);
            if (encryptedData != null) {
                byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
                String decryptedData = EncryptionUtil.decrypt(decodedData);
                String[] userData = decryptedData.split(",");
                return userData[1];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void refreshToken() {
        // Prevent concurrent token refresh attempts
        synchronized (tokenRefreshLock) {
            if (isRefreshingToken) {
                Log.d("App", "Token refresh already in progress, skipping");
                return;
            }
            isRefreshingToken = true;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        String currentUserEmail = getCurrentEmail();

        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            Log.e("App", "Cannot refresh token: no current user email");
            synchronized (tokenRefreshLock) {
                isRefreshingToken = false;
            }
            return;
        }

        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL+"/refresh-token?email="+currentUserEmail);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + TOKEN);
                conn.setDoOutput(true);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String newToken = jsonResponse.getString("token");
                    JSONObject userJson = jsonResponse.getJSONObject("user");

                    String displayName =  userJson.getString("first_name") + " " + userJson.getString("last_name");

                    // Save the new token to user_prefs
                    // Encrypt the user data


                    long userGroupId = userJson.isNull("group_id") ? 0 : userJson.getLong("group_id");
//                    System.out.println("The new Encrypt is" + displayName + "," + userJson.getString("email") + "," + "No_Password" + "," + userJson.getLong("group_id")  + "," + userJson.getString("role") + "," + newToken);
                    byte[] encryptedData = EncryptionUtil.encrypt(displayName + "," + userJson.getString("email") + "," + "No_Password" + "," + userGroupId  + "," + userJson.getString("role") + "," + newToken);

                    SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("user_data", Base64.encodeToString(encryptedData, Base64.DEFAULT));
                    editor.apply();

                    handler.post(() -> {
                        Log.d("RefreshToken", "Token refreshed successfully");
                        synchronized (tokenRefreshLock) {
                            isRefreshingToken = false;
                        }

                        syncUsersOnLogout(App.this, new SyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d("RefreshToken", "Users synced after token refresh");
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e("RefreshToken", "Failed to sync users after token refresh: " + errorMessage);
                            }
                        });

                        syncTimeEntriesOnLogout(App.this, new SyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d("RefreshToken", "Time entries synced after token refresh");
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e("RefreshToken", "Failed to sync time entries after token refresh: " + errorMessage);
                            }
                        });
                    });

                } else {
                    handler.post(() -> {
                        Log.e("RefreshToken", "Failed to refresh token, response code: " + responseCode);
                        synchronized (tokenRefreshLock) {
                            isRefreshingToken = false;
                        }
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                handler.post(() -> {
                    Log.e("RefreshToken", "Error refreshing token: " + e.getMessage());
                    synchronized (tokenRefreshLock) {
                        isRefreshingToken = false;
                    }
                });
            } finally {
                // Ensure flag is reset even if there's an uncaught exception
                synchronized (tokenRefreshLock) {
                    isRefreshingToken = false;
                }
            }
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


    public class FileCleanupUtil {

        public void deleteOldFiles(long minutes) {

            // Define the folder path
            File folder = new File(Environment.getExternalStorageDirectory(), "snapshots");

            // Check if the folder exists
            if (!folder.exists() || !folder.isDirectory()) {
                return;
            }

            // Get the current time
            long currentTime = System.currentTimeMillis();

            // Convert minutes to milliseconds
            long thresholdInMillis = minutes * 60 * 1000;

            // List all files in the folder
            File[] files = folder.listFiles();

            if (files == null || files.length == 0) {
                return;
            }

            // Iterate through the files
            for (File file : files) {
                if (file.isFile()) {
                    long lastModified = file.lastModified();

                    // Check if the file is older than the specified time
                    if (currentTime - lastModified > thresholdInMillis) {
                        boolean isDeleted = file.delete();
                    }
                }
            }
        }

    }


}

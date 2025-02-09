package com.example.bms;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.bms.databinding.ActivityConfigurationBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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


    private DeviceRepository deviceRepository;

    private String token;

    private SweetAlertDialog dialog;

    private static final int REQUEST_CODE_PRIMARY_LOGO = 1;
    private static final int REQUEST_CODE_SECONDARY_LOGO = 2;



    private String getAccess() {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("ACCESS", "offline");
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
            query = "SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE " + DatabaseHelper.COLUMN_STATUS + " = 'active' AND " + DatabaseHelper.COLUMN_ROLE + " = 'employee'";
            queryArgs = new String[]{};
        } else if ("groupadmin".equals(role)) {
            query = "SELECT * FROM " + DatabaseHelper.TABLE_USERS + " WHERE " + DatabaseHelper.COLUMN_STATUS + " = 'active' AND " + DatabaseHelper.COLUMN_GROUP_ID + " = ? AND " + DatabaseHelper.COLUMN_ROLE + " = 'employee'";
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

    private void getApiEndpoint() {
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

    private String getSerial(){
        String serialNo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                serialNo = Build.getSerial();
            } catch (SecurityException e) {
                serialNo = Build.SERIAL;
//                        serialNo = "Permission not granted";
                runOnUiThread(() -> Toast.makeText(this, "Permission not available to get serial, selecting default.", Toast.LENGTH_SHORT).show());
            }
        } else {
            serialNo = Build.SERIAL;
        }
        return serialNo;
    }

    private void getTrackerSwitches() {

        String serialNo = getSerial();

        DeviceModel deviceModel = deviceRepository.getDevice(serialNo);

        SwitchMaterial switchTimeRegister = findViewById(R.id.switch_time_register);

        disableTimeSwitches();

        // Load the saved state
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        switchTimeRegister.setChecked(deviceModel.isManualTimeEntry());

        if (deviceModel.isManualTimeEntry()) {
            enableTimeSwitches();
        }

        token = getToken(this);
        Log.d("Configuration", "Token: " + token);

//        getApiEndpoint();

        SwitchMaterial checkInSwitch = findViewById(R.id.check_in_switch);
        SwitchMaterial checkOutSwitch = findViewById(R.id.check_out_switch);
        SwitchMaterial breakInSwitch = findViewById(R.id.break_in_switch);
        SwitchMaterial breakOutSwitch = findViewById(R.id.break_out_switch);
        SwitchMaterial overtimeInSwitch = findViewById(R.id.overtime_in_switch);
        SwitchMaterial overtimeOutSwitch = findViewById(R.id.overtime_out_switch);


        checkInSwitch.setChecked(deviceModel.isCheckIn());
        checkOutSwitch.setChecked(deviceModel.isCheckOut());
        breakInSwitch.setChecked(deviceModel.isBreakIn());
        breakOutSwitch.setChecked(deviceModel.isBreakOut());
        overtimeInSwitch.setChecked(deviceModel.isOvertimeIn());
        overtimeOutSwitch.setChecked(deviceModel.isOvertimeOut());
    }

    private void disableTimeSwitches() {
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

        syncOnDatabase();
    }

    private void syncOnDatabase() {

        String serialNo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                serialNo = Build.getSerial();
            } catch (SecurityException e) {
                serialNo = Build.SERIAL;
//                        serialNo = "Permission not granted";
                runOnUiThread(() -> Toast.makeText(this, "Permission not available to get serial, selecting default.", Toast.LENGTH_SHORT).show());
            }
        } else {
            serialNo = Build.SERIAL;
        }
        SwitchMaterial switchTimeRegister = findViewById(R.id.switch_time_register);
        SwitchMaterial checkInSwitch = findViewById(R.id.check_in_switch);
        SwitchMaterial checkOutSwitch = findViewById(R.id.check_out_switch);
        SwitchMaterial breakInSwitch = findViewById(R.id.break_in_switch);
        SwitchMaterial breakOutSwitch = findViewById(R.id.break_out_switch);
        SwitchMaterial overtimeInSwitch = findViewById(R.id.overtime_in_switch);
        SwitchMaterial overtimeOutSwitch = findViewById(R.id.overtime_out_switch);

        deviceRepository.updateDeviceTimeConfig(serialNo,
                switchTimeRegister.isChecked(),
                checkInSwitch.isChecked(),
                checkOutSwitch.isChecked(),
                breakInSwitch.isChecked(),
                breakOutSwitch.isChecked(),
                overtimeInSwitch.isChecked(),
                overtimeOutSwitch.isChecked());

        ((App) getApplication()).syncMyDevice();
    }

    private void enableTimeSwitches() {
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

        syncOnDatabase();
    }

    private void selectImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    private void initSettings(){
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String fingerprintScoreThreshold = sharedPreferences.getString("FINGERPRINT_SCORE_THRESHOLD", "50");
        String snapshotRetention = sharedPreferences.getString("SNAPSHOT_RETENTION", "30");
        String strangerDetection = sharedPreferences.getString("STRANGER_DETECTION", "off");
        String screenTimeout = sharedPreferences.getString("SCREEN_TIMEOUT", "60000");
        String syncInterval = sharedPreferences.getString("DEVICE_SYNC_INTERVAL", "60000");

        TextInputEditText inputDeviceSyncInterval = findViewById(R.id.input_device_sync_interval);
        TextInputEditText inputScreenTimeout = findViewById(R.id.input_screen_timeout);
        SwitchMaterial switchStrangerDetection = findViewById(R.id.switch_stranger_detection);
        TextInputEditText inputSnapshotRetention = findViewById(R.id.input_snapshot_retention);
        TextInputEditText inputFingerprintScoreThreshold = findViewById(R.id.input_fingerprint_score_threshold);

// Set the values to the UI elements
        inputDeviceSyncInterval.setText(syncInterval);
        inputScreenTimeout.setText(screenTimeout);
        switchStrangerDetection.setChecked(strangerDetection.equals("on"));
        inputSnapshotRetention.setText(snapshotRetention);
        inputFingerprintScoreThreshold.setText(fingerprintScoreThreshold);


        switchStrangerDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences devieSharedPrefs = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = devieSharedPrefs.edit();
            editor.putString("STRANGER_DETECTION", isChecked ? "on" : "off");
            editor.apply();
        });

        inputDeviceSyncInterval.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("DEVICE_SYNC_INTERVAL", s.toString());
                editor.apply();
            }
        });

        inputScreenTimeout.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("SCREEN_TIMEOUT", s.toString());
                editor.apply();
            }
        });

        inputSnapshotRetention.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("SNAPSHOT_RETENTION", s.toString());
                editor.apply();
            }
        });

        inputFingerprintScoreThreshold.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("FINGERPRINT_SCORE_THRESHOLD", s.toString());
                editor.apply();
            }
        });
    }

    private void uploadImage(String key, File imageFile) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        if(getAccess().equals("offline")) {
            return;
        }

        executor.execute(() -> {
            try {
                URL url = new URL(App.BASE_URL + "/settings");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****");
                conn.setRequestProperty("Authorization", "Bearer " + token);
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new SweetAlertDialog(Configuration.this, SweetAlertDialog.SUCCESS_TYPE)
                                        .setTitleText("Image uploaded successfully")
                                        .show();
                            }
                        });
                    });
                } else {
                    handler.post(() -> {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new SweetAlertDialog(Configuration.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Failed to upload image to server")
                                        .show();

                                deviceRepository.updateUnsyncDevice(getSerial());
                            }
                        });
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("Configuration", Objects.requireNonNull(e.getMessage()));
                            new SweetAlertDialog(Configuration.this, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText("Failed to upload image to server")
                                    .setContentText(e.getMessage())
                                    .show();

                            deviceRepository.updateUnsyncDevice(getSerial());
                        }
                    });
                });
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                String imagePath = getPathFromUri(selectedImage);
                Log.d("Configuration", "Image path: " + imagePath);
                if (requestCode == REQUEST_CODE_PRIMARY_LOGO) {
                    saveImagePath("PRIMARY_LOGO", imagePath);
                    uploadImage("PRIMARY_LOGO", new File(imagePath));
                } else if (requestCode == REQUEST_CODE_SECONDARY_LOGO) {
                    saveImagePath("SECONDARY_LOGO", imagePath);
                    uploadImage("SECONDARY_LOGO", new File(imagePath));
                }
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return null;
    }

    private void saveImagePath(String key, String path) {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, path);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityConfigurationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        deviceRepository = new DeviceRepository(this);

        initSettings();

//        Objects.requireNonNull(getWindow().getInsetsController()).hide(WindowInsetsCompat.Type.systemBars());

        // Override the back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                startActivity(new Intent(Configuration.this, MainActivity.class));
                finish();
            }
        });

        if(Objects.equals(getRole(), "groupadmin")){
            findViewById(R.id.secondary_logo_layout).setVisibility(View.GONE);
            findViewById(R.id.button_upload_secondary_logo).setVisibility(View.GONE);
        }

        MaterialButton buttonUploadPrimaryLogo = findViewById(R.id.button_upload_primary_logo);
        MaterialButton buttonUploadSecondaryLogo = findViewById(R.id.button_upload_secondary_logo);

        buttonUploadPrimaryLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage(REQUEST_CODE_PRIMARY_LOGO);
            }
        });

        buttonUploadSecondaryLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage(REQUEST_CODE_SECONDARY_LOGO);
            }
        });


        if (getAccess().equals("offline")) {
            findViewById(R.id.sync_time_layout).setVisibility(View.GONE);
            findViewById(R.id.sync_users_layout).setVisibility(View.GONE);
//            findViewById(R.id.api_endpoint_layout).setVisibility(View.GONE);
        }


        Toolbar toolbarHead = findViewById(R.id.toolbar_header);
        toolbarHead.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Configuration.this, MainActivity.class));
                finish();
            }
        });

        // Get reference to the SwitchMaterial
        SwitchMaterial switchTimeRegister = findViewById(R.id.switch_time_register);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


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

                runOnUiThread(() -> {
                    dialog = new SweetAlertDialog(Configuration.this, SweetAlertDialog.PROGRESS_TYPE);
                    dialog.show();
                });

                ((App) getApplication()).syncTimeEntriesPaginated(Configuration.this, new App.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            dialog = new SweetAlertDialog(Configuration.this, SweetAlertDialog.SUCCESS_TYPE)
                                    .setTitleText("Time entries synced successfully");
                            dialog.show();
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            dialog = new SweetAlertDialog(Configuration.this, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText("Failed to sync time entries")
                                    .setContentText(errorMessage);
                            dialog.show();
                        });
                    }
                });
            }
        });

        findViewById(R.id.button_sync_users).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                runOnUiThread(() -> {
                    dialog = new SweetAlertDialog(Configuration.this, SweetAlertDialog.PROGRESS_TYPE);
                    dialog.show();
                });

                ((App) getApplication()).syncUsersOnLogout(Configuration.this, new App.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            dialog.dismiss();

                            dialog = new SweetAlertDialog(Configuration.this, SweetAlertDialog.SUCCESS_TYPE)
                                    .setTitleText("Users synced successfully");
                            dialog.show();
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            dialog = new SweetAlertDialog(Configuration.this, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText("Failed to sync users")
                                    .setContentText(errorMessage);
                            dialog.show();
                        });
                    }
                });
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
//                Toast.makeText(Configuration.this, "Time Register is ON", Toast.LENGTH_SHORT).show();
            } else {
                disableTimeSwitches();
//                Toast.makeText(Configuration.this, "Time Register is OFF", Toast.LENGTH_SHORT).show();
            }
        });

        // Check In Switch
        SwitchMaterial checkInSwitch = findViewById(R.id.check_in_switch);
        checkInSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_check_in", isChecked);
            editor.apply();

            syncOnDatabase();
        });

// Check Out Switch
        SwitchMaterial checkOutSwitch = findViewById(R.id.check_out_switch);
        checkOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_check_out", isChecked);
            editor.apply();
            syncOnDatabase();
        });

// Break In Switch
        SwitchMaterial breakInSwitch = findViewById(R.id.break_in_switch);
        breakInSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_break_in", isChecked);
            editor.apply();
            syncOnDatabase();
        });

// Break Out Switch
        SwitchMaterial breakOutSwitch = findViewById(R.id.break_out_switch);
        breakOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_break_out", isChecked);
            editor.apply();
            syncOnDatabase();
        });

// Overtime In Switch
        SwitchMaterial overtimeInSwitch = findViewById(R.id.overtime_in_switch);
        overtimeInSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_overtime_in", isChecked);
            editor.apply();
            syncOnDatabase();
        });

// Overtime Out Switch
        SwitchMaterial overtimeOutSwitch = findViewById(R.id.overtime_out_switch);
        overtimeOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the switch state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TIME_REGISTER + "_overtime_out", isChecked);
            editor.apply();
            syncOnDatabase();
        });

        getTrackerSwitches();

    }
}
package com.example.bms;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bms.data.LoginDataSource;
import com.example.bms.enrollment.EnrollmentList;
import com.example.bms.time_entry.TimeEntryRegister;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {


    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    DatabaseHelper databaseHelper;
    double latitude = 0, longitude = 0;


    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;


    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            }
            return false;
        }
        return true;
    }


    private void lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow();
            new Handler().postDelayed(this::turnScreenOn, 5000);
        } else {
            // Request admin permission
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Device admin permission is required to lock the screen.");
            startActivityForResult(intent, 1);
        }
    }

    private void turnScreenOn() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "MyApp::WakeLock");
        wakeLock.acquire(3000); // Wake the screen for 3 seconds
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            lockScreen();
        }
    }

    private void initLocation(){

        if (isGooglePlayServicesAvailable()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
// Create a location request
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(1)
                    .setMinUpdateDistanceMeters(1)
                    .build();
        }else{
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> registerDevice(latitude,longitude));
        }

        // Define the location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    latitude = locationResult.getLastLocation().getLatitude();
                    longitude = locationResult.getLastLocation().getLongitude();

//                    Toast.makeText(DeviceRegistration.this, "Lat: " + latitude + ", Lon: " + longitude, Toast.LENGTH_LONG).show();
                    Log.d("Location", "Lati: " + latitude + ", Long: " + longitude + " " + locationResult.getLocations());
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> registerDevice(latitude,longitude));
                }

                Log.d("Location", "onLocationResult: " + locationResult.getLastLocation());
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                Log.d("Location", "onLocationAvailability: " + locationAvailability.isLocationAvailable());
                ExecutorService executor = Executors.newSingleThreadExecutor();
                if(locationAvailability.isLocationAvailable()) {
                    executor.execute(() -> registerDevice(latitude,longitude));
                }else{
                    executor.execute(() -> registerDevice(0,0));
                }
            }

        };

        // Request location permissions
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startLocationUpdates();
                    } else {
                        Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.execute(() -> registerDevice(latitude,longitude));
                    }
                });

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Log.d("Location", "Requesting location updates");
        if(fusedLocationClient != null) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                    .addOnSuccessListener(aVoid -> Log.d("Location", "Successfully requested location updates"))
                    .addOnCompleteListener(task -> Log.d("Location", "Completed location updates"))
                    .addOnFailureListener(e -> {
                        Log.e("Location", "Failed to request location updates", e);
                        Toast.makeText(this, "Failed to request location updates", Toast.LENGTH_SHORT).show();
                    });
        }
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

    private double[] getLatAndLong() {
        SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        double latitude = sharedPreferences.getLong("latitude", 0);
        double longitude = sharedPreferences.getLong("longitude", 0);
//        Log.d("Location", "Lat: " + latitude + ", Lon: " + longitude);
        return new double[]{latitude, longitude};
    }


    private String getName() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String encryptedData = sharedPreferences.getString("user_data", null);
            if (encryptedData != null) {
                byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
                String decryptedData = EncryptionUtil.decrypt(decodedData);
                String[] userData = decryptedData.split(",");
                return userData[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    private String getToken(Context context) {
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

    private void registerDevice(double latitude, double longitude) {
        String model = Build.MODEL;


        if(latitude != 0 && Math.round(latitude) < 5) {
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("latitude",String.valueOf(latitude));
        editor.putString("longitude",String.valueOf(longitude));
        editor.apply();

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

        String groupId = getUserGroupId();

        Log.d("DeviceRegistration", "registerDevice: " + model + " " + serialNo + " " + latitude + " " + longitude + " " + groupId);

        DeviceRepository deviceRepository = new DeviceRepository(this);
        DeviceModel device = deviceRepository.getDevice(serialNo);

        // Create a JSON object with the device details
        JSONObject deviceDetails = new JSONObject();
        try {
            deviceDetails.put("model", model);
            deviceDetails.put("serial_no", serialNo);
            deviceDetails.put("lat", latitude);
            deviceDetails.put("lon", longitude);
            deviceDetails.put("group_id", groupId);

            if(device != null) {
                deviceDetails.put("is_online", true);
                deviceDetails.put("last_sync", databaseHelper.getCurrentDateTime());
                deviceDetails.put("last_activity", databaseHelper.getCurrentDateTime());

                if(!device.isSynced()) {
                    deviceDetails.put("manual_time_entry", device.isManualTimeEntry());
                    deviceDetails.put("check_in", device.isCheckIn());
                    deviceDetails.put("check_out", device.isCheckOut());
                    deviceDetails.put("break_in", device.isBreakIn());
                    deviceDetails.put("break_out", device.isBreakOut());
                    deviceDetails.put("overtime_in", device.isOvertimeIn());
                    deviceDetails.put("overtime_out", device.isOvertimeOut());
                }

            }else{
                deviceDetails.put("is_online", true);
                deviceDetails.put("last_sync", databaseHelper.getCurrentDateTime());
                deviceDetails.put("last_activity", databaseHelper.getCurrentDateTime());
                deviceDetails.put("manual_time_entry", false);
                deviceDetails.put("check_in", false);
                deviceDetails.put("check_out", false);
                deviceDetails.put("break_in", false);
                deviceDetails.put("break_out", false);
                deviceDetails.put("overtime_in", false);
                deviceDetails.put("overtime_out", false);
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
            Log.d("DeviceRegistration", "Response Code: " + responseCode);

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                Log.d("DeviceRegistration", "Response: " + response.toString());
                deviceRepository.updateSyncedDevice(serialNo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DeviceRegistration", "Error registering device: " + e.getMessage() + getToken(this));
        }
    }

    private String getAccess()  {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("ACCESS", "offline");
    }

    private String getDeviceGroupId(){
        SharedPreferences sharedPreferences = getSharedPreferences("DEVICE_GROUP", Context.MODE_PRIVATE);
        return sharedPreferences.getString(GroupActivity.KEY_SELECTED_GROUP, null);
    }


    private String getSecondaryLogo() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String secondaryLogo = sharedPreferences.getString("SECONDARY_LOGO", null);
        if (secondaryLogo == null) {
            return "drawable/logo"; // Return the default logo resource name
        }
        return secondaryLogo;
    }

    private String getPrimaryLogo() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        // Return the default logo resource name
        return sharedPreferences.getString("PRIMARY_LOGO", null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        databaseHelper  = new DatabaseHelper(this);

        Log.d("Device", "DeviceGroupId: " + getDeviceGroupId());

        if(getAccess().equals("online")){
            initLocation();
        }
        // Set up the window insets listener
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Objects.requireNonNull(getWindow().getInsetsController()).hide(WindowInsetsCompat.Type.systemBars());


        ImageView logo = findViewById(R.id.logo);
        String secondaryLogo = getSecondaryLogo();
        if (!secondaryLogo.equals("drawable/logo")) {
            File imgFile = new File(secondaryLogo);
            Log.d("SecondaryLogo", "Path: " + imgFile.getAbsolutePath() + " Exists: " + imgFile.exists());
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                logo.setImageBitmap(myBitmap);
            } else {
                logo.setImageResource(R.drawable.logo);
            }
        } else {
            logo.setImageResource(R.drawable.logo);
        }

        // Get reference to the User Enrollment button
        Button btnUserEnrollment = findViewById(R.id.btn_user_enrollment);
        Button btnGroup = findViewById(R.id.btn_group);


        if(getAccess().equals("offline")){
            btnGroup.setVisibility(View.GONE);
        }

        if(getRole().equals("groupadmin")){
            btnGroup.setVisibility(View.GONE);
        }

        TextView name = findViewById(R.id.user_name);
        name.setText(getName());

        btnGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GroupActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Set onClickListener for the button
        btnUserEnrollment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the EnrollmentActivity when the button is clicked
                Intent intent = new Intent(MainActivity.this, EnrollmentActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_user_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EnrollmentList.class);
                startActivity(intent);
            }
        });


        findViewById(R.id.btn_time_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TimeEntryRegister.class);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.btn_config).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Configuration.class);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //show loading
                SweetAlertDialog dialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.PROGRESS_TYPE)
                        .setTitleText("Logging out");
                dialog.show();


                ((App) getApplication()).syncUsersOnLogout(MainActivity.this, new App.SyncCallback() {
                    @Override
                    public void onSuccess() {

                        ((App) getApplication()).syncTimeEntriesOnLogout(MainActivity.this, new App.SyncCallback() {
                            @Override
                            public void onSuccess() {
                                dialog.dismiss();
                                LoginDataSource loginDataSource = new LoginDataSource(MainActivity.this);
                                loginDataSource.logout(MainActivity.this);
                                Intent intent = new Intent(MainActivity.this, SplashScreen.class);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                dialog.dismiss();
                                new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Error")
                                        .setContentText("Logout failed: " + errorMessage)
                                        .show();

                                ((App) getApplication()).refreshToken();
                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        dialog.dismiss();
                        new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Error")
                                .setContentText("Logout failed: " + errorMessage)
                                .show();
                        ((App) getApplication()).refreshToken();
                    }
                });
            }
        });
    }
}

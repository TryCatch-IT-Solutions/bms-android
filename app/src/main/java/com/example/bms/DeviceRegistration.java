package com.example.bms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
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
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.databinding.ActivityDeviceRegistrationBinding;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class DeviceRegistration extends AppCompatActivity {

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    LoggedInUser loggedInUser;

    private SharedPreferences sharedPreferences;

    double latitude = 0, longitude = 0;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ActivityDeviceRegistrationBinding binding = ActivityDeviceRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.lottieAnimation.setAnimationFromUrl("https://lottie.host/a094d2a3-45f3-4d43-a183-3639cf6eac2a/iTsyy0SWpU.lottie");
        binding.lottieAnimation.playAnimation();

        findViewById(R.id.save_button).setOnClickListener(v -> {
            saveApiEndpoint();
        });

        Log.d("DeviceRegistration", "onCreate: Device Registration");
        if (isGooglePlayServicesAvailable()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//             Create a location request
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setMinUpdateIntervalMillis(5000)
                    .build();

//            locationRequest = LocationRequest.create()
//                    .setInterval(10000) // 10 seconds
//                    .setFastestInterval(5000) // 5 seconds
//                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else {
            Toast.makeText(this, "Google Play Services not available", Toast.LENGTH_LONG).show();
            new SweetAlertDialog(DeviceRegistration.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Google Play Services not available")
                    .setContentText("Please install or update Google Play Services, and try again.")
                    .show();
            return;
        }

        Log.d("DeviceRegistration", "onCreate: Device registered");

        sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);

        LoginDataSource loginDataSource = new LoginDataSource(DeviceRegistration.this);
        loggedInUser = loginDataSource.getUserData(this);

        // Define the location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    latitude = locationResult.getLastLocation().getLatitude();
                    longitude = locationResult.getLastLocation().getLongitude();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong("latitude", Double.doubleToLongBits(latitude));
                    editor.putLong("longitude", Double.doubleToLongBits(longitude));
                    editor.apply();

//                    Toast.makeText(DeviceRegistration.this, "Lat: " + latitude + ", Lon: " + longitude, Toast.LENGTH_LONG).show();
                    Log.d("Location", "Lat: " + latitude + ", Lon: " + longitude);
                    initMain();
                }

                Log.d("Location", "onLocationResult: " + locationResult.getLocations());
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                Log.d("Location", "onLocationAvailability: " + locationAvailability.isLocationAvailable());
                if (!locationAvailability.isLocationAvailable()) {
                    new SweetAlertDialog(DeviceRegistration.this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Location not available")
                            .setContentText("Please enable internet and location services in your device settings, and try again.")
                            .show();
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
                        new SweetAlertDialog(DeviceRegistration.this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Permission denied")
                                .setContentText("Please enable location permission in your device settings, and try again.")
                                .show();
                    }
                });

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
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

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Log.d("Location", "Requesting location updates");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                .addOnFailureListener(e -> {

                    Log.e("Location", "Failed to request location updates", e);
                    Toast.makeText(this, "Failed to request location updates", Toast.LENGTH_SHORT).show();

                    //check if device is registered
                    boolean isRegistered = sharedPreferences.getBoolean("isRegistered", false);
                    if (!isRegistered) {
                        startActivity(new Intent(DeviceRegistration.this, DeviceRegistration.class));
                        finish();
                        return;
                    }

                    initMain();
                });
    }


    private void registerDevice() {
        String model = Build.MODEL;
        String serialNo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                serialNo = Build.getSerial();
            } catch (SecurityException e) {
                serialNo = Build.SERIAL;
//                        serialNo = "Permission not granted";
                Toast.makeText(this, "Permission not available to get serial, selecting default.", Toast.LENGTH_SHORT).show();
            }
        } else {
            serialNo = Build.SERIAL;
        }

        Log.d("DeviceRegistration", "registerDevice: " + model + " " + serialNo + " " + latitude + " " + longitude);

        // Create a JSON object with the device details
        JSONObject deviceDetails = new JSONObject();
        try {
            deviceDetails.put("model", model);
            deviceDetails.put("serial_no", serialNo);
            deviceDetails.put("lat", latitude);
            deviceDetails.put("lon", longitude);
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initMain() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(this::registerDevice);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.api_endpoint_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.lottieAnimation).setVisibility(View.GONE);

                getApiEndpoint();
            }
        }, 1000);
    }

    private void getApiEndpoint()  {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        String apiEndpoint = sharedPreferences.getString("API_ENDPOINT", "http://192.168.1.58:8000/api");
        TextInputEditText editTextApiEndpoint = findViewById(R.id.api_endpoint);
        editTextApiEndpoint.setText(apiEndpoint);
        Log.d("Configuration", "API Endpoint: " + apiEndpoint);
    }

    private void saveApiEndpoint() {
        TextInputEditText editTextApiEndpoint = findViewById(R.id.api_endpoint);
        String apiEndpoint = Objects.requireNonNull(editTextApiEndpoint.getText()).toString();

        if(apiEndpoint.isEmpty()){
            new SweetAlertDialog(DeviceRegistration.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("API Endpoint not entered")
                    .setContentText("Please enter the API endpoint and try again.")
                    .show();
            return;
        }

        //check if endpoint is valid
        if(!apiEndpoint.startsWith("http://")){
            new SweetAlertDialog(DeviceRegistration.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Invalid API Endpoint")
                    .setContentText("Please enter a valid URL and try again.")
                    .show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("API_ENDPOINT", apiEndpoint);
        editor.putBoolean("isRegistered", true);

        editor.apply();
        Log.d("DeviceRegistration", "API Endpoint: " + apiEndpoint);
        App.BASE_URL = apiEndpoint;

        if (String.valueOf(loggedInUser.getGroupId()).equals("0")) {
            startActivity(new Intent(DeviceRegistration.this, GroupActivity.class));
            finish();
        } else {
            startActivity(new Intent(DeviceRegistration.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
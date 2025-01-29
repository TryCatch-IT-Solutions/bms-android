package com.example.bms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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
import com.example.bms.data.LoginRepository;
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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {


    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

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

    private void initLocation(){

        SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);

        if (isGooglePlayServicesAvailable()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
// Create a location request
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setMinUpdateIntervalMillis(5000)
                    .setMinUpdateDistanceMeters(27)
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
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong("latitude", Double.doubleToLongBits(latitude));
                    editor.putLong("longitude", Double.doubleToLongBits(longitude));
                    editor.apply();

//                    Toast.makeText(DeviceRegistration.this, "Lat: " + latitude + ", Lon: " + longitude, Toast.LENGTH_LONG).show();
                    Log.d("Location", "Lati: " + latitude + ", Long: " + longitude + " " + locationResult.getLocations());
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> registerDevice(latitude,longitude));
                }

                Log.d("Location", "onLocationResult: " + locationResult.getLocations());
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                Log.d("Location", "onLocationAvailability: " + locationAvailability.isLocationAvailable());
                if (!locationAvailability.isLocationAvailable()) {
                   Log.e("Location", "Location not available");
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> registerDevice(latitude,longitude));
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

    private void registerDevice(double lat, double lon) {
        String model = Build.MODEL;
        String serialNo;

        double[] latLong = getLatAndLong();

        if(lat != 0 && lon != 0){
            latLong[0] = lat;
            latLong[1] = lon;
        }

        System.out.println("The lat and long are: " + latLong[0] + " " + latLong[1]);
        double latitude = latLong[0];
        double longitude = latLong[1];


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

        // Create a JSON object with the device details
        JSONObject deviceDetails = new JSONObject();
        try {
            deviceDetails.put("model", model);
            deviceDetails.put("serial_no", serialNo);
            deviceDetails.put("lat", latitude);
            deviceDetails.put("lon", longitude);
            deviceDetails.put("group_id", groupId);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        getWindow().getInsetsController().hide(WindowInsetsCompat.Type.systemBars());

        // Get reference to the User Enrollment button
        Button btnUserEnrollment = findViewById(R.id.btn_user_enrollment);
        Button btnGroup = findViewById(R.id.btn_group);


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
            }
        });

        findViewById(R.id.btn_config).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Configuration.class);
                startActivity(intent);
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

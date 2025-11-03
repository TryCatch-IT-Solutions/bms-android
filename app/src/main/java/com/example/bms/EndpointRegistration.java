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
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.example.bms.databinding.ActivityEndpointRegistrationBinding;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class EndpointRegistration extends AppCompatActivity {


    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;

    private SharedPreferences sharedPreferencesGroup;
    double latitude = 0, longitude = 0;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
        dismissLoadingDialog();
    }

    private void getApiEndpoint() {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        String apiEndpoint = sharedPreferences.getString("API_ENDPOINT", "https://dai-solutions.com.ph/api");
        TextInputEditText editTextApiEndpoint = findViewById(R.id.api_endpoint);
        editTextApiEndpoint.setText(apiEndpoint);
    }

    private void getApiToken() {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("API_TOKEN", "client-123456");
        TextInputEditText editTextApiEndpoint = findViewById(R.id.api_token);
        editTextApiEndpoint.setText(token);
        Log.d("Configuration", "API Token: " + token);
    }

    private String getAccess() {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("ACCESS", "offline");
    }


    private void syncGroups() {
        try {
            Log.d("EndpointRegistration", "Starting group sync");

            runOnUiThread(() -> {
                if (loadingDialog != null) {
                    loadingDialog.setTitleText("Syncing Groups");
                    loadingDialog.setContentText("Fetching groups from server...");
                }
            });

            URL url = new URL(App.BASE_URL + "/sync/groups");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + App.TOKEN);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                conn.disconnect();
                runOnUiThread(() -> {
                    dismissLoadingDialog();
                    restoreUI();
                    new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Failed to sync groups")
                            .setContentText("Server returned error code: " + responseCode + ". Please try again.")
                            .show();
                });
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            br.close();
            conn.disconnect();

            JSONArray groups = new JSONArray(response.toString());
            GroupRepository groupRepository = new GroupRepository(this);
            groupRepository.resetTable();

            runOnUiThread(() -> {
                if (loadingDialog != null) {
                    loadingDialog.setContentText("Processing " + groups.length() + " groups...");
                }
            });

            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.getJSONObject(i);
                groupRepository.insertGroup(
                        group.getLong("id"),
                        group.getString("name"),
                        group.getString("created_at"),
                        group.getString("updated_at"));
            }

            Log.d("EndpointRegistration", "Groups synced successfully, starting user sync");

            // Continue to user sync
            syncUsers();

        } catch (JSONException e) {
            Log.e("EndpointRegistration", "JSON parsing error during group sync: " + e.getMessage(), e);
            runOnUiThread(() -> {
                dismissLoadingDialog();
                restoreUI();
                new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Data Error")
                        .setContentText("Failed to process groups from server. Please contact support.")
                        .show();
            });
        } catch (IOException e) {
            Log.e("EndpointRegistration", "Network error during group sync: " + e.getMessage(), e);
            runOnUiThread(() -> {
                dismissLoadingDialog();
                restoreUI();
                new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Network Error")
                        .setContentText("Failed to connect to server. Please check your internet connection and try again.")
                        .show();
            });
        } catch (Exception e) {
            Log.e("EndpointRegistration", "Unexpected error during group sync: " + e.getMessage(), e);
            runOnUiThread(() -> {
                dismissLoadingDialog();
                restoreUI();
                new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Sync Failed")
                        .setContentText("An unexpected error occurred: " + e.getMessage())
                        .show();
            });
        }
    }

    private void saveApiEndpoint() {
        TextInputEditText editTextApiEndpoint = findViewById(R.id.api_endpoint);
        String apiEndpoint = Objects.requireNonNull(editTextApiEndpoint.getText()).toString();

        if (apiEndpoint.isEmpty()) {
            new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("API Endpoint not entered")
                    .setContentText("Please enter the API endpoint and try again.")
                    .show();
            return;
        }

        //check if endpoint is valid
        if (!apiEndpoint.startsWith("http://") && !apiEndpoint.startsWith("https://")) {
            new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Invalid API Endpoint")
                    .setContentText("Please enter a valid URL and try again.")
                    .show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("API_ENDPOINT", apiEndpoint);
        editor.putString("ACCESS", access);
        editor.putBoolean("isRegistered", true);

        editor.apply();
        Log.d("EndpointRegistration", "API Endpoint: " + apiEndpoint);
        App.BASE_URL = apiEndpoint;

        updateNetworkSecurityConfig(apiEndpoint);

        findViewById(R.id.api_endpoint_layout).setVisibility(View.GONE);
        findViewById(R.id.lottieAnimation).setVisibility(View.VISIBLE);

        if (access.equals("online")) {
            // Show loading dialog
            loadingDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
            loadingDialog.setTitleText("Initializing");
            loadingDialog.setContentText("Preparing to sync data...");
            loadingDialog.setCancelable(false);
            loadingDialog.show();

            // Start sequential sync process in background
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    // First, get similar devices
                    runOnUiThread(() -> {
                        if (loadingDialog != null) {
                            loadingDialog.setTitleText("Device Setup");
                            loadingDialog.setContentText("Fetching device information...");
                        }
                    });
                    ((App) getApplication()).getSimilarDevices();

                    // Then sync groups and users (syncGroups will call syncUsers)
                    syncGroups();
                } catch (Exception e) {
                    Log.e("EndpointRegistration", "Error during sync process: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        dismissLoadingDialog();
                        restoreUI();
                        new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Sync Failed")
                                .setContentText("An error occurred during synchronization. Please try again.")
                                .show();
                    });
                }
            });
        }

    }


    private void saveApiToken() {
        TextInputEditText editTextApiEndpoint = findViewById(R.id.api_token);
        String apiEndpoint = Objects.requireNonNull(editTextApiEndpoint.getText()).toString();

        if (apiEndpoint.isEmpty()) {
            new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("API TOKEN not entered")
                    .setContentText("Please enter the API token and try again.")
                    .show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("API_TOKEN", apiEndpoint);

        editor.apply();
        Log.d("EndpointRegistration", "API TOKEN: " + apiEndpoint);
        App.TOKEN = apiEndpoint;
    }


    private void updateNetworkSecurityConfig(String apiEndpoint) {
        String domain = apiEndpoint.replaceFirst("http://", "").split("/")[0];
        String config = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<network-security-config>\n" +
                "    <domain-config cleartextTrafficPermitted=\"true\">\n" +
                "        <domain includeSubdomains=\"true\">" + domain + "</domain>\n" +
                "    </domain-config>\n" +
                "</network-security-config>";

        try (FileOutputStream fos = openFileOutput("network_security_config.xml", Context.MODE_PRIVATE)) {
            fos.write(config.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("EndpointRegistration", "Error updating network security config: " + e.getMessage(), e);
        }
    }

    private SweetAlertDialog loadingDialog;

    private void syncUsers() {
        try {
            Log.d("EndpointRegistration", App.BASE_URL + "/sync/users/login");

            runOnUiThread(() -> {
                if (loadingDialog != null) {
                    loadingDialog.setTitleText("Syncing Users");
                    loadingDialog.setContentText("Fetching user data from server...");
                }
            });

            URL url = new URL(App.BASE_URL + "/sync/users/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + App.TOKEN);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                conn.disconnect();
                runOnUiThread(() -> {
                    dismissLoadingDialog();
                    restoreUI();
                    new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Failed to sync users")
                            .setContentText("Server returned error code: " + responseCode + ". Please try again.")
                            .show();
                });
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            br.close();
            conn.disconnect();

            Log.d("EndpointRegistration", "Users received: " + response.length() + " characters");
            JSONArray users = new JSONArray(response.toString());

            runOnUiThread(() -> {
                if (loadingDialog != null) {
                    loadingDialog.setContentText("Processing " + users.length() + " users...");
                }
            });

            UserRepository userRepository = new UserRepository(this);
            userRepository.resetUsersTable();

            BiometricRepository biometricRepository = new BiometricRepository(EndpointRegistration.this);
            FingerprintRepository fingerprintRepository = new FingerprintRepository(EndpointRegistration.this);

            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);

                long groupId = user.isNull("group_id") ? 0 : user.getLong("group_id");
                long userId = userRepository.insertSyncUser(
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
                    String biometricType = biometric.getString("type");

                    long biometricId = biometricRepository.insertBiometric(
                            biometric.getString("key"),
                            userId,
                            biometricType);

                    // Only process fingerprints array for fingerprint type biometrics
                    // Face and RFID biometrics store data directly in the key field
                    if (biometricType.equals("fingerprint") && biometric.has("fingerprints")) {
                        JSONArray fingerprints = biometric.getJSONArray("fingerprints");
                        for (int k = 0; k < fingerprints.length(); k++) {
                            JSONObject fingerprint = fingerprints.getJSONObject(k);
                            fingerprintRepository.insertFingerprint(
                                    biometricId,
                                    fingerprint.getString("key")
                            );
                        }
                    }
                    // For face and rfid types, the template data is already stored in biometric.key
                }
            }

            Log.d("EndpointRegistration", "Sync completed successfully");

            runOnUiThread(() -> {
                dismissLoadingDialog();
                startActivity(new Intent(EndpointRegistration.this, SplashScreen.class));
                finish();
            });

        } catch (JSONException e) {
            Log.e("EndpointRegistration", "JSON parsing error during user sync: " + e.getMessage(), e);
            runOnUiThread(() -> {
                dismissLoadingDialog();
                restoreUI();
                new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Data Error")
                        .setContentText("Failed to process server response. Please contact support.")
                        .show();
            });
        } catch (IOException e) {
            Log.e("EndpointRegistration", "Network error during user sync: " + e.getMessage(), e);
            runOnUiThread(() -> {
                dismissLoadingDialog();
                restoreUI();
                new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Network Error")
                        .setContentText("Failed to connect to server. Please check your internet connection and try again.")
                        .show();
            });
        } catch (Exception e) {
            Log.e("EndpointRegistration", "Unexpected error during user sync: " + e.getMessage(), e);
            runOnUiThread(() -> {
                dismissLoadingDialog();
                restoreUI();
                new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Sync Failed")
                        .setContentText("An unexpected error occurred: " + e.getMessage())
                        .show();
            });
        }
    }

    private void restoreUI() {
        findViewById(R.id.api_endpoint_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.lottieAnimation).setVisibility(View.GONE);
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    private boolean isValidBase64(String base64) {
        try {
            Base64.decode(base64, Base64.DEFAULT);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
                });
    }

    private void initLocation() {
        // Define the location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    latitude = locationResult.getLastLocation().getLatitude();
                    longitude = locationResult.getLastLocation().getLongitude();
                    SharedPreferences.Editor editor = sharedPreferencesGroup.edit();
                    editor.putLong("latitude", Double.doubleToLongBits(latitude));
                    editor.putLong("longitude", Double.doubleToLongBits(longitude));
                    editor.apply();

//                    Toast.makeText(DeviceRegistration.this, "Lat: " + latitude + ", Lon: " + longitude, Toast.LENGTH_LONG).show();
                    Log.d("Location", "Lat: " + latitude + ", Lon: " + longitude);
//                    initMain();
                }

                Log.d("Location", "onLocationResult: " + locationResult.getLocations());
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                Log.d("Location", "onLocationAvailability: " + locationAvailability.isLocationAvailable());
            }
        };

        // Request location permissions
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startLocationUpdates();
                    } else {
                        new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
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

    private CardView offlineCard, onlineCard;
    private String access;

    private void initOfflineVariables() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("APP_TOKEN", null);
        editor.putString("FINGERPRINT_SCORE_THRESHOLD", "85");
        editor.putString("PRIMARY_LOGO", null);
        editor.putString("PRIMARY_LOGO_URL", null);
        editor.putString("SECONDARY_LOGO", null);
        editor.putString("SECONDARY_LOGO_URL", null);
        editor.putString("SNAPSHOT_RETENTION", "43200");
        editor.putString("STRANGER_DETECTION", "on");
        editor.putString("SCREEN_TIMEOUT", "60000");
        editor.putString("DEVICE_SYNC_INTERVAL", "60000");
        editor.apply();
    }

    private String getSerial() {
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean("isRegistered", false)) {
            startActivity(new Intent(EndpointRegistration.this, SplashScreen.class));
            finish();
        }

        ActivityEndpointRegistrationBinding binding = ActivityEndpointRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.lottieAnimation.setAnimationFromUrl("https://lottie.host/a094d2a3-45f3-4d43-a183-3639cf6eac2a/iTsyy0SWpU.lottie");
        binding.lottieAnimation.playAnimation();

        findViewById(R.id.save_button).setOnClickListener(v -> {
            if (access == null) {
                new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Access not selected")
                        .setContentText("Please select the access type and try again.")
                        .show();
                return;
            }

            DeviceRepository deviceRepository = new DeviceRepository(this);
            DatabaseHelper dbHelper = new DatabaseHelper(this);

            deviceRepository.insertOrUpdateDevice(1,
                    Build.MODEL,
                    getSerial(),
                    0,
                    0,
                    dbHelper.getCurrentDateTime(),
                    true,
                    dbHelper.getCurrentDateTime(),
                    dbHelper.getCurrentDateTime(),
                    "",
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );

            if (access.equals("offline")) {
                try {
                    dbHelper.resetUsersTable();
                    dbHelper.insertSuperAdmin();

                    @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("API_ENDPOINT", "http://localhost:8000/api");
                    editor.putString("ACCESS", access);
                    editor.putBoolean("isRegistered", true);
                    editor.apply();

                    initOfflineVariables();

                    saveApiToken();
                    saveApiEndpoint();

                    startActivity(new Intent(EndpointRegistration.this, SplashScreen.class));
                    finish();

                } catch (Exception e) {
                    new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Failed to create database")
                            .setContentText("Failed to create database. Please close the app, and try again.")
                            .show();
                }

                return;
            }


            // Make a request to /health
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {

                    TextInputEditText editTextApiEndpoint = findViewById(R.id.api_endpoint);
                    String apiEndpoint = Objects.requireNonNull(editTextApiEndpoint.getText()).toString();

                    URL url = new URL(apiEndpoint + "/health");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String output;
                        while ((output = br.readLine()) != null) {
                            response.append(output);
                        }
                        br.close();
                        conn.disconnect();

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.has("status") && "ok".equals(jsonResponse.getString("status"))) {
                            runOnUiThread(() -> {
                                saveApiToken();
                                saveApiEndpoint();
                            });
                        } else {
                          runOnUiThread(() -> {
                              new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                                      .setTitleText("Invalid API Endpoint")
                                      .setContentText("Please enter a valid API endpoint and try again.")
                                      .show();
                          });
                        }
                    } else {
                        runOnUiThread(() -> {
                            new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText("Invalid API Endpoint")
                                    .setContentText("Please enter a valid API endpoint and try again.")
                                    .show();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Invalid API Endpoint")
                                .setContentText("Please enter a valid API endpoint and try again.")
                                .show();
                    });

                }

            });


        });

        offlineCard = findViewById(R.id.offline_card);
        onlineCard = findViewById(R.id.online_card);

        offlineCard.setOnClickListener(v -> {
            offlineCard.setBackground(getDrawable(R.color.primary));
            onlineCard.setBackground(getDrawable(R.color.white));
            access = "offline";
            findViewById(R.id.api_endpoint_container).setVisibility(View.GONE);
        });

        onlineCard.setOnClickListener(v -> {
            onlineCard.setBackground(getDrawable(R.color.primary));
            offlineCard.setBackground(getDrawable(R.color.white));
            access = "online";
            findViewById(R.id.api_endpoint_container).setVisibility(View.VISIBLE);
        });

        getApiEndpoint();
        getApiToken();

        // ARTA and EAMC quick selection buttons
        findViewById(R.id.arta_button).setOnClickListener(v -> {
            TextInputEditText editTextApiEndpoint = findViewById(R.id.api_endpoint);
            editTextApiEndpoint.setText("https://arta-bms-api.dai-solutions.com.ph/api");
        });

        findViewById(R.id.eamc_button).setOnClickListener(v -> {
            TextInputEditText editTextApiEndpoint = findViewById(R.id.api_endpoint);
            editTextApiEndpoint.setText("https://eamc-bms-api.dai-solutions.com.ph/api");
        });

        sharedPreferencesGroup = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);

        if (isGooglePlayServicesAvailable()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//             Create a location request
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setMinUpdateIntervalMillis(5000)
                    .build();
        } else {
            Toast.makeText(this, "Google Play Services not available", Toast.LENGTH_LONG).show();
            new SweetAlertDialog(EndpointRegistration.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Google Play Services not available")
                    .setContentText("Please install or update Google Play Services, and try again.")
                    .show();
            return;
        }

        initLocation();
    }


}
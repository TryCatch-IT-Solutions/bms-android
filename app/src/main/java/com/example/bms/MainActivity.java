package com.example.bms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bms.data.LoginDataSource;
import com.example.bms.data.LoginRepository;
import com.example.bms.enrollment.EnrollmentList;
import com.example.bms.time_entry.TimeEntryRegister;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {


    private double[] getLatAndLong() {
        SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        double latitude = sharedPreferences.getLong("latitude", 0);
        double longitude = sharedPreferences.getLong("longitude", 0);
        Log.d("Location", "Lat: " + latitude + ", Lon: " + longitude);
        return new double[]{latitude, longitude};
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

    private void registerDevice() {
        String model = Build.MODEL;
        String serialNo;

        double[] latLong = getLatAndLong();
        double latitude = latLong[0];
        double longitude = latLong[1];


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
            Log.e("DeviceRegistration", "Error registering device: " + e.getMessage() + getToken(this));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        registerDevice();
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
                    }
                });
            }
        });
    }
}

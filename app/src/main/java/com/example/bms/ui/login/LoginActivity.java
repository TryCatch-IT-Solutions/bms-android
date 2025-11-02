package com.example.bms.ui.login;

import android.app.Activity;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bms.App;
import com.example.bms.BiometricRepository;
import com.example.bms.Configuration;
import com.example.bms.DeviceRepository;
import com.example.bms.EncryptionUtil;
import com.example.bms.FingerprintRepository;
import com.example.bms.GroupActivity;
import com.example.bms.GroupRepository;
import com.example.bms.MyAdminReceiver;
import com.example.bms.R;
import com.example.bms.SplashScreen;
import com.example.bms.UserRepository;
import com.example.bms.data.Result;
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.databinding.ActivityLoginBinding;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;
    private SweetAlertDialog resetDialog;

    long modelGroupId;

    private void syncUsers(){
        try {
            URL url = new URL(App.BASE_URL + "/sync/users/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + App.TOKEN);
            conn.setConnectTimeout(10000); // 10 second connection timeout
            conn.setReadTimeout(15000); // 15 second read timeout

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                Log.e("LoginActivity", "Failed to sync users: HTTP " + responseCode);
                throw new RuntimeException("Failed : HTTP error code : " + responseCode);
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
            UserRepository userRepository = new UserRepository(this);
            userRepository.resetUsersTable();

            BiometricRepository biometricRepository = new BiometricRepository(LoginActivity.this);
            FingerprintRepository fingerprintRepository = new FingerprintRepository(LoginActivity.this);

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
                        user.getString("created_at"));

                JSONArray biometrics = user.getJSONArray("biometrics");
                for (int j = 0; j < biometrics.length(); j++) {
                    JSONObject biometric = biometrics.getJSONObject(j);
                    long biometricId = biometricRepository.insertBiometric(
                            biometric.getString("key"),
                            userId,
                            biometric.getString("type"));

                    JSONArray fingerprints = biometric.getJSONArray("fingerprints");
                    for (int k = 0; k < fingerprints.length(); k++) {
                        JSONObject fingerprint = fingerprints.getJSONObject(k);

                        fingerprintRepository.insertFingerprint(
                                biometricId,
                                fingerprint.getString("key")
                        );
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("LoginActivity", "Error during user sync: " + e.getMessage(), e);
            // Show error to user on UI thread
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(LoginActivity.this, "Failed to sync users: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    private String getAccess()  {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("ACCESS", "offline");
    }

    private void syncGroups() {

        if(getAccess().equals("offline")){
            return;
        }

        try {
            URL url = new URL(App.BASE_URL + "/sync/groups");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + App.TOKEN);

            if (conn.getResponseCode() != 200) {
                new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Failed to sync groups")
                        .setContentText("Failed to sync groups from the server. Please close the app, and try again.")
                        .show();
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            conn.disconnect();

            JSONArray groups = new JSONArray(response.toString());
            GroupRepository groupRepository = new GroupRepository(this);
            // Assuming you have a method to reset the groups table
            groupRepository.resetTable();

            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.getJSONObject(i);

                groupRepository.insertGroup(
                        group.getLong("id"),
                        group.getString("name"),
                        group.getString("created_at"),
                        group.getString("updated_at"));
            }

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(LoginActivity.this::syncUsers);
                }
            }, 100);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("GroupActivity", "Error during group sync: " + e.getMessage(), e);
        }
    }

    private void performReset() {
        // Show confirmation dialog
        new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Reset Database")
                .setContentText("This will delete all local data and re-sync from the server. Are you sure?")
                .setCancelText("Cancel")
                .setConfirmText("Reset")
                .showCancelButton(true)
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();
                    startReset();
                })
                .show();
    }

    private void startReset() {
        if(getAccess().equals("offline")) {
            new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Offline Mode")
                    .setContentText("Reset is not available in offline mode.")
                    .show();
            return;
        }

        // Show loading dialog
        resetDialog = new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.PROGRESS_TYPE);
        resetDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.primary));
        resetDialog.setTitleText("Resetting...");
        resetDialog.setContentText("Please wait while we reset and sync data");
        resetDialog.setCancelable(false);
        resetDialog.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // Reset all tables
                UserRepository userRepository = new UserRepository(LoginActivity.this);
                BiometricRepository biometricRepository = new BiometricRepository(LoginActivity.this);
                FingerprintRepository fingerprintRepository = new FingerprintRepository(LoginActivity.this);
                GroupRepository groupRepository = new GroupRepository(LoginActivity.this);

                handler.post(() -> {
                    if (resetDialog != null) {
                        resetDialog.setContentText("Clearing local database...");
                    }
                });

                // Clear all tables
                userRepository.resetUsersTable();
                biometricRepository.resetBiometricsTable();
                fingerprintRepository.resetFingerprintsTable();
                groupRepository.resetTable();

                handler.post(() -> {
                    if (resetDialog != null) {
                        resetDialog.setContentText("Syncing groups...");
                    }
                });

                // Sync groups first
                syncGroupsForReset();

                // Wait a bit for groups to sync
                Thread.sleep(2000);

                handler.post(() -> {
                    if (resetDialog != null) {
                        resetDialog.setContentText("Syncing users and biometrics...");
                    }
                });

                // Sync users and biometrics
                syncUsers();

                // Wait for sync to complete
                Thread.sleep(2000);

                handler.post(() -> {
                    if (resetDialog != null) {
                        resetDialog.dismissWithAnimation();
                    }

                    new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Reset Complete")
                            .setContentText("Database has been reset and synced successfully")
                            .setConfirmClickListener(dialog -> {
                                dialog.dismissWithAnimation();
                            })
                            .show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("LoginActivity", "Error during reset: " + e.getMessage(), e);

                handler.post(() -> {
                    if (resetDialog != null) {
                        resetDialog.dismissWithAnimation();
                    }

                    new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Reset Failed")
                            .setContentText("Failed to reset database: " + e.getMessage())
                            .show();
                });
            }
        });
    }

    private void syncGroupsForReset() {
        try {
            URL url = new URL(App.BASE_URL + "/sync/groups");
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

            JSONArray groups = new JSONArray(response.toString());
            GroupRepository groupRepository = new GroupRepository(this);

            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.getJSONObject(i);

                groupRepository.insertGroup(
                        group.getLong("id"),
                        group.getString("name"),
                        group.getString("created_at"),
                        group.getString("updated_at"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("LoginActivity", "Error during group sync for reset: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void requestAllPermissions() {
        String[] permissions = {
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.CALL_PHONE
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission();
            }
        }

        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, MyAdminReceiver.class);

        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Device admin permission is required to lock the screen.");
            startActivityForResult(intent, 1);
        }
    }

    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 1;
    private void requestManageExternalStoragePermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory(LoginActivity.this))
                .get(LoginViewModel.class);

        Log.d("LoginActivity", App.BASE_URL);

        if(getAccess().equals("online")) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(this::syncGroups);
        }

        requestAllPermissions();

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final MaterialButton loginButton = (MaterialButton) binding.login;
        final ProgressBar loadingProgressBar = binding.loading;

        findViewById(R.id.toggle_password_visibility).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(passwordEditText.getInputType() == (EditorInfo.TYPE_TEXT_VARIATION_PASSWORD | EditorInfo.TYPE_CLASS_TEXT)){
                    passwordEditText.setInputType(EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }else{
                    passwordEditText.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD | EditorInfo.TYPE_CLASS_TEXT);
                }
            }
        });

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    try {
                        updateUiWithUser(loginResult.getSuccess());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "Error during login", e);
                    }
                }
                setResult(Activity.RESULT_OK);
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(LoginActivity.this,usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(LoginActivity.this,usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });

        // Reset button click listener
        MaterialButton resetButton = binding.resetButton;
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performReset();
            }
        });
    }

    private void updateUiWithUser(LoggedInUserView model) throws Exception {

        if(getAccess().equals("offline")){
            EncryptionUtil.generateKey();
            byte[] encryptedData = EncryptionUtil.encrypt(model.getDisplayName() + "," + model.getEmail() + "," + model.getPassword() + "," + "1" + "," + model.getRole()+ ","+ "no-token-offline");
            SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_data", Base64.encodeToString(encryptedData, Base64.DEFAULT));
            editor.apply();
            startActivity(new Intent(LoginActivity.this, SplashScreen.class));
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Result<LoggedInUser> result;
            boolean success = false;
            String token = null;
            try {
                URL url = new URL(App.BASE_URL + "/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                String jsonInputString = "{\"email\": \"" + model.getEmail() + "\", \"password\": \"" + model.getPassword() + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d("LoginDataSource", "Response Code: " + responseCode);

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    Log.d("LoginDataSource", "Response: " + response.toString());

                    // Parse the response to get the error message
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.has("errors")) {
                        String errorMessage = jsonResponse.getString("errors");
                        Log.e("LoginDataSource", "Error: " + errorMessage);

                    }else{
                        success = responseCode == HttpURLConnection.HTTP_OK;
                        token = jsonResponse.getString("token");
                        Log.d("LoginDataSource", "Token: " + token);
                    }
                }

                if(model.getRole().equals("groupadmin")) {

                    DeviceRepository deviceRepository = new DeviceRepository(LoginActivity.this);
                    modelGroupId = deviceRepository.getModelGroupId(Build.MODEL);

                    Log.d("LognActivity", "Group ID     : " + model.getGroupId());

                    if(model.getGroupId() == 0){

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Error")
                                        .setContentText("You are not assigned to any group")
                                        .show();
                            }
                        }, 100);
                        return;
                    }


                    if(modelGroupId != -1 && modelGroupId != model.getGroupId()){
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Device Group & Model Mismatch")
                                        .setContentText("You are not allowed to login to this device.")
                                        .show();
                            }
                        }, 100);

                        return;
                    }

                    SharedPreferences groupPrefs = getSharedPreferences("DEVICE_GROUP", Context.MODE_PRIVATE);
                    String groupId = groupPrefs.getString(GroupActivity.KEY_SELECTED_GROUP, null);
                    if(groupId == null){
                        SharedPreferences.Editor editorGroup = groupPrefs.edit();
                        editorGroup.putString(GroupActivity.KEY_SELECTED_GROUP, String.valueOf(model.getGroupId()));
                        editorGroup.apply();
                    }else{
                        if(!groupId.equals(String.valueOf(model.getGroupId()))){
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE)
                                            .setTitleText("Group Mismatch")
                                            .setContentText("You are not allowed to login to this device.")
                                            .show();
                                }
                            }, 100);

                            success = false;
                            return;
                        }
                    }
                }else{
                    SharedPreferences groupPrefs = getSharedPreferences("DEVICE_GROUP", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editorGroup = groupPrefs.edit();
                    editorGroup.putString(GroupActivity.KEY_SELECTED_GROUP, String.valueOf(model.getGroupId()));
                    editorGroup.apply();
                }

                if(success) {
                    EncryptionUtil.generateKey();
                    byte[] encryptedData = EncryptionUtil.encrypt(model.getDisplayName() + "," + model.getEmail() + "," + model.getPassword() + "," + model.getGroupId() + "," + model.getRole()+ ","+ token);
                    SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("user_data", Base64.encodeToString(encryptedData, Base64.DEFAULT));
                    editor.apply();
                }
            } catch (Exception e) {
                Log.e("LoginDataSource", "Error during login request", e);
            }

            boolean finalSuccess = success;
            handler.post(() -> {
                if (finalSuccess) {

                    // Handle the result on the main thread
                    Log.d("LoginActivity", "Login successful");
                    if(modelGroupId != -1 && modelGroupId != model.getGroupId()){
                        startActivity(new Intent(LoginActivity.this, GroupActivity.class));
                        finish();
                        return;
                    }

                    startActivity(new Intent(LoginActivity.this, SplashScreen.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                }

            });
        });


    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}
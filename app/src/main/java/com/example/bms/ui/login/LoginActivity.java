package com.example.bms.ui.login;

import android.app.Activity;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bms.App;
import com.example.bms.BiometricRepository;
import com.example.bms.Configuration;
import com.example.bms.EncryptionUtil;
import com.example.bms.FingerprintRepository;
import com.example.bms.R;
import com.example.bms.SplashScreen;
import com.example.bms.UserRepository;
import com.example.bms.data.Result;
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.ui.login.LoginViewModel;
import com.example.bms.ui.login.LoginViewModelFactory;
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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;


    private void syncUsers(){
        try {
            URL url = new URL(App.BASE_URL + "/sync/users/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

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
            UserRepository userRepository = new UserRepository(this);
            userRepository.resetUsersTable();

            BiometricRepository biometricRepository = new BiometricRepository(LoginActivity.this);
            FingerprintRepository fingerprintRepository = new FingerprintRepository(LoginActivity.this);

            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);

                System.out.println("User: " + user.toString());



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
                        user.getString("password"));

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

                        byte[] decodedBytes = Base64.decode(fingerprint.getString("key"), Base64.DEFAULT);
                        String decodedKey = new String(decodedBytes, StandardCharsets.UTF_8);
                        fingerprintRepository.insertFingerprint(
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(this::syncUsers);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory(LoginActivity.this))
                .get(LoginViewModel.class);

        Log.d("LoginActivity", App.BASE_URL);

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
                    updateUiWithUser(loginResult.getSuccess());
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
    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();

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
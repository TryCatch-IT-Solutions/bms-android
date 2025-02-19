package com.example.bms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;

import com.example.bms.data.LoginDataSource;
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.time_entry.TimeEntryRegister;
import com.example.bms.ui.login.LoginActivity;

import java.io.File;
import java.util.Objects;

public class SplashScreen extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    String savedGroupId;

    long modelGroupId = -1;

    private String getToken() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
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


    private String getSecondaryLogo() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String secondaryLogo = sharedPreferences.getString("SECONDARY_LOGO", null);
        if (secondaryLogo == null) {
            return "drawable/logo"; // Return the default logo resource name
        }
        return secondaryLogo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences= getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        Objects.requireNonNull(getWindow().getInsetsController()).hide(WindowInsetsCompat.Type.systemBars());

        LoginDataSource loginDataSource = new LoginDataSource(SplashScreen.this);
        LoggedInUser data = loginDataSource.getUserData(this);

        Log.d("SplashScreen", "onCreate: Token-" + getToken());
        if (getToken() == null) {
            startActivity(new Intent(SplashScreen.this, LoginActivity.class));
            finish();
            return;
        }

        Log.d("SplashScreen", "onCreate: Data-" + (getToken() instanceof String));

        SharedPreferences configShared = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isRegistered = configShared.getBoolean("isRegistered", false);
        if(!isRegistered){
            startActivity(new Intent(SplashScreen.this, DeviceRegistration.class));
            finish();
            return;
        }


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


        DeviceRepository deviceRepository = new DeviceRepository(this);
        modelGroupId = deviceRepository.getModelGroupId(Build.MODEL);

        savedGroupId = String.valueOf(data.getGroupId());

        SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(GroupActivity.KEY_SELECTED_GROUP, savedGroupId).apply();

        initMain();
    }


    private void initMain(){
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                Log.d("SplashScreen", "run: SavedGroupId-" + savedGroupId + " ModelGroupId-" + modelGroupId);
                if (savedGroupId.equals("0")) {
                    startActivity(new Intent(SplashScreen.this, GroupActivity.class));
                    finish();
                }else if(modelGroupId == -1){
                    startActivity(new Intent(SplashScreen.this, GroupActivity.class));
                    finish();
                }else{
//                    startActivity(new Intent(SplashScreen.this, TimeEntryRegister.class));
                    startActivity(new Intent(SplashScreen.this, MainActivity.class));
//                    startActivity(new Intent(SplashScreen.this, EndpointRegistration.class));
                    finish();
                }
            }
        }, 1000);
    }

}
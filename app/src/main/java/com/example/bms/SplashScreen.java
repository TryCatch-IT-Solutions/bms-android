package com.example.bms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.example.bms.data.Result;
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.ui.login.LoginActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Objects;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class SplashScreen extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    String savedGroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences= getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LoginDataSource loginDataSource = new LoginDataSource(SplashScreen.this);
        LoggedInUser data = loginDataSource.getUserData(this);

        if (data == null) {
            startActivity(new Intent(SplashScreen.this, LoginActivity.class));
            finish();
            return;
        }

        SharedPreferences configShared = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isRegistered = configShared.getBoolean("isRegistered", false);
        if(!isRegistered){
            startActivity(new Intent(SplashScreen.this, DeviceRegistration.class));
            finish();
            return;
        }

        savedGroupId = String.valueOf(data.getGroupId());

        SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(GroupActivity.KEY_SELECTED_GROUP, savedGroupId).apply();

        initMain();
    }

    private void initMain(){
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (savedGroupId.equals("0")) {
                    startActivity(new Intent(SplashScreen.this, GroupActivity.class));
                    finish();
                }else{
                    startActivity(new Intent(SplashScreen.this, MainActivity.class));
                    finish();
                }
            }
        }, 1000);
    }

}
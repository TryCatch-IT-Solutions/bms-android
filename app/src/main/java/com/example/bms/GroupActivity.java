package com.example.bms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.bms.data.LoginDataSource;
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.databinding.ActivityGroupBinding;
import com.example.bms.ui.login.LoggedInUserView;
import com.example.bms.ui.login.LoginResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class GroupActivity extends AppCompatActivity {

    private ActivityGroupBinding binding;
    private RecyclerView recyclerView;
    private GroupAdapter adapter;
    private GroupRepository groupRepository;
    private DeviceRepository deviceRepository;
    private SharedPreferences sharedPreferences;
    public static final String PREFS_NAME = "GroupPrefs";
    public static final String KEY_SELECTED_GROUP = "selected_group";
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();


    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(GroupActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        initMain();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Location Permission")
                    .setContentText("Location permission not granted")
                    .setConfirmText("OK")
                    .setConfirmClickListener(SweetAlertDialog::dismissWithAnimation)
                    .show();
        }
    }

    private void initMain(){

        recyclerView = findViewById(R.id.recycler_view_groups);
        groupRepository = new GroupRepository(this);
        deviceRepository = new DeviceRepository(this);

        LoginDataSource loginDataSource = new LoginDataSource(GroupActivity.this);
        LoggedInUser data = loginDataSource.getUserData(this);

        List<Group> groupList = Collections.emptyList();
        if(data.getRole().equals("groupadmin")) {
            groupList = groupRepository.getGroup(data.getGroupId());
        } else if (data.getRole().equals("superadmin")) {
            groupList = groupRepository.getAllGroups();
            Log.d("Group","Groups: " + groupList);
        }

        Log.d("Group","GroupsHere: " + groupList);


        adapter = new GroupAdapter(groupList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedGroupId = sharedPreferences.getString(KEY_SELECTED_GROUP, null);
        if (savedGroupId != null) {
            for (Group group : groupList) {
                if (group.getId() == Long.parseLong(savedGroupId)) {
                    adapter.setSelectedGroup(group);
                    break;
                }
            }
        }

        findViewById(R.id.button_save).setOnClickListener(v -> {

            Group selectedGroup = adapter.getSelectedGroup();
            if (selectedGroup != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_SELECTED_GROUP, String.valueOf(selectedGroup.getId()));
                editor.apply();

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

                double latitude = Double.longBitsToDouble(sharedPreferences.getLong("latitude", Double.doubleToLongBits(0.0)));
                double longitude = Double.longBitsToDouble(sharedPreferences.getLong("longitude", Double.doubleToLongBits(0.0)));

                deviceRepository.insertOrUpdateDevice(selectedGroup.getId(), model, serialNo, latitude, longitude, "registeredAt");

                UserRepository repository = new UserRepository(GroupActivity.this);
                repository.updateUserGroupByEmail(data.getEmail(), selectedGroup.getId());


                try {
                    //call syncUsers from Configuration
                    ((App)getApplication()).syncUsers(GroupActivity.this, new App.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            try {
                                String token = ((App) getApplication()).getToken(GroupActivity.this);
                                EncryptionUtil.generateKey();
                                byte[] encryptedData = EncryptionUtil.encrypt(data.getDisplayName() + "," + data.getEmail() + "," + data.getPassword() + "," + selectedGroup.getId() + "," + data.getRole()+ ","+ token);
                                SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                                SharedPreferences.Editor userEditor = sharedPreferences.edit();
                                userEditor.putString("user_data", Base64.encodeToString(encryptedData, Base64.DEFAULT));
                                userEditor.apply();
                                Log.d("Group", "The Token: " + token);
//                                loginResult.setValue(new LoginResult(new LoggedInUserView(data.getDisplayName(), data.getEmail(), data.getPassword(), selectedGroup.getId(), data.getRole(), data.getToken())));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }


                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    // Handle success
                                    Intent intent = new Intent(GroupActivity.this, SplashScreen.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }, 2000);

                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            // Handle failure
                            Toast.makeText(GroupActivity.this, "Sync failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }




            } else {
                Toast.makeText(this, "No Group Selected", Toast.LENGTH_SHORT).show();
            }
        });

    }


}



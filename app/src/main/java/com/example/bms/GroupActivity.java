package com.example.bms;

import android.Manifest;
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
import android.widget.Toast;

import com.example.bms.data.LoginDataSource;
import com.example.bms.data.model.LoggedInUser;
import com.example.bms.databinding.ActivityGroupBinding;
import com.example.bms.ui.login.LoginResult;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

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

//        Objects.requireNonNull(getWindow().getInsetsController()).hide(WindowInsetsCompat.Type.systemBars());

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

                long deviceGroupId = deviceRepository.getModelGroupId(model, serialNo);

                if(deviceGroupId == -1) {
                    deviceGroupId = deviceRepository.getModelGroupId(model);
                }

                String deviceModel = deviceRepository.getDeviceGroupModel(selectedGroup.getId());

                if(deviceGroupId != -1 && deviceModel != null && !deviceModel.equals(model)) {
                    new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Device Model Mismatch")
                            .setContentText("Device model does not match the pre-selected model for this device")
                            .setConfirmText("OK")
                            .setConfirmClickListener(SweetAlertDialog::dismissWithAnimation)
                            .show();
                    return;
                }

                UserRepository repository = new UserRepository(GroupActivity.this);
                repository.updateUserGroupByEmail(data.getEmail(), selectedGroup.getId());

                SharedPreferences groupPrefs = getSharedPreferences("DEVICE_GROUP", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = groupPrefs.edit();
                editor.putString(KEY_SELECTED_GROUP, String.valueOf(selectedGroup.getId()));
                editor.apply();

                deviceRepository.updateDeviceGroupIdBySerial(serialNo, selectedGroup.getId());

                try {
                    //call syncUsers from Configuration
                    ((App)getApplication()).syncUsersOnLogout(GroupActivity.this, new App.SyncCallback() {
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



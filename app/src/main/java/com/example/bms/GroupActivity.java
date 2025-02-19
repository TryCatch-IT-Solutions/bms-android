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

    SweetAlertDialog sweetAlertDialog;

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

    private String getUserGroupId() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String encryptedData = sharedPreferences.getString("user_data", null);
        if (encryptedData != null) {
          try {
              byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
              String decryptedData = EncryptionUtil.decrypt(decodedData);
              String[] userData = decryptedData.split(",");
              return userData[3];
          }catch (Exception e){
              e.printStackTrace();
              return "0";
          }
        }
        return null;
    }

    private void initMain() {

        recyclerView = findViewById(R.id.recycler_view_groups);
        groupRepository = new GroupRepository(this);
        deviceRepository = new DeviceRepository(this);

        LoginDataSource loginDataSource = new LoginDataSource(GroupActivity.this);
        LoggedInUser data = loginDataSource.getUserData(this);

        List<Group> groupList = Collections.emptyList();
        if (data.getRole().equals("groupadmin")) {
            groupList = groupRepository.getGroup(data.getGroupId());
        } else if (data.getRole().equals("superadmin")) {
            groupList = groupRepository.getAllGroups();
            Log.d("Group", "Groups: " + groupList);
        }



        adapter = new GroupAdapter(groupList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedGroupId = sharedPreferences.getString(KEY_SELECTED_GROUP, null);

        Log.d("Group", "Saved Group Id: " + savedGroupId);

        if (savedGroupId != null) {
            for (Group group : groupList) {
                if (group.getId() == Long.parseLong(savedGroupId)) {
                    adapter.setSelectedGroup(group);
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
        } else {
            //get model group
            long modelGroupId = deviceRepository.getModelGroupId(Build.MODEL);

            Log.d("Group", "Model Group Id: " + modelGroupId);
            for (Group group : groupList) {
                if (group.getId() == Long.parseLong(getUserGroupId())) {
                    adapter.setSelectedGroup(group);
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
//            if (modelGroupId != -1) {
//                for (Group group : groupList) {
//                    if (group.getId() == modelGroupId) {
//                        adapter.setSelectedGroup(group);
//                        adapter.notifyDataSetChanged();
//                        break;
//                    }
//                }
//            }
        }


        findViewById(R.id.button_save).setOnClickListener(v -> {


            runOnUiThread(() -> {
                sweetAlertDialog = new SweetAlertDialog(GroupActivity.this, SweetAlertDialog.PROGRESS_TYPE);
                sweetAlertDialog.setTitleText("Saving Group");
                sweetAlertDialog.setCancelable(false);
                sweetAlertDialog.show();
            });


            Group selectedGroup = adapter.getSelectedGroup();
            if (selectedGroup != null) {

                DeviceRepository deviceRepository = new DeviceRepository(GroupActivity.this);
                List<Long> deviceGroupIds = deviceRepository.getModelGroupIds(Build.MODEL);
                String groupModel = deviceRepository.getGroupModel(selectedGroup.getId());

                Log.d("Group", "Device Group: " + deviceGroupIds.toString() + " Group Model: " + groupModel);

                String model = Build.MODEL;
                String serialNo;

                if (groupModel != null && !deviceGroupIds.isEmpty() && !deviceGroupIds.contains(selectedGroup.getId())) {
                    runOnUiThread(() -> {
                        sweetAlertDialog.dismiss();
                        sweetAlertDialog = new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Device Group Mismatch")
                                .setContentText("Device group does not match the pre-selected group for this model")
                                .setConfirmText("OK")
                                .setConfirmClickListener(SweetAlertDialog::dismissWithAnimation);
                        sweetAlertDialog.show();
                    });
                    return;
                }

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

                UserRepository repository = new UserRepository(GroupActivity.this);
                repository.updateUserGroupByEmail(data.getEmail(), selectedGroup.getId());

                SharedPreferences groupPrefs = getSharedPreferences("DEVICE_GROUP", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = groupPrefs.edit();
                editor.putString(KEY_SELECTED_GROUP, String.valueOf(selectedGroup.getId()));
                editor.apply();

                try {
                    //call syncUsers from Configuration
                    ((App) getApplication()).syncUsersOnLogout(GroupActivity.this, new App.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            try {
                                String token = ((App) getApplication()).getToken(GroupActivity.this);
                                EncryptionUtil.generateKey();
                                byte[] encryptedData = EncryptionUtil.encrypt(data.getDisplayName() + "," + data.getEmail() + "," + data.getPassword() + "," + selectedGroup.getId() + "," + data.getRole() + "," + token);
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
                                    sweetAlertDialog.dismiss();

                                    // Handle success
                                    Intent intent = new Intent(GroupActivity.this, SplashScreen.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }, 2000);

                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            sweetAlertDialog.dismiss();

                            // Handle failure
                            Toast.makeText(GroupActivity.this, "Sync failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            } else {

                runOnUiThread(() -> {
                    sweetAlertDialog.dismiss();
                    sweetAlertDialog = new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("No Group Selected")
                            .setContentText("Please select a group")
                            .setConfirmText("OK")
                            .setConfirmClickListener(SweetAlertDialog::dismissWithAnimation);
                    sweetAlertDialog.show();
                });
            }
        });

    }


}



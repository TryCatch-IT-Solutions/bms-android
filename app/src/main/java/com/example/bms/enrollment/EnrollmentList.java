// src/main/java/com/example/bms/enrollment/EnrollmentList.java
package com.example.bms.enrollment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bms.DatabaseHelper;
import com.example.bms.EncryptionUtil;
import com.example.bms.Group;
import com.example.bms.GroupActivity;
import com.example.bms.MainActivity;
import com.example.bms.R;
import com.example.bms.data.model.User;
import com.example.bms.databinding.ActivityEnrollmentListBinding;

import java.util.List;

public class EnrollmentList extends AppCompatActivity {

    private ActivityEnrollmentListBinding binding;
    private RecyclerView recyclerView;
    private EmployeeAdapter employeeAdapter;
    private DatabaseHelper dbHelper;

    public String getGroupId(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
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

    public String getRole(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEnrollmentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        binding.toolbarLayout.setTitle(getTitle());


        dbHelper = new DatabaseHelper(this);
        List<User> employeeList;
        if(getRole(this).equals("superadmin")) {
            employeeList = dbHelper.getAllUsers();
        } else {
            String groupId = getGroupId(this);
            employeeList = dbHelper.getUsersByGroupId(groupId);
        }

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        employeeAdapter = new EmployeeAdapter(this, employeeList);
        recyclerView.setAdapter(employeeAdapter);

        Toolbar toolbarHead = findViewById(R.id.toolbar_header);
        toolbarHead.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
// src/main/java/com/example/bms/enrollment/EnrollmentList.java
package com.example.bms.enrollment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bms.DatabaseHelper;
import com.example.bms.EncryptionUtil;
import com.example.bms.R;
import com.example.bms.data.model.User;
import com.example.bms.databinding.ActivityEnrollmentListBinding;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentList extends AppCompatActivity {

    private ActivityEnrollmentListBinding binding;
    private RecyclerView recyclerView;
    private EmployeeAdapter employeeAdapter;
    private DatabaseHelper dbHelper;
    private EditText searchInput;
    private int currentPage = 0;
    private final int PAGE_SIZE = 10;

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

    private void loadUsers() {
        List<User> employeeList;
        if (getRole(this).equals("superadmin")) {
            employeeList = dbHelper.getPaginatedUsers(PAGE_SIZE, currentPage * PAGE_SIZE);
        } else {
            String groupId = getGroupId(this);
            employeeList = dbHelper.getPaginatedUsersByGroupId(groupId, PAGE_SIZE, currentPage * PAGE_SIZE);
        }
        employeeAdapter.updateData(employeeList);
        employeeAdapter.notifyDataSetChanged();

        // Check if there are more users for the next page
        List<User> nextPageList;
        if (getRole(this).equals("superadmin")) {
            nextPageList = dbHelper.getPaginatedUsers(PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);
        } else {
            String groupId = getGroupId(this);
            nextPageList = dbHelper.getPaginatedUsersByGroupId(groupId, PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);
        }

        // Enable/disable buttons based on the current page and next page data
        findViewById(R.id.button_prev).setEnabled(currentPage > 0);
        findViewById(R.id.button_next).setEnabled(!nextPageList.isEmpty());
    }


    private void filterUsers(String query) {

        if(getRole(this).equals("superadmin")) {
            List<User> filteredList = dbHelper.searchUsers(query,  PAGE_SIZE, currentPage * PAGE_SIZE,null);
            employeeAdapter.updateData(filteredList);
        }else{
            List<User> filteredList = dbHelper.searchUsersByGroupId(query,getGroupId(this),  PAGE_SIZE, currentPage * PAGE_SIZE);
            employeeAdapter.updateData(filteredList);
        }

        employeeAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEnrollmentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        binding.toolbarLayout.setTitle(getTitle());

        getWindow().getInsetsController().hide(WindowInsetsCompat.Type.systemBars());

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        employeeAdapter = new EmployeeAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(employeeAdapter);

        dbHelper = new DatabaseHelper(this);
        loadUsers();

        searchInput = findViewById(R.id.first_name);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPage = 0;
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        Toolbar toolbarHead = findViewById(R.id.toolbar_header);
        toolbarHead.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.button_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage > 0) {
                    currentPage--;
                    loadUsers();
                }
            }
        });

        findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPage++;
                loadUsers();
            }
        });
    }
}
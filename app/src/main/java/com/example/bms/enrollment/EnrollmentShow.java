package com.example.bms.enrollment;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bms.DatabaseHelper;
import com.example.bms.R;
import com.example.bms.data.model.User;

public class EnrollmentShow extends AppCompatActivity {

    private User employee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_enrollment_show);
        String employeeId = getIntent().getStringExtra("employeeId");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        if (employeeId != null) {
            try (DatabaseHelper dbHelper = new DatabaseHelper(this)) {
               employee = dbHelper.getUserById(employeeId);
            }

            if (employee != null) {
                TextView nameTextView = findViewById(R.id.name);
                TextView nameHeaderTextView = findViewById(R.id.name_header);
                TextView emailTextView = findViewById(R.id.email);
                TextView emailHeaderTextView = findViewById(R.id.userEmail);
                TextView phoneTextView = findViewById(R.id.phone);
                TextView addressTextView = findViewById(R.id.address);
                TextView birthDateTextView = findViewById(R.id.birth_date);

                nameTextView.setText(employee.getDisplayName());
                nameHeaderTextView.setText(employee.getDisplayName());
                emailHeaderTextView.setText(employee.getEmail());
                emailTextView.setText(employee.getEmail());
                phoneTextView.setText(employee.getPhone());
                addressTextView.setText(employee.getAddress1() + ", " + employee.getAddress2() + ", " + employee.getBarangay() + ", " + employee.getMunicipality() + ", " + employee.getProvince());
                birthDateTextView.setText(employee.getBirthDate());
            } else {
                Log.e("EnrollmentShow", "Employee not found with ID: " + employeeId);
            }
        }
    }
}
package com.example.bms.enrollment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bms.App;
import com.example.bms.DatabaseHelper;
import com.example.bms.EncryptionUtil;
import com.example.bms.R;
import com.example.bms.UserRepository;
import com.example.bms.data.model.User;

import java.util.List;
import java.util.Objects;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private final List<User> employeeList;
    private final Context context;

    public EmployeeAdapter(Context context, List<User> employeeList) {
        this.context = context;
        this.employeeList = employeeList;
    }

    private String getRole() {
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

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_employee, parent, false);

        return new EmployeeViewHolder(view);
    }

    public void updateData(List<User> newEmployeeList) {
        employeeList.clear();
        employeeList.addAll(newEmployeeList);
        notifyDataSetChanged();
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

    public String getCurrentEmail() {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String encryptedData = sharedPreferences.getString("user_data", null);
            if (encryptedData != null) {
                byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
                String decryptedData = EncryptionUtil.decrypt(decodedData);
                String[] userData = decryptedData.split(",");
                return userData[1];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getGroupId(Context context) {
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

    private void reloadEmployees(){
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        List<User> employeeList;
        if (getRole(context).equals("superadmin")) {
            employeeList = dbHelper.getPaginatedUsers(10, 0);
        } else {
            String groupId = getGroupId(context);
            employeeList = dbHelper.getPaginatedUsersByGroupId(groupId, 10, 0);
        }

        updateData(employeeList);
    }



    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        User employee = employeeList.get(position);
        holder.employeeName.setText(employee.getDisplayName());
        holder.employeeEmail.setText(employee.getEmail());
        holder.employeePhone.setText(employee.getRole());

        UserRepository userRepository = new UserRepository(context);

        if(Objects.equals(getRole(), "groupadmin")) {
            holder.buttonDelete.setVisibility(View.GONE);
            holder.buttonEdit.setVisibility(View.GONE);
            holder.unassignButton.setVisibility(View.VISIBLE);
        }

        if(Objects.equals(getCurrentEmail(), employee.getEmail())) {
            holder.buttonEdit.setVisibility(View.VISIBLE);
        }

        if(Objects.equals(employee.getRole(), "groupadmin") || Objects.equals(employee.getRole(), "superadmin")) {
            holder.unassignButton.setEnabled(false);
            holder.unassignButton.setBackgroundColor(context.getColor(R.color.gray_300));
        }else{
            holder.unassignButton.setEnabled(true);
            holder.unassignButton.setBackgroundColor(context.getColor(R.color.primary));
        }

        if(Objects.equals(employee.getRole(), "superadmin")) {
            holder.buttonDelete.setEnabled(false);
            holder.buttonDelete.setBackgroundColor(context.getColor(R.color.gray_300));
        }else{
            holder.buttonDelete.setEnabled(true);
            holder.buttonDelete.setBackgroundColor(context.getColor(R.color.primary));
        }


        holder.card_employee_holder.setOnClickListener(v -> {
            // Handle view action
            Intent intent = new Intent(context, EnrollmentShow.class);
            intent.putExtra("employeeId", employee.getUserId());
            context.startActivity(intent);
        });

        holder.buttonEdit.setOnClickListener(v -> {
            // Handle edit action
            Intent intent = new Intent(context, EnrollmentEdit.class);
            intent.putExtra("employeeId", employee.getUserId());
            context.startActivity(intent);
        });

        holder.unassignButton.setOnClickListener(v -> {
            // Handle unassign action
            new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Are you sure?")
                    .setContentText("You want to unassign this employee?")
                    .setConfirmText("Confirm")
                    .setConfirmClickListener(sDialog -> {
                        sDialog.dismissWithAnimation();
                        // Unassign employee
                        userRepository.unassignUser(Long.parseLong(employee.getUserId()),context);
                        employeeList.remove(position);
                        notifyDataSetChanged();

                        new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Unassigned!")
                                .setContentText("Employee has been unassigned!")
                                .show();

                        reloadEmployees();

                        ((App) context.getApplicationContext()).syncUsersOnLogout(context, new App.SyncCallback() {
                            @Override
                            public void onSuccess() {
                                // Success logic
                                Log.d("EmployeeAdapter", "Employee has been unassigned!");
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                // Failure logic
                                Toast.makeText(context, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setCancelButton("Cancel", SweetAlertDialog::dismissWithAnimation)
                    .show();
        });

        holder.buttonDelete.setOnClickListener(v -> {
            // Handle delete action
            new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Are you sure?")
                    .setContentText("You want to archive this employee?")
                    .setConfirmText("Confirm")
                    .setConfirmClickListener(sDialog -> {
                        sDialog.dismissWithAnimation();
                        // Delete employee
                        userRepository.deleteUser(Long.parseLong(employee.getUserId()),context);
                        employeeList.remove(position);
                        notifyDataSetChanged();

                        reloadEmployees();

                        new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Archived!")
                                .setContentText("Employee has been archived!")
                                .show();

                        ((App) context.getApplicationContext()).syncUsersOnLogout(context, new App.SyncCallback() {
                            @Override
                            public void onSuccess() {
                                // Success logic
                                Log.d("EmployeeAdapter", "Employee has been archived!");
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                // Failure logic
                                Toast.makeText(context, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setCancelButton("Cancel", SweetAlertDialog::dismissWithAnimation)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView employeeName;
        TextView employeeEmail;
        TextView employeePhone;
        Button buttonEdit;
        Button buttonDelete;
        Button unassignButton;
        LinearLayout card_employee_holder;

        EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            employeeName = itemView.findViewById(R.id.employee_name);
            employeeEmail = itemView.findViewById(R.id.employee_email);
            employeePhone = itemView.findViewById(R.id.employee_phone);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            unassignButton = itemView.findViewById(R.id.button_unassign);
            card_employee_holder = itemView.findViewById(R.id.card_employee_holder);
        }
    }
}
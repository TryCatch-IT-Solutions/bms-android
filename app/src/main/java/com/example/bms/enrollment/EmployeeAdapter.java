package com.example.bms.enrollment;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bms.R;
import com.example.bms.UserRepository;
import com.example.bms.data.model.User;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private final List<User> employeeList;
    private final Context context;

    public EmployeeAdapter(Context context, List<User> employeeList) {
        this.context = context;
        this.employeeList = employeeList;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        User employee = employeeList.get(position);
        holder.employeeName.setText(employee.getDisplayName());
        holder.employeeEmail.setText(employee.getEmail());
        holder.employeePhone.setText(employee.getPhone());

        UserRepository userRepository = new UserRepository(context);

        holder.buttonView.setOnClickListener(v -> {
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

        holder.buttonDelete.setOnClickListener(v -> {
            // Handle delete action
            new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Are you sure?")
                    .setContentText("Won't be able to recover this employee!")
                    .setConfirmText("Confirm")
                    .setConfirmClickListener(sDialog -> {
                        sDialog.dismissWithAnimation();
                        // Delete employee
                        userRepository.deleteUser(Long.parseLong(employee.getUserId()),context);
                        employeeList.remove(position);
                        notifyDataSetChanged();

                        new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Deleted!")
                                .setContentText("Employee has been deleted!")
                                .show();
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
        Button buttonView;
        Button buttonEdit;
        Button buttonDelete;

        EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            employeeName = itemView.findViewById(R.id.employee_name);
            employeeEmail = itemView.findViewById(R.id.employee_email);
            employeePhone = itemView.findViewById(R.id.employee_phone);
            buttonView = itemView.findViewById(R.id.button_view);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
}
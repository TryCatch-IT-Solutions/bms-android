package com.example.bms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ResetPasswordActivity extends AppCompatActivity {


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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        EditText editTextCurrentPassword = findViewById(R.id.edit_text_current_password);
        EditText editTextNewPassword = findViewById(R.id.edit_text_new_password);
        CheckBox checkBoxShowPassword = findViewById(R.id.checkbox_show_password);
        Button buttonSubmit = findViewById(R.id.button_submit);

        DatabaseHelper dbHelper = new DatabaseHelper(this);


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

        checkBoxShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editTextCurrentPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                editTextNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                editTextCurrentPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                editTextNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        buttonSubmit.setOnClickListener(v -> {
            // Handle password reset logic here

            //check if password is empty
            if(editTextCurrentPassword.getText().toString().isEmpty() || editTextNewPassword.getText().toString().isEmpty()) {
                new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Password Reset")
                        .setContentText("Password reset failed. Password cannot be empty.")
                        .show();
                Log.d("ResetPasswordActivity", "Password reset failed");
                return;
            }

            //check if password match
            if(!dbHelper.passwordAdminMatch(editTextCurrentPassword.getText().toString())) {
                new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Password Reset")
                        .setContentText("Password reset failed. Password does not match the current password.")
                        .show();
                Log.d("ResetPasswordActivity", "Password reset failed");
                return;
            }

            dbHelper.resetAdminPassword(editTextNewPassword.getText().toString());




            new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Password Reset")
                    .setContentText("Password reset successful")
                    .setConfirmClickListener(sweetAlertDialog -> {
                        sweetAlertDialog.dismissWithAnimation();
                        finish();
                    })
                    .show();
            Log.d("ResetPasswordActivity", "Password reset successful");
        });
    }
}
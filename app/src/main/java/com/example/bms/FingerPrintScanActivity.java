package com.example.bms;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.example.bms.data.model.User;
import com.example.bms.time_entry.TimeEntryRegister;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.bms.databinding.ActivityFingerPrintScanBinding;
import com.hfteco.finger.FingerSDK;
import com.hfteco.finger.OnCaptureBytesListener;
import com.hfteco.finger.OnCaptureListener;
import com.hfteco.finger.OnSdkInitListener;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import doorx.utils.OnDeviceCheckClickListener;
import doorx.utils.ProgressDialogUtils;
import facex.utils.TextToSpeechUtil;

public class FingerPrintScanActivity extends AppCompatActivity {

    private ActivityFingerPrintScanBinding binding;
    private FingerSDK fingerSDK;

    ImageView[] fingerImageViews = new ImageView[10];
    Button scan_finger, save_finger;

    private boolean deviceModelNameCheck = false;
    private int currentFingerIndex = 0;

    private HashMap<Integer, String> fingerDataMap = new HashMap<>();

    private Toolbar toolbarHead;

    private List<Fingerprint> fingerprints;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFingerPrintScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        deviceModelNameCheck = FingerSDK.licenceDevice();

        FingerprintRepository fingerprintRepository = new FingerprintRepository(this);
        fingerprints = fingerprintRepository.getAllFingerprints();

//        Toolbar toolbar = binding.toolbar;
//        setSupportActionBar(toolbar);
//        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
//        toolBarLayout.setTitle(getTitle());

        toolbarHead = (Toolbar) findViewById(R.id.toolbar);
        toolbarHead.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        fingerImageViews[0] = findViewById(R.id.fingerIv_1);
        fingerImageViews[1] = findViewById(R.id.fingerIv_2);
        fingerImageViews[2] = findViewById(R.id.fingerIv_3);
        fingerImageViews[3] = findViewById(R.id.fingerIv_4);
        fingerImageViews[4] = findViewById(R.id.fingerIv_5);
        fingerImageViews[5] = findViewById(R.id.fingerIv_6);
        fingerImageViews[6] = findViewById(R.id.fingerIv_7);
        fingerImageViews[7] = findViewById(R.id.fingerIv_8);
        fingerImageViews[8] = findViewById(R.id.fingerIv_9);
        fingerImageViews[9] = findViewById(R.id.fingerIv_10);

        scan_finger = findViewById(R.id.scan_finger);
        save_finger = findViewById(R.id.save_finger);

        if (!checkPermission()) {
            Toast.makeText(this, "Please grant permission in settings to use fingerprint", Toast.LENGTH_SHORT).show();
            return;
        }
        // Remove src from all ImageViews
        for (ImageView fingerImageView : fingerImageViews) {
            fingerImageView.setImageDrawable(null);
        }

        fingerSDK = new FingerSDK(FingerPrintScanActivity.this, new OnSdkInitListener() {
            @Override
            public void initResult(int i, String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("FingerPrintScanActivity", "initResult: " + i + " " + s);
                        if (i != FingerSDK.RESULT_OK) {
                            AlertDialog retryDialog =
                                    new AlertDialog.Builder(FingerPrintScanActivity.this)
                                            .setCancelable(false)
                                            .setTitle("INIT FAILED").setMessage("Failed to initialize fingerprint scanner")
                                            .setPositiveButton("Try Again",
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                            fingerSDK.launch();
                                                        }
                                                    }).create();
                            retryDialog.show();
                        }
                    }
                });
            }

            @Override
            public void onOpticalSensorInterrupt() {

            }

            @Override
            public void onOpticalSensorLost() {

            }
        });

        save_finger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (areAllFingersScanned()) {
                    Intent intent = new Intent();
                    intent.putExtra("fingerDataMap", fingerDataMap);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(FingerPrintScanActivity.this, "Please scan all fingers", Toast.LENGTH_SHORT).show();
                }
            }
        });

        scan_finger.setOnClickListener(new OnDeviceCheckClickListener(deviceModelNameCheck) {
            @Override
            public void onPassClick(View v) {
                hostCapture();
            }

            @Override
            public void onRejectClick() {
                Log.d("FingerPrintScanActivity", "onRejectClick: Device not supported");
            }
        });

        for (int i = 0; i < fingerImageViews.length; i++) {
            final int index = i;
            fingerImageViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setActiveFinger(index);
                }
            });
        }

        findViewById(R.id.reset_button_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(0);
            }
        });

        findViewById(R.id.reset_button_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(1);
            }
        });

        findViewById(R.id.reset_button_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(2);
            }
        });

        findViewById(R.id.reset_button_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(3);
            }
        });

        findViewById(R.id.reset_button_5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(4);
            }
        });
    }

    public boolean checkPermission() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return false;
        }
        boolean manage = Environment.isExternalStorageManager();
        if (!manage) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(fingerSDK != null) {
            fingerSDK.launch();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(fingerSDK != null) {
            fingerSDK.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(fingerSDK != null) {
            fingerSDK.release();
        }
    }

    private void showDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressDialogUtils.showProgressDialog(FingerPrintScanActivity.this, "please press finger...");
            }
        });
    }

    private void closeDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressDialogUtils.dismissProgressDialog();
            }
        });
    }

    private void updateFingerBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("FingerPrintScanActivity", "updateFingerBitmap: " + currentFingerIndex);
                fingerImageViews[currentFingerIndex].setImageBitmap(bitmap);

                // check if all fingers have been scanned
                if (areAllFingersScanned()) {
                    save_finger.setVisibility(View.VISIBLE);
                }else{
                    save_finger.setVisibility(View.GONE);
                }

                for (int idx = 0; idx < fingerImageViews.length; idx++) {
                    ImageView fingerImageView = fingerImageViews[idx];
                    if (fingerImageView.getDrawable() == null) {
                        setActiveFinger(idx);
                        return;
                    }
                }
            }
        });
    }

    private boolean areAllFingersScanned() {
        for (ImageView fingerImageView : fingerImageViews) {
            if (fingerImageView.getDrawable() == null) {
                return false;
            }
        }
        return true;
    }

    private void resetFingerImage(int index) {
        if (index >= 0 && index < fingerImageViews.length) {
            fingerImageViews[index].setImageBitmap(null);
            fingerImageViews[index].setImageDrawable(null);
            if (index < currentFingerIndex) {
                currentFingerIndex = index;
            }
            Toast.makeText(this, "Finger " + (index + 1) + " reset", Toast.LENGTH_SHORT).show();
        }
        save_finger.setVisibility(View.GONE);
    }

    private void setActiveFinger(int index) {
        currentFingerIndex = index;
        for (int i = 0; i < fingerImageViews.length; i++) {
            if (i == index) {
                fingerImageViews[i].setBackgroundResource(R.drawable.active_border);
            } else {
                fingerImageViews[i].setBackgroundResource(0);
            }
        }
    }

    private boolean validateFingerPrint(String tempString){
        for(Fingerprint fingerprint: fingerprints) {
            int score = fingerSDK.compareTemplateBytes(FingerSDK.TEMPLEATES.valueOf("ISO_19794_2_2011"),tempString.getBytes(StandardCharsets.ISO_8859_1),fingerprint.getKey().getBytes(StandardCharsets.ISO_8859_1));
            Log.d("TimeEntryRegister", "Score: " + score);
            if (score > 85) {
                return false;
            }
        }

        return true;
    }

    private void hostCapture() {
        if (areAllFingersScanned()) {
            Toast.makeText(this, "All fingers have been scanned", Toast.LENGTH_SHORT).show();
            return;
        }

        setActiveFinger(currentFingerIndex);

        showDialog();
        fingerSDK.captureBytes(FingerSDK.TEMPLEATES.valueOf("ISO_19794_2_2011"), new OnCaptureBytesListener() {
            @Override
            public void capture(int i, byte[] bytes, Bitmap bitmap, byte[] temp) {
                closeDialog();
                if (i == FingerSDK.RESULT_OK) {
                    String tempString = new String(temp, StandardCharsets.ISO_8859_1);
                    Log.d("FingerPrintScanActivity", "tempString: " + tempString);

                    if(!validateFingerPrint(tempString)){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new SweetAlertDialog(FingerPrintScanActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Error")
                                        .setContentText("Fingerprint already exists")
                                        .show();
                            }
                        });

                        return;
                    }

                    updateFingerBitmap(bitmap);
                    Log.d("FingerPrintScanActivity", "capture success: " + bytes.length);

                    Log.d("FingerPrintScanActivity", "currentFingerIndex: " + currentFingerIndex);
                    fingerDataMap.put(currentFingerIndex, tempString);

                } else {
                    Log.d("FingerPrintScanActivity", "capture failed: " + i);
                    if (bitmap != null) {
                        updateFingerBitmap(bitmap);
                    }
                }
            }
        });
    }
}

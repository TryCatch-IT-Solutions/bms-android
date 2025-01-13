package com.example.bms;

import static facex.facepass.InitFacePassHandler.group_name;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bms.time_entry.TimeEntryRegister;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import cn.pedant.SweetAlert.SweetAlertDialog;
import facex.facepass.InitFacePassHandler;
import facex.facepass.camera.CameraActivity;
import facex.facepass.db.User;
import mcv.facepass.FacePassException;
import mcv.facepass.FacePassHandler;
import mcv.facepass.types.FacePassAddFaceResult;

public class FaceScanner extends AppCompatActivity {


    private HashMap<Integer, String> faceDataMap = new HashMap<>();
    private int currentIndex = 0;

    ImageView[] faceImageViews = new ImageView[3];

    private Button take_photo, save_photo;
    private ProgressDialog mProgressDialog;


    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == 200) {
                    Intent intent = result.getData();
                    String path = intent.getStringExtra("data");

                    mProgressDialog.setMessage(getString(R.string.loading));
                    mProgressDialog.show();
                    InitFacePassHandler.init(FaceScanner.this, new InitFacePassHandler.IFacePassInit() {
                        @Override
                        public void result(FacePassHandler facePassHandler) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.dismiss();
                                }
                            });
                            if (facePassHandler != null) {
                                Bitmap bitmap = BitmapFactory.decodeFile(path);

                                try {
                                    FacePassAddFaceResult result = facePassHandler.addFace(bitmap);
                                    if (result != null) {
                                        if (result.result == 0) {

                                            if(!facePassHandler.bindGroup(group_name,result.faceToken)){
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(FaceScanner.this,getString(R.string.failed),Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                return;
                                            }
                                            User user = new User();
                                            user.id = System.currentTimeMillis();
                                            user.faceToken = new String(result.faceToken, StandardCharsets.ISO_8859_1);
                                            Log.e("faceToken",user.faceToken);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    BiometricRepository biometricRepository = new BiometricRepository(FaceScanner.this);
                                                    Biometric biometric = biometricRepository.findFaceBiometricByKey(user.faceToken);
                                                    if (biometric != null) {
                                                        new SweetAlertDialog(FaceScanner.this, SweetAlertDialog.ERROR_TYPE)
                                                                .setTitleText("Error")
                                                                .setContentText("Face already exists")
                                                                .show();
                                                        return;
                                                    }

                                                    Toast.makeText(FaceScanner.this,getString(R.string.success),Toast.LENGTH_SHORT).show();
                                                    faceDataMap.put(currentIndex, user.faceToken);
                                                    updateBitmap(bitmap);

                                                }
                                            });
                                        } else if (result.result == 1) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    new SweetAlertDialog(FaceScanner.this, SweetAlertDialog.ERROR_TYPE)
                                                            .setTitleText("Error")
                                                            .setContentText("No face detected")
                                                            .show();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    new SweetAlertDialog(FaceScanner.this, SweetAlertDialog.ERROR_TYPE)
                                                            .setTitleText("Error")
                                                            .setContentText("Quality problem")
                                                            .show();
                                                }
                                            });
                                        }
                                    }else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                new SweetAlertDialog(FaceScanner.this, SweetAlertDialog.ERROR_TYPE)
                                                        .setTitleText("Error")
                                                        .setContentText("face chek failed")
                                                        .show();
                                            }
                                        });
                                    }
                                } catch (FacePassException e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(FaceScanner.this,getString(R.string.failed),Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(FaceScanner.this,"Error Face Handler: 1",Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            }
                        }
                    });
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_face_scanner);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.reset_button_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetImage(0);
            }
        });

        findViewById(R.id.reset_button_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetImage(1);
            }
        });

        findViewById(R.id.reset_button_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetImage(2);
            }
        });



        mProgressDialog = new ProgressDialog(this);


        take_photo = findViewById(R.id.take_photo);
        save_photo = findViewById(R.id.save_photo);

        take_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultLauncher.launch(new Intent(FaceScanner.this, CameraActivity.class));
            }
        });

        save_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (areAllScanned()) {
                    Intent intent = new Intent();
                    intent.putExtra("dataMap", faceDataMap);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(FaceScanner.this, "Please scan all fingers", Toast.LENGTH_SHORT).show();
                }
            }
        });


        faceImageViews[0] = findViewById(R.id.face_1);
        faceImageViews[1] = findViewById(R.id.face_2);
        faceImageViews[2] = findViewById(R.id.face_3);

        // Remove src from all ImageViews
        for (ImageView iv : faceImageViews) {
            iv.setImageDrawable(null);
        }

        for (int i = 0; i < faceImageViews.length; i++) {
            final int index = i;
            faceImageViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setActive(index);
                }
            });
        }
    }

    private void setActive(int index) {
        currentIndex = index;
        for (int i = 0; i < faceImageViews.length; i++) {
            if (i == index) {
                faceImageViews[i].setBackgroundResource(R.drawable.active_border);
            } else {
                faceImageViews[i].setBackgroundResource(0);
            }
        }
    }

    private void updateBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("FingerPrintScanActivity", "updateFingerBitmap: " + currentIndex);
                faceImageViews[currentIndex].setImageBitmap(bitmap);

                // check if all fingers have been scanned
                if (areAllScanned()) {
                    save_photo.setVisibility(View.VISIBLE);
                }else{
                    save_photo.setVisibility(View.GONE);
                }

                for (int idx = 0; idx < faceImageViews.length; idx++) {
                    ImageView fingerImageView = faceImageViews[idx];
                    if (fingerImageView.getDrawable() == null) {
                        setActive(idx);
                        return;
                    }
                }
            }
        });
    }

    private boolean areAllScanned() {
        for (ImageView fingerImageView : faceImageViews) {
            if (fingerImageView.getDrawable() == null) {
                return false;
            }
        }
        return true;
    }

    private void resetImage(int index) {
        if (index >= 0 && index < faceImageViews.length) {
            faceImageViews[index].setImageBitmap(null);
            faceImageViews[index].setImageDrawable(null);
            if (index < currentIndex) {
                currentIndex = index;
            }
            Toast.makeText(this, "Finger " + (index + 1) + " reset", Toast.LENGTH_SHORT).show();
        }
        save_photo.setVisibility(View.GONE);
    }


}
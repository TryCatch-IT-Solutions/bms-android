package com.example.bms.time_entry;

import static facex.facepass.InitFacePassHandler.group_name;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hibory.Conversion;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bms.AnnouncementAdapter;
import com.example.bms.AnnouncementModel;
import com.example.bms.AnnouncementRepository;
import com.example.bms.App;
import com.example.bms.Biometric;
import com.example.bms.BiometricRepository;
import com.example.bms.Configuration;
import com.example.bms.DatabaseHelper;
import com.example.bms.EncryptionUtil;
import com.example.bms.Fingerprint;
import com.example.bms.FingerprintRepository;
import com.example.bms.GroupActivity;
import com.example.bms.MainActivity;
import com.example.bms.MyAdminReceiver;
import com.example.bms.R;
import com.example.bms.data.model.User;
import com.hfteco.finger.FingerSDK;
import com.hfteco.finger.OnCaptureBytesListener;
import com.hfteco.finger.OnSdkInitListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

import cn.pedant.SweetAlert.SweetAlertDialog;
import doorx.utils.ProgressDialogUtils;
import facex.facepass.FaceView;
import facex.facepass.InitFacePassHandler;
import facex.facepass.RecognizeData;
import facex.facepass.camera.CameraManager;
import facex.facepass.camera.CameraPreview;
import facex.facepass.camera.CameraPreviewData;
import facex.facepass.camera.CameraSettingActivity;
import facex.utils.TextToSpeechUtil;
import mcv.facepass.FacePassException;
import mcv.facepass.FacePassHandler;
import mcv.facepass.types.FacePassAgeGenderResult;
import mcv.facepass.types.FacePassDetectionResult;
import mcv.facepass.types.FacePassFace;
import mcv.facepass.types.FacePassImage;
import mcv.facepass.types.FacePassImageType;
import mcv.facepass.types.FacePassRCAttribute;
import mcv.facepass.types.FacePassRecognitionResult;
import mcv.facepass.types.FacePassRecognitionState;
import mcv.facepass.types.FacePassTrackOptions;

public class TimeEntryRegister extends CameraSettingActivity implements CameraManager.CameraListener {


    /* SDK 实例对象 */ FacePassHandler mFacePassHandler;

    /* 相机预览界面 */
    private CameraPreview cameraView;

    /* 在预览界面圈出人脸 */
    private FaceView faceView;

    RecognizeThread mRecognizeThread;
    FeedFrameThread mFeedFrameThread;

    ArrayBlockingQueue<RecognizeData> mRecognizeDataQueue;
    ArrayBlockingQueue<CameraPreviewData> mFeedFrameQueue;

    boolean isLocalGroupExist = true;
    boolean ignoreThreadWhile = false;
    private ProgressDialog mProgressDialog;

    private SweetAlertDialog sweetAlertDialog;

    private String serialNo;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private DatabaseHelper dbHelper;
    private GridLayout clockLayout;
    private LinearLayout scanLayout;
    private Biometric biometric;
    private TextView welcomeText;

    private FingerSDK fingerSDK;
    private boolean deviceModelNameCheck = false;
    List<Fingerprint> fingerprints;
    private boolean allowCapture = true;

    private BiometricRepository biometricRepository;
    private TimeRepository timeRepository;

    private TextView greeting;
    private TextView timeEntry;
    private TextView welcomeDialogName;
    private TextView reminderLabel;

    boolean isTimeRegisterOn;

    private Dialog welcomeDialog;
    RecyclerView announcementList;

    private String snapshotPath;
    private boolean takeSnapshot = false;

    private boolean hasPreview = false;

    private SharedPreferences sharedPreferences;

    int fingerprintScoreThreshold = 80;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private String detectStranger;

    private boolean isLockScreen = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            lockScreen();
        }
    }

    private void lockScreen() {

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (hasPreview || clockLayout.getVisibility() == View.VISIBLE || !allowCapture || sweetAlertDialog.isShowing()) {
                    mFeedFrameThread.resetLastFeedTimeSaver();
                    isLockScreen = false;
                    Log.d("TimeEntryRegister", "Screen already on");
                    return;
                }

                Log.d("TimeEntryRegister", "Locking screen" + hasPreview);
                if (devicePolicyManager.isAdminActive(adminComponent)) {
                    devicePolicyManager.lockNow();
                    isLockScreen = true;
                } else {
                    // Request admin permission
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Device admin permission is required to lock the screen.");
                    startActivityForResult(intent, 1);
                }
            }
        }, 2000);


    }

    private PowerManager.WakeLock wakeLock;

    private void turnScreenOn() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!powerManager.isInteractive()) {
            mFeedFrameThread.resetLastFeedTimeSaver();
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "MyApp::WakeLock");
            wakeLock.acquire(3000); // Wake the screen for 3 seconds
            isLockScreen = false;
        }
    }

    private void syncTimeEntries() {
        ((App) getApplication()).syncTimeEntriesOnLogout(TimeEntryRegister.this, new App.SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d("TimeEntryRegister", "Time entries synced");
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.d("TimeEntryRegister", "Time entries sync failed: " + errorMessage);
                Toast.makeText(TimeEntryRegister.this, "Time entries sync failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStrangerDetection() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        return sharedPreferences.getString("STRANGER_DETECTION", "on");
    }

    private String getSecondaryLogo() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String secondaryLogo = sharedPreferences.getString("SECONDARY_LOGO", null);
        if (secondaryLogo == null) {
            return "drawable/logo"; // Return the default logo resource name
        }
        return secondaryLogo;
    }

    private int fingerprintScoreThreshold() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String fingerprintScoreThreshold = sharedPreferences.getString("FINGERPRINT_SCORE_THRESHOLD", "80");
        return Integer.parseInt(fingerprintScoreThreshold);
    }

    private String getUserPassword() throws Exception {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String encryptedData = sharedPreferences.getString("user_data", null);
        if (encryptedData != null) {
            byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
            String decryptedData = EncryptionUtil.decrypt(decodedData);
            String[] userData = decryptedData.split(",");
            return userData[2]; // Assuming the hashed password is the 2nd element in the array
        }
        return "";
    }

    private void checkPassword(String password) throws Exception {
//        BCrypt.checkpw(password, Objects.requireNonNull(getUserHashedPassword()))
        Log.d("TimeEntryRegister", "Checking password: " + password + " " + getUserPassword());
        if (password.equals(getUserPassword())) {

            allowCapture = false;

            if (mFeedFrameThread != null) {
                mFeedFrameThread.interrupt();
            }

            if (mRecognizeThread != null) {
                mRecognizeThread.interrupt();
            }

            if (mNfcAdapter != null) {
                mNfcAdapter.disableForegroundDispatch(TimeEntryRegister.this);
            }

            if (fingerSDK != null) {
                fingerSDK.clear();
                fingerSDK.release();
                fingerSDK = null;
                System.out.println("Finger SDK released");
            }

            Intent intent = new Intent(TimeEntryRegister.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (sweetAlertDialog != null) {
                        sweetAlertDialog.dismiss();
                    }
                    sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Wrong Password").setContentText("Please try again.");
                    sweetAlertDialog.show();
                }
            });
        }
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Password");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();
                try {
                    checkPassword(password);
                } catch (Exception e) {
                    Log.e("TimeEntryRegister", "Error checking password", e);
                    throw new RuntimeException(e);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.show();
            }
        });
    }

    @Override
    public void onCreateBase(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        dbHelper = new DatabaseHelper(this);
        setContentView(R.layout.activity_time_entry_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ((App) getApplication()).getAnnouncements();

        if (getWindow().hasFeature(Window.FEATURE_ACTION_BAR)) {
            getWindow().invalidatePanelMenu(Window.FEATURE_ACTION_BAR);
        }

        fingerprintScoreThreshold = fingerprintScoreThreshold();
        detectStranger = getStrangerDetection();



        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyAdminReceiver.class);

        Log.d("TimeEntryRegister", "Fingerprint score threshold: " + fingerprintScoreThreshold);

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

        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPasswordDialog();
            }
        });

        serialNo = Build.getSerial();

        Objects.requireNonNull(getWindow().getInsetsController()).hide(WindowInsetsCompat.Type.systemBars());

        sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        welcomeText = findViewById(R.id.heading_time_register);
        biometricRepository = new BiometricRepository(this);

        clockLayout = findViewById(R.id.clock_layout);
        scanLayout = findViewById(R.id.scan_layout);

        welcomeDialog = new Dialog(this);
        welcomeDialog.setContentView(R.layout.activity_welcome_dialog);

        cameraView = findViewById(R.id.preview);
        faceView = findViewById(R.id.fcview);

        manager = new CameraManager();
        manager.setPreviewDisplay(cameraView);
        manager.setListener(this);
        CameraSetting();

        mProgressDialog = new ProgressDialog(this);

        // Load the saved state
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        isTimeRegisterOn = sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER, false);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(welcomeDialog.getWindow().getAttributes());
        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        welcomeDialog.getWindow().setAttributes(layoutParams);

        welcomeDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_bg_white));
        welcomeDialog.setCancelable(false);


        if (!isTimeRegisterOn) {
            greeting = welcomeDialog.findViewById(R.id.greeting);
            timeEntry = welcomeDialog.findViewById(R.id.time_entry);
            welcomeDialogName = welcomeDialog.findViewById(R.id.name);
            reminderLabel = welcomeDialog.findViewById(R.id.reminder_label);
            announcementList = welcomeDialog.findViewById(R.id.announcement_list);

        } else {
            greeting = findViewById(R.id.greeting);
            timeEntry = findViewById(R.id.time_entry);
            welcomeDialogName = findViewById(R.id.name);
            announcementList = findViewById(R.id.announcement_list);
            reminderLabel = findViewById(R.id.reminder_label);
        }

        Log.d("TimeEntryRegister", "Access: " + getAccess() + ".");
        if (getAccess().equals("offline") && reminderLabel != null) {
            reminderLabel.setVisibility(View.GONE);
        }

        getFingerPrints();
        getTrackerSwitches();

        mProgressDialog.setMessage(getString(R.string.loading));
//        mProgressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                mRecognizeDataQueue = new ArrayBlockingQueue<RecognizeData>(5);
                mFeedFrameQueue = new ArrayBlockingQueue<CameraPreviewData>(1);
                InitFacePassHandler.init(TimeEntryRegister.this, new InitFacePassHandler.IFacePassInit() {
                    @Override
                    public void result(FacePassHandler facePassHandler) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressDialog.dismiss();
                            }
                        });
                        if (facePassHandler != null) {
                            TimeEntryRegister.this.mFacePassHandler = facePassHandler;
                            checkGroup();

                            mRecognizeThread = new TimeEntryRegister.RecognizeThread();
                            mRecognizeThread.start();
                            mFeedFrameThread = new TimeEntryRegister.FeedFrameThread();
                            mFeedFrameThread.start();
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(TimeEntryRegister.this, "Error Face Handler: 2", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                        }
                    }
                });
            }
        }).start();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

//                allowCapture = false;
//
//                if (mFeedFrameThread != null) {
//                    mFeedFrameThread.interrupt();
//                }
//
//                if (mRecognizeThread != null) {
//                    mRecognizeThread.interrupt();
//                }
//
//                if (mNfcAdapter != null) {
//                    mNfcAdapter.disableForegroundDispatch(TimeEntryRegister.this);
//                }
//
//                if (fingerSDK != null) {
//                    fingerSDK.clear();
//                    fingerSDK.release();
//                    fingerSDK = null;
//                    System.out.println("Finger SDK released");
//                }

//                Intent intent = new Intent(TimeEntryRegister.this, MainActivity.class);
//                startActivity(intent);
//                finish();
            }
        });

        deviceModelNameCheck = FingerSDK.licenceDevice();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.PROGRESS_TYPE).setTitleText("Loading Scanner");
                sweetAlertDialog.show();
            }
        });

        fingerSDK = new FingerSDK(TimeEntryRegister.this, new OnSdkInitListener() {
            @Override
            public void initResult(int i, String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("TimeEntryRegister", "initResult: " + i + " " + s);
                        if (fingerSDK == null) {
                            return;
                        }

                        if (i != 1 && i != 0) {
                            fingerSDK.launch();
                        } else {
                            sweetAlertDialog.dismiss();
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

        timeRepository = new TimeRepository(this);
        findViewById(R.id.check_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sweetAlertDialog != null) {
                    sweetAlertDialog.dismiss();
                }

                if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_check_in", false)) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Disabled").setContentText("Check in is disabled");
                            sweetAlertDialog.show();
                        }
                    }, 100);
                    return;
                }

                showScanLayout();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long id = timeRepository.insertTimeEntry(biometric.getUserId(), "time-in", snapshotPath, serialNo);
                        if (id > 0) {
                            allowCapture = true;
                            Toast.makeText(TimeEntryRegister.this, "Clocked in", Toast.LENGTH_SHORT).show();
                            hostCapture();
                            welcomeText.setText("Time Register");
                            syncTimeEntries();

                        } else {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Failed").setContentText("Failed to clock in, Please try again.");
                            sweetAlertDialog.show();
                        }
                    }
                });

            }
        });

        findViewById(R.id.check_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sweetAlertDialog != null) {
                    sweetAlertDialog.dismiss();
                }

                if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_check_out", false)) {
                    sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Disabled").setContentText("Check out is disabled");
                    sweetAlertDialog.show();
                    return;
                }

                showScanLayout();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        long id = timeRepository.insertTimeEntry(biometric.getUserId(), "time-out", snapshotPath, serialNo);
                        if (id > 0) {
                            allowCapture = true;
                            Toast.makeText(TimeEntryRegister.this, "Clocked out", Toast.LENGTH_SHORT).show();
                            hostCapture();
                            welcomeText.setText("Time Register");
                            syncTimeEntries();

                        } else {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Failed").setContentText("Failed to clock out, Please try again.");
                            sweetAlertDialog.show();
                        }
                    }
                }, 10);

            }
        });

        findViewById(R.id.break_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sweetAlertDialog != null) {
                    sweetAlertDialog.dismiss();
                }

                if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_break_in", false)) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Disabled").setContentText("Break in is disabled");
                            sweetAlertDialog.show();
                        }
                    }, 100);
                    return;
                }

                showScanLayout();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        long id = timeRepository.insertTimeEntry(biometric.getUserId(), "break-in", snapshotPath, serialNo);
                        if (id > 0) {
                            allowCapture = true;
                            Toast.makeText(TimeEntryRegister.this, "Break in", Toast.LENGTH_SHORT).show();
                            hostCapture();
                            welcomeText.setText("Time Register");
                            syncTimeEntries();

                        } else {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Failed").setContentText("Failed to break in, Please try again.");
                            sweetAlertDialog.show();
                        }
                    }
                }, 10);


            }
        });

        findViewById(R.id.break_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sweetAlertDialog != null) {
                    sweetAlertDialog.dismiss();
                }

                if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_break_out", false)) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Disabled").setContentText("Break out is disabled");
                            sweetAlertDialog.show();
                        }
                    }, 100);
                    return;
                }

                showScanLayout();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        long id = timeRepository.insertTimeEntry(biometric.getUserId(), "break-out", snapshotPath, serialNo);
                        if (id > 0) {
                            allowCapture = true;
                            Toast.makeText(TimeEntryRegister.this, "Break out", Toast.LENGTH_SHORT).show();
                            hostCapture();
                            welcomeText.setText("Time Register");
                            syncTimeEntries();

                        } else {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Failed").setContentText("Failed to break out, Please try again.");
                            sweetAlertDialog.show();
                        }
                    }
                }, 10);
            }
        });

        findViewById(R.id.overtime_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sweetAlertDialog != null) {
                    sweetAlertDialog.dismiss();
                }

                if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_overtime_in", false)) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Disabled").setContentText("Overtime in is disabled");
                            sweetAlertDialog.show();
                        }
                    }, 100);
                    return;
                }

                showScanLayout();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        long id = timeRepository.insertTimeEntry(biometric.getUserId(), "ot-in", snapshotPath, serialNo);
                        if (id > 0) {
                            allowCapture = true;
                            Toast.makeText(TimeEntryRegister.this, "Overtime in", Toast.LENGTH_SHORT).show();
                            hostCapture();
                            welcomeText.setText("Time Register");
                            syncTimeEntries();

                        } else {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Failed").setContentText("Failed to overtime-in, Please try again.");
                            sweetAlertDialog.show();
                        }
                    }
                }, 10);
            }
        });

        findViewById(R.id.overtime_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sweetAlertDialog != null) {
                    sweetAlertDialog.dismiss();
                }

                if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_overtime_out", false)) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Disabled").setContentText("Overtime out is disabled");
                            sweetAlertDialog.show();
                        }
                    }, 100);
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long id = timeRepository.insertTimeEntry(biometric.getUserId(), "ot-out", snapshotPath, serialNo);
                        if (id > 0) {
                            allowCapture = true;
                            Toast.makeText(TimeEntryRegister.this, "Overtime out", Toast.LENGTH_SHORT).show();
                            showScanLayout();
                            hostCapture();
                            syncTimeEntries();
                            welcomeText.setText("Time Register");
                        } else {
                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Failed").setContentText("Failed to overtime-out, Please try again.");
                            sweetAlertDialog.show();
                        }
                    }
                });

            }
        });

    }

    private void getTrackerSwitches() {

        sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);

        if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_check_in", false)) {
            CardView cardView = (CardView) findViewById(R.id.check_in);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        } else {
            CardView cardView = (CardView) findViewById(R.id.check_in);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green));
        }

        if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_check_out", false)) {
            CardView cardView = (CardView) findViewById(R.id.check_out);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        } else {
            CardView cardView = (CardView) findViewById(R.id.check_out);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.error));
        }

        if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_break_in", false)) {
            CardView cardView = (CardView) findViewById(R.id.break_in);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        } else {
            CardView cardView = (CardView) findViewById(R.id.break_in);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.amber));
        }

        if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_break_out", false)) {
            CardView cardView = (CardView) findViewById(R.id.break_out);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        } else {
            CardView cardView = (CardView) findViewById(R.id.break_out);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.error_stroke_color));
        }

        if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_overtime_in", false)) {
            CardView cardView = (CardView) findViewById(R.id.overtime_in);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        } else {
            CardView cardView = (CardView) findViewById(R.id.overtime_in);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.fuchsia));
        }

        if (!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_overtime_out", false)) {
            CardView cardView = (CardView) findViewById(R.id.overtime_out);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        } else {
            CardView cardView = (CardView) findViewById(R.id.overtime_out);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.rose));
        }

    }

    @Override
    public void onRestart() {
        super.onRestart();
        faceView.clear();
        faceView.invalidate();
    }

    private double[] getLatAndLong() {
        SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        double latitude = sharedPreferences.getLong("latitude", 0);
        double longitude = sharedPreferences.getLong("longitude", 0);
//        Log.d("Location", "Lat: " + latitude + ", Lon: " + longitude);
        return new double[]{latitude, longitude};
    }

    private void checkGroup() {
        if (mFacePassHandler == null) {
            return;
        }
        try {
            String[] localGroups = mFacePassHandler.getLocalGroups();
            isLocalGroupExist = false;
            if (localGroups == null || localGroups.length == 0) {
                faceView.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TimeEntryRegister.this, group_name + " " + getString(R.string.failed), Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            for (String group : localGroups) {
                if (group_name.equals(group)) {
                    isLocalGroupExist = true;
                }
            }
            if (!isLocalGroupExist) {
                faceView.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TimeEntryRegister.this, group_name + " " + getString(R.string.failed), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (FacePassException e) {
            e.printStackTrace();
        }
    }

    private void getFingerPrints() {
        // Fetch all fingerprints
        FingerprintRepository fingerprintRepository = new FingerprintRepository(this);

        // Get the group ID from local storage (assuming SharedPreferences is used)
        SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String groupId = sharedPreferences.getString(GroupActivity.KEY_SELECTED_GROUP, null);

        Log.d("TimeEntryRegister", "Group ID: " + groupId);

        if (groupId != null) {
            // Get fingerprints by group ID
            fingerprints = fingerprintRepository.getFingerprintsByGroupId(Long.parseLong(groupId));
            Log.d("TimeEntryRegister", "Fingerprints here: " + fingerprints.size());
        } else {
            Log.e("TimeEntryRegister", "Group ID not found in local storage");
            Toast.makeText(this, "Group ID not found in local storage", Toast.LENGTH_SHORT).show();
        }
    }

    private void adjustGreeting(User user) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            greeting.setText("Good Morning");
        } else if (hour < 18) {
            greeting.setText("Good Afternoon");
        } else {
            greeting.setText("Good Evening");
        }

        System.out.println("The time is: " + hour);

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = sdf.format(calendar.getTime());
        timeEntry.setText("Time Entry: " + currentTime);

        welcomeDialogName.setText(user.getDisplayName());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(TimeEntryRegister.this);

        if (mNfcAdapter == null) {
            // Device does not support NFC
            new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("NFC Not Supported").setContentText("This device does not support NFC").show();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            // NFC is not enabled
            new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("NFC Not Enabled").setContentText("Please enable NFC in your device settings").show();
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);

        if (sweetAlertDialog != null) {
            sweetAlertDialog.dismiss();
        }


//        TextToSpeechUtil.say(getApplicationContext(), "Welcome to Time Register");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.PROGRESS_TYPE).setTitleText("Loading Scanner");
                sweetAlertDialog.show();
            }
        });

//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                sweetAlertDialog.dismiss();
//            }
//        }, 3000);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                hostCapture();
            }
        }, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        fingerSDK.launch();

        manager.open(getWindowManager(), cameraFront, RelativeLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ignoreThreadWhile = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLockScreen) {
            if (fingerSDK != null) {
                fingerSDK.release();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("ScanFace", "onPause: " + isLockScreen);
        if (!isLockScreen) {
            if (fingerSDK != null) {
                fingerSDK.release();
                Log.d("TimeEntryRegister", "2: Finger SDK released");
            }
            ignoreThreadWhile = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d("ScanFace", "onDestroy");

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        allowCapture = false;
        if (mRecognizeThread != null) {
            mRecognizeThread.interrupt();
        }
        if (mFeedFrameThread != null) {
            mFeedFrameThread.interrupt();
        }
        if (manager != null) {
            manager.release();
        }
        InitFacePassHandler.release();

        if (fingerSDK != null) {
            fingerSDK.release();
            System.out.println("Finger SDK released");
        }
    }

    private void showDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressDialogUtils.showProgressDialog(TimeEntryRegister.this, "please press finger...");
            }
        });
    }

    private void handleAnnouncements(User user) {
        AnnouncementRepository repository = new AnnouncementRepository(TimeEntryRegister.this);
        AnnouncementModel[] announcements = repository.getAnnouncements(Long.parseLong(user.getUserId()));

        sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        isTimeRegisterOn = sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER, false);

        if (!isTimeRegisterOn) {
            adjustGreeting(user);
            welcomeDialog.show();
            allowCapture = false;
            hasPreview = true;
        }


        announcementList.setLayoutManager(new LinearLayoutManager(this));
        AnnouncementAdapter adapter = new AnnouncementAdapter(Arrays.asList(announcements));
        announcementList.setAdapter(adapter);

        if (announcements.length == 0 && reminderLabel != null) {
            reminderLabel.setVisibility(View.GONE);
        }

        if (isTimeRegisterOn && announcements.length == 0) {
            findViewById(R.id.announcementCard).setVisibility(View.GONE);
        }
    }

    private void showUserProfile(User user) {

        handleAnnouncements(user);

        ((App) getApplication()).syncTimeEntriesOnLogout(TimeEntryRegister.this, new App.SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d("TimeEntryRegister", "Time entries synced");
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.d("TimeEntryRegister", "Time entries sync failed: " + errorMessage);
                Toast.makeText(TimeEntryRegister.this, "Time entries sync failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                welcomeDialog.dismiss();
                allowCapture = true;
                hasPreview = false;
                hostCapture();
            }
        }, 5000);
    }

    private void hostCapture() {
        if (!isLockScreen && (!allowCapture || hasPreview || fingerSDK == null)) {
            return;
        }
        fingerSDK.clear();

        fingerSDK.captureBytes(FingerSDK.TEMPLEATES.valueOf("ISO_19794_2_2011"), new OnCaptureBytesListener() {
            @Override
            public void capture(int i, byte[] bytes, Bitmap bitmap, byte[] temp) {
                Log.d("TimeEntryRegister", "capture here: " + i + allowCapture);

                if (i == FingerSDK.RESULT_OK) {
                    turnScreenOn();
                }

                if (!allowCapture) {

                    if (fingerSDK != null) {
                        fingerSDK.clear();
                    }
                    Log.d("TimeEntryRegister", "capture not allowed");
                    return;
                }

                if (i == FingerSDK.RESULT_OK) {

                    try {
                        String tempString = new String(temp, "ISO8859-1");
                        for (Fingerprint fingerprint : fingerprints) {

                            byte[] storedFingerprint = Base64.decode(fingerprint.getKey(), Base64.DEFAULT);
                            int score = fingerSDK.compareTemplateBytes(FingerSDK.TEMPLEATES.valueOf("ISO_19794_2_2011"), tempString.getBytes(StandardCharsets.ISO_8859_1), storedFingerprint);
                            Log.d("TimeEntryRegister", "Score: " + score);
                            if (score > fingerprintScoreThreshold) {
                                allowCapture = false;

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        biometric = biometricRepository.getBiometricById(fingerprint.getBiometricId());
                                        User user = dbHelper.getUserById("" + biometric.getUserId());

                                        if (user.getGroupId() != Long.parseLong(getDeviceGroupId())) {
                                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    TextToSpeechUtil.say(getApplicationContext(), "Not Allowed! Group MisMatch");

                                                    if (sweetAlertDialog != null) {
                                                        sweetAlertDialog.dismiss();
                                                    }

                                                    sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Group MisMatch").setContentText("You are not allowed to clock in/out in this group");

                                                    sweetAlertDialog.show();

                                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            sweetAlertDialog.dismiss();
                                                        }
                                                    }, 2500);
                                                }
                                            }, 100);

                                            return;
                                        }

                                        handleTimeEntry(user);

                                    }
                                }, 100);

                                return;
                            }
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                TextToSpeechUtil.say(getApplicationContext(), "Not Allowed!");
                                showScanLayout();
                                hostCapture();
                            }
                        }, 100);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        Log.d("TimeEntryRegister", "UnsupportedEncodingException: " + e.getMessage());
                    }
                } else {
                    Log.d("TimeEntryRegister", "capture failed: " + i);

                    if (!allowCapture) {
                        return;
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hostCapture();
                        }
                    }, 1000);
                }
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

    private void showScanLayout() {
        clockLayout.setVisibility(View.GONE);
        scanLayout.setVisibility(View.VISIBLE);
    }

    private void showClockLayout(User user) {
        handleAnnouncements(user);

        getTrackerSwitches();

        clockLayout.setVisibility(View.VISIBLE);
        scanLayout.setVisibility(View.GONE);
    }

    private String getAccess() {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("ACCESS", "offline");
    }

    private String getDeviceGroupId() {

        if (getAccess().equals("offline")) {
            return "1";
        }

        SharedPreferences sharedPreferences = getSharedPreferences("DEVICE_GROUP", Context.MODE_PRIVATE);
        return sharedPreferences.getString(GroupActivity.KEY_SELECTED_GROUP, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d("Scanface", "New Intent");
        turnScreenOn();

        if (sweetAlertDialog != null) {
            sweetAlertDialog.dismiss();
        }

        if (!allowCapture || hasPreview) {
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String id = Conversion.Bytes2HexString(tag.getId());

//        manager.takePicture();
        biometric = dbHelper.getRfidByKey(id);
        if (biometric != null) {
            User user = dbHelper.getUserById("" + biometric.getUserId());

            if (user.getGroupId() != Long.parseLong(getDeviceGroupId())) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        TextToSpeechUtil.say(getApplicationContext(), "Not Allowed! Group MisMatch");

                        sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Group MisMatch").setContentText("You are not allowed to clock in/out in this group");
                        sweetAlertDialog.show();

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sweetAlertDialog.dismiss();
                            }
                        }, 2500);
                    }
                }, 100);

                return;
            }
            handleTimeEntry(user);
        } else {
            TextToSpeechUtil.say(getApplicationContext(), "User Not Found");
            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("User Not Found").setContentText("User not found in the system");
            sweetAlertDialog.show();

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    sweetAlertDialog.dismiss();
                }
            }, 2500);

            showScanLayout();
            welcomeText.setText("Time Register");
        }
    }

    @Override
    public void onPictureTaken(CameraPreviewData cameraPreviewData) {
//        Log.d("ScanFace", "onPictureTaken: " + cameraPreviewData);
        mFeedFrameQueue.offer(cameraPreviewData);
    }

    public String saveToGallery(Bitmap bitmap) {
        String dir = Environment.getExternalStorageDirectory() + "/snapshots/";
        File dirFile = new File(Environment.getExternalStorageDirectory(), "snapshots");
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        String filePath = System.currentTimeMillis() + "_photo" + ".jpg";
        File file = new File(dirFile, filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, fos);
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Log.d("ScanFace", "saveToGallery: " + dir + filePath);
        return dir + filePath;
    }

    private String handleSnapshot(CameraPreviewData cameraPreviewData) {
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(cameraPreviewData.nv21Data, ImageFormat.NV21, cameraPreviewData.width, cameraPreviewData.height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new android.graphics.Rect(0, 0, cameraPreviewData.width, cameraPreviewData.height), 40, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("ScanFace", "handleSnapshot: " + bitmap);
        if (bitmap != null) {
            int degrees = 0;
            degrees = getSaveDegree();

            if (degrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(degrees);

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            return saveToGallery(bitmap);
        }

        return null;
    }

    private void findUserByFaceToken(String faceToken, long trackId) {
        if (TextUtils.isEmpty(faceToken)) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                List<facex.facepass.db.User> userList = ((App) getApplication()).getUserDao().queryBuilder().where(UserDao.Properties.FaceToken.eq(faceToken)).list();
                Log.d("ScanFace", "findUserByFaceToken: " + faceToken);
                if (!allowCapture || hasPreview) {
                    return;
                }
                BiometricRepository biometricRepository = new BiometricRepository(TimeEntryRegister.this);
                biometric = biometricRepository.findFaceBiometricByKey(faceToken);

                Log.d("ScanFace", "findUserByFaceTokenBiometric: " + biometric);

//                manager.takePicture();
                if (biometric != null) {
                    User user = dbHelper.getUserById("" + biometric.getUserId());

                    if (user.getGroupId() != Long.parseLong(getDeviceGroupId())) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                TextToSpeechUtil.say(getApplicationContext(), "Not Allowed! Group MisMatch");

                                if (sweetAlertDialog != null) {
                                    sweetAlertDialog.dismiss();
                                }

                                sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Group MisMatch").setContentText("You are not allowed to clock in/out in this group");

                                sweetAlertDialog.show();

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        sweetAlertDialog.dismiss();
                                    }
                                }, 2500);

                            }
                        }, 100);

                        return;
                    }

                    handleTimeEntry(user);
                } else {
                    Toast.makeText(TimeEntryRegister.this, "No biometric found", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void handleTimeEntry(User user) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (sweetAlertDialog != null) {
                                sweetAlertDialog.dismiss();
                            }

                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.PROGRESS_TYPE).setTitleText("Loading");
                            sweetAlertDialog.show();

                        }
                    });

                    snapshotPath = handleSnapshot(mFeedFrameQueue.take());
                    Log.d("ScanRFID", "Snapshot path is: " + snapshotPath);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sweetAlertDialog.dismiss();

                            sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
                            isTimeRegisterOn = sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER, false);

                            if (isTimeRegisterOn) {
                                TextToSpeechUtil.say(getApplicationContext(), "Welcome " + user.getDisplayName());
                                adjustGreeting(user);
                                showClockLayout(user);
                            } else {
                                if (hasPreview) {
                                    return;
                                }

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        long timeId = timeRepository.insertTimeEntry(biometric.getUserId(), null, snapshotPath, serialNo);
                                        if (timeId > 0) {
                                            TextToSpeechUtil.say(getApplicationContext(), "Welcome " + user.getDisplayName());
                                            showUserProfile(user);
                                            showScanLayout();
                                        } else {
                                            sweetAlertDialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.ERROR_TYPE).setTitleText("Failed to record time").setContentText("Please try again");
                                            sweetAlertDialog.show();
                                        }
                                    }
                                }, 100);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Log.e("ScanRFID", "Error taking snapshot", e);
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void showFacePassFace(FacePassFace[] detectResult) {
        faceView.clear();
        for (FacePassFace face : detectResult) {

            Matrix mat = new Matrix();
            int w = cameraView.getMeasuredWidth();
            int h = cameraView.getMeasuredHeight();

            int cameraHeight = manager.getCameraheight();
            int cameraWidth = manager.getCameraWidth();

            float left = 0;
            float top = 0;
            float right = 0;
            float bottom = 0;
            switch (cameraRotation) {
                case 0:
                    left = face.rect.left;
                    top = face.rect.top;
                    right = face.rect.right;
                    bottom = face.rect.bottom;
                    mat.setScale(mirror ? -1 : 1, 1);
                    mat.postTranslate(mirror ? (float) cameraWidth : 0f, 0f);
                    mat.postScale((float) w / (float) cameraWidth, (float) h / (float) cameraHeight);
                    break;
                case 90:
                    mat.setScale(mirror ? -1 : 1, 1);
                    mat.postTranslate(mirror ? (float) cameraHeight : 0f, 0f);
                    mat.postScale((float) w / (float) cameraHeight, (float) h / (float) cameraWidth);
                    left = face.rect.top;
                    top = cameraWidth - face.rect.right;
                    right = face.rect.bottom;
                    bottom = cameraWidth - face.rect.left;
                    break;
                case 180:
                    mat.setScale(1, mirror ? -1 : 1);
                    mat.postTranslate(0f, mirror ? (float) cameraHeight : 0f);
                    mat.postScale((float) w / (float) cameraWidth, (float) h / (float) cameraHeight);
                    left = face.rect.right;
                    top = face.rect.bottom;
                    right = face.rect.left;
                    bottom = face.rect.top;
                    break;
                case 270:
                    mat.setScale(mirror ? -1 : 1, 1);
                    mat.postTranslate(mirror ? (float) cameraHeight : 0f, 0f);
                    mat.postScale((float) w / (float) cameraHeight, (float) h / (float) cameraWidth);
                    left = cameraHeight - face.rect.bottom;
                    top = face.rect.left;
                    right = cameraHeight - face.rect.top;
                    bottom = face.rect.right;
            }

            RectF drect = new RectF();
            RectF srect = new RectF(left, top, right, bottom);
            mat.mapRect(drect, srect);
            faceView.addRect(drect);
        }
        faceView.invalidate();
    }

    private int getScreenTimeOut() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        return Integer.parseInt(sharedPreferences.getString("SCREEN_TIMEOUT", "60000"));
    }


    private class FeedFrameThread extends Thread {
        boolean isInterrupt = false;

        long lastFeedTime = 0;
        long lastFeedTimeSaver = 0;

        public void resetLastFeedTimeSaver() {
            lastFeedTimeSaver = System.currentTimeMillis();
        }

        @Override
        public void run() {
            lastFeedTimeSaver = System.currentTimeMillis();

            while (!isInterrupt) {

                if (ignoreThreadWhile) {
                    continue;
                }
                if (mFacePassHandler == null) {
                    continue;
                }
                if (isFinishing()) {
                    continue;
                }
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    continue;
                }
                /* 将每一帧FacePassImage 送入SDK算法， 并得到返回结果 */
                FacePassDetectionResult detectionResult = null;
                try {
                    CameraPreviewData cameraPreviewData = null;
                    try {
                        cameraPreviewData = mFeedFrameQueue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        continue;
                    }
                    FacePassImage imageRGB = new FacePassImage(cameraPreviewData.nv21Data, cameraPreviewData.width, cameraPreviewData.height, cameraRotation, FacePassImageType.NV21);
                    detectionResult = mFacePassHandler.feedFrame(imageRGB);
                } catch (FacePassException e) {
                    e.printStackTrace();
                }

                if (detectionResult == null || detectionResult.faceList.length == 0) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            faceView.clear();
                            faceView.invalidate();
                        }
                    });
                } else {
                    final FacePassFace[] bufferFaceList = detectionResult.faceList;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showFacePassFace(bufferFaceList);
                        }
                    });
                }


                /*离线模式，将识别到人脸的，message不为空的result添加到处理队列中*/
                if (detectionResult != null && detectionResult.message.length != 0) {
                    Log.e("ScanFace", "get a face");
                    turnScreenOn();
                    /*送识别的人脸框的属性信息*/
                    FacePassTrackOptions[] trackOpts = new FacePassTrackOptions[detectionResult.images.length];
                    for (int i = 0; i < detectionResult.images.length; ++i) {
                        if (detectionResult.images[i].rcAttr.respiratorType != FacePassRCAttribute.FacePassRespiratorType.INVALID && detectionResult.images[i].rcAttr.respiratorType != FacePassRCAttribute.FacePassRespiratorType.NO_RESPIRATOR) {
                            float searchThreshold = 60f;
                            float livenessThreshold = 80f; // -1.0f will not change the liveness threshold
                            float livenessGaThreshold = 85f;
                            float smallsearchThreshold = -1.0f; // -1.0f will not change the smallsearch threshold
                            trackOpts[i] = new FacePassTrackOptions(detectionResult.images[i].trackId, searchThreshold, livenessThreshold, livenessGaThreshold, smallsearchThreshold);
                        }
                    }
                    if (System.currentTimeMillis() - lastFeedTime > 500) {
                        RecognizeData mRecData = new RecognizeData(detectionResult.message, trackOpts);
                        Log.e("ScanFace", "make face data ready");
                        mRecognizeDataQueue.offer(mRecData);
                        Log.e("ScanFace", "send face data to recognize queue");
                        lastFeedTime = System.currentTimeMillis();
                        lastFeedTimeSaver = System.currentTimeMillis();
                    }
                }

                if ((detectionResult == null || detectionResult.faceList.length == 0) && (System.currentTimeMillis() - lastFeedTimeSaver) > getScreenTimeOut()) {
                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    lastFeedTimeSaver = System.currentTimeMillis();
                    Log.d("ScanFace", "Log 111111111");

                    if (powerManager != null && powerManager.isInteractive() && clockLayout.getVisibility() == View.GONE && !hasPreview) {
                        Log.d("ScanFace", "no face found, turn off screen + ");
                        lockScreen();
                    }
                }
            }

        }

        @Override
        public void interrupt() {
            isInterrupt = true;
            super.interrupt();
        }
    }


    private class RecognizeThread extends Thread {

        boolean isInterrupt = false;
        long lastStrangerFindTime = 0;

        @Override
        public void run() {
            while (!isInterrupt) {
                if (ignoreThreadWhile) {
                    continue;
                }
                try {

                    RecognizeData recognizeData = mRecognizeDataQueue.take();
                    FacePassAgeGenderResult[] ageGenderResult = null;

                    if (isLocalGroupExist) {
                        Log.e("ScanFace", "recognize start");
                        FacePassRecognitionResult[][] recognizeResultArray = mFacePassHandler.recognize(group_name, recognizeData.message, 1, recognizeData.trackOpt);
                        if (recognizeResultArray != null && recognizeResultArray.length > 0) {
                            Log.e("ScanFace", "recognize done");
                            for (FacePassRecognitionResult[] recognizeResult : recognizeResultArray) {
                                if (recognizeResult != null && recognizeResult.length > 0) {
                                    boolean ok = false;
                                    for (FacePassRecognitionResult result : recognizeResult) {
                                        String faceToken = new String(result.faceToken, StandardCharsets.ISO_8859_1);
                                        Log.e("ScanFace", "recognize a face with faceToken：" + faceToken);
                                        if (FacePassRecognitionState.RECOGNITION_PASS == result.recognitionState) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    faceView.setFaceRectUser();
                                                }
                                            });
                                            Log.d("ScanFace", "faceTokens:" + Arrays.toString(result.faceToken));
                                            findUserByFaceToken(faceToken, result.trackId);
                                            ok = true;
                                        }
                                        int idx = findidx(ageGenderResult, result.trackId);
                                    }
                                    if (!ok && System.currentTimeMillis() - lastStrangerFindTime > 3000) {
                                        lastStrangerFindTime = System.currentTimeMillis();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                faceView.setFaceRectError();
                                                detectStranger = getStrangerDetection();

                                                if (detectStranger.equals("on")) {
                                                    TextToSpeechUtil.say(getApplicationContext(), getString(R.string.stranger));
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        } else if (recognizeResultArray != null && recognizeResultArray.length == 0) {
                            if (System.currentTimeMillis() - lastStrangerFindTime > 3000) {
                                lastStrangerFindTime = System.currentTimeMillis();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        faceView.setFaceRectError();
                                        detectStranger = getStrangerDetection();
                                        if (detectStranger.equals("on")) {
                                            TextToSpeechUtil.say(getApplicationContext(), getString(R.string.stranger));
                                        }
                                    }
                                });
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (FacePassException e) {
                    e.printStackTrace();
                }
            }
            Log.e("ScanFace", "recognize quit");
        }

        @Override
        public void interrupt() {
            isInterrupt = true;
            super.interrupt();
        }
    }

    int findidx(FacePassAgeGenderResult[] results, long trackId) {
        int result = -1;
        if (results == null) {
            return result;
        }
        for (int i = 0; i < results.length; ++i) {
            if (results[i].trackId == trackId) {
                return i;
            }
        }
        return result;
    }

}
package com.example.bms.time_entry;

import static facex.facepass.InitFacePassHandler.group_name;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hibory.Conversion;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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
import com.example.bms.FingerPrintScanActivity;
import com.example.bms.Fingerprint;
import com.example.bms.FingerprintRepository;
import com.example.bms.GroupActivity;
import com.example.bms.MainActivity;
import com.example.bms.R;
import com.example.bms.ScanFaceActivity;
import com.example.bms.SplashScreen;
import com.example.bms.data.LoginDataSource;
import com.example.bms.data.model.User;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.hfteco.finger.FingerSDK;
import com.hfteco.finger.OnCaptureBytesListener;
import com.hfteco.finger.OnEnrollListener;
import com.hfteco.finger.OnSdkInitListener;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import facex.greendao.gen.UserDao;
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

public class TimeEntryRegister extends CameraSettingActivity implements CameraManager.CameraListener{


    /* SDK 实例对象 */
    FacePassHandler mFacePassHandler;

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

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private DatabaseHelper dbHelper;
    private GridLayout clockLayout;
    private LinearLayout scanLayout;
    private Biometric biometric;
    private TextView welcomeText;

    private ActivityResultLauncher<Intent> fingerprintScanLauncher;
    private FingerSDK fingerSDK;
    private boolean deviceModelNameCheck = false;
    List<Fingerprint> fingerprints;
    private boolean allowCapture = true;

    private BiometricRepository biometricRepository;
    private TimeRepository timeRepository;

    private TextView greeting;
    private TextView timeEntry;
    private TextView welcomeDialogName;

    boolean isTimeRegisterOn;

    private Dialog welcomeDialog;

    private boolean hasPreview = false;

    private SharedPreferences sharedPreferences;

    private void syncTimeEntries(){
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


        if(!isTimeRegisterOn) {
            greeting = welcomeDialog.findViewById(R.id.greeting);
            timeEntry = welcomeDialog.findViewById(R.id.time_entry);
            welcomeDialogName = welcomeDialog.findViewById(R.id.name);
        }else{
            greeting = findViewById(R.id.greeting);
            timeEntry = findViewById(R.id.time_entry);
            welcomeDialogName = findViewById(R.id.name);
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
                        }else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(TimeEntryRegister.this,"Error Face Handler: 2",Toast.LENGTH_SHORT).show();
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

                allowCapture = false;

                if(mNfcAdapter != null) {
                    mNfcAdapter.disableForegroundDispatch(TimeEntryRegister.this);
                }

                if(fingerSDK != null) {
                    fingerSDK.release();
                    System.out.println("Finger SDK released");
                }


                Intent intent = new Intent(TimeEntryRegister.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        deviceModelNameCheck = FingerSDK.licenceDevice();
        fingerSDK = new FingerSDK(TimeEntryRegister.this, new OnSdkInitListener() {
            @Override
            public void initResult(int i, String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("FingerPrintScanActivity", "initResult: " + i + " " + s);
                        if (i != FingerSDK.RESULT_OK) {
                            fingerSDK.launch();
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

                if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_check_in", false)) {
                    Toast.makeText(TimeEntryRegister.this, "Check in is disabled", Toast.LENGTH_SHORT).show();
                    return;
                }

                long id = timeRepository.insertTimeEntry(biometric.getUserId(),"time-in");
                if (id > 0) {
                    allowCapture = true;
                    Toast.makeText(TimeEntryRegister.this, "Clocked in", Toast.LENGTH_SHORT).show();
                    showScanLayout();
                    hostCapture();
                    welcomeText.setText("Time Register");
                    syncTimeEntries();

                } else {
                    Toast.makeText(TimeEntryRegister.this, "Failed to clock in, Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.check_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_check_out", false)) {
                    Toast.makeText(TimeEntryRegister.this, "Check out is disabled", Toast.LENGTH_SHORT).show();
                    return;
                }

                long id = timeRepository.insertTimeEntry(biometric.getUserId(),"time-out");
                if (id > 0) {
                    allowCapture = true;
                    Toast.makeText(TimeEntryRegister.this, "Clocked out", Toast.LENGTH_SHORT).show();
                    showScanLayout();
                    hostCapture();
                    welcomeText.setText("Time Register");
                    syncTimeEntries();

                } else {
                    Toast.makeText(TimeEntryRegister.this, "Failed to clock out, Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.break_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_break_in", false)) {
                    Toast.makeText(TimeEntryRegister.this, "Break in is disabled", Toast.LENGTH_SHORT).show();
                    return;
                }

                long id = timeRepository.insertTimeEntry(biometric.getUserId(),"break-in");
                if (id > 0) {
                    allowCapture = true;
                    Toast.makeText(TimeEntryRegister.this, "Break in", Toast.LENGTH_SHORT).show();
                    showScanLayout();
                    hostCapture();
                    welcomeText.setText("Time Register");
                    syncTimeEntries();

                } else {
                    Toast.makeText(TimeEntryRegister.this, "Failed to break in, Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.break_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_break_out", false)) {
                    Toast.makeText(TimeEntryRegister.this, "Break out is disabled", Toast.LENGTH_SHORT).show();
                    return;
                }

                long id = timeRepository.insertTimeEntry(biometric.getUserId(),"break-out");
                if (id > 0) {
                    allowCapture = true;
                    Toast.makeText(TimeEntryRegister.this, "Break out", Toast.LENGTH_SHORT).show();
                    showScanLayout();
                    hostCapture();
                    welcomeText.setText("Time Register");
                    syncTimeEntries();

                } else {
                    Toast.makeText(TimeEntryRegister.this, "Failed to break out, Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.overtime_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_overtime_in", false)) {
                    Toast.makeText(TimeEntryRegister.this, "Overtime in is disabled", Toast.LENGTH_SHORT).show();
                    return;
                }

                long id = timeRepository.insertTimeEntry(biometric.getUserId(),"ot-in");
                if (id > 0) {
                    allowCapture = true;
                    Toast.makeText(TimeEntryRegister.this, "Overtime in", Toast.LENGTH_SHORT).show();
                    showScanLayout();
                    hostCapture();
                    welcomeText.setText("Time Register");
                    syncTimeEntries();

                } else {
                    Toast.makeText(TimeEntryRegister.this, "Failed to overtime-in, Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.overtime_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_overtime_out", false)) {
                    Toast.makeText(TimeEntryRegister.this, "Overtime out is disabled", Toast.LENGTH_SHORT).show();
                    return;
                }

                long id = timeRepository.insertTimeEntry(biometric.getUserId(),"ot-out");
                if (id > 0) {
                    allowCapture = true;
                    Toast.makeText(TimeEntryRegister.this, "Overtime out", Toast.LENGTH_SHORT).show();
                    showScanLayout();
                    hostCapture();
                    syncTimeEntries();
                    welcomeText.setText("Time Register");
                } else {
                    Toast.makeText(TimeEntryRegister.this, "Failed to overtime-out, Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void getTrackerSwitches() {

        if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_check_in", false)) {
            CardView cardView = (CardView) findViewById(R.id.check_in);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        }

        if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_check_out", false)) {
            CardView cardView = (CardView) findViewById(R.id.check_out);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        }

        if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_break_in", false)) {
            CardView cardView = (CardView) findViewById(R.id.break_in);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        }

        if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_break_out", false)) {
            CardView cardView = (CardView) findViewById(R.id.break_out);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        }

        if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_overtime_in", false)) {
            CardView cardView = (CardView) findViewById(R.id.overtime_in);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        }

        if(!sharedPreferences.getBoolean(Configuration.KEY_TIME_REGISTER + "_overtime_out", false)) {
            CardView cardView = (CardView) findViewById(R.id.overtime_out);
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_btn_bg_color));
        }

    }

    @Override
    public void onRestart() {
        super.onRestart();
        faceView.clear();
        faceView.invalidate();
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
                        Toast.makeText(TimeEntryRegister.this,group_name + " " + getString(R.string.failed),Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(TimeEntryRegister.this,group_name + " " + getString(R.string.failed),Toast.LENGTH_SHORT).show();
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

        if (groupId != null) {
            // Get fingerprints by group ID
           fingerprints = fingerprintRepository.getFingerprintsByGroupId(Long.parseLong(groupId));
           Log.d("TimeEntryRegister", "Fingerprints here: " + fingerprints.size());
        } else {
            Log.e("TimeEntryRegister", "Group ID not found in local storage");
            Toast.makeText(this, "Group ID not found in local storage", Toast.LENGTH_SHORT).show();
        }
    }

    private void adjustGreeting(User user){
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
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            // Device does not support NFC
        }
        if (!mNfcAdapter.isEnabled()) {
            // NFC is not enabled
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);

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
        fingerSDK.release();
    }


    @Override
    public void onPause() {
        super.onPause();
        fingerSDK.release();
        ignoreThreadWhile = true;
    }

    @Override
    public void onDestroy() {
        allowCapture = false;
        if(mRecognizeThread!=null) {
            mRecognizeThread.interrupt();
        }
        if(mFeedFrameThread!=null) {
            mFeedFrameThread.interrupt();
        }
        if (manager != null) {
            manager.release();
        }
        InitFacePassHandler.release();

        if(fingerSDK != null) {
            fingerSDK.release();
            System.out.println("Finger SDK released");
        }
        super.onDestroy();
    }

    private void showDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressDialogUtils.showProgressDialog(TimeEntryRegister.this, "please press finger...");
            }
        });
    }

    private void showUserProfile(User user) {

        AnnouncementRepository repository = new AnnouncementRepository(TimeEntryRegister.this);
        AnnouncementModel[] announcements = repository.getAnnouncements(Long.parseLong(user.getUserId()));

        System.out.println("The announcements are:" + announcements);

        adjustGreeting(user);
        welcomeDialog.show();
        allowCapture = false;
        hasPreview = true;

        RecyclerView announcementList = welcomeDialog.findViewById(R.id.announcement_list);
        announcementList.setLayoutManager(new LinearLayoutManager(this));
        AnnouncementAdapter adapter = new AnnouncementAdapter(Arrays.asList(announcements));
        announcementList.setAdapter(adapter);


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
        if (!allowCapture || hasPreview) {
            return;
        }
        fingerSDK.clear();
        fingerSDK.captureBytes(FingerSDK.TEMPLEATES.valueOf("ISO_19794_2_2011"), new OnCaptureBytesListener() {
            @Override
            public void capture(int i, byte[] bytes, Bitmap bitmap, byte[] temp) {
                Log.d("FingerPrintScanActivity", "capture here: " + i);

                if(!allowCapture) {
                    return;
                }

                if (i == FingerSDK.RESULT_OK) {
                    try {
                        String tempString = new String(temp, "ISO8859-1");
                        for(Fingerprint fingerprint: fingerprints) {
                            int score = fingerSDK.compareTemplateBytes(FingerSDK.TEMPLEATES.valueOf("ISO_19794_2_2011"),tempString.getBytes("ISO8859-1"),fingerprint.getKey().getBytes("ISO8859-1"));
                            Log.d("TimeEntryRegister", "Score: " + score);
                            if (score > 85) {
                                allowCapture = false;

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        biometric = biometricRepository.getBiometricById(fingerprint.getBiometricId());
                                        User user = dbHelper.getUserById(""+biometric.getUserId());

                                        if(isTimeRegisterOn) {
                                            Toast.makeText(TimeEntryRegister.this, "Welcome Employee", Toast.LENGTH_SHORT).show();
                                            welcomeText.setText("Welcome, ".concat( user.getDisplayName()).concat(" 👋"));
                                            adjustGreeting(user);
                                            showClockLayout();
                                        }else{
                                            if (hasPreview) {
                                                return;
                                            }
                                            long timeId = timeRepository.insertTimeEntry(biometric.getUserId(),null);
                                            if (timeId > 0) {
                                                /*Toast.makeText(TimeEntryRegister.this, "Time Recorded", Toast.LENGTH_SHORT).show();
                                                SweetAlertDialog dialog = new SweetAlertDialog(TimeEntryRegister.this, SweetAlertDialog.SUCCESS_TYPE)
                                                        .setTitleText("Time Entry Recorded");
                                                dialog.show();*/
                                                TextToSpeechUtil.say(getApplicationContext(), "Welcome "+ user.getDisplayName());
                                                showUserProfile(user);
                                                showScanLayout();
                                            } else {
                                                Toast.makeText(TimeEntryRegister.this, "Failed to record time, Please try again.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                }, 100);

                                return;
                            }
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(TimeEntryRegister.this, "Unknown fingerprint", Toast.LENGTH_SHORT).show();
                                showScanLayout();
                                hostCapture();
                            }
                        }, 100);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        Log.d("FingerPrintScanActivity", "UnsupportedEncodingException: " + e.getMessage());
                    }
                } else {
                    Log.d("FingerPrintScanActivity", "capture failed: " + i);

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            allowCapture = true;
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

    private void showClockLayout() {
        clockLayout.setVisibility(View.VISIBLE);
        scanLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if(!allowCapture || hasPreview) {
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String id = Conversion.Bytes2HexString(tag.getId());


        biometric = dbHelper.getRfidByKey(id);
        if (biometric!= null) {
            User user = dbHelper.getUserById(""+biometric.getUserId());

            TextToSpeechUtil.say(getApplicationContext(), "Welcome "+ user.getDisplayName());
            Toast.makeText(this, "Welcome Employee", Toast.LENGTH_SHORT).show();
            welcomeText.setText("Welcome, ".concat( user.getDisplayName()).concat(" 👋"));
            if(isTimeRegisterOn) {
                adjustGreeting(user);

                showClockLayout();
            }else{
                if (hasPreview) {
                    return;
                }
                long timeId = timeRepository.insertTimeEntry(biometric.getUserId(),null);
                if (timeId > 0) {
                    showUserProfile(user);
                    showScanLayout();
                    welcomeText.setText("Time Register");
                } else {
                    Toast.makeText(TimeEntryRegister.this, "Failed to clock out, Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            showScanLayout();
            welcomeText.setText("Time Register");
        }
    }

    @Override
    public void onPictureTaken(CameraPreviewData cameraPreviewData) {
        mFeedFrameQueue.offer(cameraPreviewData);
    }

    private void showFaceSignWindowAndRecord(facex.facepass.db.User user, String type, long trackId) {
        if (isFinishing()) {
            return;
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMessage(getString(R.string.hello)+" "+user.name);
        mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(faceView!=null){
                    faceView.setFaceRectNormal();
                }
                getWindow().getDecorView().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mFacePassHandler != null) {
                            mFacePassHandler.setMessage(trackId, 0);
                        }
                    }
                }, 2000);
            }
        });
        mProgressDialog.show();
        TextToSpeechUtil.say(getApplicationContext(), getString(R.string.hello) +" "+ user.name);
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
                if(!allowCapture || hasPreview) {
                    return;
                }
                BiometricRepository biometricRepository = new BiometricRepository(TimeEntryRegister.this);
                biometric = biometricRepository.findFaceBiometricByKey(faceToken);

                Log.d("ScanFace", "findUserByFaceTokenBiometric: " + biometric);

                if(biometric != null) {
                    User user = dbHelper.getUserById(""+biometric.getUserId());
                    TextToSpeechUtil.say(getApplicationContext(), "Welcome "+ user.getDisplayName());

                    if(isTimeRegisterOn) {
                        Toast.makeText(TimeEntryRegister.this, "Welcome Employee", Toast.LENGTH_SHORT).show();
                        welcomeText.setText("Welcome, ".concat( user.getDisplayName()).concat(" 👋"));
                        adjustGreeting(user);
                        showClockLayout();
                    }else{
                        if (hasPreview) {
                            return;
                        }
                        long timeId = timeRepository.insertTimeEntry(biometric.getUserId(),null);
                        if (timeId > 0) {
                            showUserProfile(user);
                            showScanLayout();
                        } else {
                            Toast.makeText(TimeEntryRegister.this, "Failed to record time, Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }else{
                    Toast.makeText(TimeEntryRegister.this,"No biometric found",Toast.LENGTH_SHORT).show();
                }

            }
        });
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


    private class FeedFrameThread extends Thread {
        boolean isInterrupt = false;

        long lastFeedTime = 0;

        @Override
        public void run() {
            while (!isInterrupt) {
                if(ignoreThreadWhile){
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
                    /* 当前帧没有检出人脸 */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            faceView.clear();
                            faceView.invalidate();
                        }
                    });
                } else {
                    /* 将识别到的人脸在预览界面中圈出，并在上方显示人脸位置及角度信息 */
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
                    /*送识别的人脸框的属性信息*/
                    FacePassTrackOptions[] trackOpts = new FacePassTrackOptions[detectionResult.images.length];
                    for (int i = 0; i < detectionResult.images.length; ++i) {
                        if (detectionResult.images[i].rcAttr.respiratorType != FacePassRCAttribute.FacePassRespiratorType.INVALID
                                && detectionResult.images[i].rcAttr.respiratorType != FacePassRCAttribute.FacePassRespiratorType.NO_RESPIRATOR) {
                            float searchThreshold = 60f;
                            float livenessThreshold = 80f; // -1.0f will not change the liveness threshold
                            float livenessGaThreshold = 85f;
                            float smallsearchThreshold = -1.0f; // -1.0f will not change the smallsearch threshold
                            trackOpts[i] = new FacePassTrackOptions(detectionResult.images[i].trackId, searchThreshold, livenessThreshold, livenessGaThreshold, smallsearchThreshold);
                        }
                    }
                    if(System.currentTimeMillis()-lastFeedTime>500) {
                        RecognizeData mRecData = new RecognizeData(detectionResult.message, trackOpts);
                        Log.e("ScanFace", "make face data ready");
                        mRecognizeDataQueue.offer(mRecData);
                        Log.e("ScanFace", "send face data to recognize queue");
                        lastFeedTime = System.currentTimeMillis();
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
                if(ignoreThreadWhile){
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
                                                TextToSpeechUtil.say(getApplicationContext(), getString(R.string.stranger));
                                            }
                                        });
                                    }
                                }
                            }
                        }else if(recognizeResultArray!=null && recognizeResultArray.length ==0){
                            if (System.currentTimeMillis() - lastStrangerFindTime > 3000) {
                                lastStrangerFindTime = System.currentTimeMillis();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        faceView.setFaceRectError();
                                        TextToSpeechUtil.say(getApplicationContext(), getString(R.string.stranger));
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
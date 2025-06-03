package com.example.bms;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsCompat;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bms.databinding.ActivityFingerPrintScanBinding;
import com.fgtit.data.wsq;
import com.fgtit.data.Conversions;
import com.fgtit.fpcore.FPMatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import cn.pedant.SweetAlert.SweetAlertDialog;
import doorx.utils.ProgressDialogUtils;
import facex.utils.TextToSpeechUtil;
import fgtit.fpengine.constants;
import fgtit.fpengine.fpdevice;


public class FingerPrintScanActivity extends AppCompatActivity {

    private ActivityFingerPrintScanBinding binding;

    ImageView[] fingerImageViews = new ImageView[10];
    Button scan_finger, save_finger;

    private int currentFingerIndex = 0;

    private HashMap<Integer, String> fingerDataMap = new HashMap<>();

    private Toolbar toolbarHead;

    private List<Fingerprint> fingerprints;

    private static fpdevice fpdev = new fpdevice();

    private static boolean isopening = false;
    private static boolean isworking = false;

    private int mWorkmode = 0;

    private static byte bmpdata[] = new byte[74806];
    private static int bmpsize[] = new int[1];

    private static byte fpdata[] = new byte[512];
    private static int fpsize[] = new int[1];

    private Timer mTimer = null;
    private TimerTask mTimerTask = null;

    public static int mRefCount = 0;

    private static String sDirectory = "";


    private TextView tvStatus;

    private static byte isodata[] = new byte[512];

    public static byte mRefList[][] = new byte[2048][512];  


    public void CreateDirectory() {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            sDirectory = Environment.getExternalStorageDirectory() + "/FingerprintReader";
            File destDir = new File(sDirectory);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
        }
    }

    public static void SaveWsqFile(byte[] rawdata, int rawsize, String filename) {
        byte[] outdata = new byte[73728];
        int[] outsize = new int[1];
        wsq.getInstance().RawToWsq(rawdata, rawsize, 256, 288, outdata, outsize, 2.833755f);
        try {
            File fs = new File(sDirectory + "/" + filename);
            if (fs.exists()) {
                fs.delete();
            }
            new File(sDirectory + "/" + filename);
            RandomAccessFile randomFile = new RandomAccessFile(sDirectory + "/" + filename, "rw");
            long fileLength = randomFile.length();
            randomFile.seek(fileLength);
            randomFile.write(outdata, 0, outsize[0]);
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void SavePngFile(Bitmap bmp, String filename) {
        if (bmp == null) {
            Log.e("FingerPrintScanActivity", "SavePngFile: Bitmap is null, not saving PNG.");
            return;
        }
        File f = new File(sDirectory + "/" + filename);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (IOException e) {
        }
    }

    public static void ConverTemplate(byte[] reftp, int refsize, byte[] isotp) {
        byte mTmpCoord[] = new byte[512];
        byte mTmpData[] = new byte[512];

        switch (Conversions.getInstance().GetDataType(reftp)) {
            case 1: {
                //STD
                Conversions.getInstance().StdChangeCoord(reftp, 256, mTmpCoord, 1);
                Conversions.getInstance().StdToIso(2, mTmpCoord, isotp);
                //String bsiso=Base64.encodeToString(isotp,0,378,Base64.DEFAULT);
                //mEditText.setText(bsiso);
            }
            break;
            case 2: {
                //ISO 1
                Conversions.getInstance().IsoToStd(1, reftp, mTmpData);
                Conversions.getInstance().StdChangeCoord(mTmpData, 256, mTmpCoord, 1);
                Conversions.getInstance().StdToIso(2, mTmpCoord, isotp);
                //String bsiso=Base64.encodeToString(isotp,0,378,Base64.DEFAULT);
                //mEditText.setText(bsiso);
            }
            break;
            case 3: {
                //ISO 2
                System.arraycopy(reftp, 0, isotp, 0, refsize);
            }
            break;
        }
    }

    // Using a static handler class with WeakReference to prevent memory leaks
    private static class FingerPrintHandler extends Handler {
        private final WeakReference<FingerPrintScanActivity> activityReference;

        public FingerPrintHandler(FingerPrintScanActivity activity) {
            super(android.os.Looper.getMainLooper());
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FingerPrintScanActivity activity = activityReference.get();
            Log.d("FingerPrintScanActivity", "handleMessage: " + msg.what);

            if (activity.tvStatus == null) {
                activity.tvStatus = activity.findViewById(R.id.tvStatus);
            }

            if (activity == null) {
                // Activity has been garbage collected, no need to process message
                return;
            }

            switch (msg.what) {
                case 1: {
                    int work = activity.fpdev.GetWorkMsg();
                    int ret = activity.fpdev.GetRetMsg();

                    Log.d("FingerPrintScanActivity", "work: " + work + ", ret: " + ret + "mWorkmode: " + activity.mWorkmode);
                    switch (work) {
                        case constants.FPM_DEVICE:
                            Log.d("FingerPrintScanActivity", "Please Open Device");
                            break;
                        case constants.FPM_PLACE:
                            Log.d("FingerPrintScanActivity", "Place Finger");
                            if (activity.tvStatus != null) {
                                activity.tvStatus.setText("Place Finger");
                            }
                            break;
                        case constants.FPM_LIFT:
                            Log.d("FingerPrintScanActivity", "Lift Finger");
                            activity.tvStatus.setText("Please hold finger still");
                            break;
                        case constants.FPM_GENCHAR: {
                            activity.TimerStop();
                            activity.isworking = false;
                            if (ret == 1) {
                                switch (activity.mWorkmode) {
                                    case 1: { // enrol
                                        // Implementation omitted for brevity as it references undefined variables
                                        // This would need to be properly implemented with the activity reference
                                        Bitmap bm1 = BitmapFactory.decodeByteArray(bmpdata, 0, 74806);
                                        Log.d("FingerPrintScanActivity", "bm1: " + bm1);
                                        Log.d("FingerPrintScanActivity", "bmpdata: " + Arrays.toString(bmpdata));

                                        // Check fingerprint image quality
                                        if (!activity.isFingerprintImageClear(bm1)) {
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                                        .setTitleText("Image Quality Error")
                                                        .setContentText("Fingerprint image is too blurry or incomplete. Please try again.")
                                                        .show();
                                                }
                                            });
                                            activity.tvStatus.setText("Image quality too low. Please scan again.");
                                            return;
                                        }

                                        // SavePngFile(bm1, String.valueOf(mRefCount + 1) + ".png");                                        //WSQ
                                        byte[] inpdata = new byte[73728];
                                        int inpsize = 73728;
                                        try {
                                            // The 1078 offset is the BMP header size - ensure it's correct for your specific format
                                            System.arraycopy(bmpdata, 1078, inpdata, 0, inpsize);
                                            
                                            // Apply image enhancement if needed
                                            // You could add image processing here to enhance fingerprint quality
                                            
                                            Log.d("FingerPrintScanActivity", "Raw data copied successfully, size: " + inpsize);
                                        } catch (Exception e) {
                                            Log.e("FingerPrintScanActivity", "Error in data extraction: " + e.getMessage());
                                        }

                                        // Get fingerprint template
                                        fpdev.GetTemplateByGen(fpdata, fpsize);

                                        ConverTemplate(fpdata, fpsize[0], isodata);
                                        // Always copy isodata before encoding/storing
                                        byte[] isodataCopy = Arrays.copyOf(isodata, isodata.length);
                                        String base64String = Base64.encodeToString(isodataCopy, 0, 512, Base64.DEFAULT);
                // String bsiso=Base64.encodeToString(isotp,0,378,Base64.DEFAULT);

                                        if (!activity.validateFingerPrint(base64String)) {
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE).setTitleText("Error").setContentText("Fingerprint already exists").show();
                                                }
                                            });

                                            return;
                                        }

                                        // SaveWsqFile(inpdata, inpsize, String.valueOf(mRefCount + 1) + ".wsq");

                                        activity.updateFingerBitmap(bm1);
                                        activity.tvStatus.setText("Enrol Template OK");

                                        activity.fingerDataMap.put(activity.currentFingerIndex, base64String);
                                        Log.d("FingerPrintScanActivity", "Base64 String: " + base64String);

                                        if (mRefCount < 2048) {
                                            System.arraycopy(isodataCopy, 0, mRefList[mRefCount], 0, 512);
                                            mRefCount++;
                                            activity.tvStatus.setText("Enroll OK:" + String.valueOf(mRefCount));
                                        }

                                    }
                                    break;
                                }
                            } else {
                                 activity.tvStatus.setText("Fail");
                            }
                        }
                        if (activity.isContinuous) {
                            activity.NextMatch();
                        }
                        break;
                        case constants.FPM_NEWIMAGE: {
                            // Implementation omitted for brevity as it references undefined variables
                            // This would need to be properly implemented with the activity reference
                            fpdev.GetBmpImage(bmpdata, bmpsize);
                            activity.tvStatus.setText("Failed: Please press Scan again.");
                        }
                        break;
                        case constants.FPM_TIMEOUT:
                            activity.tvStatus.setText("Time Out");
                            activity.isworking = false;
                            break;
                    }
                }
                break;
            }
            super.handleMessage(msg);
        }
    }

    private Handler handler = null;

    // Method to stop the timer
    private void TimerStop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }


    // Variables needed for the handler implementation
    private boolean isContinuous = false;

    private void NextMatch() {
        // Implementation would go here
    }

    private void TimerStart() {
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
        }
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
                Log.d("FingerPrintScanActivity", "TimerTask run: " + message.what);
            }
        };
        if (mTimer != null && mTimerTask != null) mTimer.schedule(mTimerTask, 200, 200);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRefCount = 0;

        tvStatus = findViewById(R.id.tvStatus);
        CreateDirectory();

        binding = ActivityFingerPrintScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fpdev.SetInstance(this);

        // Initialize the handler using the static inner class
        handler = new FingerPrintHandler(this);

        FingerprintRepository fingerprintRepository = new FingerprintRepository(this);
        fingerprints = fingerprintRepository.getAllFingerprints();

        Objects.requireNonNull(getWindow().getInsetsController()).hide(WindowInsetsCompat.Type.systemBars());

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

        scan_finger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("FingerPrintScanActivity", "scan_finger clicked");

                if (areAllFingersScanned()) {
                    Toast.makeText(FingerPrintScanActivity.this, "All fingers have been scanned", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isopening) {
                    if (isworking) return;
                    mWorkmode = 1;
                    TimerStart();
                    fpdev.GenerateTemplate();
                    isworking = true;
                }
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

        findViewById(R.id.reset_button_6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(5);
            }
        });

        findViewById(R.id.reset_button_7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(6);
            }
        });

        findViewById(R.id.reset_button_8).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(7);
            }
        });

        findViewById(R.id.reset_button_9).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(8);
            }
        });

        findViewById(R.id.reset_button_10).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFingerImage(9);
            }
        });

    }

    public boolean checkPermission() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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

        fpdev.CloseDevice();
        isopening = false;

        switch(fpdev.OpenDevice()) {
            case 0:
                isopening = true;
                Log.d("ScanFace", "OpenDevice: Success");
                break;
            case -1:
                Log.d("ScanFace", "OpenDevice: Link Device Fail");
                break;
            case -2:
                Log.d("ScanFace", "OpenDevice: Evaluation version expires");
                break;
            case -3:
                Log.d("ScanFace", "OpenDevice: Open Device Fail");
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        // Stop any running timers and the fingerprint device when the activity is destroyed
        TimerStop();
        fpdev.CloseDevice();
        super.onDestroy();
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
                } else {
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
            fingerDataMap.remove(index);
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

    // Fingerprint image quality check: returns true if image is clear and complete
    private boolean isFingerprintImageClear(Bitmap bitmap) {
        if (bitmap == null) return false;

        // 1. Blurriness check using variance of Laplacian
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Convert to grayscale
        int[] gray = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            gray[i] = (r + g + b) / 3;
        }

        // Compute Laplacian variance
        double sum = 0, sumSq = 0;
        int count = 0;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int idx = y * width + x;
                int lap = 4 * gray[idx] - gray[idx - 1] - gray[idx + 1] - gray[idx - width] - gray[idx + width];
                sum += lap;
                sumSq += lap * lap;
                count++;
            }
        }
        double mean = sum / count;
        double variance = (sumSq / count) - (mean * mean);

        // 2. Coverage check: percent of non-white pixels
        int nonWhite = 0;
        for (int i = 0; i < gray.length; i++) {
            if (gray[i] < 240) nonWhite++;
        }
        double coverage = (double) nonWhite / gray.length;

        // Thresholds (tune as needed)
        boolean notBlurry = variance > 200; // adjust threshold as needed
        boolean enoughCoverage = coverage > 0.25; // at least 25% of area is fingerprint

        Log.d("FingerPrintScanActivity", "Laplacian variance: " + variance + ", coverage: " + coverage);

        return notBlurry && enoughCoverage;
    }

    public int MatchTemplate(byte[] reftp, int refsize, byte[] mattp, int matsize) {
        if (reftp == null || mattp == null || refsize <= 0 || matsize <= 0) {
            Log.e("Fingerprint", "Invalid template data for matching");
            return 0; // Return 0 score for invalid data
        }
        
        byte[] refbuf = new byte[512];
        byte[] matbuf = new byte[512];
        
        try {
            // Convert reference template to standard format
            switch(Conversions.getInstance().GetDataType(reftp)){
                case 1:{	//STD
                    System.arraycopy(reftp, 0, refbuf, 0, Math.min(refsize, refbuf.length));
                }
                break;
                case 2:{	//ISO 1
                    Conversions.getInstance().IsoToStd(1, reftp, refbuf);
                }
                break;
                case 3:{	//ISO 2
                    Conversions.getInstance().IsoToStd(2, reftp, refbuf);
                }
                break;
            }
            
            // Convert match template to standard format
            switch(Conversions.getInstance().GetDataType(mattp)){
                case 1:{	//STD
                    System.arraycopy(mattp, 0, matbuf, 0, Math.min(matsize, matbuf.length));
                }
                break;
                case 2:{	//ISO 1
                    Conversions.getInstance().IsoToStd(1, mattp, matbuf);
                }
                break;
                case 3:{	//ISO 2
                    Conversions.getInstance().IsoToStd(2, mattp, matbuf);
                }
                break;
            }
            
            // Apply multiple matching attempts with slight rotations for better accuracy
            int maxScore = 0;
            
            // Original match
            int mret = FPMatch.getInstance().MatchTemplate(refbuf, matbuf);
            maxScore = mret;
            
            // The code below would perform multiple match attempts with rotations,
            // but would require custom implementation to rotate the templates
            // This is a placeholder for potential future improvement
            
            Log.d("Fingerprint", "Match score: " + maxScore);
            return maxScore;
        } catch (Exception e) {
            Log.e("Fingerprint", "Error in template matching: " + e.getMessage());
            return 0;
        }
    }

    private int fingerprintScoreThreshold() {
        SharedPreferences sharedPreferences = getSharedPreferences("device_settings", Context.MODE_PRIVATE);
        String fingerprintScoreThreshold = sharedPreferences.getString("FINGERPRINT_SCORE_THRESHOLD", "70");
        // Lower threshold means more permissive matching (60-70 may be better for reliability)
        return Integer.parseInt(fingerprintScoreThreshold);
    }

    private boolean validateFingerPrint(String tempString) {
        int threshold = fingerprintScoreThreshold();
        Log.d("Fingerprint", "Using threshold: " + threshold);
        
        // If no fingerprints to compare against, return true (new fingerprint)
        if (fingerprints == null || fingerprints.isEmpty()) {
            Log.d("Fingerprint", "No stored fingerprints to compare against");
            return true;
        }

        int highestScore = 0;
        String matchedFingerprint = null;
        
        for (Fingerprint fingerprint : fingerprints) {
            if(fingerprint.getKey() == null) continue;
            
            try {
                byte[] storedFingerprint = Base64.decode(fingerprint.getKey(), Base64.DEFAULT);
                byte[] fingerPrint = Base64.decode(tempString, Base64.DEFAULT);

                int score = MatchTemplate(storedFingerprint, 512, fingerPrint, 512);
                
                Log.d("Fingerprint", "Score against fingerprint " + fingerprint.getId() + ": " + score);
                
                if (score > highestScore) {
                    highestScore = score;
                    matchedFingerprint = String.valueOf(fingerprint.getId());
                }
                
                // If we find a match above the threshold, reject this as a duplicate
                if (score > threshold) {
                    Log.d("Fingerprint", "Match found! Score: " + score + " > Threshold: " + threshold);
                    return false;
                }
            } catch (Exception e) {
                Log.e("Fingerprint", "Error matching fingerprint: " + e.getMessage());
            }
        }
        Log.d("Fingerprint", "Highest score: " + highestScore + " (Threshold: " + threshold + ")");

        return matchedFingerprint == null || highestScore < 10;
    }

    private void hostCapture() {
        if (areAllFingersScanned()) {
            Toast.makeText(this, "All fingers have been scanned", Toast.LENGTH_SHORT).show();
            return;
        }

        setActiveFinger(currentFingerIndex);

        showDialog();
    }
}

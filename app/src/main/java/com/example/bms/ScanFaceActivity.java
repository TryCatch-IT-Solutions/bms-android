package com.example.bms;

import static facex.facepass.InitFacePassHandler.group_name;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import facex.facepass.FaceView;
import facex.facepass.InitFacePassHandler;
import facex.facepass.RecognizeData;
import facex.facepass.camera.CameraActivity;
import facex.facepass.camera.CameraManager;
import facex.facepass.camera.CameraPreview;
import facex.facepass.camera.CameraPreviewData;
import facex.facepass.camera.CameraSettingActivity;
import facex.facepass.db.User;
import facex.greendao.gen.UserDao;
import facex.utils.TextToSpeechUtil;

import com.example.bms.databinding.ActivityScanFaceBinding;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnInputConfirmListener;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import mcv.facepass.FacePassException;
import mcv.facepass.FacePassHandler;
import mcv.facepass.types.FacePassAddFaceResult;
import mcv.facepass.types.FacePassAgeGenderResult;
import mcv.facepass.types.FacePassDetectionResult;
import mcv.facepass.types.FacePassFace;
import mcv.facepass.types.FacePassImage;
import mcv.facepass.types.FacePassImageType;
import mcv.facepass.types.FacePassRCAttribute;
import mcv.facepass.types.FacePassRecognitionResult;
import mcv.facepass.types.FacePassRecognitionState;
import mcv.facepass.types.FacePassTrackOptions;

public class ScanFaceActivity extends CameraSettingActivity implements CameraManager.CameraListener {
    ActivityScanFaceBinding scanFaceBinding;

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

    private ProgressDialog mProgressDialog;

    boolean ignoreThreadWhile = false;

    private String signName= "";


    private void initView() {
        scanFaceBinding.actionbar.backTitleTextStyle(getString(R.string.face), getString(R.string.enroll), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new XPopup.Builder(ScanFaceActivity.this).asInputConfirm(getString(R.string.user_name), "", new OnInputConfirmListener() {
                    @Override
                    public void onConfirm(String text) {
                        if(!text.isEmpty()){
                            signName= text;
                            resultLauncher.launch(new Intent(ScanFaceActivity.this, CameraActivity.class));
                        }
                    }
                }).show();
            }
        });
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == 200) {
                    Intent intent = result.getData();
                    String path = intent.getStringExtra("data");

                    mProgressDialog.setMessage(getString(R.string.loading));
                    mProgressDialog.show();
                    InitFacePassHandler.init(ScanFaceActivity.this, new InitFacePassHandler.IFacePassInit() {
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
                                                        Toast.makeText(ScanFaceActivity.this,getString(R.string.failed),Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                return;
                                            }
                                            User user = new User();
                                            user.id = System.currentTimeMillis();
                                            user.faceToken = new String(result.faceToken, StandardCharsets.ISO_8859_1);
                                            user.name = signName;
                                            ((App)getApplication()).getUserDao().insert(user);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(ScanFaceActivity.this,getString(R.string.success),Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else if (result.result == 1) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(ScanFaceActivity.this,"no face ！",Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(ScanFaceActivity.this,"quality problem！",Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(ScanFaceActivity.this,"face chek failed！",Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                } catch (FacePassException e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ScanFaceActivity.this,getString(R.string.failed),Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ScanFaceActivity.this,"Error Face Handler: 1",Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            }
                        }
                    });
                }
            });

    @Override
    public void onCreateBase(Bundle bundle) {
        scanFaceBinding = ActivityScanFaceBinding.inflate(getLayoutInflater());
        setContentView(scanFaceBinding.getRoot());

        mProgressDialog = new ProgressDialog(this);

        cameraView = scanFaceBinding.preview;
        faceView = scanFaceBinding.fcview;

        manager = new CameraManager();
        manager.setPreviewDisplay(cameraView);
        manager.setListener(this);
        CameraSetting();

        initView();

        mProgressDialog.setMessage(getString(R.string.loading));
        mProgressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                mRecognizeDataQueue = new ArrayBlockingQueue<RecognizeData>(5);
                mFeedFrameQueue = new ArrayBlockingQueue<CameraPreviewData>(1);
                InitFacePassHandler.init(ScanFaceActivity.this, new InitFacePassHandler.IFacePassInit() {
                    @Override
                    public void result(FacePassHandler facePassHandler) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressDialog.dismiss();
                            }
                        });
                        if (facePassHandler != null) {
                            ScanFaceActivity.this.mFacePassHandler = facePassHandler;
                            checkGroup();

                            mRecognizeThread = new RecognizeThread();
                            mRecognizeThread.start();
                            mFeedFrameThread = new FeedFrameThread();
                            mFeedFrameThread.start();
                        }else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ScanFaceActivity.this,"Error Face Handler: 2",Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                        }
                    }
                });
            }
        }).start();

    }

    @Override
    public void onRestart() {
        super.onRestart();
        faceView.clear();
        faceView.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        manager.open(getWindowManager(), cameraFront, RelativeLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ignoreThreadWhile = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        ignoreThreadWhile = true;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
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
        super.onDestroy();
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
                                        Log.d("ScanFace", "faceTokens:" + Arrays.toString(result.faceToken));
                                        String faceToken = new String(result.faceToken, StandardCharsets.ISO_8859_1);
                                        Log.e("ScanFace", "recognize a face with faceToken：" + faceToken);
                                        if (FacePassRecognitionState.RECOGNITION_PASS == result.recognitionState) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    faceView.setFaceRectUser();
                                                }
                                            });
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
                        Toast.makeText(ScanFaceActivity.this,group_name + " " + getString(R.string.failed),Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(ScanFaceActivity.this,group_name + " " + getString(R.string.failed),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (FacePassException e) {
            e.printStackTrace();
        }
    }

    /* 相机回调函数 */
    @Override
    public void onPictureTaken(CameraPreviewData cameraPreviewData) {
        mFeedFrameQueue.offer(cameraPreviewData);
    }

    private void findUserByFaceToken(String faceToken, long trackId) {
        if (TextUtils.isEmpty(faceToken)) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<User> userList = ((App) getApplication()).getUserDao().queryBuilder().where(UserDao.Properties.FaceToken.eq(faceToken)).list();
                if (userList.size() > 0) {
                    try {
                        showFaceSignWindowAndRecord(userList.get(0), getString(R.string.face), trackId);
                    } catch (NullPointerException e) {

                    }
                } else {
                    Toast.makeText(ScanFaceActivity.this,group_name + " " + getString(R.string.failed),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showFaceSignWindowAndRecord(User user, String type, long trackId) {
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

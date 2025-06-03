package facex.facepass;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import mcv.facepass.FacePassException;
import mcv.facepass.FacePassHandler;
import mcv.facepass.auth.AuthApi.AuthApi;
import mcv.facepass.auth.AuthApi.AuthApplyResponse;
import mcv.facepass.auth.AuthApi.ErrorCodeConfig;
import mcv.facepass.types.FacePassConfig;
import mcv.facepass.types.FacePassModel;
import mcv.facepass.types.FacePassPose;

public class InitFacePassHandler {
    public static final String CERT_PATH = "Download/CBG_Android_Face_Reco---30-Trial-one-stage.cert";
    public static final String group_name = "facex_face";
    private static FacePassHandler mFacePassHandler;
    private static FacePassHandler mFacePassHandler2;

    public interface IFacePassInit {
        void result(FacePassHandler facePassHandler);
    }

    public interface ICert {
        void result(boolean s);
    }

    public static boolean sdkMax30(){
        if(Build.VERSION.SDK_INT>30){
            return false;
        }else {
            return true;
        }
    }

    public static boolean checkChipOrAuthFileExist(Context context){
        Context mContext = context.getApplicationContext();
        FacePassHandler.initSDK(mContext);
        if(FacePassHandler.isAuthorized()){
            return true;
        }
        // 金雅拓授权接口
        boolean auth_status = FacePassHandler.authCheck();
        if(!auth_status){
            String cert = FileUtil.readExternal(CERT_PATH).trim();
            if (TextUtils.isEmpty(cert)) {
                return false;
            }else {
                return true;
            }
        }else {
            return true;
        }
    }

    private static void initFacePassSDK(Context activityOrServer, ICert iCert) {
        Context mContext = activityOrServer.getApplicationContext();
        FacePassHandler.initSDK(mContext);
        if(FacePassHandler.isAuthorized()){
            iCert.result(true);
            return;
        }
        // 金雅拓授权接口
        boolean auth_status = FacePassHandler.authCheck();
        if (!auth_status) {
            singleCertification(mContext, new ICert() {
                @Override
                public void result(boolean s) {
                    if (s) {
                        iCert.result(true);
                    } else {
                        iCert.result(false);
                    }
                }
            });
        } else {
            iCert.result(true);
        }
    }

    private static void singleCertification(Context mContext, ICert iCert) {
        String cert = FileUtil.readExternal(CERT_PATH).trim();
        if (TextUtils.isEmpty(cert)) {
            Log.d("mcvsafe", "cert is null");
            iCert.result(false);
            return;
        }
        final AuthApplyResponse[] resp = {new AuthApplyResponse()};
        FacePassHandler.authDevice(mContext.getApplicationContext(), cert, "", new AuthApi.AuthDeviceCallBack() {
            @Override
            public void GetAuthDeviceResult(AuthApplyResponse result) {
                resp[0] = result;
                if (resp[0].errorCode == ErrorCodeConfig.AUTH_SUCCESS) {
                    iCert.result(true);
                } else {
                    iCert.result(false);
                }
            }
        });
    }

    public static void release() {
        if (mFacePassHandler != null) {
            mFacePassHandler.release();
        }
        if (mFacePassHandler2 !=null){
            mFacePassHandler2.release();
        }
        mFacePassHandler = null;
        mFacePassHandler2 = null;
    }

    public static void init(Activity activity, IFacePassInit iFacePassInit) {
        if (mFacePassHandler != null) {
            iFacePassInit.result(mFacePassHandler);
            return;
        }
        Log.d("mcvsafe", "initFacePassSDK");
        initFacePassSDK(activity, new ICert() {
            @Override
            public void result(boolean s) {
                Log.d("mcvsafe", "The Result: " + s);
                if (s) {
                    new Thread() {
                        @Override
                        public void run() {
                            while (true && !activity.isFinishing()) {
                                while (FacePassHandler.isAvailable()) {
                                    FacePassConfig config;
                                    try {
                                        /* use bin file */
                                        config = new FacePassConfig();
                                        config.poseBlurModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "attr.pose_blur.arm.190630.bin");

                                        config.livenessModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "liveness.CPU.rgb.G.bin");

                                        config.searchModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "feat2.arm.K.v1.0_1core.bin");

                                        config.detectModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "detector.arm.G.bin");
                                        config.detectRectModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "detector_rect.arm.G.bin");
                                        config.landmarkModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "pf.lmk.arm.E.bin");

                                        config.rcAttributeModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "attr.RC.arm.G.bin");
                                        config.occlusionFilterModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "attr.occlusion.arm.20201209.bin");

                                        /* set value of algorithm */
                                        config.rcAttributeAndOcclusionMode = 1;
                                        config.searchThreshold = 65f;
                                        config.livenessThreshold = 80f;
                                        config.livenessGaThreshold = 85f;

                                        config.livenessEnabled = true;
                                        config.rgbIrLivenessEnabled = false;

                                        config.poseThreshold = new FacePassPose(35f, 35f, 35f);
                                        config.blurThreshold = 0.8f;
                                        config.lowBrightnessThreshold = 30f;
                                        config.highBrightnessThreshold = 210f;
                                        config.brightnessSTDThreshold = 80f;
                                        config.faceMinThreshold = 60;
                                        config.retryCount = 10;
                                        config.smileEnabled = false;
                                        config.maxFaceEnabled = true;
                                        config.fileRootPath = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

                                        /* build FacePssHandler */
                                        mFacePassHandler = new FacePassHandler(config);

                                        /* set value of algorithm */
                                        FacePassConfig addFaceConfig = mFacePassHandler.getAddFaceConfig();
                                        addFaceConfig.poseThreshold.pitch = 35f;
                                        addFaceConfig.poseThreshold.roll = 35f;
                                        addFaceConfig.poseThreshold.yaw = 35f;
                                        addFaceConfig.blurThreshold = 0.7f;
                                        addFaceConfig.lowBrightnessThreshold = 70f;
                                        addFaceConfig.highBrightnessThreshold = 220f;
                                        addFaceConfig.brightnessSTDThresholdLow = 14.14f;
                                        addFaceConfig.brightnessSTDThreshold = 63.25f;
                                        addFaceConfig.faceMinThreshold = 100;
                                        addFaceConfig.rcAttributeAndOcclusionMode = 2;
                                        mFacePassHandler.setAddFaceConfig(addFaceConfig);

                                        boolean contain = false;
                                        if(mFacePassHandler.getLocalGroups()!=null) {
                                            for (String g : mFacePassHandler.getLocalGroups()) {
                                                if (g.equals(group_name)) {
                                                    contain = true;
                                                }
                                            }
                                        }
                                        if (!contain) {
                                            mFacePassHandler.createLocalGroup(group_name);
                                        }
                                        mFacePassHandler.initLocalGroup(group_name);

                                        iFacePassInit.result(mFacePassHandler);
                                    } catch (FacePassException e) {
                                        Log.d("mcvsafe", "The Exception: " + e.getMessage());
                                        e.printStackTrace();
                                        iFacePassInit.result(null);
                                        return;
                                    }
                                    return;
                                }
                                try {
                                    /* 如果SDK初始化未完成则需等待 */
                                    sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }.start();
                } else {
                    iFacePassInit.result(null);
                }
            }
        });
    }

    public static void initDUALCAM(Activity activity, IFacePassInit iFacePassInit) {
        if (mFacePassHandler2 != null) {
            iFacePassInit.result(mFacePassHandler2);
            return;
        }
        initFacePassSDK(activity, new ICert() {
            @Override
            public void result(boolean s) {
                if (s) {
                    new Thread() {
                        @Override
                        public void run() {
                            while (true && !activity.isFinishing()) {
                                while (FacePassHandler.isAvailable()) {
                                    FacePassConfig config;
                                    try {
                                        /* use bin file */
                                        config = new FacePassConfig();
                                        config.poseBlurModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "attr.pose_blur.arm.190630.bin");
                                        config.livenessModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "liveness.CPU.rgb.G.bin");
                                        config.searchModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "feat2.arm.K.v1.0_1core.bin");
                                        config.detectModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "detector.arm.G.bin");
                                        config.detectRectModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "detector_rect.arm.G.bin");
                                        config.landmarkModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "pf.lmk.arm.E.bin");
                                        config.rcAttributeModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "attr.RC.arm.G.bin");
                                        config.occlusionFilterModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "attr.occlusion.arm.20201209.bin");
                                        //DUALCAM
                                        config.rgbIrLivenessModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "liveness.CPU.rgbir.I.bin");
                                        config.rgbIrGaLivenessModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "liveness.CPU.rgbir.ga_case.A.bin");
                                        config.livenessGPUCache = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "liveness.GPU.rgbir.I.cache");
                                        config.rgbIrLivenessGpuModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "liveness.GPU.rgbir.I.bin");
                                        config.rgbIrGaLivenessGpuModel = FacePassModel.initModel(activity.getApplicationContext().getAssets(), "liveness.GPU.rgbir.ga_case.A.bin");
                                        //DUALCAM

                                        /* set value of algorithm */
                                        config.rcAttributeAndOcclusionMode = 1;
                                        config.searchThreshold = 65f;
                                        config.livenessThreshold = 80f;
                                        config.livenessGaThreshold = 85f;
                                        config.livenessEnabled = false;
                                        config.rgbIrLivenessEnabled = true;      // 启用双目活体功能(默认CPU)
                                        config.rgbIrLivenessGpuEnabled = true;   // 启用双目活体GPU功能
                                        config.rgbIrGaLivenessEnabled = true;    // 启用真假人同屏功能(默认CPU)
                                        config.rgbIrGaLivenessGpuEnabled = true; // 启用真假人同屏GPU功能

                                        config.poseThreshold = new FacePassPose(35f, 35f, 35f);
                                        config.blurThreshold = 0.8f;
                                        config.lowBrightnessThreshold = 30f;
                                        config.highBrightnessThreshold = 210f;
                                        config.brightnessSTDThreshold = 80f;
                                        config.faceMinThreshold = 60;
                                        config.retryCount = 10;
                                        config.smileEnabled = false;
                                        config.maxFaceEnabled = true;
                                        config.fileRootPath = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

                                        /* build FacePssHandler */
                                        mFacePassHandler2 = new FacePassHandler(config);

                                        /* set value of algorithm */
                                        FacePassConfig addFaceConfig = mFacePassHandler2.getAddFaceConfig();
                                        addFaceConfig.poseThreshold.pitch = 35f;
                                        addFaceConfig.poseThreshold.roll = 35f;
                                        addFaceConfig.poseThreshold.yaw = 35f;
                                        addFaceConfig.blurThreshold = 0.7f;
                                        addFaceConfig.lowBrightnessThreshold = 70f;
                                        addFaceConfig.highBrightnessThreshold = 220f;
                                        addFaceConfig.brightnessSTDThresholdLow = 14.14f;
                                        addFaceConfig.brightnessSTDThreshold = 63.25f;
                                        addFaceConfig.faceMinThreshold = 100;
                                        addFaceConfig.rcAttributeAndOcclusionMode = 2;
                                        mFacePassHandler2.setAddFaceConfig(addFaceConfig);

                                        boolean contain = false;
                                        if(mFacePassHandler2.getLocalGroups()!=null) {
                                            for (String g : mFacePassHandler2.getLocalGroups()) {
                                                if (g.equals(group_name)) {
                                                    contain = true;
                                                }
                                            }
                                        }
                                        if (!contain) {
                                            mFacePassHandler2.createLocalGroup(group_name);
                                        }
                                        mFacePassHandler2.initLocalGroup(group_name);

                                        iFacePassInit.result(mFacePassHandler2);
                                    } catch (FacePassException e) {
                                        e.printStackTrace();
                                        iFacePassInit.result(null);
                                        return;
                                    }
                                    return;
                                }
                                try {
                                    /* 如果SDK初始化未完成则需等待 */
                                    sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }.start();
                } else {
                    iFacePassInit.result(null);
                }
            }
        });
    }
}

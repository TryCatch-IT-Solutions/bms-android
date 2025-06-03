package facex.facepass.camera;

import android.os.Build;

import facex.BaseActivity;

public abstract class CameraSettingActivity extends BaseActivity {
    public int cameraRotation = 0;
    public boolean mirror= true ;
    public boolean cameraFront = true;
    public CameraManager manager;
    public CameraManager irmanager;

    public void CameraSetting(){
        String modelName = Build.MODEL.replace("-","");
        //前置后置摄像头
        if (modelName.contains("X05")||modelName.contains("rk3568") || modelName.contains("F68V")) {
            cameraFront = false;
            if(modelName.contains("rk3568") || modelName.contains("F68V")) {
                mirror = false;
            }
        } else {
            cameraFront = true;
        }
        //cameraRotation算法角度，manager.setCameraRotate相机图像角度
        if (modelName.contains("FP08")) {
            cameraRotation = 90;
            manager.setCameraRotate(90);
            if(irmanager!=null){
                irmanager.setCameraRotate(90);
            }
        } else if (modelName.contains("X05")||modelName.contains("rk3568")) {
            cameraRotation = 270;
            manager.setCameraRotate(90);
            if(irmanager!=null){
                irmanager.setCameraRotate(90);
            }
        }else if(modelName.contains("FP07")){
            cameraRotation = 270;
            manager.setCameraRotate(270);
            if(irmanager!=null){
                irmanager.setCameraRotate(270);
            }
        }else if(modelName.contains("F68V")){
            cameraRotation = 90;
            manager.setCameraRotate(270);
            if(irmanager!=null){
                irmanager.setCameraRotate(270);
            }
        }else if(modelName.contains("FP09")){
            cameraRotation = 90;
            manager.setCameraRotate(90);
            if(irmanager!=null){
                irmanager.setCameraRotate(90);
            }
        }else if(modelName.contains("FP520")){
            cameraRotation = 90;
            manager.setCameraRotate(90);
            if(irmanager!=null){
                irmanager.setCameraRotate(90);
            }
        } else {
            cameraRotation = 0;
            manager.setCameraRotate(0);
            if(irmanager!=null){
                irmanager.setCameraRotate(0);
            }
        }
    }

    public int getSaveDegree(){
        int degrees = 0;
        String modelName = Build.MODEL.replace("-","");
        if(modelName.contains("rk3568")){
            degrees = 90;
        }
        if (modelName.contains("FP08")) {
            degrees = -90;
        } else if (modelName.contains("F68V")) {
            degrees = -90;
        } else if(modelName.contains("X05")){
            degrees = 90;
        }else if (modelName.contains("FP07")) {
            degrees = -270;
        } else if (modelName.contains("FP09")) {
            degrees = -90;
        } else if (modelName.contains("FP520")) {
            degrees = -90;
        }
        return degrees;
    }
}

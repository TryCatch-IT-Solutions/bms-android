package facex.facepass.camera;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.example.bms.R;
import com.example.bms.databinding.ActivityCameraBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends CameraSettingActivity implements CameraManager.CameraListener {
    private CameraPreview cameraView;

    ActivityCameraBinding cameraBinding;

    ProgressDialog progressDialog;

    boolean takePhoto = false;

    int degrees = 0;

    @Override
    public void onCreateBase(Bundle bundle) {
        cameraBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(cameraBinding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.loading));

        cameraBinding.actionbar.backTitleStyle(getString(R.string.take_photo));
        cameraView = cameraBinding.preview;

        manager = new CameraManager();
        manager.setPreviewDisplay(cameraView);
        manager.setListener(this);
        CameraSetting();

        cameraBinding.takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto = true;
            }
        });

        cameraBinding.backFront.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraFront =!cameraFront;
                manager.open(getWindowManager(), cameraFront, RelativeLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        manager.open(getWindowManager(), cameraFront, RelativeLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (manager != null) {
            manager.release();
        }
    }

    @Override
    public void onPictureTaken(CameraPreviewData cameraPreviewData) {
        if (takePhoto == true) {
            takePhoto = false;
            progressDialog.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = null;
                    try {
                        YuvImage image = new YuvImage(cameraPreviewData.nv21Data, ImageFormat.NV21, cameraPreviewData.width, cameraPreviewData.height, null);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        image.compressToJpeg(new android.graphics.Rect(0, 0, cameraPreviewData.width, cameraPreviewData.height), 100, stream);
                        bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (bitmap != null) {
                        degrees = 0;
                        degrees = getSaveDegree();

                        if(degrees!=0){
                            Matrix matrix = new Matrix();
                            matrix.postRotate(degrees); // 传入你想要旋转的角度，以度为单位

                            // 创建一个新的Bitmap，它将是原始Bitmap旋转后的结果
                            // 注意：这里假设源Bitmap是可变的，如果不是，你需要先将其转换为可变的
                            bitmap = Bitmap.createBitmap(
                                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true
                            );
                        }
                        String path = saveToGallery(bitmap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(path!=null) {
                                    Intent intent = new Intent();
                                    intent.putExtra("data", path);
                                    setResult(200, intent);
                                    progressDialog.dismiss();
                                    finish();
                                }else {
                                    progressDialog.dismiss();
                                }
                            }
                        });
                    }else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    public String saveToGallery(Bitmap bitmap) {
        // 指定保存图片的文件名（包括扩展名）
        String dir = getFilesDir() + "/camera/";
        File dirFile = new File(dir);
        if(!dirFile.exists()){
            dirFile.mkdir();
        }
        String filePath = dir + System.currentTimeMillis() + "_photo" + ".jpg";
        File file = new File(filePath);
        // 写入文件
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return filePath;
    }

}

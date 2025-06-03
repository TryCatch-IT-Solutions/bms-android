package facex;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;

/**
 * @author tx
 * @date 2023/5/30 9:02
 * @target 简单的封装权限请求
 */
public class EasyPermission {

    public interface IPermissionResult {
        void result(boolean allGranted);
    }

    Activity context;

    public EasyPermission(Activity context) {
        this.context = context;
    }

    public void camera(IPermissionResult iPermissionResult) {
//        XXPermissions.with(context)
//                .permission(Permission.CAMERA)
//                .request(new OnPermissionCallback() {
//                    @Override
//                    public void onGranted(List<String> permissions, boolean allGranted) {
//                        iPermissionResult.result(allGranted);
//                    }
//                });
    }

    public void storageAndCameraPhoneState(IPermissionResult iPermissionResult) {
//        XXPermissions.with(context)
//                .permission(Permission.CAMERA)
//                .permission(Permission.READ_PHONE_STATE)
//                .permission(Permission.READ_EXTERNAL_STORAGE)
//                .permission(Permission.WRITE_EXTERNAL_STORAGE)
//                .request(new OnPermissionCallback() {
//                    @Override
//                    public void onGranted(List<String> permissions, boolean allGranted) {
//                        iPermissionResult.result(allGranted);
//                    }
//                });
    }

    public boolean manage_storage(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean manage = Environment.isExternalStorageManager();
            if (!manage) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivity(intent);
                }
                return false;
            }else {
                return true;
            }
        } else {
            return true;
        }
    }

}

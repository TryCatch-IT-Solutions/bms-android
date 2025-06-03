package facex;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    public String MODEL ="";

    public abstract void onCreateBase(Bundle bundle);

    public String getVersionName() {
        PackageInfo packageInfo = getPackageInfo();
        return packageInfo == null ? "" : packageInfo.versionName;
    }

    public int getVersionCode() {
        PackageInfo packageInfo = getPackageInfo();
        return packageInfo.versionCode;
    }

    private PackageInfo getPackageInfo() {
        try {
            PackageManager e = getPackageManager();
            return e.getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException var1) {
            var1.printStackTrace();
            return null;
        }
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        MODEL = android.os.Build.MODEL.replace("-","");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        onCreateBase(bundle);
    }
}

package doorx.utils;

import android.view.View;

public abstract class OnDeviceCheckClickListener implements View.OnClickListener {

    boolean deviceOk = false;

    public abstract void onPassClick(View v);
    public abstract void onRejectClick();

    public OnDeviceCheckClickListener(boolean deviceOk){
        this.deviceOk = deviceOk;
    }

    @Override
    public void onClick(View v) {
        if(deviceOk){
            onPassClick(v);
        }else {
            onRejectClick();
        }
    }
}

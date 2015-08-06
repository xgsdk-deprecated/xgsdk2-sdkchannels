
package com.xgsdk.client.impl.callback;

import cn.uc.gamesdk.UCCallbackListener;
import cn.uc.gamesdk.UCGameSDKStatusCode;

import com.xgsdk.client.api.callback.ExitCallBack;
import com.xgsdk.client.core.utils.XGLog;

import android.app.Activity;

public class ExitCallback implements UCCallbackListener<String> {
    private Activity mActivity;
    private ExitCallBack mExitCallBack;

    public ExitCallback(Activity act, ExitCallBack ecb) {
        mActivity = act;
        mExitCallBack = ecb;
    }

    @Override
    public void callback(int statuscode, String data) {
        if (UCGameSDKStatusCode.SDK_EXIT_CONTINUE == statuscode) {
            XGLog.d("Exit Cancel");
            mExitCallBack.onCancel();

        } else if (UCGameSDKStatusCode.SDK_EXIT == statuscode) {
            XGLog.d("Exit Game");
            mExitCallBack.onExit();
        }

    }

}

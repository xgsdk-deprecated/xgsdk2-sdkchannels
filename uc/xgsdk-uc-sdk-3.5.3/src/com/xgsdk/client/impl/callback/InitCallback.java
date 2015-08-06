
package com.xgsdk.client.impl.callback;

import cn.uc.gamesdk.UCCallbackListener;
import cn.uc.gamesdk.UCGameSDKStatusCode;

import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.UserCallBack;
import com.xgsdk.client.core.utils.XGLog;
import com.xgsdk.client.impl.XGChannelImpl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class InitCallback implements UCCallbackListener<String> {

    private Activity mActivity;
    private XGChannelImpl mImpl;
    private UserCallBack mUserCallback;

    public InitCallback(Activity act, XGChannelImpl impl, UserCallBack ucb) {
        mActivity = act;
        mImpl = impl;
        mUserCallback = ucb;
    }

    @Override
    public void callback(int code, String msg) {
        XGLog.i("msg:" + msg);// 返回的消息
        switch (code) {
            case UCGameSDKStatusCode.SUCCESS:
                showDialog(mActivity, "INIT SUCCESS");
                mImpl.setInitSuccess(true);
                break;
            case UCGameSDKStatusCode.INIT_FAIL:
                showDialog(mActivity, "INIT FAIL");
                mImpl.setInitSuccess(false);
                mUserCallback.onInitFail(XGErrorCode.SDK_CLIENT_INIT_FAILED,
                        "INIT FAIL"); // 错误码到底给什么
                break;
            default:
                break;
        }

    }

    void showDialog(Activity activity, String msg) {
        new AlertDialog.Builder(activity).setTitle("Message").setMessage(msg)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub

                    }
                }).create().show();
    }
}

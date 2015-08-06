
package com.xgsdk.client.impl.callback;

import cn.uc.gamesdk.UCCallbackListener;
import cn.uc.gamesdk.UCGameSDKStatusCode;

import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.UserCallBack;
import com.xgsdk.client.impl.XGChannelImpl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class LogoutCallback implements UCCallbackListener<String> {
    private Activity mActivity;
    private XGChannelImpl mImpl;
    private UserCallBack mUserCallBack;

    public LogoutCallback(Activity act, XGChannelImpl impl, UserCallBack usb) {
        mActivity = act;
        mImpl = impl;
        mUserCallBack = usb;
    }

    @Override
    public void callback(int statuscode, String arg1) {
        switch (statuscode) {
            case UCGameSDKStatusCode.NO_INIT:
                showDialog(mActivity, "LOGOUT NO_INIT");
                mUserCallBack.onLogoutFail(XGErrorCode.SDK_CLIENT_INIT_FAILED,
                        "Logout no_init");
                break;
            case UCGameSDKStatusCode.NO_LOGIN:
                showDialog(mActivity, "LOGOUT NO_LOGIN");
                mUserCallBack.onLogoutFail(XGErrorCode.OTHER_ERROR,
                        "Logout no_login");
                break;
            case UCGameSDKStatusCode.SUCCESS:
                showDialog(mActivity, "LOGOUT SUCCESS");
                mUserCallBack.onLogoutSuccess("Logout success");

                break;
            case UCGameSDKStatusCode.FAIL:
                showDialog(mActivity, "LOGOUT FAIL");
                mUserCallBack.onLogoutFail(XGErrorCode.OTHER_ERROR,
                        "Logout failed");
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

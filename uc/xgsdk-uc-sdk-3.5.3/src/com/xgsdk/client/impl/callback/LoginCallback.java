
package com.xgsdk.client.impl.callback;

import cn.uc.gamesdk.UCCallbackListener;
import cn.uc.gamesdk.UCGameSDK;
import cn.uc.gamesdk.UCGameSDKStatusCode;

import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.UserCallBack;
import com.xgsdk.client.core.service.AuthService;
import com.xgsdk.client.core.utils.XGLog;
import com.xgsdk.client.impl.XGChannelImpl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class LoginCallback implements UCCallbackListener<String> {
    private Activity mActivity;
    private XGChannelImpl mImpl;
    private UserCallBack mUserCallback;

    public LoginCallback(Activity act, XGChannelImpl impl, UserCallBack ucb) {
        mActivity = act;
        mImpl = impl;
        mUserCallback = ucb;
    }

    @Override
    public void callback(int code, String msg) {
        if (code == UCGameSDKStatusCode.SUCCESS) {
            showDialog(mActivity, "Login SUCCESS" + msg);
            String sid;
            String authInfo;
            try {
                sid = UCGameSDK.defaultSDK().getSid();
                authInfo = AuthService.genAuthInfo(mActivity, sid, "", "");
                XGLog.d("Login info:" + "sid:" + sid + "authinfo:" + authInfo);
                mUserCallback.onLoginSuccess(authInfo);
            } catch (Exception e) {
                XGLog.e("login success, exception is :" + e.getMessage(), e);
            }

            mImpl.setLoginSuccess(true);
        }
        if (code == UCGameSDKStatusCode.LOGIN_EXIT) {
            showDialog(mActivity, "Login CANCEL" + msg);
            mUserCallback.onLoginCancel("Login CANCEL");
            mImpl.setLoginSuccess(false);
        }
        if (code == UCGameSDKStatusCode.NO_INIT) {
            showDialog(mActivity, "Login NO_INIT" + msg);
            mUserCallback.onLoginFail(XGErrorCode.SDK_CLIENT_INIT_FAILED,
                    "Login NO_INIT");
            mImpl.setLoginSuccess(false);
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

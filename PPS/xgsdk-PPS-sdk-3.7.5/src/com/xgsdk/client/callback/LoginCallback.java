package com.xgsdk.client.callback;

import android.app.Activity;

import com.pps.sdk.platform.PPSPlatform;
import com.pps.sdk.platform.PPSPlatformListener;
import com.pps.sdk.platform.PPSResultCode;
import com.pps.sdk.platform.PPSUser;
import com.xgsdk.client.api.callback.UserCallBack;
import com.xgsdk.client.core.XGInfo;
import com.xgsdk.client.core.service.AuthService;
import com.xgsdk.client.core.utils.XGLog;

public class LoginCallback extends PPSPlatformListener {
	private Activity activity;
	protected UserCallBack mUserCallBack;

	/**
	 * 
	 */
	public LoginCallback(Activity activity, UserCallBack mUserCallBack) {
		this.activity = activity;
		this.mUserCallBack = mUserCallBack;
	}

	private boolean isLoginClicked;

	public boolean isLoginClicked() {
		return isLoginClicked;
	}

	public void setLoginClicked(boolean isLoginClicked) {
		this.isLoginClicked = isLoginClicked;
	}

	@Override
	public void leavePlatform() {
		super.leavePlatform();

		mUserCallBack.onLoginCancel("Login cancel");
		XGLog.d("登录后，退出pps平台中心。");
	}

	@Override
	public void loginResult(int resultCode, PPSUser user) {
		super.loginResult(resultCode, user);
		
		/*
		 * 处理切换账号回调。 
		 */
		if(!isLoginClicked){
			XGLog.d("this loginCallback is from switch account,will send logout msg to game...");
			mUserCallBack.onLogoutSuccess("用户已登出。");
			return;
		}else
		{
			XGLog.d("this loginCallback is from user,will send login msg to game...");
			// 清除login标志位。
			isLoginClicked=false;
		}
		
		if (resultCode == PPSResultCode.SUCCESSLOGIN) {
			XGLog.d("用户登入成功");
			XGLog.d("uid => " + user.uid);
			XGLog.d("timestamp => " + user.timestamp);
			XGLog.d("sign => " + user.sign);
			try {
				String authInfo = AuthService.genAuthInfo(activity,user.sign,user.uid, user.name);
				XGLog.d("generate authinfo:" + authInfo);
				// 添加 SDK浮标
				XGLog.d("start call pps showToolbar");
				PPSPlatform.getInstance().initSliderBar(activity);
				XGLog.d("end call pps showToolbar");
				mUserCallBack.onLoginSuccess(authInfo);
			} catch (Exception e) {
				e.printStackTrace();
				XGLog.e("login success, exception is :" + e.getMessage(),
						e);
			}
		}
		if (resultCode == PPSResultCode.ERRORLOGIN) {
			XGLog.e("xgsdk 登录失败");
			mUserCallBack.onLoginFail(resultCode, "xgsdk 登录失败");
		}
	}
}
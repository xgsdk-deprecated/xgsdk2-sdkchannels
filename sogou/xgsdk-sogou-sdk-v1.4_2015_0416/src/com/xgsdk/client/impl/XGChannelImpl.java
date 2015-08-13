package com.xgsdk.client.impl;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.sogou.gamecenter.sdk.ErrorCode;
import com.sogou.gamecenter.sdk.SogouGamePlatform;
import com.sogou.gamecenter.sdk.bean.SogouGameConfig;
import com.sogou.gamecenter.sdk.bean.UserInfo;
import com.sogou.gamecenter.sdk.listener.InitCallbackListener;
import com.sogou.gamecenter.sdk.listener.LoginCallbackListener;
import com.sogou.gamecenter.sdk.listener.OnExitListener;
import com.sogou.gamecenter.sdk.listener.PayCallbackListener;
import com.sogou.gamecenter.sdk.listener.SwitchUserListener;
import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.ExitCallBack;
import com.xgsdk.client.api.callback.PayCallBack;
import com.xgsdk.client.api.entity.PayInfo;
import com.xgsdk.client.core.XGInfo;
import com.xgsdk.client.core.service.AuthService;
import com.xgsdk.client.core.service.PayService;
import com.xgsdk.client.core.utils.XGLog;
import com.xgsdk.client.inner.XGChannel;

public class XGChannelImpl extends XGChannel{
	private SogouGamePlatform mSogouGamePlatform = SogouGamePlatform.getInstance();

	@Override
	public void onCreate(final Activity activity){
		XGLog.d("onCreate calling...");
		SogouGameConfig config = new SogouGameConfig();
		config.devMode = false;
		config.gid = 100;
		config.appKey = "f0da851f02e56c515fca559ac8af9251b7baf6fdc1783fca112467e180380cad";
		config.gameName = "XGSDK";
		mSogouGamePlatform.prepare(activity, config);
	}
	@Override
	public String getChannelId() {
		return "sogou";
	}

	@Override
	public String getChannelAppId(Context context) {
		return XGInfo.getXGAppId(context);
		
	}

	@Override
	public void init(Activity activity) {
		mSogouGamePlatform.init(activity, new InitCallbackListener(){
			@Override
			public void initSuccess(){
				XGLog.d("init success");
			}
			
			@Override
			public void initFail(int code, String msg){
				mUserCallBack.onInitFail(XGErrorCode.INIT_FAILED,"sogou error code: " + code + " msg: " + msg);
			}

			
		});
		
	}

	@Override
	public void login(final Activity activity, String customParams) {
		mSogouGamePlatform.login(activity, new LoginCallbackListener(){

			@Override
			public void loginSuccess(int code, UserInfo userInfo) {
				long uid = userInfo.getUserId();
				String userName = userInfo.getUser();
				String session = userInfo.getSessionKey();
				try {
					String authInfo = AuthService.genAuthInfo(
					        activity, session,
					        String.valueOf(uid), userName);
				} catch (Exception e) {
					XGLog.e("login success, exception is :"
                            + e.getMessage(), e);
				}
				
			}
			
			@Override
			public void loginFail(int code, String msg) {
				if(code == LoginCallbackListener.LOGIN_CANCEL){
					mUserCallBack.onLoginCancel(msg);
				} else {
					mUserCallBack.onLoginFail(XGErrorCode.LOGIN_FAILED, getChannelId() + ": " + code + ", " + msg);
					XGLog.e("login fail");
				}
				
			}

			
			
		});
		
	}

	@Override
	public void pay(final Activity activity, final PayInfo payInfo, final PayCallBack payCallBack) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("currency", payInfo.getCurrencyName());
		data.put("rate", XGInfo.getSdkConfig(activity, "rate", null));
		data.put("product_name", payInfo.getProductName());
		data.put("amount", payInfo.getTotalPrice());
		data.put("unit_price", payInfo.getProductUnitPrice());
		data.put("product_number", payInfo.getXgOrderId());
		data.put("appmodes", false);
		mSogouGamePlatform.pay(activity, data, new PayCallbackListener(){

			@Override
			public void paySuccess(String orderId, String appData) {
				try {
                    // 更新订单// 购买成功
                    updateOrder(activity, payInfo);
                } catch (Exception e) {
                    XGLog.e("pay success, exception is :"
                            + e.getMessage(), e);
                }
                payCallBack.onSuccess("pay success.");
				
			}
			
			@Override
			public void payFail(int code, String orderId, String appData) {
				payCallBack
                .onFail(XGErrorCode.PAY_FAILED,
                        "code : " + code);
				
			}

			
			
		});
	}
	
	@Override
	 public void switchAccount(final Activity activity, final String customParams) {
		mSogouGamePlatform.switchUser(activity, new SwitchUserListener() {

			@Override
			public void switchSuccess(int code, UserInfo userInfo) {
				// 切换帐号成功回调此方法，拿到最新登录态信息
				XGLog.d("SwitchAccount Success");
			}

			@Override
			public void switchFail(int code, String msg) {
				// 切换帐号失败回调
				XGLog.e("SwitchAccount Fail, code:" + code + ", message:" + msg);
			}
		});
	 }
	
	@Override
	public void exit(final Activity activity, final ExitCallBack exitCallBack,
            final String customParams) {
        mSogouGamePlatform.exit(new OnExitListener(activity){

			@Override
			public void onCompleted() {
				exitCallBack.onExit();
				
			}
        	
        });
        
    }
	
	

}

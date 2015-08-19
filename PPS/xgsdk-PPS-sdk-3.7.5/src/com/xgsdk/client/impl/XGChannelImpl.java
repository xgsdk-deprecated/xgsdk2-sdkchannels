package com.xgsdk.client.impl;

import android.app.Activity;
import android.content.Context;

import com.pps.sdk.platform.PPSGamePlatformInitListener;
import com.pps.sdk.platform.PPSPlatform;
import com.pps.sdk.platform.PPSPlatformListener;
import com.pps.sdk.platform.PPSResultCode;
import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.ExitCallBack;
import com.xgsdk.client.api.callback.PayCallBack;
import com.xgsdk.client.api.entity.GameServerInfo;
import com.xgsdk.client.api.entity.PayInfo;
import com.xgsdk.client.api.entity.RoleInfo;
import com.xgsdk.client.api.entity.XGUser;
import com.xgsdk.client.callback.LoginCallback;
import com.xgsdk.client.core.XGInfo;
import com.xgsdk.client.core.utils.XGLog;
import com.xgsdk.client.inner.XGChannel;
import com.pps.sdk.platform.PPSUser;



public class XGChannelImpl extends XGChannel{

	//pps两次回调，导致订单重复cancel。该标志位用于放置退出平台时发送两次回调。
	private boolean isProcessedFlag = false;
		
	@Override
	public String getChannelId() {
		return "pps";
	}

	@Override
	public String getChannelAppId(Context context) {
		return XGInfo.getSdkConfig(context, "gameid", null);
	}
	
	@Override
	public void onCreate(final Activity activity){
		PPSPlatform ppsPlatform = PPSPlatform.getInstance();
		ppsPlatform.initPlatform(activity, getChannelAppId(activity), new PPSGamePlatformInitListener(){
			@Override
			public void onSuccess(){
				XGLog.d("Init success");
			}

			@Override
			public void onFail(String arg0) {
				mUserCallBack.onInitFail(XGErrorCode.INIT_FAILED, "Init fail");
				XGLog.e("Init fail");
			}
		});
	}
	
/*	@Override
	public void init(Activity activity) {
		PPSPlatform ppsPlatform = PPSPlatform.getInstance();
		ppsPlatform.initPlatform(activity, getChannelAppId(activity), new PPSGamePlatformInitListener(){
			@Override
			public void onSuccess(){
				XGLog.d("Init success");
			}

			@Override
			public void onFail(String arg0) {
				mUserCallBack.onInitFail(XGErrorCode.INIT_FAILED, "Init fail");
				XGLog.e("Init fail");
			}
		});
		
		
	}*/

	@Override
	public void login(Activity activity, String customParams) {
		LoginCallback loginCallback = new LoginCallback(activity, mUserCallBack);
		loginCallback.setLoginClicked(true);
		PPSPlatform.getInstance().ppsLogin(activity,
				loginCallback);
		XGLog.d("login finish");
	}

	@Override
	public void pay(final Activity activity, final PayInfo payInfo, final PayCallBack payCallBack) {
		isProcessedFlag = false;
		XGLog.d("pay calling...");
		try{
			PPSPlatform ppsPlatform = PPSPlatform.getInstance();
			int code = 0;
			if(payInfo.getTotalPrice() != 0){
				code = ppsPlatform.ppsPayment(activity, payInfo.getTotalPrice(), payInfo.getRoleId(),
						"ppsmobile_s" + payInfo.getServerId(), payInfo.getXgOrderId(), new PPSPlatformListener(){
					
					@Override
					public void leavePlatform(){
						super.leavePlatform();
						XGLog.d("支付后，退出pps平台中心。");
						if(isProcessedFlag)
						{
							XGLog.d("pps pay result has been processed,will skip this callback.");
							isProcessedFlag=false;
						}else
						{	
							// 从支付渠道选择界面直接退出，算做支付取消。
							try {
								cancelOrder(activity, payInfo.getXgOrderId());
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							XGLog.d("pps支付取消");
							payCallBack.onCancel("支付取消");
						}
					}
					
					@Override
					public void paymentResult(int result){
						super.paymentResult(result);
						if(result == PPSResultCode.SUCCESSPAYMENT){
							try{
								updateOrder(activity, payInfo);
								XGLog.d("支付成功" + result);
								isProcessedFlag = true;
								payCallBack.onSuccess("pay success");
							}catch(Exception e){
								XGLog.e("pay success, exception is :"
	                                    + e.getMessage(), e);
							}
						} else {
							try {
								// 支付失败
								cancelOrder(activity, payInfo.getXgOrderId());
								XGLog.d("支付失败,错误信息:" + result);
								payCallBack.onFail(result, "pay fail");
								isProcessedFlag=true;
							} catch (Exception e) {
								e.printStackTrace();
								XGLog.e("支付失败, exception is :" + e.getMessage(), e);
							}
						}
					}

					
				});
			} else {
				code = ppsPlatform.ppsPayment(activity, payInfo.getRoleId(),
						"ppsmobile_s" +payInfo.getServerId(), payInfo.getXgOrderId(), new PPSPlatformListener(){
					
					@Override
					public void leavePlatform(){
						super.leavePlatform();
						XGLog.d("支付后，退出pps平台中心。");
						if(isProcessedFlag)
						{
							XGLog.d("pps pay result has been processed,will skip this callback.");
							isProcessedFlag=false;
						}else
						{	
							// 从支付渠道选择界面直接退出，算做支付取消。
							try {
								cancelOrder(activity, payInfo.getXgOrderId());
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							XGLog.d("pps支付取消");
							payCallBack.onCancel("支付取消");
						}
					}
					
					@Override
					public void paymentResult(int result){
						super.paymentResult(result);
						if(result == PPSResultCode.SUCCESSPAYMENT){
							try{
								updateOrder(activity, payInfo);
								XGLog.d("支付成功" + result);
								isProcessedFlag = true;
								payCallBack.onSuccess("pay success");
							}catch(Exception e){
								XGLog.e("pay success, exception is :"
	                                    + e.getMessage(), e);
							}
						} else {
							try {
								// 支付失败
								cancelOrder(activity, payInfo.getXgOrderId());
								XGLog.d("支付失败,错误信息:" + result);
								payCallBack.onFail(result, "pay fail");
								isProcessedFlag=true;
							} catch (Exception e) {
								e.printStackTrace();
								XGLog.e("支付失败, exception is :" + e.getMessage(), e);
							}
						}
					}

					
				});
			}
			if(code != 10){
				XGLog.e("pay error is:" + code);
				payCallBack.onFail(code, "pay fail");
			}
		}catch (Exception e) {
			XGLog.e("pay error ",e.getMessage());
			payCallBack.onFail(XGErrorCode.PAY_FAILED, e.getMessage());
		}
		
	}
	
	
	@Override
	public void onEnterGame(final Activity activity, XGUser user,
            RoleInfo roleInfo, GameServerInfo serverInfo) {
		PPSPlatform ppsPlatform = PPSPlatform.getInstance();
		ppsPlatform.enterGame(activity, "ppsmobile_s" + serverInfo.getServerId());
    }
	
	@Override
	public void onCreateRole(final Activity activity, final XGUser user,
            final RoleInfo info,final GameServerInfo serverInfo){
		XGLog.d("ppsmobile_s" + serverInfo.getServerId());
		PPSPlatform ppsPlatform = PPSPlatform.getInstance();
		ppsPlatform.createRole(activity,"ppsmobile_s" + serverInfo.getServerId());
	}
	
	@Override
	public void openUserCenter(final Activity activity,
            final String customParams) {
		final PPSPlatform ppsPlatform = PPSPlatform.getInstance();
		ppsPlatform.ppsChangeAccount(activity, new PPSPlatformListener() {
			@Override
			public void leavePlatform(){
				super.leavePlatform();
				XGLog.d("PPSSDKPlatform", "ppsChangeAccount leavePlatform");
			}
			
			@Override
			public void loginResult(int result, PPSUser user){
				super.loginResult(result, user);
				ppsPlatform.initSliderBar(activity);
				XGLog.d("PPSSDKPlatform", "ppsChangeAccount loginResult");
			}
			
			@Override
			public void logout(){
				super.logout();
				XGLog.d("PPSSDKPlatform", "ppsChangeAccount leavePlatform");
			}
		});
    }
	
	@Override
	public void switchAccount(final Activity activity, final String customParams){
		XGLog.d("logout calling...");
		int code = PPSPlatform.getInstance().ppsChangeAccount(activity,
				new LoginCallback(activity, mUserCallBack));
		if (code != 10) {
			XGLog.e("ppsChangeAccount失败,错误码:" + code);
		}	
	}
	
	@Override
	public void exit(final Activity activity, final ExitCallBack exitCallBack,
            final String customParams) {
		XGLog.d("exitSDK calling...");
		PPSPlatform ppsPlatform = PPSPlatform.getInstance();
		ppsPlatform.ppsLogout(activity, new PPSPlatformListener(){
			@Override
			public void leavePlatform(){
				super.leavePlatform();
			}
			
			@Override
			public void logout(){
				super.logout();
				mUserCallBack.onLogoutSuccess("logout success");
				XGLog.d("exit success");
				activity.finish();
			}
		});
	}

	@Override
	public void init(Activity activity) {
		// TODO Auto-generated method stub
		
	}
	

}

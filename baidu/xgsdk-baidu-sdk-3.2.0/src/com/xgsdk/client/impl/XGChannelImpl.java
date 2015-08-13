package com.xgsdk.client.impl;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.baidu.gamesdk.ActivityAdPage;
import com.baidu.gamesdk.ActivityAnalytics;
import com.baidu.gamesdk.BDGameSDK;
import com.baidu.gamesdk.BDGameSDKSetting;
import com.baidu.gamesdk.ActivityAdPage.Listener;
import com.baidu.gamesdk.BDGameSDKSetting.Domain;
import com.baidu.gamesdk.BDGameSDKSetting.Orientation;
import com.baidu.gamesdk.IResponse;
import com.baidu.gamesdk.ResultCode;
import com.baidu.platformsdk.PayOrderInfo;
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
	
	private ActivityAdPage mActivityAdPage;
	private ActivityAnalytics mActivityAnalytics;

	@Override
	public String getChannelId() {
		return "baidu";
	}
	

	@Override
	public void init(final Activity activity) {
		XGLog.d("login calling...");
		try{
			BDGameSDKSetting mBDGameSDKSetting = new BDGameSDKSetting();
			String appId = XGInfo.getSdkConfig(activity, "AppID", null);
			String appKey = XGInfo.getSdkConfig(activity, "AppKey", null);
			mBDGameSDKSetting.setAppID(Integer.parseInt(appId));
			mBDGameSDKSetting.setAppKey(appKey);
			mBDGameSDKSetting.setDomain(Domain.RELEASE);
			mBDGameSDKSetting.setOrientation(Orientation.LANDSCAPE);
			BDGameSDK.init(activity, mBDGameSDKSetting, new IResponse<Void>(){
				
				@Override
				public void onResponse(int resultCode, String resultDesc, Void extraData){
					
					switch(resultCode){
					case ResultCode.INIT_SUCCESS:
						XGLog.d(getChannelId() + " init success.");
					case ResultCode.INIT_FAIL:
						XGLog.e(getChannelId() + " init fail.");
						mUserCallBack.onInitFail(XGErrorCode.INIT_FAILED,
			                   resultDesc);
					}
					
				}
			});
			mActivityAnalytics = new ActivityAnalytics(activity);

			mActivityAdPage = new ActivityAdPage(activity, new Listener() {

				@Override
				public void onClose() {
					// TODO 关闭暂停页, CP可以让玩家继续游戏
					Toast.makeText(activity.getApplicationContext(), "继续游戏", Toast.LENGTH_LONG).show();
				}

			});
		}catch(Exception e){
			XGLog.d("Init Fail");
			mUserCallBack.onInitFail(XGErrorCode.INIT_FAILED, "Init Fail");
			
		}
	}

	@Override
	public void login(final Activity activity, String customParams) {
		BDGameSDK.login(new IResponse<Void>() {
			
			@Override
			public void onResponse(int resultCode, String resultDesc, Void extraData){
				switch(resultCode){
				case ResultCode.LOGIN_SUCCESS:
					String uid = BDGameSDK.getLoginUid();
					String accessToken = BDGameSDK.getLoginAccessToken();
					 try {
                         String authInfo = AuthService.genAuthInfo(
                        		 activity, accessToken,
                                 String.valueOf(uid), "");
                         mUserCallBack.onLoginSuccess(authInfo);
                         BDGameSDK.showFloatView(activity);
                     } catch (Exception e) {
                         XGLog.d("login success, exception is :"
                                 + e.getMessage(), e);
                     }
					 break;
				case ResultCode.LOGIN_CANCEL:
					mUserCallBack.onLoginCancel(getChannelId() + resultDesc);
					break;
					
				case ResultCode.LOGIN_FAIL:
					mUserCallBack.onLogoutFail(resultCode, resultDesc);
					break;
				default:
					mUserCallBack.onLoginFail(
                            XGErrorCode.LOGIN_FAILED,
                            getChannelId() + " errcode: " + resultCode);
					break;
				}
			}
		});
		
	}

	@Override
	public void pay(final Activity activity, final PayInfo payment, final PayCallBack payCallBack) {
		try{
			if(!BDGameSDK.isLogined()){
				payCallBack
	            .onFail(XGErrorCode.PAY_FAILED,
	                    "User hasn't logined in");
				return;
			}
			//定额支付
			PayOrderInfo payOrderInfo = new PayOrderInfo();
			payOrderInfo.setCooperatorOrderSerial(payment.getGameTradeNo());
			payOrderInfo.setProductName(payment.getProductName());
			payOrderInfo.setTotalPriceCent(payment.getTotalPrice() * 100);
			payOrderInfo.setRatio(1);
			payOrderInfo.setExtInfo(payment.getUid());
			BDGameSDK.pay(payOrderInfo, null, new IResponse<PayOrderInfo>(){
				
				@Override
				public void onResponse(int resultCode, String resultDesc, PayOrderInfo extraData) {
					switch(resultCode){
					case ResultCode.PAY_SUCCESS:
						try {
	                        // 更新订单
							updateOrder(
	                                activity,
	                               payment);
	                    } catch (Exception e) {
	                        XGLog.d("pay success, exception is :"
	                                + e.getMessage(), e);
	                    }
	                    payCallBack.onSuccess("pay success.");
	                    break;
					case ResultCode.PAY_SUBMIT_ORDER:
						payCallBack
	                    .onProgress("The Order had been commited, message: " + resultDesc);
						break;
					case ResultCode.PAY_CANCEL:
						 try {
	                        cancelOrder(
	                                activity,
	                                payment.getXgOrderId());
	                    } catch (Exception e) {
	                        XGLog.e("pay cancel,exception is :"
	                                + e.getMessage(), e);
	                    }
	                    payCallBack.onCancel("cancel pay . message : "
	                            + resultDesc);
	                    break;
					case ResultCode.PAY_FAIL:
						payCallBack
	                    .onFail(XGErrorCode.PAY_FAILED,
	                            "PayFail, message : " + resultDesc);
						break;
					default:
						payCallBack
	                    .onFail(XGErrorCode.PAY_FAILED,
	                            "message : " + resultDesc);
						break;
					}
					
				}
			});
		}catch (Exception e) {
			 XGLog.e("pay error.", e);
	            payCallBack.onFail(XGErrorCode.PAY_FAILED, "pay fail");
	        
		}
		
	}
	
	@Override
	public void logout(final Activity activity, final String customParams){
		try{
			mUserCallBack.onLogoutSuccess("Logout Success");
		}catch(Exception e){
			mUserCallBack.onLogoutFail(XGErrorCode.OTHER_ERROR, "Logout Fail");
		}
		
	}
	
	@Override
	public void onDestroy(final Activity activity){
		XGLog.d("calling destory...");
		if (mActivityAdPage != null) {
			mActivityAdPage.onDestroy();
		}
		BDGameSDK.closeFloatView(activity);
		XGLog.d("destory finished");
	}
	
	@Override
	public void switchAccount(final Activity activity, final String customParams){
		XGLog.d("calling switchAccount...");
		logout(activity, customParams);
		BDGameSDK.setSuspendWindowChangeAccountListener(new IResponse<Void>(){
			
			@Override 
			public void onResponse(int resultCode, String resultDesc, Void extraData){
				switch(resultCode){
				case ResultCode.LOGIN_SUCCESS:
					String uid = BDGameSDK.getLoginUid();
					String accessToken = BDGameSDK.getLoginAccessToken();
					 try {
                         String authInfo = AuthService.genAuthInfo(
                        		 activity, accessToken,
                                 String.valueOf(uid), "");
                         mUserCallBack.onLoginSuccess(authInfo);
                     } catch (Exception e) {
                         XGLog.d("login success, exception is :"
                                 + e.getMessage(), e);
                     }
					 break;
				case ResultCode.LOGIN_CANCEL:
					mUserCallBack.onLoginCancel(getChannelId() + resultDesc);
					break;
					
				case ResultCode.LOGIN_FAIL:
					mUserCallBack.onLogoutFail(resultCode, resultDesc);
					break;
				default:
					mUserCallBack.onLoginFail(
                            XGErrorCode.LOGIN_FAILED,
                            getChannelId() + " errcode: " + resultCode);
					break;
					
				}
			}
		});
		XGLog.e("SwitchAccount Finished");
	}
	
	@Override
	public void exit(final Activity activity, final ExitCallBack exitCallBack,
            final String customParams){
		XGLog.d("calling exit...");
		BDGameSDK.closeFloatView(activity);
		try {
			BDGameSDK.destroy();
			exitCallBack.onExit();
		} catch (Exception e) {
			e.printStackTrace();
			exitCallBack.onCancel();	
		}
		XGLog.d("exitSDK finish...");
	}

	@Override
	public String getChannelAppId(Context context) {
		return XGInfo.getXGAppId(context);
		
	}
	
	@Override
	public void onPause(final Activity activity) {
		XGLog.d("onPause calling...");
		try {
			if (mActivityAdPage != null) {
				mActivityAdPage.onPause();
			}
			if (mActivityAnalytics != null) {
				mActivityAnalytics.onPause();
			}
		} catch (Throwable t) {
			XGLog.e(t.getMessage(), t);
		}
		XGLog.d("onPause finish...");
	}
	
	@Override
	public void onResume(final Activity activity) {
		XGLog.d("onResume calling...");
		XGLog.d(String.valueOf(mActivityAdPage));
		XGLog.d(String.valueOf(mActivityAnalytics));
		try {
			if (mActivityAdPage != null) {
				mActivityAdPage.onResume();
			}
			if (mActivityAnalytics != null) {
				mActivityAnalytics.onResume();
			}
		} catch (Throwable t) {
			XGLog.e(t.getMessage(), t);
		}
		XGLog.d("onResume finish...");
	}
	
	@Override
	public void onStop(final Activity activity) {
		XGLog.e("onStop calling...");
		// undo
		BDGameSDK.closeFloatView(activity);
		if (mActivityAdPage != null) {
			mActivityAdPage.onStop();
		}
		XGLog.e("onStop finish...");
	}
	
	
	

}

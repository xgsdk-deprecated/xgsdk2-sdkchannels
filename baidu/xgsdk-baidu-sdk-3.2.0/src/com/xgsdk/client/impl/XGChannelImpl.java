package com.xgsdk.client.impl;

import android.app.Activity;
import android.content.Context;

import com.baidu.gamesdk.BDGameSDK;
import com.baidu.gamesdk.BDGameSDKSetting;
import com.baidu.gamesdk.BDGameSDKSetting.Domain;
import com.baidu.gamesdk.BDGameSDKSetting.Orientation;
import com.baidu.gamesdk.IResponse;
import com.baidu.gamesdk.ResultCode;
import com.baidu.platformsdk.PayOrderInfo;
import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.PayCallBack;
import com.xgsdk.client.api.entity.PayInfo;
import com.xgsdk.client.core.XGInfo;
import com.xgsdk.client.core.service.AuthService;
import com.xgsdk.client.core.service.PayService;
import com.xgsdk.client.core.utils.XGLog;
import com.xgsdk.client.inner.XGChannel;


public class XGChannelImpl extends XGChannel{

	@Override
	public String getChannelId() {
		return "baidu";
	}

	@Override
	public void init(Activity activity) {
		BDGameSDKSetting mBDGameSDKSetting = new BDGameSDKSetting();
		String appId = XGInfo.getXGAppId(activity);
		String appKey = XGInfo.getXGAppKey(activity);
		mBDGameSDKSetting.setAppID(Integer.parseInt(appId));
		mBDGameSDKSetting.setAppKey(appKey);
		mBDGameSDKSetting.setDomain(Domain.RELEASE);
		mBDGameSDKSetting.setOrientation(Orientation.LANDSCAPE);
		BDGameSDK.init(activity, mBDGameSDKSetting, new IResponse<Void>(){
			
			@Override
			public void onResponse(int resultCode, String resultDesc, Void extraData){
				
				switch(resultCode){
				case ResultCode.INIT_SUCCESS:
					XGLog.e(getChannelId() + " init success.");
				case ResultCode.INIT_FAIL:
					XGLog.e(getChannelId() + " init fail.");
					mUserCallBack.onInitFail(XGErrorCode.SDK_CLIENT_INIT_FAILED,
		                   resultDesc);
				}
				
			}
		});
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
                     } catch (Exception e) {
                         XGLog.e("login success, exception is :"
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
                            XGErrorCode.CHANNEL_ERROR,
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
	            .onFail(XGErrorCode.PAY_FAILED_CHANNEL_ERROR,
	                    "User hasn't logined in");
				return;
			}
			//定额支付
			PayOrderInfo payOrderInfo = new PayOrderInfo();
			payOrderInfo.setCooperatorOrderSerial(payment.getGameOrderId());
			payOrderInfo.setProductName(payment.getProductName());
			payOrderInfo.setTotalPriceCent(payment.getProductTotalPrice() * 100);
			payOrderInfo.setRatio(0);
			payOrderInfo.setExtInfo("Pay Success");
			BDGameSDK.pay(payOrderInfo, null, new IResponse<PayOrderInfo>(){
				
				@Override
				public void onResponse(int resultCode, String resultDesc, PayOrderInfo extraData) {
					switch(resultCode){
					case ResultCode.PAY_SUCCESS:
						try {
	                        // 更新订单// 购买成功
	                        PayService.updateOrderInThread(
	                                activity,
	                                payment.getXgOrderId(), null,
	                                null, null, null, null, null,
	                                null, null, null, null, null, null, null, null);
	                    } catch (Exception e) {
	                        XGLog.e("pay success, exception is :"
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
	                        PayService.cancelOrderInThread(
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
	                    .onFail(XGErrorCode.PAY_FAILED_CHANNEL_ERROR,
	                            "PayFail, message : " + resultDesc);
						break;
					default:
						payCallBack
	                    .onFail(XGErrorCode.PAY_FAILED_CHANNEL_ERROR,
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
			mUserCallBack.onLogoutFail(9999, "Logout Fail");
		}
		
	}
	
	public void destory(){
		XGLog.e("calling destory...");
		BDGameSDK.destroy();
		XGLog.e("destory finished");
	}
	
	@Override
	public void switchAccount(final Activity activity, final String customParams){
		XGLog.e("calling switchAccount...");
		logout(activity, "");
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
                         XGLog.e("login success, exception is :"
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
                            XGErrorCode.CHANNEL_ERROR,
                            getChannelId() + " errcode: " + resultCode);
					break;
					
				}
			}
		});
		XGLog.e("SwitchAccount Finished");
	}
	
	

}

package com.xgsdk.client.impl;

import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.PayCallBack;
import com.xgsdk.client.api.entity.PayInfo;
import com.xgsdk.client.core.XGInfo;
import com.xgsdk.client.core.service.AuthService;
import com.xgsdk.client.core.service.PayService;
import com.xgsdk.client.core.utils.XGLog;
import com.xgsdk.client.inner.XGChannel;
import com.xiaomi.gamecenter.sdk.GameInfoField;
import com.xiaomi.gamecenter.sdk.MiCommplatform;
import com.xiaomi.gamecenter.sdk.MiErrorCode;
import com.xiaomi.gamecenter.sdk.OnLoginProcessListener;
import com.xiaomi.gamecenter.sdk.OnPayProcessListener;
import com.xiaomi.gamecenter.sdk.entry.MiAccountInfo;
import com.xiaomi.gamecenter.sdk.entry.MiAppInfo;
import com.xiaomi.gamecenter.sdk.entry.MiBuyInfo;
import com.xiaomi.gamecenter.sdk.entry.ScreenOrientation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public class XGChannelImpl extends XGChannel {

	@Override
	public String getChannelId() {
		return "mi";
	}

	@Override
	public String getChannelAppId(Context context) {
		return XGInfo.getSdkConfig(context, "AppID", null);
	}

	@Override
	public void init(Activity activity) {

	}

	@Override
	public void onApplicationCreate(Context context) {// 灏忕背娓犻亾init涓嶈兘鏀惧湪Activity鐨刼nCreate涓仛锛屼細鎶ntentReceiverLeaked閿欒
		super.onApplicationCreate(context);
		try {
			MiAppInfo appInfo = new MiAppInfo();
			String appId = XGInfo.getSdkConfig(context, "AppID", null);
			String appKey = XGInfo.getSdkConfig(context, "AppKey", null);
			appInfo.setAppId(appId);
			appInfo.setAppKey(appKey);
			appInfo.setOrientation(XGInfo.isLandspcape(context) ? ScreenOrientation.horizontal
					: ScreenOrientation.vertical);
			MiCommplatform.Init(context, appInfo);
		} catch (Exception e) {
			XGLog.e(getChannelId() + " init error.", e);
			mUserCallBack.onInitFail(XGErrorCode.INIT_FAILED, e.getMessage());
		}
	}

	@Override
	public void login(final Activity activity, String customParams) {
		MiCommplatform.getInstance().miLogin(activity,
				new OnLoginProcessListener() {

					@Override
					public void finishLoginProcess(int code, MiAccountInfo info) {

						switch (code) {
						case MiErrorCode.MI_XIAOMI_PAYMENT_SUCCESS:
							// 鐧婚檰鎴愬姛鑾峰彇鐢ㄦ埛鐨勭櫥闄嗗悗鐨刄ID锛堝嵆鐢ㄦ埛鍞竴鏍囪瘑锛�
							long uid = info.getUid();
							// 鑾峰彇鐢ㄦ埛鐨勭櫥闄嗙殑Session锛堣鍙傝��5.3.3娴佺▼鏍￠獙Session鏈夋晥鎬э級
							String session = info.getSessionId();
							// 鐢ㄨ繖涓仛灞曠幇
							String uname = info.getNikename();
							// 璇峰紑鍙戣�呭畬鎴愬皢uid鍜宻ession鎻愪氦缁欏紑鍙戣�呰嚜宸辨湇鍔″櫒杩涜session楠岃瘉
							try {
								String authInfo = AuthService.genAuthInfo(
										activity, session, String.valueOf(uid),
										uname);
								mUserCallBack.onLoginSuccess(authInfo);
							} catch (Exception e) {
								XGLog.e("login success, exception is :"
										+ e.getMessage(), e);
							}
							break;
						case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_CANCEL:
							// 鍙栨秷鐧诲綍
							mUserCallBack.onLoginCancel(getChannelId()
									+ " errcode: " + code);
							break;
						case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_ACTION_EXECUTED:
							// 鐧诲綍鎿嶄綔姝ｅ湪杩涜涓�
							break;
						case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_LOGIN_FAIL:
						default:
							mUserCallBack.onLoginFail(XGErrorCode.LOGIN_FAILED,
									getChannelId() + " errcode: " + code);
							break;
						}
					}
				});
	}

	@Override
	public void pay(final Activity activity, final PayInfo payInfo,
			final PayCallBack payCallBack) {
		try {
			MiBuyInfo miBuyInfo = new MiBuyInfo();
			miBuyInfo.setCpUserInfo(payInfo.getUid()); // 姝ゅ弬鏁板湪鐢ㄦ埛鏀粯鎴愬姛鍚庝細閫忎紶缁機P鐨勬湇鍔″櫒
			miBuyInfo.setCpOrderId(payInfo.getXgOrderId());// 璁㈠崟鍙峰敮涓�锛堜笉涓虹┖锛�
			// 鐢ㄦ埛淇℃伅锛岀綉娓稿繀椤昏缃�佸崟鏈烘父鎴忔垨搴旂敤鍙��
			Bundle mBundle = new Bundle();
			mBundle.putString(GameInfoField.GAME_USER_BALANCE,
					payInfo.getBalance()); // 浣欓淇℃伅
			mBundle.putString(GameInfoField.GAME_USER_GAMER_VIP,
					payInfo.getVipLevel()); // vip绫诲瀷

			mBundle.putString(GameInfoField.GAME_USER_LV, payInfo.getLevel()); // 瑙掕壊绛夌骇
			mBundle.putString(GameInfoField.GAME_USER_ROLE_NAME,
					payInfo.getRoleName()); // 瑙掕壊鍚嶇О
			mBundle.putString(GameInfoField.GAME_USER_ROLEID,
					payInfo.getRoleId()); // 瑙掕壊id
			mBundle.putString(GameInfoField.GAME_USER_SERVER_NAME,
					payInfo.getServerName()); // 鎵�鍦ㄦ湇鍔″櫒
			miBuyInfo.setExtraInfo(mBundle); // 璁剧疆鐢ㄦ埛淇℃伅
			// 濡傛灉閲戦涓虹┖ 鍒欐寜浜у搧鏀粯
			if (payInfo.getProductTotalPrice() == 0) {
				miBuyInfo.setProductCode(payInfo.getProductId());// 鍟嗗搧浠ｇ爜锛屽紑鍙戣�呯敵璇疯幏寰楋紙涓嶄负绌猴級
				miBuyInfo.setCount(payInfo.getProductCount());// 璐拱鏁伴噺(鍟嗗搧鏁伴噺鏈�澶�9999锛屾渶灏�1)锛堜笉涓虹┖锛�
			} else {
				// 鎸夐噾棰濅粯璐�
				miBuyInfo.setAmount(payInfo.getProductTotalPrice());// 蹇呴』鏄ぇ浜�1鐨勬暣鏁帮紝10浠ｈ〃10绫冲竵锛屽嵆10鍏冧汉姘戝竵锛堜笉涓虹┖锛�
			}
			// 璋冪敤鏀粯鐣岄潰
			MiCommplatform.getInstance().miUniPay(activity, miBuyInfo,
					new OnPayProcessListener() {

						@Override
						public void finishPayProcess(int code) {
							switch (code) {
							case MiErrorCode.MI_XIAOMI_PAYMENT_SUCCESS:
								try {
									// 鏇存柊璁㈠崟// 璐拱鎴愬姛
									PayService.updateOrder(activity,
											payInfo.getXgOrderId(), null, null,
											null, null, null, null, null, null,
											null, null, null, null, null, null,
											null);
								} catch (Exception e) {
									XGLog.e("pay success, exception is :"
											+ e.getMessage(), e);
								}
								payCallBack.onSuccess("pay success.");
								break;
							case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_PAY_CANCEL:
								// 鍙栨秷璐拱
								try {
									PayService.cancelOrder(activity,
											payInfo.getXgOrderId(), null);
								} catch (Exception e) {
									XGLog.e("pay cancel,exception is :"
											+ e.getMessage(), e);
								}
								payCallBack.onCancel("cancel pay . code : "
										+ code);
								break;
							case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_ACTION_EXECUTED:
								// 鎿嶄綔姝ｅ湪杩涜涓�
								payCallBack
										.onProgress("action executed code : "
												+ code);
								break;
							case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_PAY_REPEAT:
								// 宸茶喘涔拌繃锛屾棤闇�璐拱锛屽彲鐩存帴浣跨敤
								payCallBack
										.onFail(XGErrorCode.PAY_FAILED_CHANNEL_RESPONSE,
												"repeat pay .code : " + code);
								XGLog.i(XGErrorCode
										.parseCode(XGErrorCode.PAY_FAILED_CHANNEL_RESPONSE)
										+ " repeat pay.code : " + code);
								break;
							case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_PAY_FAILURE:
							default:
								payCallBack
										.onFail(XGErrorCode.PAY_FAILED_CHANNEL_RESPONSE,
												"code : " + code);
								XGLog.i(XGErrorCode
										.parseCode(XGErrorCode.PAY_FAILED_CHANNEL_RESPONSE)
										+ " .code : " + code);
								break;
							}

						}

					});
		} catch (Exception e) {
			XGLog.e("pay error.", e);
			payCallBack.onFail(XGErrorCode.PAY_FAILED, "pay fail");
		}
	}

}

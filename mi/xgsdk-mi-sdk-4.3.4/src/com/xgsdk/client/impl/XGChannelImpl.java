
package com.xgsdk.client.impl;

import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.PayCallBack;
import com.xgsdk.client.api.entity.PayInfo;
import com.xgsdk.client.core.ProductInfo;
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
    public void init(Activity activity) {

    }

    @Override
    public void onApplicationCreate(Context context) {// 小米渠道init不能放在Activity的onCreate中做，会报IntentReceiverLeaked错误
        super.onApplicationCreate(context);
        try {
            MiAppInfo appInfo = new MiAppInfo();
            String appId = ProductInfo.getXGConfig(context, "AppID", null);
            String appKey = ProductInfo.getXGConfig(context, "AppKey", null);
            appInfo.setAppId(appId);
            appInfo.setAppKey(appKey);
            appInfo.setOrientation(ProductInfo.isLandspcape(context) ? ScreenOrientation.horizontal
                    : ScreenOrientation.vertical);
            MiCommplatform.Init(context, appInfo);
        } catch (Exception e) {
            XGLog.e(getChannelId() + " init error.", e);
            mUserCallBack.onInitFail(XGErrorCode.SDK_CLIENT_INIT_FAILED,
                    e.getMessage());
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
                                // 登陆成功获取用户的登陆后的UID（即用户唯一标识）
                                long uid = info.getUid();
                                // 获取用户的登陆的Session（请参考5.3.3流程校验Session有效性）
                                String session = info.getSessionId();
                                // 用这个做展现
                                String uname = info.getNikename();
                                // 请开发者完成将uid和session提交给开发者自己服务器进行session验证
                                try {
                                    String authInfo = AuthService.genAuthInfo(
                                            activity, session,
                                            String.valueOf(uid), uname);
                                    mUserCallBack.onLoginSuccess(authInfo);
                                } catch (Exception e) {
                                    XGLog.e("login success, exception is :"
                                            + e.getMessage(), e);
                                }
                                break;
                            case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_CANCEL:
                                // 取消登录
                                mUserCallBack.onLoginCancel(getChannelId()
                                        + " errcode: " + code);
                                break;
                            case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_ACTION_EXECUTED:
                                // 登录操作正在进行中
                                break;
                            case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_LOGIN_FAIL:
                            default:
                                mUserCallBack.onLoginFail(
                                        XGErrorCode.CHANNEL_ERROR,
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
            miBuyInfo.setCpUserInfo(payInfo.getUid()); // 此参数在用户支付成功后会透传给CP的服务器
            miBuyInfo.setCpOrderId(payInfo.getXgOrderId());// 订单号唯一（不为空）
            // 用户信息，网游必须设置、单机游戏或应用可选
            Bundle mBundle = new Bundle();
            mBundle.putString(GameInfoField.GAME_USER_BALANCE,
                    payInfo.getBalance()); // 余额信息
            String vipLevel = getRoleInfo() == null ? "0" : getRoleInfo()
                    .getVipLevel();
            mBundle.putString(GameInfoField.GAME_USER_GAMER_VIP, vipLevel); // vip类型

            String level = getRoleInfo() == null ? "0" : getRoleInfo()
                    .getLevel();
            mBundle.putString(GameInfoField.GAME_USER_LV, level); // 角色等级
            mBundle.putString(GameInfoField.GAME_USER_ROLE_NAME,
                    payInfo.getRoleName()); // 角色名称
            mBundle.putString(GameInfoField.GAME_USER_ROLEID,
                    payInfo.getRoleId()); // 角色id
            mBundle.putString(GameInfoField.GAME_USER_SERVER_NAME,
                    payInfo.getServerName()); // 所在服务器
            miBuyInfo.setExtraInfo(mBundle); // 设置用户信息
            // 如果金额为空 则按产品支付
            if (payInfo.getProductTotalPrice() == 0) {
                miBuyInfo.setProductCode(payInfo.getProductId());// 商品代码，开发者申请获得（不为空）
                miBuyInfo.setCount(payInfo.getProductCount());// 购买数量(商品数量最大9999，最小1)（不为空）
            } else {
                // 按金额付费
                miBuyInfo.setAmount(payInfo.getProductTotalPrice());// 必须是大于1的整数，10代表10米币，即10元人民币（不为空）
            }
            // 调用支付界面
            MiCommplatform.getInstance().miUniPay(activity, miBuyInfo,
                    new OnPayProcessListener() {

                        @Override
                        public void finishPayProcess(int code) {
                            switch (code) {
                                case MiErrorCode.MI_XIAOMI_PAYMENT_SUCCESS:
                                    try {
                                        // 更新订单// 购买成功
                                        PayService.updateOrderInThread(
                                                activity,
                                                payInfo.getXgOrderId(), null,
                                                null, null, null, null, null,
                                                null, null, null, null, null);
                                    } catch (Exception e) {
                                        XGLog.e("pay success, exception is :"
                                                + e.getMessage(), e);
                                    }
                                    payCallBack.onSuccess("pay success.");
                                    break;
                                case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_PAY_CANCEL:
                                    // 取消购买
                                    try {
                                        PayService.cancelOrderInThread(
                                                activity,
                                                payInfo.getXgOrderId());
                                    } catch (Exception e) {
                                        XGLog.e("pay cancel,exception is :"
                                                + e.getMessage(), e);
                                    }
                                    payCallBack.onCancel("cancel pay . code : "
                                            + code);
                                    break;
                                case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_ACTION_EXECUTED:
                                    // 操作正在进行中
                                    payCallBack
                                            .onProgress("action executed code : "
                                                    + code);
                                    break;
                                case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_PAY_REPEAT:
                                    // 已购买过，无需购买，可直接使用
                                    payCallBack
                                            .onFail(XGErrorCode.PAY_FAILED_CHANNEL_ERROR,
                                                    "repeat pay .code : "
                                                            + code);
                                    break;
                                case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_PAY_FAILURE:
                                default:
                                    payCallBack
                                            .onFail(XGErrorCode.PAY_FAILED_CHANNEL_ERROR,
                                                    "code : " + code);
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

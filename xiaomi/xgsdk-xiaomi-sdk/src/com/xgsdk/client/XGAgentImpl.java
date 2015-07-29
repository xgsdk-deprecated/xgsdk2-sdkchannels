
package com.xgsdk.client;

import com.xgsdk.client.agent.XGAgent;
import com.xgsdk.client.callback.PayCallBack;
import com.xgsdk.client.core.util.XGLogger;
import com.xgsdk.client.entity.PayInfo;
import com.xgsdk.client.entity.XGErrorCode;
import com.xgsdk.client.service.AuthService;
import com.xgsdk.client.service.PayService;
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
import android.os.Bundle;

public class XGAgentImpl extends XGAgent {

    @Override
    public String getChannelId() {
        return "mi";
    }

    @Override
    public void init(Activity activity) {

        try {
            MiAppInfo appInfo = new MiAppInfo();
            String appId = SdkConfig.getAppId(activity);
            String appKey = SdkConfig.getAppKey(activity);
            appInfo.setAppId(appId);
            appInfo.setAppKey(appKey);
            appInfo.setOrientation(ProductInfo.isLandspcape(activity) ? ScreenOrientation.horizontal
                    : ScreenOrientation.vertical);
            MiCommplatform.Init(activity, appInfo);
        } catch (Exception e) {
            XGLogger.e(getChannelId() + " init error.", e);
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
                                    XGLogger.e("login success, exception is :"
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
                                        XGLogger.e(
                                                "pay success, exception is :"
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
                                        XGLogger.e("pay cancel,exception is :"
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
            XGLogger.e("pay error.", e);
            payCallBack.onFail(XGErrorCode.PAY_FAILED, "pay fail");
        }
    }
}

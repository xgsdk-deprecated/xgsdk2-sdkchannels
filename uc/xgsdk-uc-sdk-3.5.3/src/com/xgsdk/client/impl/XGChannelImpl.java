
package com.xgsdk.client.impl;

import cn.uc.gamesdk.UCCallbackListener;
import cn.uc.gamesdk.UCCallbackListenerNullException;
import cn.uc.gamesdk.UCGameSDK;
import cn.uc.gamesdk.UCLogLevel;
import cn.uc.gamesdk.UCLoginFaceType;
import cn.uc.gamesdk.UCOrientation;
import cn.uc.gamesdk.info.FeatureSwitch;
import cn.uc.gamesdk.info.GameParamInfo;
import cn.uc.gamesdk.info.PaymentInfo;

import com.xgsdk.client.api.callback.ExitCallBack;
import com.xgsdk.client.api.callback.PayCallBack;
import com.xgsdk.client.api.entity.GameServerInfo;
import com.xgsdk.client.api.entity.PayInfo;
import com.xgsdk.client.api.entity.RoleInfo;
import com.xgsdk.client.api.entity.XGUser;
import com.xgsdk.client.core.XGInfo;
import com.xgsdk.client.core.utils.XGLog;
import com.xgsdk.client.impl.callback.ExitCallback;
import com.xgsdk.client.impl.callback.InitCallback;
import com.xgsdk.client.impl.callback.LoginCallback;
import com.xgsdk.client.impl.callback.LogoutCallback;
import com.xgsdk.client.impl.callback.PayCallback;
import com.xgsdk.client.inner.XGChannel;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;

public class XGChannelImpl extends XGChannel {

    private boolean isInitSuccess = false;
    private boolean isLoginSuccess = false;
    public static final String cpId_KEY = "cpId";
    public static final String apiKey_KEY = "apiKey";
    public static final String gameId_KEY = "gameId";
    public static final String ONSITE_MODE = "ucOnsite";

    public boolean isLoginSuccess() {
        return isLoginSuccess;
    }

    public void setLoginSuccess(boolean isLoginSuccess) {
        this.isLoginSuccess = isLoginSuccess;
    }

    public boolean isInitSuccess() {
        return isInitSuccess;
    }

    public void setInitSuccess(boolean isInitSuccess) {
        this.isInitSuccess = isInitSuccess;
    }

    @Override
    public String getChannelId() {
        return "uc";
    }

    @Override
    public void init(Activity activity) {
        GameParamInfo gpi = new GameParamInfo();
        String gameId = XGInfo.getSdkConfig(activity, gameId_KEY, "");
        String ucOnsite = XGInfo.getSdkConfig(activity, ONSITE_MODE, "");
        XGLog.i("gameId = " + gameId + " ucOnsite = " + ucOnsite);
        boolean isDebug;
        if (ucOnsite.equalsIgnoreCase("false")) {
            isDebug = false;
        } else {
            isDebug = true;
        }
        gpi.setGameId(Integer.valueOf(gameId));
        gpi.setFeatureSwitch(new FeatureSwitch(true, false));
        try {

            UCGameSDK.defaultSDK().setOrientation(UCOrientation.PORTRAIT);
            UCGameSDK.defaultSDK().setLoginUISwitch(UCLoginFaceType.USE_WIDGET);
            UCGameSDK.defaultSDK().initSDK(activity, UCLogLevel.WARN, isDebug,
                    gpi, new InitCallback(activity, this, this.mUserCallBack));

        } catch (UCCallbackListenerNullException e) {
            XGLog.e("init failed");
        }
        try {
            UCCallbackListener<String> logoutListener = new LogoutCallback(
                    activity, this, this.mUserCallBack);
            UCGameSDK.defaultSDK().setLogoutNotifyListener(logoutListener);
        } catch (Exception e) {
            XGLog.e("set logout failed");
        }
    }

    @Override
    public void login(final Activity activity, String customParams) {
        XGLog.i("login calling...");
        if (isInitSuccess) {
            try {
                UCGameSDK.defaultSDK().login(activity,
                        new LoginCallback(activity, this, this.mUserCallBack));
            } catch (UCCallbackListenerNullException e) {
                XGLog.e("login failed" + e.getMessage());
            }
        }
    }

    @Override
    public void logout(final Activity activity, final String customParams) {
        XGLog.d("logout calling...");
        if (isInitSuccess && isLoginSuccess)
            try {
                UCGameSDK.defaultSDK().logout();
            } catch (UCCallbackListenerNullException e) {
                e.printStackTrace();
                XGLog.e("logout error is:" + e.getMessage(), e);
            }
    }

    @Override
    public void switchAccount(final Activity activity,
            final String customParams) {
        XGLog.d("switchaccount calling...");
        logout(activity, customParams);
        XGLog.d("switchaccount finish...");
    }

    @Override
    public void pay(final Activity activity, final PayInfo payInfo,
            final PayCallBack payCallBack) {
        PaymentInfo pInfo = new PaymentInfo();
        String xgOrderId = payInfo.getXgOrderId();
        pInfo.setCustomInfo(xgOrderId);
        pInfo.setRoleId(payInfo.getRoleId());
        pInfo.setRoleName(payInfo.getRoleName());

        try {
            UCGameSDK.defaultSDK().pay(activity, pInfo, new PayCallback(
                    activity, xgOrderId, payCallBack, payInfo, this));
        } catch (Exception e) {
            XGLog.e("pay failed" + e.getMessage());

        }
    }

    @Override
    public void exit(final Activity activity, final ExitCallBack exitCallBack,
            final String customParams) {
        XGLog.d("exitSDK calling...");
        try {
            UCGameSDK.defaultSDK().exitSDK(activity,
                    new ExitCallback(activity, exitCallBack));
        } catch (Exception e) {
            e.printStackTrace();
            XGLog.e("exitSDK error is :" + e.getMessage(), e);
        }
        XGLog.d("exitSDK finish...");

    }

    @Override
    public void onEnterGame(final Activity activity, XGUser user,
            RoleInfo roleInfo, GameServerInfo serverInfo) {
        try {
            JSONObject jsonExData = new JSONObject();
            jsonExData.put("roleId", roleInfo.getRoleId());
            jsonExData.put("roleName", roleInfo.getRoleName());
            jsonExData.put("roleLevel", roleInfo.getLevel());
            jsonExData.put("zoneId", serverInfo.getZoneId());
            jsonExData.put("zoneName", serverInfo.getZoneName());
            UCGameSDK.defaultSDK().submitExtendData("loginGameRole",
                    jsonExData);
        } catch (Exception e) {
            XGLog.d("submit data exception" + e.getMessage());
        }
    }

    @Override
    public String getChannelAppId(Context context) {
        // TODO Auto-generated method stub
        return XGInfo.getSdkConfig(context, gameId_KEY, "");
    }

}

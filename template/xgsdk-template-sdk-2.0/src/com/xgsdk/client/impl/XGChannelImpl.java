
package com.xgsdk.client.impl;

import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.PayCallBack;
import com.xgsdk.client.api.entity.PayInfo;
import com.xgsdk.client.core.XGInfo;
import com.xgsdk.client.core.service.AuthService;
import com.xgsdk.client.core.utils.XGLog;
import com.xgsdk.client.inner.XGChannel;

import android.app.Activity;
import android.content.Context;

/**
 * @ClassName: XGChannelImpl
 * @Description:TODO(XGChannel的模板渠道实现，所有渠道的实现参照此类进行编写)
 * @author: liujie
 * @date: 2015年8月5日 上午9:28:32
 */
public class XGChannelImpl extends XGChannel {

    /**
     * <p>
     * Title: getChannelId
     * </p>
     * <p>
     * Description: 【必须实现】<br>
     * 返回当前渠道的channelId
     * </p>
     * 
     * @return
     * @see com.xgsdk.client.inner.XGChannel#getChannelId()
     */
    @Override
    public String getChannelId() {
        return "mi";
    }

    /**
     * <p>
     * Title: getChannelAppId
     * </p>
     * <p>
     * Description: 【必须实现】<br>
     * 此接口用于创建订单，返回渠道分配给游戏的appid，应使用XGInfo.getSdkConfig从sdk_config.properties中获取
     * </p>
     * 
     * @param context
     * @return
     * @see com.xgsdk.client.inner.XGChannel#getChannelAppId(android.content.Context)
     */
    @Override
    public String getChannelAppId(Context context) {
        return XGInfo.getSdkConfig(context, "appId", null);
    }

    /**
     * <p>
     * Title: init
     * </p>
     * <p>
     * Description: 【必须实现】<br>
     * 此方法将在游戏主Activity的onCreate中被调用， 若有渠道不允许init方法在Activity的onCreate调用，请使用其他方式
     * 例如：小米init方法推荐在Application的onCreate方法中调用，需要Override
     * XGChannel的onApplicationCreate方法，在其中进行init <br>
     * <br>
     * 调用渠道的init方法，init成功后不做callback，init失败后，要调用 mUserCallBack.onInitFail
     * </p>
     * 
     * @param activity
     * @see com.xgsdk.client.inner.XGChannel#init(android.app.Activity)
     */
    @Override
    public void init(Activity activity) {

        // 调用渠道的init方法，init成功后不做callback

        mUserCallBack.onInitFail(XGErrorCode.SDK_CLIENT_INIT_FAILED, "");// init失败后，要调用
                                                                         // mUserCallBack.onInitFail

    }

    /**
     * <p>
     * Title: login
     * </p>
     * <p>
     * Description: 【必须实现】<br>
     * 调用渠道的登录接口，login的callback是父类中的mUserCallBack<br>
     * 
     * </p>
     * 
     * @param activity
     * @param customParams 扩展参数暂未使用
     * @see com.xgsdk.client.inner.XGChannel#login(android.app.Activity,
     *      java.lang.String)
     */
    @Override
    public void login(Activity activity, String customParams) {

        String token = "xmvtzrenntgaisdfaochj";// 渠道登录成功后，返回的token，有的渠道叫sessionId或sid
        String uId = "199342";// 渠道登录成功后，返回的uId，账号的唯一标识
        String name = "flanker";// 登录成功后返回的用户名，若渠道未返回可以填null
        String authInfo;
        try {
            authInfo = AuthService.genAuthInfo(activity, token, uId, name);

            mUserCallBack.onLoginSuccess(authInfo);// 登录成功后调用
        } catch (Exception e) {
            XGLog.e(getChannelId() + "  login error", e);// 异常使用XGLog的e级别进行记录
            mUserCallBack.onLoginFail(XGErrorCode.OTHER_ERROR, e.getMessage());
        }
    }

    /**
     * <p>
     * Title: pay
     * </p>
     * <p>
     * Description: 【必须实现】<br>
     * 调用渠道的支付接口，将payInfo中参数对应赋值给渠道的支付接口
     * 其中渠道的ext字段（用于支付成功后原样返回给我们）一定要填写西瓜的订单号，使用payInfo.getXgOrderId()获取
     * </p>
     * 
     * @param activity
     * @param payInfo
     * @param payCallBack
     * @see com.xgsdk.client.inner.XGChannel#pay(android.app.Activity,
     *      com.xgsdk.client.api.entity.PayInfo,
     *      com.xgsdk.client.api.callback.PayCallBack)
     */
    @Override
    public void pay(Activity activity, PayInfo payInfo, PayCallBack payCallBack) {

        payCallBack.onSuccess("pay success");// 当支付成功后，调用payCallBack.onSuccess方法，
        updateOrder(activity, payInfo);// 支付成功后，调用XGChannel中的updateOrder方法，通知XGSDK
                                       // SERVER，注意不要改动payInfo中的值

        payCallBack.onFail(XGErrorCode.PAY_FAILED, "若渠道返回了失败信息，请填写到此处");// 若支付失败，调用失败payCallBack.onFail，其中

        payCallBack.onCancel("pay cancel");// 当取消支付时，调用payCallBack.onCancel
        cancelOrder(activity, payInfo.getXgOrderId());// 当取消支付时，调用XGChannel中的cancelOrder方法，通知XGSDK
                                                      // SERVER
    }

}

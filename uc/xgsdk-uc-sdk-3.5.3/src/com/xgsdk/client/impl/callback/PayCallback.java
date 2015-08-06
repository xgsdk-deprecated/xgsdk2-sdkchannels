
package com.xgsdk.client.impl.callback;

import cn.uc.gamesdk.UCCallbackListener;
import cn.uc.gamesdk.UCGameSDKStatusCode;
import cn.uc.gamesdk.info.OrderInfo;

import com.xgsdk.client.api.XGErrorCode;
import com.xgsdk.client.api.callback.PayCallBack;
import com.xgsdk.client.api.entity.PayInfo;
import com.xgsdk.client.core.utils.XGLog;
import com.xgsdk.client.impl.XGChannelImpl;

import android.app.Activity;

public class PayCallback implements UCCallbackListener<OrderInfo> {
    private Activity mActivity;
    private String mOrderId;
    private PayCallBack mPayCallBack;
    private PayInfo mPayInfo;
    private XGChannelImpl mImpl;

    public PayCallback(Activity act, String orderId, PayCallBack payCallBack,
            PayInfo payInfo, XGChannelImpl impl) {
        mActivity = act;
        mOrderId = orderId;
        mPayCallBack = payCallBack;
        mPayInfo = payInfo;
        mImpl = impl;
    }

    @Override
    public void callback(int statudcode, OrderInfo orderInfo) {
        if (statudcode == UCGameSDKStatusCode.NO_INIT) {
            XGLog.i("支付结果 statusCode = " + statudcode + "msg = "
                    + "PAY NOT INIT");
            try {
                mImpl.cancelOrder(mActivity, mPayInfo.getXgOrderId());
            } catch (Exception e) {
                XGLog.e("pay fail, exception is :" + e.getMessage(), e);
            }
            mPayCallBack.onFail(XGErrorCode.PAY_FAILED, "pay failed not init");
        }
        if (statudcode == UCGameSDKStatusCode.SUCCESS) {
            // 订单生成生成，非充值成功，充值结果由服务端回调判断
            if (orderInfo != null) {
                String ordered = orderInfo.getOrderId();// 获取订单号
                float amount = orderInfo.getOrderAmount();// 获取订单金额
                int payWay = orderInfo.getPayWay();// 获取充值类型，具体可参考支付通道编码列表
                String payWayName = orderInfo.getPayWayName();// 充值类型的中文名称
                try {
                    mImpl.updateOrder(mActivity, mPayInfo);
                    XGLog.i("支付成功" + "channelOrderId = " + ordered
                            + "total price = " + amount + "payWay = " + payWay
                            + "payWayName = " + payWayName + "XGOrderId = "
                            + mOrderId);
                    mPayCallBack.onSuccess("支付成功");

                } catch (Exception e) {
                    XGLog.e("支付成功 , 异常是" + e.getMessage());
                    mPayCallBack.onFail(XGErrorCode.PAY_FAILED, "支付异常跳出");
                }
            }
        }
        if (statudcode == UCGameSDKStatusCode.PAY_USER_EXIT) {
            XGLog.i("支付结果 statusCode = " + statudcode + " msg = "
                    + "PAY_USER_EXIT");
            try {
                mImpl.cancelOrder(mActivity, mPayInfo.getXgOrderId());
                mPayCallBack.onCancel("pay cancel");
            } catch (Exception e) {
                XGLog.e("pay fail, exception is :" + e.getMessage(), e);
            }
        }

    }

}

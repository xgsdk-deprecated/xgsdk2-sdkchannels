
package com.xgsdk.client;

import android.content.Context;

public class SdkConfig {
    public static final String AppID_KEY = "AppID";
    public static final String AppKey_KEY = "AppKey";

    private static String tmpAppId;
    private static String tmpAppKey;

    public static String getAppId(Context context) {
        if (tmpAppId != null) {
            return tmpAppId;
        }
        return tmpAppId = ProductInfo.getXGConfig(context, AppID_KEY, null);
    }

    public static String getAppKey(Context context) {
        if (tmpAppKey != null) {
            return tmpAppKey;
        }
        return tmpAppKey = ProductInfo.getXGConfig(context,AppKey_KEY, null);
    }

}

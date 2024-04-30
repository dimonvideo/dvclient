package com.dimonvideo.client.util;

import android.content.Context;
import android.util.Log;

import com.huawei.agconnect.AGConnectInstance;
import com.huawei.hms.analytics.HiAnalytics;
import com.huawei.hms.analytics.HiAnalyticsInstance;
import com.huawei.hms.analytics.HiAnalyticsTools;
import com.huawei.hms.push.HmsMessaging;

import com.dimonvideo.client.Config;

public class Analytics {
    static HiAnalyticsInstance instance;

    public static void init(Context context) {
        HiAnalyticsTools.enableLog();
        instance = HiAnalytics.getInstance(context);
        instance.setUserProfile("dv", "hms");
        if (AGConnectInstance.getInstance() == null) {
            AGConnectInstance.initialize(context);
        }


    }

}
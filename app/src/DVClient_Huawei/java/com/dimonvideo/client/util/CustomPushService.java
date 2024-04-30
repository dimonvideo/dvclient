package com.dimonvideo.client.util;


import android.util.Log;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

import com.dimonvideo.client.Config;

public class CustomPushService extends HmsMessageService {
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.i(Config.TAG, "receive token:" + token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        try {

            Log.i(Config.TAG, "getSignal: " + message.getData());



        } catch (Exception ignored) {

        }

    }
}
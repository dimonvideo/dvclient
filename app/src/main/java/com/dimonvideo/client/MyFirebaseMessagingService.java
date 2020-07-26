package com.dimonvideo.client;

import android.content.Intent;
import android.util.Log;

import com.dimonvideo.client.util.MessageEvent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.greenrobot.eventbus.EventBus;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private LocalBroadcastManager broadcaster;

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            String count_pm = remoteMessage.getData().get("count_pm");
            Log.e("pm", action);

            if (action != null && !action.isEmpty() && action.equals("new_pm")) {

                Intent intent = new Intent();
                intent.putExtra("count_pm", count_pm);
                intent.setAction("com.dimonvideo.client");
                sendBroadcast(intent);


            }

            if (action != null && !action.isEmpty() && action.equals("new")) {


            }

        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("tag", "Refreshed token: " + token);
    }
}

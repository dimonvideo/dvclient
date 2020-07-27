package com.dimonvideo.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.dimonvideo.client.util.MessageEvent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            String count_pm = remoteMessage.getData().get("count_pm");

            if (!action.isEmpty() && action.equals("new_pm")) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor;
                editor = sharedPrefs.edit();
                assert count_pm != null;
                editor.putInt("pm_unread", Integer.parseInt(count_pm));
                editor.apply();
                Log.e("pmservice", ""+count_pm);
                Intent local = new Intent();
                local.setAction("com.dimonvideo.client.PM");
                this.sendBroadcast(local);


            }

            if (!action.isEmpty() && action.equals("new")) {


            }

        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("tag", "Refreshed token: " + token);
    }
}

package com.dimonvideo.client.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if (action != null) {
                String id = intent.getStringExtra("id");

                Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

                if (action.equals("deletePm")) {

                    performAction1(context, id);


                } else if (action.equals("replyPm")) {

                    performAction2(context, id);

                }

                assert id != null;
                NotificationManagerCompat.from(context.getApplicationContext()).cancel(Integer.parseInt(id));
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) context.sendBroadcast(it);

            }

        }
    }

    // delete PM from notify
    public void performAction1(Context context, String id){
        NetworkUtils.deletePm(context, Integer.parseInt(id), 0);
    }

    // open PM fragment from notify
    public void performAction2(Context context, String id){
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra("action", "PmFragment");
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            context.startActivity(notificationIntent);
        } else {

            NetworkUtils.readPm(context, Integer.parseInt(id));


        }
    }
}

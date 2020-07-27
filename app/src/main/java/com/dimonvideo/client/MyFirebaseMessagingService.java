package com.dimonvideo.client;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import com.dimonvideo.client.util.MessageEvent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            String count_pm = remoteMessage.getData().get("count_pm");

            if (!action.isEmpty() && action.equals("new_pm")) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                final boolean dvc_pm_notify = sharedPrefs.getBoolean("dvc_pm_notify", false);
                SharedPreferences.Editor editor;
                editor = sharedPrefs.edit();
                assert count_pm != null;
                editor.putInt("pm_unread", Integer.parseInt(count_pm));
                editor.apply();
                Intent local = new Intent();
                local.setAction("com.dimonvideo.client.PM");
                this.sendBroadcast(local);
                if ((Integer.parseInt(count_pm) > 0) && (!dvc_pm_notify))
                    generateNotification(getApplicationContext(), remoteMessage.getData().get("subj"), Objects.requireNonNull(remoteMessage.getData().get("text")).substring(0, 100));

                Log.e("pm", "---"+count_pm);
            }

            if (!action.isEmpty() && action.equals("new")) {


            }

        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("tag", "Refreshed token: " + token);
    }

    private void generateNotification(Context context, String msg, String text) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "dimonvideo.client";
        String channelName = "PM";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(mChannel);
        }

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.putExtra("action", "PmFragment");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);

        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        mBuilder.setContentTitle(msg);
        mBuilder.setContentText(text);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);
        assert notificationManager != null;
        notificationManager.notify(1, mBuilder.build());
    }

}

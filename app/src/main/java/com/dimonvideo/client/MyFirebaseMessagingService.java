package com.dimonvideo.client;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dimonvideo.client.util.ActionReceiver;
import com.dimonvideo.client.util.GetToken;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

public class MyFirebaseMessagingService extends FirebaseMessagingService {


    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            String count_pm = remoteMessage.getData().get("count_pm");
            int id = Integer.parseInt(Objects.requireNonNull(remoteMessage.getData().get("id")));

            assert action != null;
            if (action.equals("new_pm")) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                final boolean dvc_pm_notify = sharedPrefs.getBoolean("dvc_pm_notify", false);
                final String is_pm = sharedPrefs.getString("dvc_pm", "off");
                SharedPreferences.Editor editor;
                editor = sharedPrefs.edit();
                assert count_pm != null;
                editor.putInt("pm_unread", Integer.parseInt(count_pm));
                editor.apply();

                if ((Integer.parseInt(count_pm) > 0) && (!dvc_pm_notify) && (is_pm.equals("on"))) {



                    Intent local = new Intent();
                    local.setAction(Config.INTENT_NEW_PM);
                    local.putExtra("pm_unread", count_pm);
                    local.putExtra("id", id);
                    getApplicationContext().sendBroadcast(local);

                    Log.e(Config.TAG, "Send broadcast to PM #" + id);

                    getBitmapAsync(getApplicationContext(),
                            Objects.requireNonNull(remoteMessage.getData().get("subj")),
                            Objects.requireNonNull(remoteMessage.getData().get("text")), id,
                            Objects.requireNonNull(remoteMessage.getData().get("image")));
                }
            }

        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        GetToken.getToken(this);
    }

    private void getBitmapAsync(Context context, String msg, String text, int id, String imageUrl) {

        final Bitmap[] bitmap = {null};

        Glide.with(getApplicationContext())
                .asBitmap()
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                        bitmap[0] = resource;
                        generateNotification(context, msg, text, id, bitmap[0]);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void generateNotification(Context context, String msg, String text, int id, Bitmap bitmap) {

        NotificationManager notificationManager;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "dimonvideo.client";
        String channelName = "PM";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(mChannel);
        }


        Log.e(Config.TAG, "Notify new PM #" + id);

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.putExtra("action", "PmFragment");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), id - 100, notificationIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else pendingIntent = PendingIntent.getActivity(getApplicationContext(), id - 100, notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        Intent intentAction = new Intent(context, ActionReceiver.class);
        intentAction.putExtra("action", "deletePm");
        intentAction.putExtra("id", String.valueOf(id));

        PendingIntent pIntentDelete;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pIntentDelete = PendingIntent.getBroadcast(context, id - 200, intentAction, PendingIntent.FLAG_CANCEL_CURRENT  | PendingIntent.FLAG_IMMUTABLE);
        } else pIntentDelete = PendingIntent.getBroadcast(context, id - 200, intentAction, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intentAction2 = new Intent(context, ActionReceiver.class);
        intentAction2.putExtra("action", "replyPm");
        intentAction2.putExtra("id", String.valueOf(id));

        PendingIntent pIntentReply;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pIntentReply = PendingIntent.getBroadcast(context, id - 300, intentAction2, PendingIntent.FLAG_CANCEL_CURRENT  | PendingIntent.FLAG_IMMUTABLE);
        } else pIntentReply = PendingIntent.getBroadcast(context, id - 300, intentAction2, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId);

        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setLargeIcon(bitmap);
        mBuilder.setContentTitle(msg);
        mBuilder.setContentText(text);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            mBuilder.addAction(android.R.drawable.stat_notify_more, getString(R.string.tab_pm), pIntentReply);
        } else {
            mBuilder.addAction(android.R.drawable.stat_notify_more, getString(R.string.pm_read), pIntentReply);
        }
        mBuilder.addAction(android.R.drawable.ic_delete, getString(R.string.pm_delete), pIntentDelete);

        assert notificationManager != null;
        notificationManager.notify(id, mBuilder.build());
    }

}

package com.dimonvideo.client;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dimonvideo.client.util.ActionReceiver;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.GetToken;
import com.dimonvideo.client.util.MessageEvent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private String razdel;

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        razdel = event.razdel;
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");
            String count_pm = remoteMessage.getData().get("count_pm");
            int id = Integer.parseInt(Objects.requireNonNull(remoteMessage.getData().get("id")));

            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
            }

            if (action != null && action.equals("new_pm")) {
                final boolean dvc_pm_notify = AppController.getInstance().isPmNotify();
                final String is_pm = AppController.getInstance().isPm();
                if (count_pm != null) {
                    AppController.getInstance().putPmUnread(Integer.parseInt(count_pm));
                }

                if ((Integer.parseInt(Objects.requireNonNull(count_pm)) > 0) && (!dvc_pm_notify) && (is_pm.equals("on"))) {

                    EventBus.getDefault().post(new MessageEvent(razdel, null, null, count_pm, "restored", null));
                    Log.e("---", "MyFirebaseMessagingService razdel: "+razdel);
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
                .diskCacheStrategy(DiskCacheStrategy.ALL)
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
        notificationIntent.putExtra("action", "PmFragmentTabs");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent;
        pendingIntent = PendingIntent.getActivity(getApplicationContext(), id - 100, notificationIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Intent intentAction = new Intent(context, ActionReceiver.class);
        intentAction.putExtra("action", "deletePm");
        intentAction.putExtra("id", String.valueOf(id));

        PendingIntent pIntentDelete;
        pIntentDelete = PendingIntent.getBroadcast(context, id - 200, intentAction, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent intentAction2 = new Intent(context, ActionReceiver.class);
        intentAction2.putExtra("action", "replyPm");
        intentAction2.putExtra("id", String.valueOf(id));

        PendingIntent pIntentReply;
        pIntentReply = PendingIntent.getBroadcast(context, id - 300, intentAction2, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

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

    @Override
    public void onCreate() {
        super.onCreate();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

}

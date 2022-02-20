package com.dimonvideo.client.util;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.dimonvideo.client.MainActivity;

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            String id = intent.getStringExtra("id");
            Log.e("pmse", "---" + id);

            assert id != null;
            if (Integer.parseInt(id) > 0) {
                NotificationManager notificationManager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);

                assert notificationManager != null;
                notificationManager.cancel(Integer.parseInt(id));
            }

            assert action != null;
            if (action.equals("deletePm")) {
                performAction1(context, id);
            } else if (action.equals("replyPm")) {
                performAction2(context, id);

            }

            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) context.sendBroadcast(it); else NotificationManagerCompat.from(context.getApplicationContext()).cancelAll();

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
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(notificationIntent);

    }
}

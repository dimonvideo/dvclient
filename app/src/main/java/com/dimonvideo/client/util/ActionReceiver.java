package com.dimonvideo.client.util;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            String id = intent.getStringExtra("id");

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
            //This is used to close the notification tray
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        }
    }
    public void performAction1(Context context, String id){

        NetworkUtils.deletePm(context, Integer.parseInt(id), 0);


    }

    public void performAction2(Context context, String id){

    }
}

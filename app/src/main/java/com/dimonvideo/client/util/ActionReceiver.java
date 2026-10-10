/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.util.pm.PmDeletionQueue;

public class ActionReceiver extends BroadcastReceiver {

    /** Routes notification actions without dismissing a deletion before its intent is durable. */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if (action != null) {
                String id = intent.getStringExtra("id");
                if (id == null) return;

                Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

                if (action.equals("deletePm")) {

                    performAction1(context, id);


                } else if (action.equals("replyPm")) {

                    performAction2(context, id);
                    NotificationManagerCompat.from(context.getApplicationContext()).cancel(Integer.parseInt(id));

                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) context.sendBroadcast(it);

            }

        }
    }

    /** Keeps the receiver alive until the shared queue commits and dismisses its notification. */
    public void performAction1(Context context, String id){
        int messageId;
        try {
            messageId = Integer.parseInt(id);
        } catch (NumberFormatException exception) {
            return;
        }
        PendingResult pending = goAsync();
        try {
            PmDeletionQueue.enqueue(context, messageId, pending::finish, pending::finish);
        } catch (RuntimeException exception) {
            pending.finish();
        }
    }

    // open PM fragment from notify
    public void performAction2(Context context, String id){
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra("action", "PmFragmentTabs");
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            context.startActivity(notificationIntent);
        } else {

            NetworkUtils.readPm(context, Integer.parseInt(id));


        }
    }
}

package com.dimonvideo.client.util.pm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;

/** Keeps personal-message notifications consistent with durable, account-bound deletions. */
public final class PmNotifications {
    public static final String CHANNEL_ID = "dimonvideo.client";

    /** Prevents instantiation of the shared notification coordinator. */
    private PmNotifications() { }

    /**
     * Publishes a loaded push only while its original account is active and its message is undeleted.
     * The queue serializes this lookup with deletion commits; main-thread publication and acceptance
     * therefore cannot leave a late avatar notification visible after accepted deletion.
     */
    @SuppressLint("MissingPermission")
    public static void publish(Context context, String accountKey, int messageId,
                               Notification notification) {
        if (accountKey == null || messageId <= 0 || notification == null) return;
        Context appContext = context.getApplicationContext();
        PmDeletionQueue.getDeletionState(appContext, accountKey, messageId, state -> {
            if (state != PmDeletionQueue.DeletionState.ABSENT) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return;
            try {
                NotificationManager manager = appContext.getSystemService(NotificationManager.class);
                if (manager != null) manager.notify(messageId, notification);
            } catch (RuntimeException exception) {
                // An unavailable notification service must not fail the accepted message operation.
            }
        });
    }

    /** Removes only this message's untagged notification without affecting tagged download progress. */
    public static void dismiss(Context context, int messageId) {
        if (messageId <= 0) return;
        try {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.cancel(messageId);
        } catch (RuntimeException exception) {
            // Notification cleanup is best effort and never changes durable deletion acceptance.
        }
    }

    /** Clears the PM channel when entering the mailbox while preserving other application notifications. */
    public static void dismissAll(Context context) {
        try {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;
            for (StatusBarNotification notification : manager.getActiveNotifications()) {
                if (CHANNEL_ID.equals(notification.getNotification().getChannelId())) {
                    manager.cancel(notification.getTag(), notification.getId());
                }
            }
        } catch (RuntimeException exception) {
            // Opening the mailbox must still work if the notification service is unavailable.
        }
    }

    /**
     * Repairs notification cleanup lost with a process after a deletion commit or worker completion.
     * Inspects only displayed PMs, including completed receipts excluded from the live work queue.
     */
    public static void reconcile(Context context) {
        Context appContext = context.getApplicationContext();
        String accountKey = PmDeletionQueue.currentAccountKey();
        if (accountKey == null) return;
        try {
            NotificationManager manager = appContext.getSystemService(NotificationManager.class);
            if (manager == null) return;
            for (StatusBarNotification notification : manager.getActiveNotifications()) {
                if (!CHANNEL_ID.equals(notification.getNotification().getChannelId())) continue;
                PmDeletionQueue.getDeletionState(appContext, accountKey, notification.getId(), state -> {
                    if (state == PmDeletionQueue.DeletionState.PENDING
                            || state == PmDeletionQueue.DeletionState.COMPLETED) {
                        dismiss(appContext, notification.getId());
                    }
                });
            }
        } catch (RuntimeException exception) {
            // A later startup can reconcile notifications; accepted work remains recoverable.
        }
    }
}

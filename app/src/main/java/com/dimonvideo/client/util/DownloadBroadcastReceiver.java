/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.util;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.dimonvideo.client.R;

public class DownloadBroadcastReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "DownloadBroadcastReceiver";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            handleDownloadComplete(context, intent, downloadManager);
        }
    }

    private void handleDownloadComplete(Context context, Intent intent, DownloadManager downloadManager) {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        if (downloadManager == null) {
            Log.e(TAG, "DownloadManager is null");
            return;
        }

        if (progressRunnable != null) {
            handler.removeCallbacks(progressRunnable);
        }

        try (Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
            if (cursor.moveToFirst()) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(statusIndex);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                createNotificationChannel(context);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.baseline_download_for_offline_24)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    builder.setContentTitle(context.getString(R.string.download_complete))
                            .setContentText(context.getString(R.string.download_complete));
                    Toast.makeText(context, context.getString(R.string.download_complete), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Download completed: ID=" + downloadId);
                } else {
                    int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(reasonIndex);
                    builder.setContentTitle(context.getString(R.string.error_network))
                            .setContentText(context.getString(R.string.error_network));
                    Log.e(TAG, "Download failed: ID=" + downloadId + ", Reason=" + reason);
                }

                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                notificationManager.notify(NOTIFICATION_ID, builder.build());

            }
        }
    }

    // Создание канала уведомлений для Android 8.0+
    private static void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(context.getString(R.string.download_channel_description));
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    // Запуск мониторинга прогресса
    public static void startProgressMonitoring(Context context, long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            Log.e(TAG, "DownloadManager is null");
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null");
            return;
        }

        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_cloud_download_24)
                .setContentTitle(context.getString(R.string.downloading))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setProgress(100, 0, false);

        DownloadBroadcastReceiver receiver = new DownloadBroadcastReceiver();
        receiver.progressRunnable = new Runnable() {
            @Override
            public void run() {
                try (Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
                    if (cursor.moveToFirst()) {
                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = cursor.getInt(statusIndex);
                        if (status == DownloadManager.STATUS_RUNNING) {
                            int bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                            int bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                            long bytesDownloaded = cursor.getLong(bytesDownloadedIndex);
                            long bytesTotal = cursor.getLong(bytesTotalIndex);
                            if (bytesTotal > 0) {
                                int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                                builder.setProgress(100, progress, false);
                                notificationManager.notify(NOTIFICATION_ID, builder.build());
                                Log.d(TAG, "Progress: " + progress + "% for ID=" + downloadId);
                            }
                            handler.postDelayed(this, 1000); // Обновлять каждую секунду
                        } else {
                            handler.removeCallbacks(this); // Остановить, если не выполняется
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Progress update error: " + e.getMessage());
                    handler.removeCallbacks(this);
                }
            }
        };
        handler.post(receiver.progressRunnable);
    }
}
/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.util;

import android.Manifest;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.dimonvideo.client.R;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Обрабатывает завершение загрузок и ограниченный по времени мониторинг их прогресса. */
public class DownloadBroadcastReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "DownloadBroadcastReceiver";
    private static final long PROGRESS_INTERVAL_MS = 1000;
    private static final long MAX_MONITORING_TIME_MS = TimeUnit.HOURS.toMillis(6);
    private static final Handler BACKGROUND_HANDLER = createBackgroundHandler();

    // Реестр читается и изменяется только в BACKGROUND_HANDLER, в том числе из новых receiver.
    private static final Map<Long, DownloadMonitor> MONITORS = new HashMap<>();

    /** Передаёт запрос DownloadManager в фон, сохраняя receiver живым до конца обработки. */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            return;
        }
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        Context applicationContext = context.getApplicationContext();
        if (downloadId <= 0 || applicationContext == null) {
            return;
        }

        PendingResult pendingResult = goAsync();
        BACKGROUND_HANDLER.post(() -> {
            try {
                handleDownloadComplete(applicationContext, downloadId);
            } finally {
                pendingResult.finish();
            }
        });
    }

    /** Создаёт один фоновый поток для всех запросов и изменений реестра мониторов. */
    private static Handler createBackgroundHandler() {
        HandlerThread thread = new HandlerThread("download-monitor", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        return new Handler(thread.getLooper());
    }

    /** Останавливает монитор того же ID и сообщает итог загрузки без удержания Activity. */
    private static void handleDownloadComplete(Context context, long downloadId) {
        stopMonitoring(downloadId);
        cancelNotification(context, downloadId);
        DownloadManager downloadManager = context.getSystemService(DownloadManager.class);
        if (downloadManager == null) {
            Log.e(TAG, "DownloadManager is null");
            return;
        }

        try (Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED) {
                return;
            }

            boolean successful = status == DownloadManager.STATUS_SUCCESSFUL;
            int message = successful ? R.string.download_complete : R.string.error_network;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_download_for_offline_24)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentTitle(context.getString(message))
                    .setContentText(context.getString(message));

            if (successful) {
                new Handler(context.getMainLooper()).post(() -> Toast.makeText(context,
                        context.getString(R.string.download_complete), Toast.LENGTH_SHORT).show());
                Log.d(TAG, "Download completed: ID=" + downloadId);
            } else {
                int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                Log.e(TAG, "Download failed: ID=" + downloadId + ", Reason=" + reason);
            }
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                createNotificationChannel(context, notificationManager);
            }
            notifySafely(context, downloadId, builder);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to read completed download: ID=" + downloadId, e);
        }
    }

    /** Создаёт канал уведомлений; минимальная поддерживаемая версия Android уже имеет каналы. */
    private static void createNotificationChannel(Context context, NotificationManager notificationManager) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.download_channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(context.getString(R.string.download_channel_description));
        notificationManager.createNotificationChannel(channel);
    }

    /** Публикует уведомление отдельной загрузки с проверкой разрешения и доступности сервиса. */
    private static boolean notifySafely(Context context, long downloadId, NotificationCompat.Builder builder) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null || (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)) {
            return false;
        }
        try {
            if (!notificationManager.areNotificationsEnabled()) {
                return false;
            }
            notificationManager.notify(notificationTag(downloadId), NOTIFICATION_ID, builder.build());
            return true;
        } catch (RuntimeException e) {
            // Разрешение может быть отозвано между проверкой и notify, сервис может быть недоступен.
            Log.w(TAG, "Unable to show download notification: ID=" + downloadId, e);
            return false;
        }
    }

    /** Возвращает уникальный tag, чтобы параллельные загрузки не заменяли чужие уведомления. */
    private static String notificationTag(long downloadId) {
        return "download:" + downloadId;
    }

    /** Снимает уведомление прогресса, в том числе при ошибке запроса или истечении таймаута. */
    private static void cancelNotification(Context context, long downloadId) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            try {
                notificationManager.cancel(notificationTag(downloadId), NOTIFICATION_ID);
            } catch (RuntimeException e) {
                Log.w(TAG, "Unable to cancel download notification: ID=" + downloadId, e);
            }
        }
    }

    /**
     * Запускает монитор отдельной загрузки максимум на шесть часов, используя application context.
     * Повторный вызов заменяет старую задачу того же ID; запросы выполняются вне UI-потока.
     */
    public static void startProgressMonitoring(Context context, long downloadId) {
        if (context == null || downloadId <= 0) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        if (applicationContext == null) {
            return;
        }
        BACKGROUND_HANDLER.post(() -> {
            stopMonitoring(downloadId);
            DownloadManager downloadManager = applicationContext.getSystemService(DownloadManager.class);
            if (downloadManager == null) {
                Log.e(TAG, "DownloadManager is null");
                return;
            }
            NotificationManager notificationManager = applicationContext.getSystemService(NotificationManager.class);
            if (notificationManager == null) {
                return;
            }
            try {
                createNotificationChannel(applicationContext, notificationManager);
                DownloadMonitor monitor = new DownloadMonitor(applicationContext, downloadManager, downloadId);
                MONITORS.put(downloadId, monitor);
                BACKGROUND_HANDLER.post(monitor);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to start download monitoring: ID=" + downloadId, e);
            }
        });
    }

    /** Останавливает только монитор указанного ID; системная загрузка продолжает работу. */
    public static void stopProgressMonitoring(long downloadId) {
        BACKGROUND_HANDLER.post(() -> stopMonitoring(downloadId));
    }

    /** Централизованно удаляет задачу и уведомление; вызывается только из фонового обработчика. */
    private static void stopMonitoring(long downloadId) {
        DownloadMonitor monitor = MONITORS.remove(downloadId);
        if (monitor != null) {
            BACKGROUND_HANDLER.removeCallbacks(monitor);
            cancelNotification(monitor.context, downloadId);
        }
    }

    /** Одна задача прогресса; не содержит ссылок на receiver, Activity или её View. */
    private static final class DownloadMonitor implements Runnable {
        private final Context context;
        private final DownloadManager downloadManager;
        private final long downloadId;
        private final long startedAt = SystemClock.elapsedRealtime();
        private final NotificationCompat.Builder builder;

        /** Сохраняет только application context и параметры ограниченного по времени мониторинга. */
        private DownloadMonitor(Context context, DownloadManager downloadManager, long downloadId) {
            this.context = context;
            this.downloadManager = downloadManager;
            this.downloadId = downloadId;
            builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_cloud_download_24)
                    .setContentTitle(context.getString(R.string.downloading))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true);
        }

        /** Проверяет прогресс в фоне и отменяет задачу во всех ветках завершения или ошибки. */
        @Override
        public void run() {
            if (MONITORS.get(downloadId) != this) {
                return;
            }
            if (SystemClock.elapsedRealtime() - startedAt >= MAX_MONITORING_TIME_MS) {
                stopMonitoring(downloadId);
                return;
            }

            try (Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
                if (cursor == null || !cursor.moveToFirst()) {
                    stopMonitoring(downloadId);
                    return;
                }
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status != DownloadManager.STATUS_RUNNING && status != DownloadManager.STATUS_PENDING
                        && status != DownloadManager.STATUS_PAUSED) {
                    stopMonitoring(downloadId);
                    return;
                }

                long downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                int progress = total > 0 ? (int) Math.min(100, Math.max(0, downloaded * 100.0 / total)) : 0;
                builder.setProgress(100, progress, total <= 0);
                if (!notifySafely(context, downloadId, builder)) {
                    stopMonitoring(downloadId);
                    return;
                }
                long remaining = MAX_MONITORING_TIME_MS - (SystemClock.elapsedRealtime() - startedAt);
                if (remaining <= 0) {
                    stopMonitoring(downloadId);
                } else {
                    BACKGROUND_HANDLER.postDelayed(this, Math.min(PROGRESS_INTERVAL_MS, remaining));
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Progress update error: ID=" + downloadId, e);
                stopMonitoring(downloadId);
            }
        }
    }
}

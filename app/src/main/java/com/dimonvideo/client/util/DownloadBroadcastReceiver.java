/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.util;
import static android.content.Context.DOWNLOAD_SERVICE;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;

import com.dimonvideo.client.R;

public class DownloadBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();



        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {

            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);

            if (downloadManager != null) {
                try (Cursor c = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                            Toast.makeText(context, context.getString(R.string.download_complete), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }


        }
    }
}
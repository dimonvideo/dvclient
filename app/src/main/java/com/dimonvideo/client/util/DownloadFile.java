package com.dimonvideo.client.util;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;

public class DownloadFile {

    public static void download(Context context, String link) {
        DownloadManager downloadManager;

        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        try {
            Uri uri = Uri.parse(link);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            assert downloadManager != null;
            downloadManager.enqueue(request);
        } catch (Throwable ignored) {
        }
    }
}

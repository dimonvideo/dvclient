package com.dimonvideo.client.util;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;

import com.dimonvideo.client.AllContent;

public class DownloadFile {

    private static Context context;

    public static void download(Context context, String link) {
        DownloadManager downloadManager;

        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        try {
            Uri uri = Uri.parse(link);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            downloadManager.enqueue(request);
        } catch (Throwable ignored) {
        }
    }
}

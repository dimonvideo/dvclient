package com.dimonvideo.client.util;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.URLUtil;

public class DownloadFile {

    public static void download(Context context, String link) {
        DownloadManager downloadManager;

        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        try {
            Uri uri = Uri.parse(link);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,    //Download folder
                    URLUtil.guessFileName(link, null, null));  //Name of file
            request.allowScanningByMediaScanner();
            downloadManager.enqueue(request);
        } catch (Throwable ignored) {
        }
    }
}

package com.dimonvideo.client.util;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.dimonvideo.client.R;

public class DownloadFile {

    public static void download(Context context, String link) {
        DownloadManager downloadManager;

        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        try {
            Uri uri = Uri.parse(link);
            Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_LONG).show();
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,    //Download folder
                    URLUtil.guessFileName(link, null, null));  //Name of file
            assert downloadManager != null;
            downloadManager.enqueue(request);
        } catch (Throwable ignored) {
        }
    }
}

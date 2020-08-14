package com.dimonvideo.client.util;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.dimonvideo.client.R;

public class DownloadFile {

    public static void download(Context context, String link) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean is_dvget = sharedPrefs.getBoolean("dvc_dvget", false);

        DownloadManager downloadManager;

        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        Intent intent = new Intent("android.intent.action.MAIN");
        if (isPackageInstalled("com.dv.adm", context.getPackageManager())) intent.setClassName("com.dv.adm", "com.dv.adm.AEditor");
        else if (isPackageInstalled("com.dv.get", context.getPackageManager())) intent.setClassName("com.dv.get", "com.dv.get.AEditor");
        intent.putExtra("android.intent.extra.TEXT", link);
        intent.putExtra("com.dv.get.ACTION_LIST_ADD", link);
        try {
            if ((is_dvget) && ((isPackageInstalled("com.dv.adm", context.getPackageManager())) || (isPackageInstalled("com.dv.get", context.getPackageManager())))) context.startActivity(intent); else
            if (is_dvget) Toast.makeText(context, context.getString(R.string.dvget_not_found), Toast.LENGTH_LONG).show();

        } catch (Throwable ignored) {

        }

        try {
            Uri uri = Uri.parse(link);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,    //Download folder
                    URLUtil.guessFileName(link, null, null));  //Name of file
            assert downloadManager != null;
            if (!is_dvget) Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_LONG).show();
            if (!is_dvget) downloadManager.enqueue(request);
        } catch (Throwable ignored) {
        }
    }

    public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            return packageManager.getApplicationInfo(packageName, 0).enabled;
        }
        catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}

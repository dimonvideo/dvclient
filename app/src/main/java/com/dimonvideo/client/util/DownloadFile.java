package com.dimonvideo.client.util;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.dimonvideo.client.BuildConfig;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;

public class DownloadFile {

    public static void download(Context context, String link, String razdel) {

        boolean is_dvget = AppController.getInstance().isDVGET();
        boolean is_idm = AppController.getInstance().isIDM();

        DownloadManager downloadManager;

        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        // adm - dvget
        Intent intent = new Intent("android.intent.action.MAIN");

        if (is_dvget) {
            if (isPackageInstalled("com.dv.adm", context.getPackageManager()))
                intent.setClassName("com.dv.adm", "com.dv.adm.AEditor");
            else if (isPackageInstalled("com.dv.get", context.getPackageManager()))
                intent.setClassName("com.dv.get", "com.dv.get.AEditor");
        }

        intent.putExtra("android.intent.extra.TEXT", link);
        try {
            if ((is_dvget) && ((isPackageInstalled("com.dv.adm", context.getPackageManager())) ||
                    (isPackageInstalled("com.dv.get", context.getPackageManager())))) context.startActivity(intent);
            else if (is_dvget) {
                Toast.makeText(context, context.getString(R.string.dvget_not_found), Toast.LENGTH_LONG).show();
                String url = "https://play.google.com/store/apps/details?id=com.dv.adm";

                if (BuildConfig.SAMSUNG)
                    url = "https://dimonvideo.ru/android/368661";

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        url));

                context.startActivity(browserIntent);
            }
        } catch (Throwable ignored) {
        }

        // IDM
        if (is_idm) {
            if (isPackageInstalled("idm.internet.download.manager.adm.lite", context.getPackageManager()))
                intent.setClassName("idm.internet.download.manager.adm.lite", "idm.internet.download.manager.Downloader");
            else if (isPackageInstalled("idm.internet.download.manager.plus", context.getPackageManager()))
                intent.setClassName("idm.internet.download.manager.plus", "idm.internet.download.manager.Downloader");
            else if (isPackageInstalled("idm.internet.download.manager", context.getPackageManager()))
                intent.setClassName("idm.internet.download.manager", "idm.internet.download.manager.Downloader");

            intent.putExtra("secure_uri", link);
            intent.setData(Uri.parse(link));
        }

        try {
            if (is_idm) context.startActivity(intent);
        } catch (Throwable ignored) {
            Toast.makeText(context, context.getString(R.string.idm_not_found), Toast.LENGTH_LONG).show();
            String url = "https://play.google.com/store/apps/details?id=idm.internet.download.manager";

            if (BuildConfig.FLAVOR.equals("DVClientSamsung"))
                url = "https://dimonvideo.ru/android/381954";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));

            context.startActivity(browserIntent);
        }

        if ((razdel != null) && ((razdel.equals(Config.TRACKER_RAZDEL)))){

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            try {
                context.startActivity(browserIntent);
                return;
            } catch (Throwable ignored) {
            }

        }

        try {
            Uri uri = Uri.parse(link);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,    //Download folder
                    URLUtil.guessFileName(link, null, null));  //Name of file
            assert downloadManager != null;
            if ((!is_dvget) && (!is_idm)) {
                Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_LONG).show();
                downloadManager.enqueue(request);
            }
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

package com.dimonvideo.client.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class OpenUrl {
    public static void open_url(String url, boolean is_open_link, boolean is_vuploader_play_listtext, Context context, String razdel) {
        if (!is_open_link) {
            Log.e("URL-on", "---  " + url);

            try {
                url = url.replace("https://m.dimonvideo.ru/go/?", "");
                url = url.replace("https://m.dimonvideo.ru/go?", "");
                url = url.replace("https://dimonvideo.ru/go/?", "");
                url = url.replace("https://dimonvideo.ru/go?", "");
            } catch (Throwable ignored) {
            }
            assert url != null;
            String extension = url.substring(url.lastIndexOf(".") + 1);

            switch (extension) {
                case "png":
                case "jpg":
                case "jpeg":
                    ButtonsActions.loadScreen(context, url);
                    break;
                case "apk":
                case "zip":
                case "avi":
                case "mp3":
                case "m4a":
                case "rar":
                    DownloadFile.download(context, url, razdel);
                    break;
                case "mp4":
                    if (is_vuploader_play_listtext) ButtonsActions.PlayVideo(context, url);
                    else DownloadFile.download(context, url, razdel);
                    break;
                default:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        context.startActivity(browserIntent);
                    } catch (Throwable ignored) {
                    }
                    break;
            }
        } else {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                context.startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }
    }
}

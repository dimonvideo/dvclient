package com.dimonvideo.client.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.dimonvideo.client.model.Feed;

public class OpenUrl {
    public static void open_url(String url, boolean is_open_link, Context context) {
        if (!is_open_link) {
            try {
                url = url.replace("/go/?", "");
                url = url.replace("https://m.dimonvideo.ru/go/?", "");
                url = url.replace("https://m.dimonvideo.ru/go?", "");
                url = url.replace("https://dimonvideo.ru/go/?", "");
                url = url.replace("https://dimonvideo.ru/go?", "");
            } catch (Throwable ignored) {
            }
            assert url != null;
            String extension = url.substring(url.lastIndexOf(".") + 1);

       //     Log.d("TAG", "---->" + url + " | " + extension + "\n<----");

            if ((extension.equals("png")) || (extension.equals("jpg")) || (extension.equals("jpeg")))
                ButtonsActions.loadScreen(context, url);

            else if ((extension.equals("apk")) || (extension.equals("zip")) || (extension.equals("avi"))
                    || (extension.equals("mp3"))
                    || (extension.equals("m4a"))
                    || (extension.equals("rar"))
                    || (extension.equals("mp4"))) DownloadFile.download(context, url, Feed.getRazdel());
            else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    context.startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
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

package com.dimonvideo.client.util;

import com.dimonvideo.client.Config;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

public class BBCodes {

    public static String imageCodes(String text, String image_uploaded, String razdel) {

        StringBuilder sb = new StringBuilder(text);
        if ((image_uploaded != null) && (image_uploaded.contains(".png"))) {
            String url = Config.BBCODE_URL + AppController.getInstance().userName("dvclient") + File.separator + image_uploaded;
            String thumb = Config.THUMB_URL + AppController.getInstance().userName("dvclient") + File.separator + "thumbs"  + File.separator + image_uploaded;

            sb.append("[br][url=").append(url).append("][img]").append(thumb).append("[/img][/url]");
        }
        EventBus.getDefault().post(new MessageEvent(razdel, null, null, null, null, null));
        return String.valueOf(sb);
    }
}

package com.dimonvideo.client.util;

import android.graphics.Bitmap;

public class MessageEvent {
    public final String razdel;
    public final String story;
    public final String image_uploaded;
    public String count_pm;
    public String action;
    public Bitmap bitmap;

    public MessageEvent(String razdel, String story, String image_uploaded, String count_pm, String action, Bitmap bitmap) {
        this.razdel = razdel;
        this.story = story;
        this.image_uploaded = image_uploaded;
        this.count_pm = count_pm;
        this.action = action;
        this.bitmap = bitmap;
    }
}

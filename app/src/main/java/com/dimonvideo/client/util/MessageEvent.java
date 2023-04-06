package com.dimonvideo.client.util;

public class MessageEvent {
    public final int razdel;
    public final String story;
    public final String pm;

    public MessageEvent(int razdel, String story, String pm) {
        this.razdel = razdel;
        this.story = story;
        this.pm = pm;
    }
}

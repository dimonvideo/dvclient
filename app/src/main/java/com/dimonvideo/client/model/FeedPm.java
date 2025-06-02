package com.dimonvideo.client.model;

import android.text.Html;
import android.text.Spanned;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.adater.AdapterPm;
import com.dimonvideo.client.util.URLImageParser;

public class FeedPm {
    //Data Variables
    private String imageUrl, title, full_text, date, last_poster_name, text, fullText;
    private int id, Is_new;
    private Long time;
    private Spanned spannedText;
    private Spanned spannedFullText;

    //Getters and Setters

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        if (!imageUrl.startsWith("http")) {
            imageUrl = Config.WRITE_URL + imageUrl;
        }
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getId() {
        return id;
    }
    public void setIs_new(int Is_new) {
        this.Is_new = Is_new;
    }

    public int getIs_new() {
        return Is_new;
    }
    public String getDate() {
        return date;
    }
    public Long getTime() {
        return time;
    }
    public void setTime(Long time) {
        this.time = time;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLast_poster_name() {
        return last_poster_name;
    }

    public void setLast_poster_name(String last_poster_name) {
        this.last_poster_name = last_poster_name;
    }

    public String getFullText() {
        return full_text;
    }

    public void setFullText(String full_text) {
        this.full_text = full_text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Spanned getSpannedText() {
        if (spannedText == null) {
            spannedText = Html.fromHtml(getText(), Html.FROM_HTML_MODE_LEGACY, new URLImageParser(null), new AdapterPm.TagHandler());
        }
        return spannedText;
    }

    public Spanned getSpannedFullText() {
        if (spannedFullText == null) {
            spannedFullText = Html.fromHtml(getFullText(), Html.FROM_HTML_MODE_LEGACY, new URLImageParser(null), new AdapterPm.TagHandler());
        }
        return spannedFullText;
    }
}

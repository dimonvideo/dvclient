/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.model;

import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.adater.AdapterPm;
import com.dimonvideo.client.util.URLImageParser;

public class FeedPm {
    private String imageUrl, title, date, last_poster_name, text, full_text;
    private int id, Is_new;
    private Long time;
    private Spanned spannedText;
    private Spanned spannedFullText;

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

    public void setId(int id) {
        this.id = id;
    }

    public int getIs_new() {
        return Is_new;
    }

    public void setIs_new(int Is_new) {
        this.Is_new = Is_new;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
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

    public Spanned getSpannedText(TextView textView) {
        if (spannedText == null && textView != null) {
            spannedText = Html.fromHtml(getText(), Html.FROM_HTML_MODE_LEGACY, new URLImageParser(textView), new AdapterPm.TagHandler());
        }
        return spannedText != null ? spannedText : Html.fromHtml(getText(), Html.FROM_HTML_MODE_LEGACY, null, new AdapterPm.TagHandler());
    }

    public Spanned getSpannedFullText(TextView textView) {
        if (spannedFullText == null && textView != null) {
            spannedFullText = Html.fromHtml(getFullText(), Html.FROM_HTML_MODE_LEGACY, new URLImageParser(textView), new AdapterPm.TagHandler());
        }
        return spannedFullText != null ? spannedFullText : Html.fromHtml(getFullText(), Html.FROM_HTML_MODE_LEGACY, null, new AdapterPm.TagHandler());
    }
}
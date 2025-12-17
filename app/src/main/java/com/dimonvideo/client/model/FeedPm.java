package com.dimonvideo.client.model;

import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.adater.AdapterPm;
import com.dimonvideo.client.util.URLImageParser;

public class FeedPm {
    private String imageUrl, title, date, last_poster_name;

    // ВАЖНО:
    // previewHtml = то, что сервер отдаёт в full_text (короткое превью)
    // fullHtml    = то, что сервер отдаёт в text (полный HTML)
    private String previewHtml;
    private String fullHtml;
    private Long time;

    private int id, Is_new;

    private Spanned spannedPreview;
    private Spanned spannedFull;

    public String getImageUrl() { return imageUrl; }

    public void setImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.startsWith("http")) {
            imageUrl = Config.WRITE_URL + imageUrl;
        }
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIs_new() { return Is_new; }
    public void setIs_new(int is_new) { Is_new = is_new; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLast_poster_name() { return last_poster_name; }
    public void setLast_poster_name(String last_poster_name) { this.last_poster_name = last_poster_name; }

    // --- Сеттеры из API ---
    // полный HTML (json TAG_TEXT)
    public void setFullHtml(String html) {
        this.fullHtml = html;
        this.spannedFull = null;
    }

    // превью (json TAG_FULL_TEXT)
    public void setPreviewHtml(String html) {
        this.previewHtml = html;
        this.spannedPreview = null;
    }

    public String getFullHtml() { return fullHtml; }
    public String getPreviewHtml() { return previewHtml; }
    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
    public Spanned getSpannedPreview(TextView textView) {
        String src = previewHtml != null ? previewHtml : "";
        if (spannedPreview == null) {
            spannedPreview = Html.fromHtml(
                    src,
                    Html.FROM_HTML_MODE_LEGACY,
                    textView != null ? new URLImageParser(textView) : null,
                    new AdapterPm.TagHandler()
            );
        }
        return spannedPreview;
    }

    public Spanned getSpannedFull(TextView textView) {
        String src = fullHtml != null ? fullHtml : "";
        if (spannedFull == null) {
            spannedFull = Html.fromHtml(
                    src,
                    Html.FROM_HTML_MODE_LEGACY,
                    textView != null ? new URLImageParser(textView) : null,
                    new AdapterPm.TagHandler()
            );
        }
        return spannedFull;
    }
}

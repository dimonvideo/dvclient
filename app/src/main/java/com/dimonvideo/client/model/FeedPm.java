package com.dimonvideo.client.model;

import com.dimonvideo.client.Config;

import java.util.Objects;

/** API data for a private message or member; contains no views or parsed image drawables. */
public class FeedPm {
    private String imageUrl, title, date, last_poster_name;
    // PM API: full_text is the short preview, while text contains the complete HTML body.
    private String previewHtml, fullHtml;
    private Long time;
    private int id, isNew;
    private int sourcePage = 1, sourceFolder, recipientId;
    private String senderName, recipientName;
    private boolean outgoing;

    /** Creates an empty API item for population by the response parser. */
    public FeedPm() { }

    /** Copies API values so asynchronous diffs never observe subsequent parser mutations. */
    public FeedPm(FeedPm source) {
        imageUrl = source.imageUrl;
        title = source.title;
        date = source.date;
        last_poster_name = source.last_poster_name;
        previewHtml = source.previewHtml;
        fullHtml = source.fullHtml;
        time = source.time;
        id = source.id;
        isNew = source.isNew;
        sourcePage = source.sourcePage;
        sourceFolder = source.sourceFolder;
        recipientId = source.recipientId;
        senderName = source.senderName;
        recipientName = source.recipientName;
        outgoing = source.outgoing;
    }

    /** Returns the absolute avatar address. */
    public String getImageUrl() { return imageUrl; }

    /** Resolves relative avatar addresses against the website. */
    public void setImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.startsWith("http")) {
            imageUrl = Config.WRITE_URL + imageUrl;
        }
        this.imageUrl = imageUrl;
    }

    /** Returns the subject or member name. */
    public String getTitle() { return title; }

    /** Stores the subject or member name supplied by the API. */
    public void setTitle(String title) { this.title = title; }

    /** Returns the server message/member identifier. */
    public int getId() { return id; }

    /** Stores the server message/member identifier. */
    public void setId(int id) { this.id = id; }

    /** Returns the API page that supplied this item, used only as a deletion lookup hint. */
    public int getSourcePage() { return sourcePage; }

    /** Stores the real response page without deriving it from the filtered adapter position. */
    public void setSourcePage(int page) { sourcePage = Math.max(1, page); }

    /** Returns the response folder used as a verified deletion lookup hint. */
    public int getSourceFolder() { return sourceFolder; }

    /** Stores the real response folder, including trash entries sent by this account. */
    public void setSourceFolder(int folder) { sourceFolder = folder; }

    /** Returns the original API sender without replacing it with an outgoing display label. */
    public String getSenderName() { return senderName; }

    /** Stores the original API sender used to determine message direction. */
    public void setSenderName(String sender) { senderName = sender; }

    /** Returns the original recipient name for an exact member-ID lookup before an outgoing send. */
    public String getRecipientName() { return recipientName; }

    /** Stores the original recipient independently of row display metadata. */
    public void setRecipientName(String recipient) { recipientName = recipient; }

    /** Returns whether this account sent the original message, including outgoing trash entries. */
    public boolean isOutgoing() { return outgoing; }

    /** Stores direction derived from the original API sender and current authenticated login. */
    public void setOutgoing(boolean outgoing) { this.outgoing = outgoing; }

    /** Returns a positive recipient UID only after an exact member-name lookup succeeds. */
    public int getRecipientId() { return recipientId; }

    /** Stores the resolved outgoing recipient UID on the detail sheet's isolated model copy. */
    public void setRecipientId(int userId) { recipientId = Math.max(0, userId); }

    /** Returns the API unread indicator. */
    public int getIs_new() { return isNew; }

    /** Updates the unread indicator after an API response. */
    public void setIs_new(int isNew) { this.isNew = isNew; }

    /** Returns the formatted date supplied by the API. */
    public String getDate() { return date; }

    /** Stores the formatted date supplied by the API. */
    public void setDate(String date) { this.date = date; }

    /** Returns the sender name or member status label. */
    public String getLast_poster_name() { return last_poster_name; }

    /** Stores the sender name or member status label. */
    public void setLast_poster_name(String name) { last_poster_name = name; }

    /** Stores the complete PM body (text) or member rank HTML. */
    public void setFullHtml(String html) { fullHtml = html; }

    /** Stores the short PM preview (full_text) or member action hint HTML. */
    public void setPreviewHtml(String html) { previewHtml = html; }

    /** Returns the complete HTML; rendering and image lifetime belong to the UI. */
    public String getFullHtml() { return fullHtml; }

    /** Returns the short preview HTML without parsing it on the calling thread. */
    public String getPreviewHtml() { return previewHtml; }

    /** Returns the optional Unix timestamp. */
    public Long getTime() { return time; }

    /** Stores the optional Unix timestamp. */
    public void setTime(Long time) { this.time = time; }

    /** Compares all displayed values so equal API refreshes do not rebind rows. */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FeedPm)) return false;
        FeedPm item = (FeedPm) other;
        return id == item.id && isNew == item.isNew && sourcePage == item.sourcePage
                && sourceFolder == item.sourceFolder && recipientId == item.recipientId && outgoing == item.outgoing
                && Objects.equals(senderName, item.senderName) && Objects.equals(recipientName, item.recipientName)
                && Objects.equals(imageUrl, item.imageUrl)
                && Objects.equals(title, item.title)
                && Objects.equals(date, item.date)
                && Objects.equals(last_poster_name, item.last_poster_name)
                && Objects.equals(previewHtml, item.previewHtml)
                && Objects.equals(fullHtml, item.fullHtml)
                && Objects.equals(time, item.time);
    }

    /** Produces a hash consistent with the complete displayed-value comparison. */
    @Override
    public int hashCode() {
        return Objects.hash(imageUrl, title, date, last_poster_name, previewHtml, fullHtml,
                time, id, isNew, sourcePage, sourceFolder, recipientId, senderName, recipientName, outgoing);
    }
}

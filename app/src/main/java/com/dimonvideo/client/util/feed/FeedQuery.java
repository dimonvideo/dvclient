package com.dimonvideo.client.util.feed;

import com.dimonvideo.client.util.GetRazdelName;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.TreeSet;

/** Captures the section and filters once so requests and their stored pages share one identity. */
public final class FeedQuery {
    public static final String LATEST = "latest";
    public static final String DETAILS = "details";
    public static final String FAVORITES = "favorites";

    private final String sectionKey;
    private final String baseUrl;
    private final String story;
    private final String tabMode;
    private final String categories;
    private final String loginName;
    private final int categoryId;
    private final boolean waiting;

    /** Copies every input, including mutable preference selections, before starting a load. */
    public FeedQuery(String section, int cid, String story, String tabMode, boolean waiting,
                     Set<String> selectedCategories, String loginName) {
        if (tabMode == null) tabMode = LATEST;
        if (!LATEST.equals(tabMode) && !DETAILS.equals(tabMode) && !FAVORITES.equals(tabMode)) {
            throw new IllegalArgumentException("Unknown feed tab");
        }
        this.sectionKey = GetRazdelName.getRazdelName(section, 0);
        this.story = story == null ? "" : story;
        this.tabMode = tabMode;
        this.waiting = waiting;
        this.categoryId = Math.max(0, cid);
        this.loginName = loginName == null ? "" : loginName;
        this.categories = categories(selectedCategories);
        this.baseUrl = GetRazdelName.getRazdelName(section, this.story.isEmpty() ? 2 : 1);
    }

    /** Builds a page URL without changing or re-encoding the original search text. */
    public String url(int page) {
        if (page < 1) throw new IllegalArgumentException("Feed page must be positive");
        String filter = !story.isEmpty() ? "&story=" + encode(story)
                : categoryId > 0 ? "&where=" + categoryId : "";
        if (FAVORITES.equals(tabMode)) {
            return baseUrl + page + filter + "&fav=1&login_name=" + encode(loginName);
        }
        return baseUrl + page + "&c=limit,10," + categories + filter
                + (DETAILS.equals(tabMode) && waiting ? "&st=2" : "");
    }

    /** Hashes the exact first-page request and presentation mode without exposing login or search text. */
    public String scopeKey() {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((tabMode + "\n" + url(1)).getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(hash.length * 2);
            for (byte part : hash) {
                value.append(Character.forDigit((part & 0xff) >>> 4, 16));
                value.append(Character.forDigit(part & 0x0f, 16));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /** Returns the canonical API section used by the legacy index and read marks. */
    public String sectionKey() {
        return sectionKey;
    }

    /** Identifies the full-text presentation independently of translated tab captions. */
    public boolean isDetails() {
        return DETAILS.equals(tabMode);
    }

    /** Allows old rows only when their section-only index can actually identify the requested list. */
    public boolean isLegacyCacheEligible() {
        return LATEST.equals(tabMode) && categoryId == 0 && story.isEmpty()
                && "all".equals(categories) && !"new".equals(sectionKey);
    }

    /** Keeps filtered latest items available to read marking without indexing synthetic search results. */
    public boolean isLatestIndexEligible() {
        return LATEST.equals(tabMode) && story.isEmpty();
    }

    /** Normalizes missing selections and fixes iteration order before creating a cache identity. */
    private static String categories(Set<String> selectedCategories) {
        TreeSet<String> sorted = new TreeSet<>();
        if (selectedCategories != null) {
            for (String category : selectedCategories) {
                if (category != null && !category.isEmpty()) sorted.add(category);
            }
        }
        if (sorted.isEmpty()) return "all";
        StringBuilder result = new StringBuilder();
        for (String category : sorted) {
            if (result.length() > 0) result.append(',');
            result.append(encode(category));
        }
        return result.toString();
    }

    /** Encodes one raw query value using the API's UTF-8 form encoding. */
    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is unavailable", exception);
        }
    }
}

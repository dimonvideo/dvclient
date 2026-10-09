package com.dimonvideo.client.util.feed;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.db.FeedEntity;
import com.dimonvideo.client.model.Feed;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Converts complete API pages and the older sparse index without interpreting their HTML. */
public final class FeedCodec {
    /** Prevents instances of the stateless feed conversion utility. */
    private FeedCodec() { }

    /** Validates the whole response before its rows can replace a previously usable stored page. */
    public static List<Feed> parse(JSONArray response, boolean details) throws JSONException {
        if (response == null) throw new JSONException("Missing feed response");
        List<Feed> feeds = new ArrayList<>(response.length());
        for (int index = 0; index < response.length(); index++) {
            JSONObject row = response.getJSONObject(index);
            Feed feed = new Feed();
            int id = row.getInt(Config.TAG_ID);
            if (id < 0) throw new JSONException("Invalid feed id");
            feed.setId(id);
            feed.setTitle(requiredString(row, Config.TAG_TITLE));
            String section = requiredString(row, Config.TAG_RAZDEL);
            if (section.isEmpty()) throw new JSONException("Missing feed section");
            feed.setRazdel(section);
            String text = requiredString(row, Config.TAG_TEXT);
            String fullText = requiredString(row, Config.TAG_FULL_TEXT);
            feed.setText(details ? fullText : text);
            feed.setFull_text(fullText);
            feed.setImageUrl(image(optionalString(row, Config.TAG_IMAGE_URL)));
            feed.setDate(optionalString(row, Config.TAG_DATE));
            feed.setCategory(optionalString(row, Config.TAG_CATEGORY));
            feed.setHeaders(optionalString(row, Config.TAG_HEADERS));
            feed.setUser(optionalString(row, Config.TAG_USER));
            feed.setSize(optionalString(row, Config.TAG_SIZE));
            feed.setLink(optionalString(row, Config.TAG_LINK));
            feed.setMod(optionalString(row, Config.TAG_MOD));
            feed.setComments(row.getInt(Config.TAG_COMMENTS));
            feed.setHits(row.getInt(Config.TAG_HITS));
            feed.setTime(row.getLong(Config.TAG_TIME));
            feed.setMin(row.getInt(Config.TAG_MIN));
            feed.setPlus(row.getInt(Config.TAG_PLUS));
            feed.setFav(row.getInt(Config.TAG_FAV));
            int status = row.getInt(Config.TAG_STATUS);
            feed.setState(status);
            feed.setStatus(status);
            feed.setPost_id(row.optInt(Config.TAG_POST_ID, id));
            feeds.add(feed);
        }
        return feeds;
    }

    /** Restores only fields available in the old index, supplying safe defaults for absent metadata. */
    public static List<Feed> fromLegacy(List<FeedEntity> entities, boolean details) {
        List<Feed> feeds = new ArrayList<>();
        if (entities == null) return feeds;
        for (FeedEntity entity : entities) {
            if (entity == null || entity.lid < 0 || entity.razdel == null || entity.razdel.isEmpty()) continue;
            Feed feed = new Feed();
            feed.setId(entity.lid);
            feed.setPost_id(entity.lid);
            feed.setTitle(nonNull(entity.title));
            feed.setText(nonNull(details ? entity.fullText : entity.description));
            feed.setFull_text(nonNull(entity.fullText));
            feed.setDate(nonNull(entity.date));
            feed.setTime(entity.timestamp);
            feed.setCategory(nonNull(entity.category));
            feed.setImageUrl(image(entity.img));
            feed.setRazdel(entity.razdel);
            feed.setSize(nonNull(entity.size));
            feed.setLink(nonNull(entity.url));
            feed.setState(entity.state);
            feed.setStatus(entity.state);
            feed.setHeaders("");
            feed.setUser("");
            feed.setMod("");
            feeds.add(feed);
        }
        return feeds;
    }

    /** Builds the legacy latest-feed index used by read marks without discarding full HTML or time. */
    public static List<FeedEntity> entities(List<Feed> feeds) {
        List<FeedEntity> entities = new ArrayList<>(feeds.size());
        for (Feed feed : feeds) {
            FeedEntity entity = new FeedEntity();
            entity.lid = feed.getId();
            entity.title = feed.getTitle();
            entity.description = feed.getText();
            entity.fullText = feed.getFull_text();
            entity.date = feed.getDate();
            entity.timestamp = feed.getTime() == null ? 0 : feed.getTime();
            entity.category = feed.getCategory();
            entity.img = feed.getImageUrl();
            entity.razdel = feed.getRazdel();
            entity.size = feed.getSize();
            entity.url = feed.getLink();
            entity.state = feed.getState();
            entities.add(entity);
        }
        return entities;
    }

    /** Requires a present scalar string field while preserving its characters without HTML conversion. */
    private static String requiredString(JSONObject row, String name) throws JSONException {
        Object value = row.get(name);
        if (value == JSONObject.NULL || value instanceof JSONObject || value instanceof JSONArray) {
            throw new JSONException("Invalid feed field: " + name);
        }
        return String.valueOf(value);
    }

    /** Accepts missing or nullable optional strings while rejecting nested JSON as display content. */
    private static String optionalString(JSONObject row, String name) throws JSONException {
        if (!row.has(name) || row.isNull(name)) return "";
        return requiredString(row, name);
    }

    /** Supplies the site's existing thumbnail placeholder before Feed normalizes the image URL. */
    private static String image(String value) {
        return value == null || value.isEmpty() ? "/images/soon.jpg" : value;
    }

    /** Replaces sparse legacy nulls with the empty strings expected by card rendering. */
    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}

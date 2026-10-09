package com.dimonvideo.client.util.feed;

import android.app.Application;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.db.FeedEntity;
import com.dimonvideo.client.model.Feed;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Validates saved section pages without losing HTML, API metadata or sparse legacy timestamps. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 35, application = Application.class)
public class FeedCodecTest {
    /** Converts every production API field while preserving HTML and entity text byte for byte. */
    @Test
    public void completePagePreservesRawHtmlAndMetadata() throws JSONException {
        Feed feed = FeedCodec.parse(new JSONArray().put(row()), false).get(0);
        assertEquals(42, feed.getId());
        assertEquals(100, feed.getPost_id());
        assertEquals("<b>Название &amp;</b>", feed.getTitle());
        assertEquals("<p>Коротко &#x20; &amp;</p>", feed.getText());
        assertEquals("<div>Полностью<br><a href=\"https://dimonvideo.ru\">ссылка</a></div>", feed.getFull_text());
        assertEquals(Config.WRITE_URL + "/image.jpg", feed.getImageUrl());
        assertEquals("vuploader", feed.getRazdel());
        assertEquals("09.10.2026", feed.getDate());
        assertEquals(Long.valueOf(1791532800L), feed.getTime());
        assertEquals("Категория", feed.getCategory());
        assertEquals("Видео", feed.getHeaders());
        assertEquals("author", feed.getUser());
        assertEquals("5 MB", feed.getSize());
        assertEquals("https://example.com/clip.mp4", feed.getLink());
        assertEquals("https://example.com/mod.txt", feed.getMod());
        assertEquals(3, feed.getComments());
        assertEquals(15, feed.getHits());
        assertEquals(1, feed.getMin());
        assertEquals(7, feed.getPlus());
        assertEquals(1, feed.getFav());
        assertEquals(2, feed.getState());
        assertEquals(2, feed.getStatus());
    }

    /** Presents the full HTML on the details tab without mutating the JSON saved for other modes. */
    @Test
    public void detailsOnlyChangeDisplayedText() throws JSONException {
        JSONArray response = new JSONArray().put(row());
        Feed details = FeedCodec.parse(response, true).get(0);
        assertEquals(details.getFull_text(), details.getText());
        assertEquals("<p>Коротко &#x20; &amp;</p>",
                FeedCodec.parse(response, false).get(0).getText());
        assertEquals("<p>Коротко &#x20; &amp;</p>", response.getJSONObject(0).getString("text"));
    }

    /** Rejects a partially malformed page rather than allowing its valid prefix to replace stored rows. */
    @Test
    public void oneMalformedRowRejectsTheEntireResponse() throws JSONException {
        rejects(new JSONArray().put(row()).put(new JSONObject().put("lid", 5)));
        rejects(new JSONArray().put(row()).put(JSONObject.NULL));
        rejects(new JSONArray().put("not a row"));
        JSONObject missingFullText = row();
        missingFullText.remove("full_text");
        rejects(new JSONArray().put(missingFullText));
        rejects(new JSONArray().put(row().put("title", JSONObject.NULL)));
        rejects(new JSONArray().put(row().put("razdel", "")));
        rejects(new JSONArray().put(row().put("lid", -1)));
        rejects(new JSONArray().put(row().put("time", "invalid")));
        rejects(new JSONArray().put(row().put("text", new JSONArray())));
        rejects(null);
    }

    /** Allows documented empty search results and nullable optional URLs without passing null to Feed. */
    @Test
    public void nullableImagesAndOptionalPostIdsAreSafe() throws JSONException {
        JSONObject source = row().put("image", JSONObject.NULL).put("mod", JSONObject.NULL)
                .put("file_link", JSONObject.NULL).put("lid", 0);
        source.remove("post_id");
        Feed feed = FeedCodec.parse(new JSONArray().put(source), false).get(0);
        assertEquals(0, feed.getId());
        assertEquals(0, feed.getPost_id());
        assertEquals(Config.WRITE_URL + "/images/soon.jpg", feed.getImageUrl());
        assertEquals("", feed.getMod());
        assertEquals("", feed.getLink());
        assertTrue(FeedCodec.parse(new JSONArray(), false).isEmpty());
    }

    /** Preserves all legacy table fields through conversion, including the previously missing timestamp. */
    @Test
    public void entityRoundTripRetainsHtmlAndLegacyColumns() throws JSONException {
        Feed original = FeedCodec.parse(new JSONArray().put(row()), false).get(0);
        FeedEntity entity = FeedCodec.entities(Collections.singletonList(original)).get(0);
        assertEquals(original.getId(), entity.lid);
        assertEquals(original.getTitle(), entity.title);
        assertEquals(original.getText(), entity.description);
        assertEquals(original.getFull_text(), entity.fullText);
        assertEquals(original.getDate(), entity.date);
        assertEquals(original.getTime().longValue(), entity.timestamp);
        assertEquals(original.getCategory(), entity.category);
        assertEquals(original.getImageUrl(), entity.img);
        assertEquals(original.getRazdel(), entity.razdel);
        assertEquals(original.getSize(), entity.size);
        assertEquals(original.getLink(), entity.url);
        assertEquals(original.getState(), entity.state);
        Feed restored = FeedCodec.fromLegacy(Collections.singletonList(entity), false).get(0);
        assertEquals(original.getText(), restored.getText());
        assertEquals(original.getFull_text(), restored.getFull_text());
        assertEquals(original.getTime(), restored.getTime());
        assertEquals(original.getImageUrl(), restored.getImageUrl());
        assertEquals(original.getState(), restored.getStatus());
        Feed details = FeedCodec.fromLegacy(Collections.singletonList(entity), true).get(0);
        assertEquals(original.getFull_text(), details.getText());
    }

    /** Defaults nullable old strings safely and drops rows without a usable section identity. */
    @Test
    public void sparseLegacyRowsDoNotCrashCardRendering() {
        FeedEntity sparse = new FeedEntity();
        sparse.lid = 42;
        sparse.razdel = "comments";
        sparse.timestamp = 123L;
        FeedEntity invalid = new FeedEntity();
        List<Feed> restored = FeedCodec.fromLegacy(Arrays.asList(null, invalid, sparse), true);
        assertEquals(1, restored.size());
        Feed feed = restored.get(0);
        assertEquals(42, feed.getId());
        assertEquals(42, feed.getPost_id());
        assertEquals(Long.valueOf(123), feed.getTime());
        assertEquals("", feed.getTitle());
        assertEquals("", feed.getText());
        assertEquals("", feed.getFull_text());
        assertEquals("", feed.getUser());
        assertEquals("", feed.getHeaders());
        assertEquals("", feed.getSize());
        assertEquals("", feed.getDate());
        assertEquals("", feed.getCategory());
        assertEquals("", feed.getLink());
        assertEquals("", feed.getMod());
        assertNotNull(feed.getImageUrl());
        assertTrue(FeedCodec.fromLegacy(null, false).isEmpty());
    }

    /** Confirms malformed pages fail before a caller can receive even their valid prefix. */
    private static void rejects(JSONArray response) {
        try {
            FeedCodec.parse(response, false);
            fail("Malformed page must not replace a good cache");
        } catch (JSONException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    /** Builds the documented complete section response with raw HTML and distinct metadata values. */
    private static JSONObject row() throws JSONException {
        return new JSONObject().put("lid", 42).put("post_id", 100)
                .put("title", "<b>Название &amp;</b>").put("razdel", "vuploader")
                .put("text", "<p>Коротко &#x20; &amp;</p>")
                .put("full_text", "<div>Полностью<br><a href=\"https://dimonvideo.ru\">ссылка</a></div>")
                .put("image", "/image.jpg").put("date", "09.10.2026")
                .put("category", "Категория").put("headers", "Видео").put("user", "author")
                .put("size", "5 MB").put("file_link", "https://example.com/clip.mp4")
                .put("mod", "https://example.com/mod.txt").put("time", 1791532800L)
                .put("rating", 3).put("views", 15).put("min", 1).put("plus", 7)
                .put("fav", 1).put("status", 2);
    }
}

package com.dimonvideo.client.adater;

import com.dimonvideo.client.model.FeedPm;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Protects asynchronous diff identity/content and API preview/full-body mapping. */
public class PmListSnapshotTest {
    /** A parser or fragment must not mutate models already being compared on a background thread. */
    @Test
    public void submittedSnapshotDoesNotObserveLaterParserMutations() {
        FeedPm apiItem = message(42);
        List<FeedPm> source = new ArrayList<>();
        source.add(apiItem);
        List<FeedPm> snapshot = PmListSnapshot.copy(source);
        apiItem.setTitle("edited after submission");
        apiItem.setFullHtml("changed complete HTML");
        apiItem.setPreviewHtml("changed preview");
        apiItem.setIs_new(0);
        apiItem.setSourcePage(9);
        source.clear();

        assertEquals(1, snapshot.size());
        assertEquals("subject", snapshot.get(0).getTitle());
        assertEquals("<b>complete &amp; HTML</b>", snapshot.get(0).getFullHtml());
        assertEquals("short preview", snapshot.get(0).getPreviewHtml());
        assertEquals(1, snapshot.get(0).getIs_new());
        assertEquals(3, snapshot.get(0).getSourcePage());
    }

    /** An identical refresh preserves row identity and avoids unnecessary preview/avatar rebinding. */
    @Test
    public void identicalApiValuesCompareEqualEvenForDifferentObjects() {
        FeedPm first = message(42);
        FeedPm refreshed = message(42);
        assertTrue(PmListSnapshot.DIFF.areItemsTheSame(first, refreshed));
        assertTrue(PmListSnapshot.DIFF.areContentsTheSame(first, refreshed));
        assertEquals(first, refreshed);
        assertEquals(first.hashCode(), refreshed.hashCode());
    }

    /** Read state or HTML changes must rebind the same message without treating it as a different row. */
    @Test
    public void acknowledgedReadAndHtmlEditsChangeContentsButNotIdentity() {
        FeedPm original = message(42);
        FeedPm read = new FeedPm(original);
        read.setIs_new(0);
        assertTrue(PmListSnapshot.DIFF.areItemsTheSame(original, read));
        assertFalse(PmListSnapshot.DIFF.areContentsTheSame(original, read));
        FeedPm edited = new FeedPm(original);
        edited.setFullHtml("<b>new full body &#x20;</b>");
        assertFalse(PmListSnapshot.DIFF.areContentsTheSame(original, edited));
        assertNotEquals(original, edited);
        assertFalse(PmListSnapshot.DIFF.areItemsTheSame(original, message(99)));
    }

    /** Creates all visible fields, including the PM API's deliberately different preview/body values. */
    private static FeedPm message(int id) {
        FeedPm item = new FeedPm();
        item.setId(id);
        item.setSourcePage(3);
        item.setTitle("subject");
        item.setLast_poster_name("sender");
        item.setDate("08.10.2026");
        item.setTime(1791417600L);
        item.setImageUrl("https://dimonvideo.ru/fotos/member.png");
        item.setIs_new(1);
        item.setFullHtml("<b>complete &amp; HTML</b>");
        item.setPreviewHtml("short preview");
        return item;
    }
}

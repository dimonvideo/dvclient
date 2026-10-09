package com.dimonvideo.client.util.feed;

import com.dimonvideo.client.Config;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Checks that a saved feed can only be selected by its matching section, filters and account. */
public class FeedQueryTest {
    /** Matches existing limits and category/cid parameters while canonicalizing section aliases. */
    @Test
    public void latestRequestUsesItsSectionAndCategory() {
        FeedQuery query = query("3", 17, null, FeedQuery.LATEST, false, null, "user");
        assertEquals(Config.VUPLOADER_URL + "1&c=limit,10,all&where=17", query.url(1));
        assertEquals(Config.VUPLOADER_URL + "2&c=limit,10,all&where=17", query.url(2));
        assertEquals("vuploader", query.sectionKey());
        assertEquals(query.scopeKey(), query("vuploader", 17, "", FeedQuery.LATEST,
                false, null, "other").scopeKey());
    }

    /** Separates sections, category IDs and tab modes even when two modes use the same server URL. */
    @Test
    public void scopesIsolateSectionCategoryAndPresentation() {
        FeedQuery latest = query("10", 0, null, FeedQuery.LATEST, false, null, null);
        FeedQuery details = query("10", 0, null, FeedQuery.DETAILS, false, null, null);
        assertEquals(latest.url(1), details.url(1));
        assertNotEquals(latest.scopeKey(), details.scopeKey());
        assertNotEquals(latest.scopeKey(), query("3", 0, null, FeedQuery.LATEST,
                false, null, null).scopeKey());
        assertNotEquals(latest.scopeKey(), query("10", 4, null, FeedQuery.LATEST,
                false, null, null).scopeKey());
        assertEquals(64, latest.scopeKey().length());
        assertTrue(latest.scopeKey().matches("[a-f0-9]{64}"));
    }

    /** Encodes raw Unicode, plus signs and percent signs once on every independently generated page. */
    @Test
    public void searchIsStableAndNeverMutatesEncodedStory() {
        FeedQuery search = query("2", 9, "Кот + 50% &", FeedQuery.LATEST, false, null, null);
        String suffix = "&c=limit,10,all&story=%D0%9A%D0%BE%D1%82+%2B+50%25+%26";
        assertEquals(Config.UPLOADER_SEARCH_URL + "1" + suffix, search.url(1));
        assertEquals(Config.UPLOADER_SEARCH_URL + "2" + suffix, search.url(2));
        assertEquals(Config.UPLOADER_SEARCH_URL + "1" + suffix, search.url(1));
        assertNotEquals(search.scopeKey(), query("2", 9, "Кот", FeedQuery.LATEST,
                false, null, null).scopeKey());
    }

    /** Captures preference selections and gives equal sets equal requests regardless of insertion order. */
    @Test
    public void selectedCategoriesAreSortedAndCopied() {
        Set<String> selections = new HashSet<>(Arrays.asList("soft", "games"));
        FeedQuery query = query("11", 0, null, FeedQuery.LATEST, false, selections, null);
        selections.clear();
        String expected = Config.ANDROID_URL + "1&c=limit,10,games,soft";
        assertEquals(expected, query.url(1));
        assertEquals(query.scopeKey(), query("11", 0, null, FeedQuery.LATEST, false,
                new HashSet<>(Arrays.asList("games", "soft")), null).scopeKey());
        assertNotEquals(query.scopeKey(), query("11", 0, null, FeedQuery.LATEST,
                false, Collections.singleton("games"), null).scopeKey());
        assertEquals(Config.ANDROID_URL + "1&c=limit,10,all", query("11", 0, null,
                FeedQuery.LATEST, false, Collections.emptySet(), null).url(1));
    }

    /** Includes the moderation selector only on the detailed tab and isolates its cache. */
    @Test
    public void waitingDetailsHaveADistinctScope() {
        FeedQuery waiting = query("4", 0, null, FeedQuery.DETAILS, true, null, null);
        assertEquals(Config.NEWS_URL + "1&c=limit,10,all&st=2", waiting.url(1));
        assertNotEquals(waiting.scopeKey(), query("4", 0, null, FeedQuery.DETAILS,
                false, null, null).scopeKey());
        assertEquals(Config.NEWS_URL + "1&c=limit,10,all", query("4", 0, null,
                FeedQuery.LATEST, true, null, null).url(1));
    }

    /** Uses the existing favorites format while encoding and isolating each account name. */
    @Test
    public void favoritesAreAccountScopedAndEncodedOnce() {
        FeedQuery favorite = query("3", 2, null, FeedQuery.FAVORITES, true,
                Collections.singleton("video"), "Анна +%&");
        assertEquals(Config.VUPLOADER_URL
                + "1&where=2&fav=1&login_name=%D0%90%D0%BD%D0%BD%D0%B0+%2B%25%26",
                favorite.url(1));
        assertNotEquals(favorite.scopeKey(), query("3", 2, null, FeedQuery.FAVORITES,
                true, Collections.singleton("video"), "other").scopeKey());
        assertFalse(favorite.url(1).contains("&c="));
        assertFalse(favorite.url(1).contains("&st="));
    }

    /** Restricts migration from the section-only table to an unfiltered latest list. */
    @Test
    public void legacyRowsAreUnsafeForOtherQueriesAndMixedNewSection() {
        assertTrue(query("10", 0, null, FeedQuery.LATEST, false, null, null).isLegacyCacheEligible());
        assertTrue(query("10", 0, "", FeedQuery.LATEST, false,
                Collections.singleton("all"), null).isLegacyCacheEligible());
        assertFalse(query("10", 1, null, FeedQuery.LATEST, false, null, null).isLegacyCacheEligible());
        assertFalse(query("10", 0, "search", FeedQuery.LATEST, false, null, null).isLegacyCacheEligible());
        assertFalse(query("10", 0, null, FeedQuery.DETAILS, false, null, null).isLegacyCacheEligible());
        assertFalse(query("10", 0, null, FeedQuery.FAVORITES, false, null, null).isLegacyCacheEligible());
        assertFalse(query("10", 0, null, FeedQuery.LATEST, false,
                Collections.singleton("games"), null).isLegacyCacheEligible());
        assertFalse(query("18", 0, null, FeedQuery.LATEST, false, null, null).isLegacyCacheEligible());
        assertFalse(query("new", 0, null, FeedQuery.LATEST, false, null, null).isLegacyCacheEligible());
    }

    /** Builds a query with the same raw inputs passed by the production section fragment. */
    private static FeedQuery query(String section, int cid, String story, String mode,
                                   boolean waiting, Set<String> selected, String login) {
        return new FeedQuery(section, cid, story, mode, waiting, selected, login);
    }
}

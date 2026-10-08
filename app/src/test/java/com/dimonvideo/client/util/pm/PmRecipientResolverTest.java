package com.dimonvideo.client.util.pm;

import android.app.Application;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

/** Checks that wildcard member searches cannot redirect a reply to a similar or ambiguous name. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class PmRecipientResolverTest {
    /** Accepts the exact username regardless of case and ignores prefix and substring matches. */
    @Test
    public void exactNameWinsOverPartialMatches() throws JSONException {
        JSONArray members = new JSONArray().put(member(31, "alexander"))
                .put(member(42, "ALEX")).put(member(53, "the-alex"));
        assertEquals(42, PmRecipientResolver.findExactRecipient(members, "alex"));
    }

    /** Keeps send disabled when the wildcard search contains only similar names. */
    @Test
    public void partialNamesAreUnavailable() throws JSONException {
        JSONArray members = new JSONArray().put(member(31, "alexander"))
                .put(member(53, "the-alex"));
        assertEquals(0, PmRecipientResolver.findExactRecipient(members, "alex"));
    }

    /** Ignores no-result placeholders even if their displayed title resembles the intended name. */
    @Test
    public void sentinelAndNegativeIdsCannotBecomeRecipients() throws JSONException {
        JSONArray members = new JSONArray().put(member(0, "alex"))
                .put(member(-1, "ALEX")).put(member(42, "alex"));
        assertEquals(42, PmRecipientResolver.findExactRecipient(members, "alex"));
        assertEquals(0, PmRecipientResolver.findExactRecipient(
                new JSONArray().put(member(0, "alex")), "alex"));
    }

    /** Rejects conflicting IDs with equal names so a reply cannot choose the first search hit. */
    @Test
    public void ambiguousNamesAreUnavailable() throws JSONException {
        JSONArray members = new JSONArray().put(member(42, "alex"))
                .put(member(53, "ALEX"));
        assertEquals(0, PmRecipientResolver.findExactRecipient(members, "alex"));
    }

    /** Tolerates duplicate rows for one user without treating them as a different recipient. */
    @Test
    public void repeatedSameMemberIsUnambiguous() throws JSONException {
        JSONArray members = new JSONArray().put(member(42, "alex"))
                .put(member(42, "ALEX"));
        assertEquals(42, PmRecipientResolver.findExactRecipient(members, "alex"));
    }

    /** Preserves exact spelling for non-Latin names and does not strip returned name characters. */
    @Test
    public void unicodeNamesMatchCaseWithoutTrimmingSearchHits() throws JSONException {
        JSONArray members = new JSONArray().put(member(31, " Дмитрий "))
                .put(member(42, "ДМИТРИЙ"));
        assertEquals(42, PmRecipientResolver.findExactRecipient(members, "дмитрий"));
    }

    /** Handles absent input and malformed response rows without selecting a fallback recipient. */
    @Test
    public void absentOrMalformedCandidatesAreUnavailable() throws JSONException {
        JSONArray members = new JSONArray().put(JSONObject.NULL).put("alex")
                .put(new JSONObject().put("title", "alex"));
        assertEquals(0, PmRecipientResolver.findExactRecipient(members, "alex"));
        assertEquals(0, PmRecipientResolver.findExactRecipient(members, null));
        assertEquals(0, PmRecipientResolver.findExactRecipient(members, " "));
        assertEquals(0, PmRecipientResolver.findExactRecipient(null, "alex"));
    }

    /** Builds a member fixture using the documented lid/title fields. */
    private static JSONObject member(int id, String name) throws JSONException {
        return new JSONObject().put("lid", id).put("title", name);
    }
}

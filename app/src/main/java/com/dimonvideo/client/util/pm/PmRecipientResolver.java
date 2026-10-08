package com.dimonvideo.client.util.pm;

import android.content.Context;
import android.net.Uri;

import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.util.AppController;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** Resolves a PM correspondent's name to a verified member ID without guessing from search hits. */
public final class PmRecipientResolver {
    private static final int PAGE_SIZE = 10;
    private static final int MAX_PAGES = 10;

    /** Prevents instantiation of the process-wide lookup utility. */
    private PmRecipientResolver() { }

    /** Receives the unique member identity, or a failure that must leave sending disabled. */
    public interface Callback {
        /** Accepts an exact, case-insensitive match with a positive member ID. */
        void onResolved(int positiveUid);

        /** Reports no exact match, ambiguity, a network failure, or an account change. */
        void onUnavailable();
    }

    /**
     * Starts an uncached, account-bound lookup on the application's Volley queue.
     * The public members endpoint requires no credentials. Search uses wildcards, so all
     * returned pages are checked for exact names and conflicting IDs; an incomplete search
     * fails closed after at most 100 results. Call from the UI thread; retain the handle
     * and cancel it when the form closes.
     *
     * @param context the current form context, used only to verify application availability
     * @param username the correspondent name returned by the PM API
     * @param expectedAccountKey the non-secret account identity captured by the form
     * @param callback receives one terminal result unless the lookup is cancelled
     * @return a cancellation handle for every page belonging to this lookup
     */
    public static Handle resolve(Context context, String username, String expectedAccountKey,
                                 Callback callback) {
        Handle handle = new Handle(username, expectedAccountKey, callback);
        if (context == null || context.getApplicationContext() == null
                || username == null || username.trim().isEmpty()) {
            handle.finishUnavailable();
        } else {
            handle.loadPage(1);
        }
        return handle;
    }

    /**
     * Finds one exact case-insensitive username in a members search page.
     * Zero-ID no-result placeholders and partial matches are ignored. Repeated rows for
     * the same member are safe, while matching names with different IDs are ambiguous.
     *
     * @return the unique positive member ID, or zero for no match or ambiguity
     */
    public static int findExactRecipient(JSONArray members, String username) {
        ExactMatch match = scanExactRecipients(members, username);
        return match.ambiguous ? 0 : match.id;
    }

    /** Retains ambiguity separately from absence so a later search page cannot hide a conflict. */
    private static ExactMatch scanExactRecipients(JSONArray members, String username) {
        ExactMatch match = new ExactMatch();
        if (members == null || username == null || username.trim().isEmpty()) return match;
        for (int index = 0; index < members.length(); index++) {
            JSONObject member = members.optJSONObject(index);
            if (member == null) continue;
            int id = member.optInt(Config.TAG_ID, 0);
            if (id <= 0 || !username.equalsIgnoreCase(member.optString(Config.TAG_TITLE, ""))) {
                continue;
            }
            if (match.id != 0 && match.id != id) {
                match.ambiguous = true;
                return match;
            }
            match.id = id;
        }
        return match;
    }

    /** Stores a page's unique exact match together with any conflicting member ID. */
    private static final class ExactMatch {
        int id;
        boolean ambiguous;

        /** Creates an empty match before the search page is scanned. */
        private ExactMatch() { }
    }

    /** Owns the current request and prevents callbacks after cancellation or account changes. */
    public static final class Handle {
        private final String username;
        private final String expectedAccountKey;
        private final Callback callback;
        private final Set<Integer> seenMemberIds = new HashSet<>();
        private Request<?> activeRequest;
        private boolean finished;
        private int candidateId;

        /** Captures only the intended name, account identity, and result receiver. */
        private Handle(String username, String expectedAccountKey, Callback callback) {
            this.username = username;
            this.expectedAccountKey = expectedAccountKey;
            this.callback = callback;
        }

        /** Cancels the whole lookup without notifying a form that has already closed. */
        public void cancel() {
            finished = true;
            if (activeRequest != null) activeRequest.cancel();
            activeRequest = null;
        }

        /** Checks the form's account before a request is sent or its response is accepted. */
        private boolean isCurrentAccount() {
            return expectedAccountKey != null
                    && expectedAccountKey.equals(PmDeletionQueue.currentAccountKey());
        }

        /** Requests the next wildcard-search page without adding authentication to its URL. */
        private void loadPage(int page) {
            if (finished) return;
            AppController controller = AppController.getInstance();
            if (controller == null || !isCurrentAccount()) {
                finishUnavailable();
                return;
            }
            String url = Uri.parse(Config.MEMBERS_SEARCH_URL + page).buildUpon()
                    .appendQueryParameter("story", username).build().toString();
            JsonArrayRequest request = new JsonArrayRequest(url,
                    response -> acceptPage(response, page), error -> acceptError(error, page));
            activeRequest = request;
            // AppController disables caching and supplies retries; this request is read-only.
            request.setShouldCache(false);
            controller.addToRequestQueue(request);
        }

        /** Rejects conflicting names, repeated full pages, and a search truncated by the page limit. */
        private void acceptPage(JSONArray response, int page) {
            if (finished) return;
            activeRequest = null;
            if (!isCurrentAccount()) {
                finishUnavailable();
                return;
            }
            ExactMatch match = scanExactRecipients(response, username);
            if (match.ambiguous || (candidateId > 0 && match.id > 0 && candidateId != match.id)) {
                finishUnavailable();
                return;
            }
            if (match.id > 0) candidateId = match.id;
            int added = 0;
            boolean noResults = false;
            for (int index = 0; index < response.length(); index++) {
                JSONObject member = response.optJSONObject(index);
                if (member == null) continue;
                int id = member.optInt(Config.TAG_ID, 0);
                noResults |= id == 0 && member.optInt("plus", 0) == 1;
                if (id > 0 && seenMemberIds.add(id)) added++;
            }
            if (response.length() < PAGE_SIZE || noResults) {
                finishSearch();
            } else if (added == 0 || page >= MAX_PAGES) {
                finishUnavailable();
            } else {
                loadPage(page + 1);
            }
        }

        /** Accepts the API's documented empty final page while treating other failures as unavailable. */
        private void acceptError(VolleyError error, int page) {
            if (finished) return;
            activeRequest = null;
            if (page > 1 && error instanceof ParseError && error.networkResponse != null
                    && error.networkResponse.statusCode == 200 && error.networkResponse.data != null
                    && new String(error.networkResponse.data, StandardCharsets.UTF_8).trim().isEmpty()) {
                finishSearch();
            } else {
                finishUnavailable();
            }
        }

        /** Delivers a verified unique candidate only while the original account remains active. */
        private void finishSearch() {
            if (finished) return;
            if (candidateId <= 0 || !isCurrentAccount()) {
                finishUnavailable();
                return;
            }
            finished = true;
            activeRequest = null;
            if (callback != null) callback.onResolved(candidateId);
        }

        /** Ends an unsuccessful lookup once without logging URLs, names, or server responses. */
        private void finishUnavailable() {
            if (finished) return;
            finished = true;
            if (activeRequest != null) activeRequest.cancel();
            activeRequest = null;
            if (callback != null) callback.onUnavailable();
        }
    }
}

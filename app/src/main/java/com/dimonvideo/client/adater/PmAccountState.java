package com.dimonvideo.client.adater;

import android.util.LruCache;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Account-scoped transient PM state; no draft, read flag or hidden row crosses an account switch. */
final class PmAccountState {
    private final Supplier<String> currentAccount;
    private final LruCache<String, PmMessageDialog.Draft> drafts = new LruCache<>(32);
    private final Set<String> readIds = new HashSet<>();
    private final Set<String> readingIds = new HashSet<>();
    private final Set<String> hiddenIds = new HashSet<>();
    private String accountKey;

    /** Captures the current non-secret identity; injection permits account-switch regression tests. */
    PmAccountState(Supplier<String> currentAccount) {
        this.currentAccount = currentAccount;
        accountKey = currentAccount.get();
    }

    /** Clears all private cached state when the authenticated identity changes. */
    boolean synchronize() {
        String current = currentAccount.get();
        if (Objects.equals(accountKey, current)) return false;
        accountKey = current;
        drafts.evictAll();
        readIds.clear();
        readingIds.clear();
        hiddenIds.clear();
        return true;
    }

    /** Returns the identity associated with the adapter's committed rows and cached state. */
    String accountKey() { return accountKey; }

    /** Allows public member previews after logout while rejecting rows from a replaced session. */
    boolean isVisibleAccount() { return Objects.equals(accountKey, currentAccount.get()); }

    /** Rejects callbacks captured before an account change, including before the adapter refreshes. */
    boolean isCurrent(String capturedAccount) {
        return capturedAccount != null && Objects.equals(capturedAccount, accountKey)
                && Objects.equals(capturedAccount, currentAccount.get());
    }

    /** Produces an account-qualified key rather than relying on IDs shared across users. */
    private static String key(String accountKey, int id) { return accountKey + ":" + id; }

    /** Retrieves or creates a draft owned only by the currently authenticated account and item. */
    PmMessageDialog.Draft draft(int id) {
        String key = key(accountKey, id);
        PmMessageDialog.Draft draft = drafts.get(key);
        if (draft == null) {
            draft = new PmMessageDialog.Draft();
            drafts.put(key, draft);
        }
        return draft;
    }

    /** Discards only an empty draft belonging to the exact sheet that has just closed. */
    void discardEmptyDraft(String capturedAccount, int id, PmMessageDialog.Draft draft) {
        String key = key(capturedAccount, id);
        if (draft.text.isEmpty() && draft.attachment == null && drafts.get(key) == draft) drafts.remove(key);
    }

    /** Removes the accepted message's draft without touching another user's same-ID draft. */
    void removeDraft(String capturedAccount, int id) { drafts.remove(key(capturedAccount, id)); }

    /** Returns whether reading this account's item has already been acknowledged locally. */
    boolean isRead(int id) { return readIds.contains(key(accountKey, id)); }

    /** Deduplicates read requests within one account, while preventing logged-out requests. */
    boolean beginRead(String capturedAccount, int id) {
        return isCurrent(capturedAccount) && readingIds.add(key(capturedAccount, id));
    }

    /** Records read acknowledgement only if the originating account still owns the visible adapter. */
    boolean finishRead(String capturedAccount, int id, boolean acknowledged) {
        readingIds.remove(key(capturedAccount, id));
        if (!isCurrent(capturedAccount)) return false;
        if (acknowledged) readIds.add(key(capturedAccount, id));
        return true;
    }

    /** Suppresses a removed member only in the active account until its next successful refresh. */
    void hide(int id) { hiddenIds.add(key(accountKey, id)); }

    /** Checks suppression using both the user identity and server member ID. */
    boolean isHidden(int id) { return hiddenIds.contains(key(accountKey, id)); }

    /** Clears member suppression on a successful server refresh for this account. */
    void resetHidden() { hiddenIds.clear(); }
}

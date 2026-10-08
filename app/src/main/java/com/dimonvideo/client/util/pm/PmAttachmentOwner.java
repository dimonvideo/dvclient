package com.dimonvideo.client.util.pm;

import android.os.Bundle;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

/** Activity-owned picker/upload routing that retains results while a message sheet is recreated. */
public final class PmAttachmentOwner extends ViewModel {
    private static final String STATE = "pm_attachment";
    private final SavedStateHandle savedState;
    private static final int MAX_REQUESTS = 32;

    /** Restores non-secret routing and turns an interrupted process-death upload into a retryable failure. */
    public PmAttachmentOwner(SavedStateHandle savedState) {
        this.savedState = savedState;
        Bundle records = records();
        boolean interrupted = false;
        for (String key : records.keySet()) {
            Bundle state = records.getBundle(key);
            if (state != null && "uploading".equals(state.getString("phase"))) {
                Bundle failed = new Bundle(state);
                failed.putString("phase", "finished");
                failed.putString("outcome", PmAttachmentEvent.Outcome.FAILED.name());
                records.putBundle(key, failed);
                interrupted = true;
            }
        }
        if (interrupted) write(records);
    }

    /** Reserves the exact request and account before launching the system picker. */
    public boolean beginPicker(String request, String account) {
        if (request == null || account == null || pendingPicker() != null) return false;
        Bundle records = records();
        // Changing accounts permanently invalidates old routing; callbacks still require an exact record match.
        for (String key : new java.util.ArrayList<>(records.keySet())) {
            Bundle record = records.getBundle(key);
            if (record == null || !account.equals(record.getString("account"))) records.remove(key);
        }
        String key = key(request, account);
        if (records.containsKey(key) || records.size() >= MAX_REQUESTS) return false;
        Bundle state = new Bundle();
        state.putString("request", request);
        state.putString("account", account);
        state.putString("phase", "picking");
        records.putBundle(key, state);
        write(records);
        return true;
    }

    /** Marks an upload as active without retaining a dialog, activity or bitmap in the owner. */
    public void beginUpload(String request, String account) {
        Bundle state = matching(request, account);
        if (state == null || !"picking".equals(state.getString("phase"))) return;
        Bundle uploading = new Bundle(state);
        uploading.putString("phase", "uploading");
        Bundle records = records();
        records.putBundle(key(request, account), uploading);
        write(records);
    }

    /** Buffers a correlated terminal result, discarding filenames after logout or an account change. */
    public void complete(String request, String account, String filename,
                         PmAttachmentEvent.Outcome outcome, String currentAccount) {
        Bundle state = matching(request, account);
        if (state == null || "finished".equals(state.getString("phase"))) return;
        Bundle finished = new Bundle(state);
        boolean sameAccount = account != null && account.equals(currentAccount);
        finished.putString("phase", "finished");
        finished.putString("outcome", (sameAccount ? outcome : PmAttachmentEvent.Outcome.CANCELED).name());
        finished.putString("filename", sameAccount ? filename : null);
        Bundle records = records();
        records.putBundle(key(request, account), finished);
        write(records);
    }

    /** Exposes lifecycle-aware routing updates so a stopped or absent sheet cannot lose an upload result. */
    public LiveData<Bundle> changes() { return savedState.getLiveData(STATE); }

    /** Returns a terminal result only for the matching draft, leaving other requests untouched. */
    public PmAttachmentEvent result(String request, String account) {
        Bundle state = matching(request, account);
        if (state == null || !"finished".equals(state.getString("phase"))) return null;
        return new PmAttachmentEvent(request, account, state.getString("filename"),
                PmAttachmentEvent.Outcome.valueOf(state.getString("outcome")));
    }

    /** Releases a result only after the intended draft has applied it. */
    public void consume(String request, String account) {
        if (result(request, account) == null) return;
        Bundle records = records();
        records.remove(key(request, account));
        write(records.isEmpty() ? null : records);
    }

    /** Returns the current picker routing, including after Android restores a pending activity result. */
    public Bundle pendingPicker() {
        Bundle records = records();
        for (String key : records.keySet()) {
            Bundle state = records.getBundle(key);
            if (state != null && "picking".equals(state.getString("phase"))) return new Bundle(state);
        }
        return null;
    }

    /** Checks whether the current request still has either a picker or an in-process upload running. */
    public boolean isPending(String request, String account) {
        Bundle state = matching(request, account);
        return state != null && ("picking".equals(state.getString("phase"))
                || "uploading".equals(state.getString("phase")));
    }

    /** Matches both correlation keys, preventing another sheet or account from receiving private files. */
    private Bundle matching(String request, String account) {
        Bundle state = request == null || account == null ? null : records().getBundle(key(request, account));
        return request != null && account != null && state != null
                && request.equals(state.getString("request")) && account.equals(state.getString("account"))
                ? state : null;
    }

    /** Copies the keyed immutable snapshot before one request is changed or consumed. */
    private Bundle records() {
        Bundle records = savedState.get(STATE);
        return records == null ? new Bundle() : new Bundle(records);
    }

    /** Keeps identical request strings isolated even when different accounts happen to reuse them. */
    private static String key(String request, String account) { return account + ":" + request; }

    /** Writes saved state before notifying observers, allowing an observer to consume the same result safely. */
    private void write(Bundle state) { savedState.<Bundle>getLiveData(STATE).setValue(state); }
}

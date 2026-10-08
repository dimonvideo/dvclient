package com.dimonvideo.client.util.pm;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.LongSupplier;

/** Retries account-bound deletions while guarding against the legacy API's counter side effects. */
public final class PmDeletionWorker extends Worker {
    private static final int PAGE_SIZE = 10;
    private static final int MAX_PAGES_PER_FOLDER = 200;
    private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
    private long runDeadline;
    private long intentCreatedAt;
    private String accountKey;
    private final Transport transport;
    private final AccountSession session;
    private final Scheduler scheduler;
    private final LongSupplier clock;

    /** Performs a single request; tests replace it with deterministic non-network responses. */
    interface Transport {
        /** Returns a server JSON body or throws a transport error without retrying internally. */
        String get(String address) throws Exception;
    }

    /** Supplies live credentials separately from the non-secret persistent job metadata. */
    interface AccountSession {
        /** Returns the currently active account's non-secret identity. */
        String currentKey();
        /** Returns the current login for this attempt only. */
        String login();
        /** Returns the current password for this attempt only. */
        String password();
        /** Saves an authoritative server count for the still-active original account. */
        void updateUnread(int count);
    }

    /** Persists the next WorkManager wake-up separately from the network attempt. */
    interface Scheduler {
        /** Appends a successor only after retry metadata has been committed. */
        void append(PmDeletionStore.Entry entry) throws Exception;
    }

    /** Reads current application authentication without persisting secrets in a work request. */
    private static final class AppSession implements AccountSession {
        /** Returns the current account hash used by the queue and visible mailbox. */
        @Override public String currentKey() { return PmDeletionQueue.currentAccountKey(); }
        /** Reads the login directly from the current application preferences. */
        @Override public String login() { return AppController.getInstance().userName(""); }
        /** Reads the password directly from the current application preferences. */
        @Override public String password() { return AppController.getInstance().userPassword(); }
        /** Updates the current account's unread badge after the caller checks account identity. */
        @Override public void updateUnread(int count) { AppController.getInstance().putPmUnread(count); }
    }

    /** Recreates a worker from persisted non-secret account and message identifiers. */
    public PmDeletionWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        this(context, parameters, null, new AppSession(),
                entry -> PmDeletionQueue.schedule(context, entry,
                        ExistingWorkPolicy.APPEND_OR_REPLACE), System::currentTimeMillis);
    }

    /** Injects request, account, scheduling, and time boundaries for isolated worker validation. */
    PmDeletionWorker(Context context, WorkerParameters parameters, Transport transport,
                     AccountSession session, Scheduler scheduler, LongSupplier clock) {
        super(context, parameters);
        this.transport = transport == null ? this::getNetwork : transport;
        this.session = session;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    /** Performs one guarded attempt or schedules the next wake-up within the original six hours. */
    @NonNull
    @Override
    public Result doWork() {
        accountKey = getInputData().getString("account_key");
        int messageId = getInputData().getInt("message_id", 0);
        long generation = getInputData().getLong("created_at", -1);
        if (accountKey == null || messageId <= 0 || generation < 0) return Result.failure();
        PmDeletionStore store = PmDeletionStore.get(getApplicationContext());
        PmDeletionStore.Entry entry;
        try {
            entry = store.find(accountKey, messageId);
        } catch (Exception exception) {
            return Result.retry();
        }
        if (entry == null || entry.createdAt != generation) return Result.success();
        intentCreatedAt = entry.createdAt;
        long now = clock.getAsLong();
        if (PmDeletionRetryPolicy.isExpired(entry.createdAt, now)) {
            PmDeletionQueue.finish(getApplicationContext(), entry, PmDeletionEvent.Outcome.EXPIRED);
            return Result.success();
        }
        if (now < entry.nextAttemptAt) return scheduleSuccessor(store, entry, false);
        // Bound each scan well below WorkManager's execution limit. An incomplete scan retries
        // later and never guesses that a timed-out mutation did not reach the server.
        runDeadline = now + 90_000;
        try {
            ensureActiveAccount();
            String auth = "&login_name=" + URLEncoder.encode(session.login(), "UTF-8")
                    + "&login_password=" + URLEncoder.encode(session.password(), "UTF-8");
            JSONObject identity = new JSONObject(get(Config.CHECK_AUTH_URL + auth));
            if (identity.optInt("state") != 1 || identity.optInt("user_id") != entry.userId) {
                throw new IOException("Account authentication was not confirmed");
            }
            // pm.php decrements counters even for already-deleted rows. A positive active
            // row authorizes a mutation; an absent row never authorizes a blind resend.
            boolean foundAtHint = containsOnPage(auth, entry.sourceFolder, entry.sourcePage,
                    entry.messageId);
            boolean trashConfirmed = entry.sourceFolder == 5 && foundAtHint;
            boolean active = entry.sourceFolder != 5 && foundAtHint;
            if (!trashConfirmed && !active) {
                // A prior request can have succeeded despite a lost response. Check trash before
                // scanning unrelated active history, so that reconciliation cannot starve.
                trashConfirmed = contains(auth, 5, entry.messageId);
            }
            if (!trashConfirmed && !active) {
                Set<Integer> folders = new LinkedHashSet<>();
                if (entry.sourceFolder != 5) folders.add(entry.sourceFolder);
                for (int folder : new int[]{0, 1, 2, 3}) folders.add(folder);
                for (int folder : folders) {
                    if (contains(auth, folder, entry.messageId)) {
                        active = true;
                        break;
                    }
                }
            }
            if (active) {
                ensureActiveAccount();
                JSONArray response = new JSONArray(get(Config.PM_URL + "1" + auth
                        + "&pm=10&delete=0&pm_id=" + entry.messageId));
                if (response.length() != 1 || !response.getJSONObject(0).has("status")) {
                    throw new IOException("Deletion was not acknowledged");
                }
                int status = response.getJSONObject(0).getInt("status");
                // The documented status=0 specifically means this row no longer exists.
                if (status != 1 && status != 0) throw new IOException("Unknown deletion status");
            } else if (!trashConfirmed) {
                // Absence remains ambiguous. Only a trash row or explicit status acknowledges it.
                throw new IOException("Message state could not be confirmed");
            }
            PmDeletionQueue.finish(getApplicationContext(), entry, PmDeletionEvent.Outcome.CONFIRMED);
            refreshUnreadCount(auth, entry);
            return Result.success();
        } catch (Exception exception) {
            if (PmDeletionRetryPolicy.isExpired(entry.createdAt, clock.getAsLong())) {
                PmDeletionQueue.finish(getApplicationContext(), entry, PmDeletionEvent.Outcome.EXPIRED);
                return Result.success();
            }
            if (isStopped()) return Result.retry();
            return scheduleSuccessor(store, entry, true);
        }
    }

    /** Saves retry timing before appending durable work; a failed schedule keeps the intent. */
    private Result scheduleSuccessor(PmDeletionStore store, PmDeletionStore.Entry entry,
                                    boolean failedAttempt) {
        try {
            if (failedAttempt) {
                store.retry(entry, PmDeletionRetryPolicy.nextAttemptAt(entry.createdAt,
                        clock.getAsLong(), entry.failures + 1));
            }
            PmDeletionStore.Entry next = store.find(entry.accountKey, entry.messageId);
            if (next != null && next.createdAt == entry.createdAt) scheduler.append(next);
            return Result.success();
        } catch (Exception exception) {
            return Result.retry();
        }
    }

    /** Searches a complete server folder; malformed or truncated scans cannot authorize retries. */
    private boolean contains(String auth, int folder, int messageId) throws Exception {
        for (int page = 1; page <= MAX_PAGES_PER_FOLDER; page++) {
            JSONArray messages = page(auth, folder, page);
            if (containsId(messages, messageId)) return true;
            if (messages.length() < PAGE_SIZE) return false;
        }
        throw new IOException("Folder scan was incomplete");
    }

    /** Checks the displayed server page first so deep/outbox selections do not scan unrelated history. */
    private boolean containsOnPage(String auth, int folder, int page, int messageId) throws Exception {
        return containsId(page(auth, folder, page), messageId);
    }

    /** Fetches a page through the same account, timeout, and lifetime guards as every other request. */
    private JSONArray page(String auth, int folder, int page) throws Exception {
        return new JSONArray(get(Config.PM_URL + page + auth + "&pm=" + folder));
    }

    /** Validates every returned identity before relying on a row as proof of server state. */
    private boolean containsId(JSONArray messages, int messageId) throws Exception {
        boolean found = false;
        for (int index = 0; index < messages.length(); index++) {
            int id = messages.getJSONObject(index).getInt("lid");
            if (id <= 0) throw new IOException("Invalid message identity");
            if (id == messageId) found = true;
        }
        return found;
    }

    /** Prevents work for a logged-out/replaced account and any mutation after the deadline. */
    private void ensureActiveAccount() throws IOException {
        if (isStopped() || !accountKey.equals(session.currentKey())
                || clock.getAsLong() >= runDeadline
                || PmDeletionRetryPolicy.isExpired(intentCreatedAt, clock.getAsLong())) {
            throw new IOException("Deletion attempt is paused");
        }
    }

    /**
     * Executes exactly one HTTPS request with normal certificate verification and no redirects.
     * Transport retries are deliberately disabled: only the durable policy may retry a mutation.
     */
    private String get(String address) throws Exception {
        ensureActiveAccount();
        return transport.get(address);
    }

    /** Executes the production HTTP request without an application-level retry. */
    private String getNetwork(String address) throws Exception {
        return PmHttpTransport.get(address, MAX_RESPONSE_BYTES, this::ensureActiveAccount);
    }

    /** Refreshes the badge from the server only while the original account remains active. */
    private void refreshUnreadCount(String auth, PmDeletionStore.Entry entry) {
        try {
            JSONObject identity = new JSONObject(get(Config.CHECK_AUTH_URL + auth));
            if (identity.optInt("state") != 1 || identity.optInt("user_id") != entry.userId) return;
            int unread = Math.max(0, identity.getInt("pm_unread"));
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (!entry.accountKey.equals(session.currentKey())) return;
                session.updateUnread(unread);
                EventBus.getDefault().post(new MessageEvent(null, null, null,
                        String.valueOf(unread), "deletePm", null));
            });
        } catch (Exception exception) {
            // Deletion is already acknowledged. Badge refresh must not repeat the mutation.
        }
    }
}

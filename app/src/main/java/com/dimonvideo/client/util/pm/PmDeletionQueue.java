package com.dimonvideo.client.util.pm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.dimonvideo.client.R;
import com.dimonvideo.client.util.AppController;

import org.greenrobot.eventbus.EventBus;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Saves deletion intents before UI removal and restores their durable background work. */
public final class PmDeletionQueue {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Distinguishes accepted and completed deletions from absent or unreadable durable state. */
    public enum DeletionState { PENDING, COMPLETED, ABSENT, UNAVAILABLE }

    /** Separates durable intent acceptance from best-effort background work scheduling. */
    interface Scheduler {
        /** Persists the worker request or throws while leaving the accepted intent available for recovery. */
        void schedule(Context context, PmDeletionStore.Entry entry,
                      ExistingWorkPolicy policy) throws Exception;
    }

    /** Prevents instantiation of the process-wide queue coordinator. */
    private PmDeletionQueue() { }

    /** Returns the currently signed-in account identity without exposing its credentials. */
    public static String currentAccountKey() {
        AppController controller = AppController.getInstance();
        if (controller == null || controller.isAuth() <= 0) return null;
        return PmDeletionAccount.key(controller.userName(null), controller.isUserId());
    }

    /**
     * Persists an account-bound intent off the main thread, then accepts local UI removal.
     * A failed database write keeps the message visible. Once saved, the intent is accepted
     * even if scheduling fails; recovery re-enqueues it after the scheduler becomes available.
     */
    public static void enqueue(Context context, int messageId, Runnable onAccepted) {
        enqueue(context, messageId, 0, 1, onAccepted, null);
    }

    /** Reports queue rejection so a broadcast receiver can finish its asynchronous lifetime safely. */
    public static void enqueue(Context context, int messageId, Runnable onAccepted,
                               Runnable onRejected) {
        enqueue(context, messageId, 0, 1, onAccepted, onRejected);
    }

    /** Persists a deletion with best-effort server-page hints from the currently displayed list. */
    public static void enqueue(Context context, int messageId, int sourceFolder, int sourcePage,
                               Runnable onAccepted) {
        enqueue(context, messageId, sourceFolder, sourcePage, onAccepted, null);
    }

    /** Persists source hints and completes either acceptance or rejection on the main thread. */
    public static void enqueue(Context context, int messageId, int sourceFolder, int sourcePage,
                                Runnable onAccepted, Runnable onRejected) {
        enqueue(context, messageId, sourceFolder, sourcePage, onAccepted, onRejected,
                PmDeletionQueue::schedule);
    }

    /** Lets a reply form provide its own acceptance confirmation while still reporting queue failures. */
    public static void enqueue(Context context, int messageId, int sourceFolder, int sourcePage,
                               Runnable onAccepted, Runnable onRejected, boolean showAcceptedToast) {
        enqueue(context, messageId, sourceFolder, sourcePage, onAccepted, onRejected,
                PmDeletionQueue::schedule, showAcceptedToast);
    }

    /** Uses the real durable store while allowing scheduling failures to be verified independently. */
    static void enqueue(Context context, int messageId, int sourceFolder, int sourcePage,
                        Runnable onAccepted, Runnable onRejected, Scheduler scheduler) {
        enqueue(context, messageId, sourceFolder, sourcePage, onAccepted, onRejected, scheduler, true);
    }

    /** Accepts only a persisted intent; caller-owned confirmation suppresses only the ordinary success toast. */
    static void enqueue(Context context, int messageId, int sourceFolder, int sourcePage,
                        Runnable onAccepted, Runnable onRejected, Scheduler scheduler, boolean showAcceptedToast) {
        Context appContext = context.getApplicationContext();
        String accountKey = currentAccountKey();
        int userId = AppController.getInstance().isUserId();
        if (accountKey == null || messageId <= 0) {
            reject(appContext, onRejected);
            return;
        }
        IO.execute(() -> {
            PmDeletionStore.Entry entry;
            try {
                PmDeletionStore store = PmDeletionStore.get(appContext);
                long now = System.currentTimeMillis();
                entry = store.insert(
                        accountKey, userId, messageId, now, sourceFolder, sourcePage);
            } catch (Exception exception) {
                // No durable intent was accepted, so keep the message visible.
                // Never include request URLs, credentials, or server response text in logs.
                reject(appContext, onRejected);
                return;
            }
            try {
                // Unique work includes the intent generation, so an old running/expired worker
                // cannot make KEEP discard a fresh explicit request for the same message.
                scheduler.schedule(appContext, entry, ExistingWorkPolicy.KEEP);
            } catch (Exception exception) {
                // The saved intent remains accepted and recoverable. Reporting rejection here
                // would allow a later resume to delete a message the UI still showed as unqueued.
            }
            MAIN.post(() -> {
                if (!accountKey.equals(currentAccountKey())) {
                    if (onRejected != null) onRejected.run();
                    return;
                }
                if (onAccepted != null) onAccepted.run();
                if (showAcceptedToast) {
                    Toast.makeText(appContext, R.string.pm_delete_queued, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /** Keeps local UI visible after a failed commit and completes any asynchronous receiver callback. */
    private static void reject(Context context, Runnable onRejected) {
        MAIN.post(() -> {
            Toast.makeText(context, R.string.pm_delete_queue_error, Toast.LENGTH_LONG).show();
            if (onRejected != null) onRejected.run();
        });
    }

    /** Repairs the save/schedule crash window and expires old intents when the app starts. */
    public static void resume(Context context) {
        resume(context, PmDeletionQueue::schedule);
    }

    /** Retries each saved intent independently so one unavailable work request cannot block others. */
    static void resume(Context context, Scheduler scheduler) {
        Context appContext = context.getApplicationContext();
        IO.execute(() -> {
            try {
                for (PmDeletionStore.Entry entry : PmDeletionStore.get(appContext).entries()) {
                    try {
                        if (PmDeletionRetryPolicy.isExpired(entry.createdAt, System.currentTimeMillis())) {
                            finish(appContext, entry, PmDeletionEvent.Outcome.EXPIRED);
                        } else {
                            scheduler.schedule(appContext, entry, ExistingWorkPolicy.KEEP);
                        }
                    } catch (Exception exception) {
                        // Keep this accepted intent for a later recovery and continue with others.
                    }
                }
            } catch (Exception exception) {
                // A later app start or surviving WorkManager request can recover the intent.
            }
        });
    }

    /**
     * Returns this account's live pending IDs on the main thread for refresh filtering.
     * Expired intents are never hidden, including while offline constraints delay a worker.
     */
    public static void getPendingIds(Context context, Consumer<Set<Integer>> callback) {
        Context appContext = context.getApplicationContext();
        String accountKey = currentAccountKey();
        IO.execute(() -> {
            Set<Integer> ids = new HashSet<>();
            if (accountKey != null) {
                try {
                    for (PmDeletionStore.Entry entry : PmDeletionStore.get(appContext).entries()) {
                        if (entry.accountKey.equals(accountKey)
                                && !PmDeletionRetryPolicy.isExpired(entry.createdAt,
                                        System.currentTimeMillis())) ids.add(entry.messageId);
                    }
                } catch (Exception exception) {
                    // Showing messages is safer than hiding them when queue metadata is unreadable.
                }
            }
            MAIN.post(() -> callback.accept(accountKey != null
                    && accountKey.equals(currentAccountKey()) ? ids : new HashSet<>()));
        });
    }

    /**
     * Reads one account-bound durable intent before a restored composer may submit new actions.
     * Uses the insertion executor so an earlier pending commit cannot be overtaken by this lookup.
     * Failed reads remain unavailable instead of being mistaken for an absent deletion.
     */
    public static void getDeletionState(Context context, String accountKey, int messageId,
                                        Consumer<DeletionState> callback) {
        getDeletionState(context, accountKey, messageId, 0, callback);
    }

    /**
     * Reconciles a restored snapshot with pending work and deletions completed after it was opened.
     * Older completed generations cannot invalidate a fresh composer for a message restored on the site.
     * A zero opening time conservatively reconciles snapshots saved by versions without this field.
     */
    public static void getDeletionState(Context context, String accountKey, int messageId,
                                        long composerOpenedAt, Consumer<DeletionState> callback) {
        Context appContext = context.getApplicationContext();
        IO.execute(() -> {
            DeletionState result = DeletionState.UNAVAILABLE;
            if (accountKey != null && messageId > 0 && accountKey.equals(currentAccountKey())) {
                try {
                    PmDeletionStore.Entry entry = PmDeletionStore.get(appContext)
                            .findIncludingCompleted(accountKey, messageId);
                    if (entry != null && entry.confirmed && entry.completedAt >= composerOpenedAt) {
                        result = DeletionState.COMPLETED;
                    } else if (entry != null && !entry.confirmed
                            && !PmDeletionRetryPolicy.isExpired(entry.createdAt, System.currentTimeMillis())) {
                        result = DeletionState.PENDING;
                    } else {
                        result = DeletionState.ABSENT;
                    }
                } catch (Exception exception) {
                    // Keep restored actions blocked until the user can verify the durable store.
                }
            }
            DeletionState checked = result;
            MAIN.post(() -> callback.accept(accountKey != null && accountKey.equals(currentAccountKey())
                    ? checked : DeletionState.UNAVAILABLE));
        });
    }

    /** Schedules a unique per-account/message worker and waits for durable WorkManager storage. */
    static void schedule(Context context, PmDeletionStore.Entry entry,
                         ExistingWorkPolicy policy) throws Exception {
        long delay = Math.max(0, entry.nextAttemptAt - System.currentTimeMillis());
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PmDeletionWorker.class)
                .setInputData(new Data.Builder().putString("account_key", entry.accountKey)
                        .putInt("message_id", entry.messageId)
                        .putLong("created_at", entry.createdAt).build())
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR,
                        PmDeletionRetryPolicy.FIRST_RETRY_MS, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "pm-delete-" + entry.accountKey + "-" + entry.messageId + "-" + entry.createdAt,
                policy, request)
                .getResult().get();
    }

    /** Retains confirmed evidence or expires pending work before emitting an account-scoped result. */
    static void finish(Context context, PmDeletionStore.Entry entry,
                       PmDeletionEvent.Outcome outcome) {
        PmDeletionStore store = PmDeletionStore.get(context);
        boolean finished = outcome == PmDeletionEvent.Outcome.CONFIRMED
                ? store.complete(entry) : store.remove(entry);
        if (!finished) return;
        MAIN.post(() -> {
            EventBus.getDefault().post(new PmDeletionEvent(entry.accountKey, entry.messageId, outcome));
            if (outcome == PmDeletionEvent.Outcome.EXPIRED
                    && entry.accountKey.equals(currentAccountKey())) {
                Toast.makeText(context, R.string.pm_delete_expired, Toast.LENGTH_LONG).show();
            }
        });
    }
}

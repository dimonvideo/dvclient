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
     * A failed database write keeps the message visible. Scheduling is also awaited before
     * acceptance; recovery re-enqueues saved intents after an interrupted app process.
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
        Context appContext = context.getApplicationContext();
        String accountKey = currentAccountKey();
        int userId = AppController.getInstance().isUserId();
        if (accountKey == null || messageId <= 0) {
            reject(appContext, onRejected);
            return;
        }
        IO.execute(() -> {
            try {
                PmDeletionStore store = PmDeletionStore.get(appContext);
                long now = System.currentTimeMillis();
                PmDeletionStore.Entry entry = store.insert(
                        accountKey, userId, messageId, now, sourceFolder, sourcePage);
                // Unique work includes the intent generation, so an old running/expired worker
                // cannot make KEEP discard a fresh explicit request for the same message.
                schedule(appContext, entry, ExistingWorkPolicy.KEEP);
                MAIN.post(() -> {
                    if (!accountKey.equals(currentAccountKey())) {
                        if (onRejected != null) onRejected.run();
                        return;
                    }
                    if (onAccepted != null) onAccepted.run();
                    Toast.makeText(appContext, R.string.pm_delete_queued, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception exception) {
                // Never include request URLs, credentials, or server response text in logs.
                reject(appContext, onRejected);
            }
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
        Context appContext = context.getApplicationContext();
        IO.execute(() -> {
            try {
                for (PmDeletionStore.Entry entry : PmDeletionStore.get(appContext).entries()) {
                    if (PmDeletionRetryPolicy.isExpired(entry.createdAt, System.currentTimeMillis())) {
                        finish(appContext, entry, PmDeletionEvent.Outcome.EXPIRED);
                    } else {
                        schedule(appContext, entry, ExistingWorkPolicy.KEEP);
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

    /** Removes durable metadata and emits a main-thread result scoped to its original account. */
    static void finish(Context context, PmDeletionStore.Entry entry,
                       PmDeletionEvent.Outcome outcome) {
        if (!PmDeletionStore.get(context).remove(entry)) return;
        MAIN.post(() -> {
            EventBus.getDefault().post(new PmDeletionEvent(entry.accountKey, entry.messageId, outcome));
            if (outcome == PmDeletionEvent.Outcome.EXPIRED
                    && entry.accountKey.equals(currentAccountKey())) {
                Toast.makeText(context, R.string.pm_delete_expired, Toast.LENGTH_LONG).show();
            }
        });
    }
}

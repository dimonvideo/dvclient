package com.dimonvideo.client.util.pm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.testing.TestWorkerBuilder;

import com.dimonvideo.client.Config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Runs real Worker instances against SQLite and fake HTTP; no production server is contacted. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 28, application = android.app.Application.class)
public class PmDeletionWorkerTest {
    private static final int MESSAGE_ID = 55;
    private final String accountKey = PmDeletionAccount.key("Alice", 12);
    private final AtomicLong clock = new AtomicLong(1_000_000);
    private final List<PmDeletionStore.Entry> scheduled = new ArrayList<>();
    private PmDeletionStore store;
    private FakeSession session;
    private FakeServer server;
    private boolean schedulingFails;

    /** Creates a fresh durable intent with controlled time, account, and server behavior. */
    @Before
    public void prepareIntent() {
        store = PmDeletionStore.get(RuntimeEnvironment.getApplication());
        store.getWritableDatabase().delete("pm_deletions", null, null);
        session = new FakeSession(accountKey);
        server = new FakeServer();
        store.insert(accountKey, 12, MESSAGE_ID, clock.get());
    }

    /** Actual worker failures persist the ten-minute first retry and thirty-minute next retry. */
    @Test
    public void transportFailuresPersistTheRequestedRetrySchedule() {
        server.offline = true;
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        PmDeletionStore.Entry first = store.find(accountKey, MESSAGE_ID);
        assertEquals(1, first.failures);
        assertEquals(clock.get() + TimeUnit.MINUTES.toMillis(10), first.nextAttemptAt);
        clock.set(first.nextAttemptAt);
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        PmDeletionStore.Entry second = store.find(accountKey, MESSAGE_ID);
        assertEquals(2, second.failures);
        assertEquals(clock.get() + TimeUnit.MINUTES.toMillis(30), second.nextAttemptAt);
        assertEquals(2, scheduled.size());
        assertEquals(0, server.mutations);
    }

    /** A request applied remotely but lost to a timeout is reconciled without another mutation. */
    @Test
    public void lostMutationResponseIsConfirmedFromTrashWithoutDeletingAgain() {
        server.loseMutationResponse = true;
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        PmDeletionStore.Entry retry = store.find(accountKey, MESSAGE_ID);
        assertNotNull(retry);
        assertEquals(1, server.mutations);
        clock.set(retry.nextAttemptAt);
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        assertNull(store.find(accountKey, MESSAGE_ID));
        assertEquals(1, server.mutations);
        assertTrue(store.findIncludingCompleted(accountKey, MESSAGE_ID).confirmed);
    }

    /** Expiry after app/process sleep clears the intent without even authenticating remotely. */
    @Test
    public void expiredIntentMakesNoNetworkRequest() {
        clock.addAndGet(TimeUnit.HOURS.toMillis(6));
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        assertNull(store.find(accountKey, MESSAGE_ID));
        assertEquals(0, server.requests);
        assertEquals(0, scheduled.size());
        assertNull(store.findIncludingCompleted(accountKey, MESSAGE_ID));
    }

    /** Switching accounts leaves the old intent pending and never sends the new account's credentials. */
    @Test
    public void differentActiveAccountCannotRunTheDeletion() {
        session.key = PmDeletionAccount.key("Bob", 13);
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        assertNotNull(store.find(accountKey, MESSAGE_ID));
        assertEquals(0, server.requests);
        assertEquals(1, scheduled.size());
    }

    /** HTTP success carrying an empty mutation result cannot acknowledge a deletion. */
    @Test
    public void emptyDeletionResultRemainsQueued() {
        server.mutationBody = "[]";
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        assertNotNull(store.find(accountKey, MESSAGE_ID));
        assertEquals(1, scheduled.size());
    }

    /** The documented missing-row status is a valid terminal result after a positive preflight. */
    @Test
    public void explicitMissingRowAcknowledgesAnAlreadyRemovedMessage() {
        server.mutationBody = "[{\"status\":0}]";
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        assertNull(store.find(accountKey, MESSAGE_ID));
        assertEquals(1, server.mutations);
        assertTrue(store.findIncludingCompleted(accountKey, MESSAGE_ID).confirmed);
    }

    /** Invalid authentication stops before folder reads or a destructive request. */
    @Test
    public void rejectedAuthenticationNeverDeletes() {
        server.authBody = "{\"state\":0}";
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        assertNotNull(store.find(accountKey, MESSAGE_ID));
        assertEquals(1, server.requests);
        assertEquals(0, server.mutations);
    }

    /** A scheduling crash preserves both retry metadata and the original deadline for recovery. */
    @Test
    public void failedSchedulingDoesNotLoseTheDurableIntent() {
        server.offline = true;
        schedulingFails = true;
        long createdAt = clock.get();
        assertEquals(ListenableWorker.Result.retry(), worker().doWork());
        PmDeletionStore.Entry intent = store.find(accountKey, MESSAGE_ID);
        assertNotNull(intent);
        assertEquals(createdAt, intent.createdAt);
        assertEquals(createdAt + TimeUnit.MINUTES.toMillis(10), intent.nextAttemptAt);
    }

    /** Unconfirmed absence in every folder must not authorize a destructive retry or local acknowledgement. */
    @Test
    public void absentFromAllFoldersRemainsQueuedWithoutMutation() {
        server.active = false;
        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        assertNotNull(store.find(accountKey, MESSAGE_ID));
        assertEquals(0, server.mutations);
    }

    /** An obsolete WorkManager generation cannot consume a later explicit deletion of the same message. */
    @Test
    public void oldWorkerCannotExecuteRenewedIntent() {
        PmDeletionWorker oldWorker = worker();
        clock.addAndGet(PmDeletionRetryPolicy.LIFETIME_MS);
        store.insert(accountKey, 12, MESSAGE_ID, clock.get());
        assertEquals(ListenableWorker.Result.success(), oldWorker.doWork());
        assertEquals(clock.get(), store.find(accountKey, MESSAGE_ID).createdAt);
        assertEquals(0, server.requests);
        assertFalse(store.findIncludingCompleted(accountKey, MESSAGE_ID).confirmed);
    }

    /** Successful server deletion leaves a durable marker after reopening without exposing it as pending work. */
    @Test
    public void confirmedDeletionMarkerSurvivesDatabaseReopening() {
        long generation = store.find(accountKey, MESSAGE_ID).createdAt;

        assertEquals(ListenableWorker.Result.success(), worker().doWork());
        store.close();

        assertNull(store.find(accountKey, MESSAGE_ID));
        assertTrue(store.entries().isEmpty());
        PmDeletionStore.Entry completed = store.findIncludingCompleted(accountKey, MESSAGE_ID);
        assertNotNull(completed);
        assertTrue(completed.confirmed);
        assertEquals(generation, completed.createdAt);
        assertEquals(1, server.mutations);
        assertEquals(0, scheduled.size());
    }

    /** A surviving or duplicate WorkManager request cannot repeat a mutation after its generation is confirmed. */
    @Test
    public void completedGenerationNeverRunsAgainEvenAfterSixHours() {
        PmDeletionWorker request = worker();
        assertEquals(ListenableWorker.Result.success(), request.doWork());
        int requestsAfterCompletion = server.requests;
        clock.addAndGet(PmDeletionRetryPolicy.LIFETIME_MS);

        assertEquals(ListenableWorker.Result.success(), request.doWork());

        assertEquals(requestsAfterCompletion, server.requests);
        assertEquals(1, server.mutations);
        assertEquals(0, scheduled.size());
        assertTrue(store.findIncludingCompleted(accountKey, MESSAGE_ID).confirmed);
    }

    /** A failed completion write keeps pending metadata and later reconciles trash without deleting twice. */
    @Test
    public void failedCompletionWriteRetriesDurablyAndConfirmsFromTrash() {
        SQLiteDatabase database = store.getWritableDatabase();
        database.execSQL("CREATE TRIGGER worker_test_reject_completion BEFORE UPDATE OF confirmed "
                + "ON pm_deletions WHEN NEW.confirmed=1 "
                + "BEGIN SELECT RAISE(ABORT, 'Controlled completion write failure'); END");
        server.applyMutationToTrash = true;
        try {
            assertEquals(ListenableWorker.Result.success(), worker().doWork());
            assertNotNull(store.find(accountKey, MESSAGE_ID));
            assertFalse(store.findIncludingCompleted(accountKey, MESSAGE_ID).confirmed);
            assertEquals(1, scheduled.size());
            assertEquals(1, server.mutations);
        } finally {
            database.execSQL("DROP TRIGGER worker_test_reject_completion");
        }
        clock.set(store.find(accountKey, MESSAGE_ID).nextAttemptAt);

        assertEquals(ListenableWorker.Result.success(), worker().doWork());

        assertNull(store.find(accountKey, MESSAGE_ID));
        assertTrue(store.findIncludingCompleted(accountKey, MESSAGE_ID).confirmed);
        assertEquals(1, server.mutations);
    }

    /** Builds an actual WorkManager Worker with test-only boundaries instead of changing app startup. */
    private PmDeletionWorker worker() {
        return TestWorkerBuilder.from(RuntimeEnvironment.getApplication(), PmDeletionWorker.class,
                Runnable::run).setInputData(new Data.Builder().putString("account_key", accountKey)
                        .putInt("message_id", MESSAGE_ID)
                        .putLong("created_at", store.find(accountKey, MESSAGE_ID).createdAt).build())
                .setWorkerFactory(new WorkerFactory() {
                    /** Instantiates the production worker with deterministic external prerequisites. */
                    @Override
                    public ListenableWorker createWorker(@NonNull Context context,
                                                         @NonNull String className,
                                                         @NonNull WorkerParameters parameters) {
                        return new PmDeletionWorker(context, parameters, server::get, session,
                                entry -> {
                                    if (schedulingFails) throw new IOException("Fake scheduler unavailable");
                                    scheduled.add(entry);
                                }, clock::get);
                    }
                }).build();
    }

    /** Provides authentication only in memory; no test secret is written to queue or work data. */
    private static final class FakeSession implements PmDeletionWorker.AccountSession {
        private String key;

        /** Starts with the identity belonging to the original queued intent. */
        FakeSession(String key) { this.key = key; }
        /** Returns the account selected by the test. */
        @Override public String currentKey() { return key; }
        /** Supplies a fake login only to the fake transport. */
        @Override public String login() { return "Alice"; }
        /** Supplies fake credentials only in memory. */
        @Override public String password() { return "test-password"; }
        /** Does not mutate global application preferences in these isolated worker tests. */
        @Override public void updateUnread(int count) { }
    }

    /** Models documented folder/state responses and a server mutation whose response can be lost. */
    private static final class FakeServer {
        private int requests;
        private int mutations;
        private boolean offline;
        private boolean active = true;
        private boolean trash;
        private boolean loseMutationResponse;
        private boolean applyMutationToTrash;
        private String authBody = "{\"state\":1,\"user_id\":12,\"pm_unread\":3}";
        private String mutationBody = "[{\"status\":1}]";

        /** Returns fake JSON without opening a socket or calling production APIs. */
        String get(String address) throws IOException {
            requests++;
            if (offline) throw new IOException("Fake offline transport");
            if (address.startsWith(Config.CHECK_AUTH_URL)) return authBody;
            if (address.contains("&pm=10")) {
                mutations++;
                if (applyMutationToTrash) {
                    active = false;
                    trash = true;
                }
                if (loseMutationResponse) {
                    active = false;
                    trash = true;
                    throw new IOException("Fake lost server response");
                }
                return mutationBody;
            }
            if (address.endsWith("&pm=0") && active) return "[{\"lid\":55}]";
            if (address.endsWith("&pm=5") && trash) return "[{\"lid\":55}]";
            return "[]";
        }
    }
}

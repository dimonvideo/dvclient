package com.dimonvideo.client.util.pm;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Verifies the requested retry schedule and its hard expiry across delayed executions. */
public class PmDeletionRetryPolicyTest {
    /** The first transport failure receives a ten-minute retry window. */
    @Test
    public void firstFailureRetriesAfterTenMinutes() {
        long createdAt = 1_000_000;
        long failedAt = createdAt + 6000;
        assertEquals(failedAt + TimeUnit.MINUTES.toMillis(10),
                PmDeletionRetryPolicy.nextAttemptAt(createdAt, failedAt, 1));
    }

    /** Every later failure waits thirty minutes without increasing the original lifetime. */
    @Test
    public void subsequentFailuresRetryAfterThirtyMinutes() {
        long createdAt = 1_000_000;
        for (int failures : new int[]{2, 3, 12}) {
            long failedAt = createdAt + TimeUnit.HOURS.toMillis(2);
            assertEquals(failedAt + TimeUnit.MINUTES.toMillis(30),
                    PmDeletionRetryPolicy.nextAttemptAt(createdAt, failedAt, failures));
        }
    }

    /** A retry near expiry wakes only to clear the intent instead of extending it. */
    @Test
    public void deadlineCapsTheNextWakeUp() {
        long createdAt = 1_000_000;
        assertEquals(createdAt + TimeUnit.HOURS.toMillis(6),
                PmDeletionRetryPolicy.nextAttemptAt(createdAt,
                        createdAt + TimeUnit.HOURS.toMillis(5) + TimeUnit.MINUTES.toMillis(50), 12));
    }

    /** A worker resumed after offline sleep or process death must expire at the exact boundary. */
    @Test
    public void delayedWorkerNeverSendsAfterSixHours() {
        long createdAt = 1_000_000;
        long deadline = createdAt + TimeUnit.HOURS.toMillis(6);
        assertFalse(PmDeletionRetryPolicy.isExpired(createdAt, deadline - 1));
        assertTrue(PmDeletionRetryPolicy.isExpired(createdAt, deadline));
        assertTrue(PmDeletionRetryPolicy.isExpired(createdAt, deadline + TimeUnit.DAYS.toMillis(1)));
    }
}

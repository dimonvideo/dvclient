package com.dimonvideo.client.util.pm;

import java.util.concurrent.TimeUnit;

/** Absolute deadlines and retry intervals for a private-message deletion intent. */
public final class PmDeletionRetryPolicy {
    public static final long LIFETIME_MS = TimeUnit.HOURS.toMillis(6);
    public static final long FIRST_RETRY_MS = TimeUnit.MINUTES.toMillis(10);
    public static final long FOLLOWING_RETRY_MS = TimeUnit.MINUTES.toMillis(30);

    /** Prevents instantiation of the shared scheduling policy. */
    private PmDeletionRetryPolicy() { }

    /** Returns whether the original six-hour window has ended, including its boundary. */
    public static boolean isExpired(long createdAt, long now) {
        return now >= createdAt + LIFETIME_MS;
    }

    /**
     * Returns the next wake-up after a failed attempt, capped at the original deadline.
     * The deadline wake-up performs cleanup only; it must never send another deletion.
     */
    public static long nextAttemptAt(long createdAt, long failedAt, int failureCount) {
        long interval = failureCount <= 1 ? FIRST_RETRY_MS : FOLLOWING_RETRY_MS;
        return Math.min(createdAt + LIFETIME_MS, failedAt + interval);
    }
}

package com.dimonvideo.client.util.pm;

/** A deletion result scoped to its original account and server message ID. */
public final class PmDeletionEvent {
    public enum Outcome { CONFIRMED, EXPIRED }

    public final String accountKey;
    public final int messageId;
    public final Outcome outcome;

    /** Creates an event delivered on the main thread after durable queue cleanup. */
    public PmDeletionEvent(String accountKey, int messageId, Outcome outcome) {
        this.accountKey = accountKey;
        this.messageId = messageId;
        this.outcome = outcome;
    }
}

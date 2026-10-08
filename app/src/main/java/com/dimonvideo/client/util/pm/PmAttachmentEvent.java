package com.dimonvideo.client.util.pm;

/** Non-sticky result of one account-bound PM image picker/upload request. */
public final class PmAttachmentEvent {
    /** Distinguishes an uploaded image from a canceled picker or failed upload. */
    public enum Outcome { READY, CANCELED, FAILED }

    public final String requestId;
    public final String accountKey;
    public final String filename;
    public final Outcome outcome;

    /** Associates an upload result with the exact composer request and original account. */
    public PmAttachmentEvent(String requestId, String accountKey, String filename, Outcome outcome) {
        this.requestId = requestId;
        this.accountKey = accountKey;
        this.filename = filename;
        this.outcome = outcome;
    }
}

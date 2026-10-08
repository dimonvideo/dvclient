package com.dimonvideo.client.adater;

import com.dimonvideo.client.model.FeedPm;

/** Resolves API send parameters without using a sent-message ID as a reply-to-sender target. */
final class PmSendTarget {
    final int messageId, userId;

    /** Stores the mutually exclusive legacy reply message ID or new-message recipient ID. */
    private PmSendTarget(int messageId, int userId) {
        this.messageId = messageId;
        this.userId = userId;
    }

    /** Uses new-message mode for members/outgoing messages and permits replies only for received PMs. */
    static PmSendTarget from(FeedPm feed, boolean member) {
        if (member) {
            if (feed.getId() <= 0) throw new IllegalArgumentException("Missing member ID");
            return new PmSendTarget(0, feed.getId());
        }
        if (feed.isOutgoing()) {
            if (feed.getRecipientId() <= 0) throw new IllegalArgumentException("Unresolved recipient");
            return new PmSendTarget(0, feed.getRecipientId());
        }
        if (feed.getId() <= 0) throw new IllegalArgumentException("Missing message ID");
        return new PmSendTarget(feed.getId(), 0);
    }
}

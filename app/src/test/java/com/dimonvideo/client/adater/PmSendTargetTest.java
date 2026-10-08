package com.dimonvideo.client.adater;

import com.dimonvideo.client.model.FeedPm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Verifies PHP-compatible reply/new-message parameter selection independently of HTTP transport. */
public class PmSendTargetTest {
    /** Replies to received messages use the source PM ID and let PHP target its original sender. */
    @Test
    public void incomingUsesReplyMode() {
        FeedPm feed = new FeedPm();
        feed.setId(42);
        PmSendTarget target = PmSendTarget.from(feed, false);
        assertEquals(42, target.messageId);
        assertEquals(0, target.userId);
    }

    /** Sent or outgoing-trash messages always start a new PM addressed to the verified recipient. */
    @Test
    public void outgoingNeverRepliesToOwnOriginalSender() {
        FeedPm feed = new FeedPm();
        feed.setId(42);
        feed.setOutgoing(true);
        feed.setRecipientId(88);
        PmSendTarget target = PmSendTarget.from(feed, false);
        assertEquals(0, target.messageId);
        assertEquals(88, target.userId);
    }

    /** Failed lookup must never silently fall back to replying to the sent PM and thus to this account. */
    @Test(expected = IllegalArgumentException.class)
    public void unresolvedOutgoingCannotProduceSendParameters() {
        FeedPm feed = new FeedPm();
        feed.setId(42);
        feed.setOutgoing(true);
        PmSendTarget.from(feed, false);
    }
}

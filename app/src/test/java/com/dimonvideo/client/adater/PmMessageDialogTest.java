package com.dimonvideo.client.adater;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.widget.Button;
import android.widget.EditText;

import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.pm.PmAttachmentEvent;
import com.dimonvideo.client.util.pm.PmRecipientResolver;

import org.greenrobot.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Exercises real composer UI and EventBus delivery without credentials, uploads or a server. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class PmMessageDialogTest {
    private ActivityController<Activity> activityController;
    private FakeOperations operations;
    private PmMessageDialog composer;
    private PmMessageDialog.Draft draft;
    private int dismissed, deleted;

    /** Creates a themed activity and controlled side effects without starting the application services. */
    @Before
    public void setUp() {
        activityController = Robolectric.buildActivity(Activity.class);
        activityController.get().setTheme(R.style.AppTheme);
        activityController.setup();
        operations = new FakeOperations();
        draft = new PmMessageDialog.Draft();
        draft.text = "reply to original sender";
    }

    /** Detaches the composer and parser regardless of whether a test ended during a request. */
    @After
    public void tearDown() {
        if (composer != null) composer.dismiss();
        activityController.pause().stop().destroy();
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Opening an old-account sheet must release its parser and preserve its unsent draft. */
    @Test
    public void accountChangedBeforeShowRejectsComposerAndReleasesSubscription() {
        createComposer();
        operations.account = "another-account";
        composer.show();
        assertFalse(EventBus.getDefault().isRegistered(composer));
        assertEquals(1, dismissed);
        assertEquals("reply to original sender", draft.text);
        assertEquals(0, operations.sends);
    }

    /** A send click after account switch must never submit the old message ID under new credentials. */
    @Test
    public void accountChangedBeforeSendKeepsDraftAndDoesNotSend() {
        showComposer();
        operations.account = "another-account";
        button(R.id.pm_reply_send).performClick();
        assertEquals(0, operations.sends);
        assertEquals("reply to original sender", draft.text);
        assertFalse(dialog().isShowing());
        assertFalse(EventBus.getDefault().isRegistered(composer));
    }

    /** A late success for an old account must not clear its draft or remove a new account's message. */
    @Test
    public void changedAccountDuringRequestRejectsLateDeletionCallback() {
        showComposer();
        button(R.id.pm_reply_send_delete).performClick();
        assertEquals(1, operations.sends);
        operations.account = "another-account";
        operations.callback.onSuccess();
        assertEquals("reply to original sender", draft.text);
        assertEquals(0, deleted);
        assertFalse(dialog().isShowing());
    }

    /** Transport/server rejection must retain both text and attachment and permit a manual retry. */
    @Test
    public void rejectedSendRetainsDraftAndAttachment() {
        draft.attachment = "existing.png";
        showComposer();
        button(R.id.pm_reply_send).performClick();
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        assertEquals("existing.png", operations.submittedAttachment);
        operations.callback.onError();
        assertTrue(dialog().isShowing());
        assertTrue(button(R.id.pm_reply_send).isEnabled());
        assertEquals("reply to original sender", draft.text);
        assertEquals("existing.png", draft.attachment);
        assertEquals(0, dismissed);
    }

    /** Real EventBus dispatch must unblock a canceled picker and reject another sheet's upload. */
    @Test
    public void correlatedUploadAndCancelCannotMixMessageAttachmentsOrBlockSending() {
        showComposer();
        button(R.id.pm_attach_button).performClick();
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        EventBus.getDefault().post(new PmAttachmentEvent("other-request", operations.account,
                "wrong.png", PmAttachmentEvent.Outcome.READY));
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        EventBus.getDefault().post(new PmAttachmentEvent(operations.pickedRequest, operations.account,
                null, PmAttachmentEvent.Outcome.CANCELED));
        assertTrue(button(R.id.pm_reply_send).isEnabled());
        button(R.id.pm_attach_button).performClick();
        EventBus.getDefault().post(new PmAttachmentEvent(operations.pickedRequest, operations.account,
                "right.png", PmAttachmentEvent.Outcome.READY));
        assertEquals("right.png", draft.attachment);
        assertTrue(button(R.id.pm_reply_send).isEnabled());
    }

    /** A result completed after submission must not be erased along with the acknowledged attachment. */
    @Test
    public void successClearsOnlySubmittedAttachment() {
        draft.attachment = "submitted.png";
        showComposer();
        button(R.id.pm_reply_send).performClick();
        draft.attachment = "newer.png";
        operations.callback.onSuccess();
        assertEquals("newer.png", draft.attachment);
        assertEquals("", draft.text);
        assertEquals("", ((EditText) dialog().findViewById(R.id.pm_reply_input)).getText().toString());
        assertTrue(dialog().isShowing());
        assertTrue(button(R.id.pm_reply_send).isEnabled());
    }

    /** Outgoing PM IDs must never use PHP's reply-to-user_from branch, which would target this account. */
    @Test
    public void outgoingMessageWaitsForExactRecipientAndSendsNewMessageToResolvedUid() {
        createOutgoingComposer();
        composer.show();
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        button(R.id.pm_reply_send).performClick();
        assertEquals(0, operations.sends);
        operations.recipientCallback.onResolved(88);
        button(R.id.pm_reply_send).performClick();
        assertEquals(1, operations.sends);
        assertEquals(0, operations.target.messageId);
        assertEquals(88, operations.target.userId);
    }

    /** Missing or partial-name search results keep full viewing and the draft available without sending. */
    @Test
    public void outgoingLookupFailureLeavesSendDisabledAndProvidesRetry() {
        createOutgoingComposer();
        composer.show();
        operations.recipientCallback.onUnavailable();
        button(R.id.pm_reply_send).performClick();
        assertEquals(0, operations.sends);
        assertEquals("reply to original sender", draft.text);
        assertTrue(dialog().isShowing());
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        assertEquals(android.view.View.VISIBLE, button(R.id.pm_recipient_retry).getVisibility());
    }

    /** Failed sends must not delete the original PM or persist a deletion intent. */
    @Test
    public void sendAndDeleteNeverQueuesDeletionBeforeAcknowledgedSend() {
        showComposer();
        button(R.id.pm_reply_send_delete).performClick();
        assertEquals(0, operations.queuedDeletes);
        operations.callback.onError();
        assertEquals(0, operations.queuedDeletes);
        assertEquals(0, deleted);
        assertEquals("reply to original sender", draft.text);
    }

    /** Acknowledged sends clear the draft, while row removal waits for the separate durable queue acceptance. */
    @Test
    public void sendAndDeleteSeparatesSendAcknowledgementFromDurableRemoval() {
        showComposer();
        button(R.id.pm_reply_send_delete).performClick();
        operations.callback.onSuccess();
        assertEquals(1, operations.queuedDeletes);
        assertEquals(0, deleted);
        assertEquals("", draft.text);
        assertFalse(dialog().isShowing());
        operations.deletionAccepted.run();
        assertEquals(1, deleted);
    }

    /** Sets original sender/recipient metadata without confusing the outgoing ID with a recipient UID. */
    private void createOutgoingComposer() {
        FeedPm message = new FeedPm();
        message.setId(42);
        message.setTitle("sent subject");
        message.setSenderName("my-own-login");
        message.setRecipientName("actual-recipient");
        message.setOutgoing(true);
        message.setSourceFolder(1);
        message.setFullHtml("<b>sent complete body</b>");
        composer = new PmMessageDialog(activityController.get(), message, false, draft,
                () -> deleted++, () -> dismissed++, operations, 14);
    }

    /** Builds an ordinary received message with distinct full HTML and a short list preview. */
    private void createComposer() {
        FeedPm message = new FeedPm();
        message.setId(42);
        message.setTitle("subject");
        message.setFullHtml("<b>full &amp; message</b>");
        message.setPreviewHtml("short preview");
        composer = new PmMessageDialog(activityController.get(), message, false, draft,
                () -> deleted++, () -> dismissed++, operations, 14);
    }

    /** Opens the actual sheet and lets its show/dismiss listeners run on the test main looper. */
    private void showComposer() {
        createComposer();
        composer.show();
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Returns the actual latest Material bottom sheet, rather than a mocked UI object. */
    private Dialog dialog() { return ShadowDialog.getLatestDialog(); }

    /** Resolves one production layout button for real click/enable state checks. */
    private Button button(int id) { return dialog().findViewById(id); }

    /** Controls the account and operation callbacks while recording recipient-related send values. */
    private static final class FakeOperations implements PmMessageDialog.Operations {
        String account = "original-account";
        String pickedRequest, submittedAttachment;
        int sends, queuedDeletes;
        PmSendTarget target;
        Runnable deletionAccepted;
        PmRecipientResolver.Callback recipientCallback;
        NetworkUtils.PmOperationCallback callback;

        /** Returns the controlled account identity without any real preferences or secrets. */
        @Override public String currentAccountKey() { return account; }

        /** Records a send and leaves it pending until the test delivers success or rejection. */
        @Override
        public void send(FeedPm feed, boolean member, String text, String attachment,
                         NetworkUtils.PmOperationCallback callback) {
            sends++;
            submittedAttachment = attachment;
            target = PmSendTarget.from(feed, member);
            this.callback = callback;
        }

        /** Leaves durable acceptance pending so tests can distinguish send acknowledgement from removal. */
        @Override public void enqueueDeletion(FeedPm feed, Runnable onAccepted) {
            queuedDeletes++;
            deletionAccepted = onAccepted;
        }

        /** Holds a read-only lookup callback without querying a public API. */
        @Override
        public PmRecipientResolver.Handle resolveRecipient(String name, String account,
                                                            PmRecipientResolver.Callback callback) {
            recipientCallback = callback;
            return null;
        }

        /** Records the exact correlation ID instead of launching the system media picker. */
        @Override public boolean pickImage(String requestId, String accountKey) {
            pickedRequest = requestId;
            return true;
        }
    }
}

package com.dimonvideo.client.adater;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;

import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.pm.PmAttachmentEvent;
import com.dimonvideo.client.util.pm.PmRecipientResolver;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.textfield.TextInputLayout;

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
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
        ShadowToast.reset();
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

    /** Direct deletion preserves the reply and its exact trash routing until durable storage accepts it. */
    @Test
    public void directDeleteWaitsForQueueAcceptanceAndNeverSendsReply() {
        FeedPm message = new FeedPm();
        message.setId(777);
        message.setSourceFolder(2);
        message.setSourcePage(6);
        message.setTitle("trash message");
        draft.attachment = "unsent.png";
        composer = new PmMessageDialog(activityController.get(), message, false, draft,
                () -> deleted++, () -> dismissed++, operations, 14);
        composer.show();
        button(R.id.pm_detail_delete).performClick();
        button(R.id.pm_detail_delete).performClick();
        assertEquals(0, operations.sends);
        assertEquals(1, operations.queuedDeletes);
        assertEquals(777, operations.deletionFeed.getId());
        assertEquals(2, operations.deletionFeed.getSourceFolder());
        assertEquals(6, operations.deletionFeed.getSourcePage());
        assertEquals(0, deleted);
        assertEquals(0, dismissed);
        assertTrue(dialog().isShowing());
        assertFalse(button(R.id.pm_detail_delete).isEnabled());
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        assertFalse(button(R.id.pm_detail_close).isEnabled());
        assertEquals("reply to original sender", draft.text);
        assertEquals("unsent.png", draft.attachment);
        operations.deletionAccepted.run();
        assertEquals(1, deleted);
        assertEquals(1, dismissed);
        assertFalse(dialog().isShowing());
    }

    /** A storage rejection retains freshly edited text and attachment and allows another explicit deletion. */
    @Test
    public void rejectedDirectDeleteKeepsDraftAndRestoresControls() {
        draft.attachment = "keep.png";
        showComposer();
        ((EditText) dialog().findViewById(R.id.pm_reply_input)).setText("latest unsent draft");
        button(R.id.pm_detail_delete).performClick();
        operations.deletionRejected.run();
        assertEquals(0, operations.sends);
        assertEquals(0, deleted);
        assertEquals(0, dismissed);
        assertTrue(dialog().isShowing());
        assertEquals("latest unsent draft", draft.text);
        assertEquals("latest unsent draft",
                ((EditText) dialog().findViewById(R.id.pm_reply_input)).getText().toString());
        assertEquals("keep.png", draft.attachment);
        assertTrue(button(R.id.pm_detail_delete).isEnabled());
        assertTrue(button(R.id.pm_reply_send).isEnabled());
        assertTrue(button(R.id.pm_detail_close).isEnabled());
        assertTrue(button(R.id.pm_attach_button).isEnabled());
        button(R.id.pm_detail_delete).performClick();
        assertEquals(2, operations.queuedDeletes);
    }

    /** Switching accounts before a direct delete rejects the stale sheet without queuing the old message. */
    @Test
    public void directDeleteAfterAccountSwitchCannotQueueOriginalMessage() {
        showComposer();
        operations.account = "another-account";
        button(R.id.pm_detail_delete).performClick();
        assertEquals(0, operations.queuedDeletes);
        assertEquals(0, operations.sends);
        assertEquals(0, deleted);
        assertEquals("reply to original sender", draft.text);
        assertFalse(dialog().isShowing());
    }

    /** A queue callback belonging to the old account cannot remove a row from the newly active account. */
    @Test
    public void directDeleteAcceptanceAfterAccountSwitchDoesNotRemoveAnotherAccountsRow() {
        showComposer();
        button(R.id.pm_detail_delete).performClick();
        operations.account = "another-account";
        operations.deletionAccepted.run();
        assertEquals(0, deleted);
        assertEquals(0, operations.sends);
        assertEquals("reply to original sender", draft.text);
    }

    /** Deleting an outgoing source PM requires neither a recipient lookup nor an acknowledged reply. */
    @Test
    public void directDeleteDoesNotWaitForOutgoingRecipientLookup() {
        createOutgoingComposer();
        composer.show();
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        assertTrue(button(R.id.pm_detail_delete).isEnabled());
        button(R.id.pm_detail_delete).performClick();
        assertEquals(1, operations.queuedDeletes);
        assertEquals(42, operations.deletionFeed.getId());
        assertEquals(1, operations.deletionFeed.getSourceFolder());
        assertEquals(0, operations.sends);
        operations.deletionAccepted.run();
        assertEquals(1, deleted);
    }

    /** An active image picker blocks reply sending while direct source deletion remains available. */
    @Test
    public void pendingAttachmentDoesNotPreventDirectDeletionOrTriggerSending() {
        showComposer();
        button(R.id.pm_attach_button).performClick();
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        assertTrue(button(R.id.pm_detail_delete).isEnabled());
        button(R.id.pm_detail_delete).performClick();
        assertEquals(1, operations.queuedDeletes);
        assertEquals(0, operations.sends);
        operations.deletionAccepted.run();
        assertEquals(1, deleted);
    }

    /** A short reply click acknowledges sending without deleting or showing a send-and-delete confirmation. */
    @Test
    public void ordinaryReplyClickNeverDeletesOriginal() {
        showComposer();
        button(R.id.pm_reply_send).performClick();
        operations.callback.onSuccess();
        assertEquals(1, operations.sends);
        assertEquals(0, operations.queuedDeletes);
        assertEquals(0, deleted);
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    /** Holding the reply button is consumed and confirms removal only after send and durable queue acceptance. */
    @Test
    @Config(qualifiers = "ru")
    public void longReplyPressSendsOnceAndConfirmsAcceptedDeletionInRussian() {
        showComposer();
        assertTrue(button(R.id.pm_reply_send).performLongClick());
        assertTrue(button(R.id.pm_reply_send).performLongClick());
        assertEquals(1, operations.sends);
        assertEquals(42, operations.target.messageId);
        assertEquals(0, operations.queuedDeletes);
        assertNull(ShadowToast.getTextOfLatestToast());
        operations.callback.onSuccess();
        assertEquals(1, operations.queuedDeletes);
        assertEquals(0, deleted);
        assertNull(ShadowToast.getTextOfLatestToast());
        operations.deletionAccepted.run();
        assertEquals(1, deleted);
        assertEquals("Отправлено, исходное сообщение удалено", ShadowToast.getTextOfLatestToast());
    }

    /** A rejected long-press reply leaves its source and draft intact without a misleading success toast. */
    @Test
    public void failedLongReplyPressKeepsOriginalAndDoesNotConfirmDeletion() {
        showComposer();
        button(R.id.pm_reply_send).performLongClick();
        operations.callback.onError();
        assertEquals(0, operations.queuedDeletes);
        assertEquals(0, deleted);
        assertEquals("reply to original sender", draft.text);
        assertTrue(button(R.id.pm_reply_send).isEnabled());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    /** Changing accounts between send and queue acceptance must not remove a row or show the old result. */
    @Test
    public void longReplyAcceptanceForOldAccountDoesNotConfirmOrRemoveNewAccountRow() {
        showComposer();
        button(R.id.pm_reply_send).performLongClick();
        operations.callback.onSuccess();
        operations.account = "another-account";
        operations.deletionAccepted.run();
        assertEquals(0, deleted);
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    /** A new-message form has no source PM to delete and does not expose the reply long-press action. */
    @Test
    public void newMessageLongPressCannotDeleteRecipientsUserId() {
        FeedPm recipient = new FeedPm();
        recipient.setId(88);
        recipient.setTitle("recipient");
        composer = new PmMessageDialog(activityController.get(), recipient, true, draft,
                () -> deleted++, () -> dismissed++, operations, 14);
        composer.show();
        assertEquals(View.GONE, button(R.id.pm_detail_delete).getVisibility());
        assertEquals(View.GONE, button(R.id.pm_reply_send_delete).getVisibility());
        assertEquals(activityController.get().getString(R.string.pm_send),
                button(R.id.pm_reply_send).getText().toString());
        assertFalse(button(R.id.pm_reply_send).performLongClick());
        assertEquals(0, operations.sends);
        button(R.id.pm_reply_send).performClick();
        operations.callback.onSuccess();
        assertEquals(0, operations.queuedDeletes);
        assertEquals(0, deleted);
    }

    /** Holding send cannot bypass attachment completion and submit a reply with an omitted pending upload. */
    @Test
    public void longReplyPressWaitsForPendingAttachment() {
        showComposer();
        button(R.id.pm_attach_button).performClick();
        button(R.id.pm_reply_send).performLongClick();
        assertEquals(0, operations.sends);
        assertEquals(0, operations.queuedDeletes);
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    /** Actual day-mode action, input, hint and metadata colors must remain readable on the sheet. */
    @Test
    @Config(qualifiers = "notnight")
    public void lightThemeFormMeetsTextAndActionContrast() {
        showComposer();
        assertFormContrast();
    }

    /** The application's dark toolbar primary must not make composer actions disappear into its surface. */
    @Test
    @Config(qualifiers = "night")
    public void darkThemeFormMeetsTextAndActionContrast() {
        showComposer();
        assertFormContrast();
    }

    /** Icon-only actions keep accessible names and touch targets while received messages label their reply. */
    @Test
    @Config(qualifiers = "ru")
    public void compactIconActionsRemainAccessibleAndReplyIsNamedInRussian() {
        showComposer();
        MaterialButton close = dialog().findViewById(R.id.pm_detail_close);
        MaterialButton attach = dialog().findViewById(R.id.pm_attach_button);
        assertEquals("", close.getText().toString());
        assertEquals(activityController.get().getString(R.string.pm_close), close.getContentDescription());
        assertEquals(activityController.get().getString(R.string.pm_attach_image),
                attach.getContentDescription());
        assertTrue(close.getLayoutParams().width >= dp(48));
        assertTrue(close.getLayoutParams().height >= dp(48));
        assertTrue(attach.getLayoutParams().width >= dp(48));
        assertTrue(attach.getLayoutParams().height >= dp(48));
        assertEquals("Ответить", button(R.id.pm_reply_send).getText().toString());
    }

    /** Only the outlined container draws a reply label; a second child hint would overlap it. */
    @Test
    public void replyUsesOneFloatingLabelWithoutChildHint() {
        showComposer();
        TextInputLayout reply = dialog().findViewById(R.id.pm_reply_layout);
        EditText editor = reply.getEditText();
        assertEquals(activityController.get().getString(R.string.pm_reply_text), reply.getHint());
        assertTrue(reply.isProvidingHint());
        assertNull(ReflectionHelpers.getField(editor, "mHint"));
        assertFalse(ReflectionHelpers.callInstanceMethod(reply, "isHintExpanded"));
        editor.setText("");
        View content = dialog().findViewById(R.id.pm_detail_scroll);
        content.setFocusableInTouchMode(true);
        content.requestFocus();
        ShadowLooper.shadowMainLooper().idle();
        assertTrue(ReflectionHelpers.callInstanceMethod(reply, "isHintExpanded"));
    }

    /** A member composer labels new text correctly and sizes itself to its content without an empty reader. */
    @Test
    public void shortMemberComposerWrapsContentAndUsesMessageLabel() {
        FeedPm member = new FeedPm();
        member.setId(88);
        member.setTitle("belinda");
        composer = new PmMessageDialog(activityController.get(), member, true, draft,
                () -> deleted++, () -> dismissed++, operations, 14);
        composer.show();
        ShadowLooper.shadowMainLooper().idle();
        TextInputLayout reply = dialog().findViewById(R.id.pm_reply_layout);
        assertEquals(activityController.get().getString(R.string.pm_message_text), reply.getHint());
        assertNull(ReflectionHelpers.getField(reply.getEditText(), "mHint"));
        assertEquals(View.GONE, dialog().findViewById(R.id.pm_detail_body).getVisibility());
        assertEquals(View.GONE, button(R.id.pm_reply_send_delete).getVisibility());
        assertEquals(View.GONE, button(R.id.pm_detail_delete).getVisibility());
        View sheet = dialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, sheet.getLayoutParams().height);
        NestedScrollView content = dialog().findViewById(R.id.pm_detail_scroll);
        content.measure(View.MeasureSpec.makeMeasureSpec(dp(320), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(1000), View.MeasureSpec.AT_MOST));
        assertTrue("Short composer should not reserve a full-screen message reader",
                content.getMeasuredHeight() < dp(500));
    }

    /** Large text and a long body can scroll all actions into view inside a narrow keyboard-sized viewport. */
    @Test
    @Config(qualifiers = "w320dp-h640dp")
    public void narrowLargeFontComposerKeepsFooterReachableByScrolling() {
        Configuration largeFont = new Configuration(activityController.get().getResources().getConfiguration());
        largeFont.fontScale = 1.6f;
        ContextThemeWrapper themed = new ContextThemeWrapper(activityController.get(), R.style.AppTheme);
        themed.applyOverrideConfiguration(largeFont);
        FeedPm message = new FeedPm();
        message.setId(42);
        message.setTitle("A longer subject wraps on a narrow screen");
        message.setFullHtml("Full message");
        composer = new PmMessageDialog(themed, message, false, draft,
                () -> deleted++, () -> dismissed++, operations, 22);
        composer.show();
        ShadowLooper.shadowMainLooper().idle();
        TextView body = dialog().findViewById(R.id.pm_detail_body);
        body.setText("Long message line\n".repeat(40));
        NestedScrollView content = dialog().findViewById(R.id.pm_detail_scroll);
        content.measure(View.MeasureSpec.makeMeasureSpec(dp(320), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(240), View.MeasureSpec.EXACTLY));
        content.layout(0, 0, content.getMeasuredWidth(), content.getMeasuredHeight());
        content.scrollTo(0, content.getChildAt(0).getHeight());
        assertTrue(content.canScrollVertically(-1));
        assertTrue("The final deletion action must be reachable after scrolling",
                button(R.id.pm_detail_delete).getBottom() - content.getScrollY() <= content.getHeight());
        assertTrue(((BottomSheetDialog) dialog()).getBehavior().getMaxHeight()
                <= activityController.get().getResources().getDisplayMetrics().heightPixels * 0.9f);
    }

    /** Measures the colors applied to production widgets, including attachment icons and filled-button text. */
    private void assertFormContrast() {
        View sheet = dialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        MaterialShapeDrawable background = (MaterialShapeDrawable) sheet.getBackground();
        int surface = background.getFillColor().getColorForState(sheet.getDrawableState(), 0);
        assertEquals(themedColor(com.google.android.material.R.attr.colorSurface), surface);
        assertReadable(((TextView) dialog().findViewById(R.id.pm_detail_title)).getCurrentTextColor(), surface);
        assertReadable(((TextView) dialog().findViewById(R.id.pm_detail_body)).getCurrentTextColor(), surface);
        assertReadable(((TextView) dialog().findViewById(R.id.pm_detail_sender)).getCurrentTextColor(), surface);
        assertReadable(((EditText) dialog().findViewById(R.id.pm_reply_input)).getCurrentTextColor(), surface);
        assertReadable(((EditText) dialog().findViewById(R.id.pm_reply_input)).getCurrentHintTextColor(), surface);
        MaterialButton close = dialog().findViewById(R.id.pm_detail_close);
        assertReadable(close.getIconTint().getColorForState(close.getDrawableState(), 0), surface);
        assertReadable(button(R.id.pm_detail_delete).getCurrentTextColor(), surface);
        assertReadable(button(R.id.pm_reply_send_delete).getCurrentTextColor(), surface);
        MaterialButton attach = dialog().findViewById(R.id.pm_attach_button);
        assertReadable(attach.getIconTint().getColorForState(attach.getDrawableState(), 0), surface);
        MaterialButton send = dialog().findViewById(R.id.pm_reply_send);
        assertReadable(send.getCurrentTextColor(),
                send.getBackgroundTintList().getColorForState(send.getDrawableState(), 0));
        TextInputLayout reply = dialog().findViewById(R.id.pm_reply_layout);
        reply.getEditText().requestFocus();
        ShadowLooper.shadowMainLooper().idle();
        assertFieldContrast(reply, surface);
        View content = dialog().findViewById(R.id.pm_detail_scroll);
        content.setFocusableInTouchMode(true);
        content.requestFocus();
        ShadowLooper.shadowMainLooper().idle();
        assertFieldContrast(reply, surface);
    }

    /** Checks the drawn floating label and current outline in both focused and resting field states. */
    private static void assertFieldContrast(TextInputLayout reply, int surface) {
        int label = ReflectionHelpers.callInstanceMethod(reply, "getHintCurrentCollapsedTextColor");
        assertReadable(label, surface);
        MaterialShapeDrawable box = ReflectionHelpers.callInstanceMethod(reply, "getBoxBackground");
        int outline = box.getStrokeColor().getColorForState(reply.getDrawableState(), 0);
        assertTrue("Input boundary contrast must be at least 3:1",
                ColorUtils.calculateContrast(outline, surface) >= 3.0);
    }

    /** Requires WCAG's normal-text contrast so theme regressions fail on the actual assigned colors. */
    private static void assertReadable(int foreground, int background) {
        assertTrue("Text/action contrast must be at least 4.5:1",
                ColorUtils.calculateContrast(foreground, background) >= 4.5);
    }

    /** Resolves one Material role from the sheet context, which differs from the activity toolbar theme. */
    private int themedColor(int attribute) {
        TypedValue value = new TypedValue();
        assertTrue(dialog().getContext().getTheme().resolveAttribute(attribute, value, true));
        return value.data;
    }

    /** Converts test viewport dimensions using the themed activity's current display density. */
    private int dp(int value) {
        return Math.round(value * activityController.get().getResources().getDisplayMetrics().density);
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
        Runnable deletionRejected;
        FeedPm deletionFeed;
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
            enqueueDeletion(feed, onAccepted, null);
        }

        /** Records exact source routing and both outcomes without contacting the durable store or server. */
        @Override public void enqueueDeletion(FeedPm feed, Runnable onAccepted, Runnable onRejected) {
            queuedDeletes++;
            deletionFeed = new FeedPm(feed);
            deletionAccepted = onAccepted;
            deletionRejected = onRejected;
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

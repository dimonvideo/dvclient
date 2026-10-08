package com.dimonvideo.client.adater;

import android.os.Bundle;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.pm.PmAttachmentEvent;
import com.dimonvideo.client.util.pm.PmAttachmentOwner;
import com.dimonvideo.client.util.pm.PmDeletionQueue;
import com.dimonvideo.client.util.pm.PmRecipientResolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Exercises the real plain-sheet fallback after FragmentManager has saved its state. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = PmFallbackAttachmentTest.TestApp.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class PmFallbackAttachmentTest {
    private ActivityController<TestActivity> activity;
    private PmAttachmentOwner owner;
    private PmMessageDialog composer;
    private PmMessageDialog.Draft draft;
    private String account;
    private boolean pickerAvailable = true;

    /** Supplies a verified account and a real saved-state activity without application startup services. */
    @Before
    public void setUp() {
        AppController.getInstance().getSharedPreferences().edit()
                .clear().putString("dvc_login", "original-user").putInt("user_id", 12)
                .putInt("auth_state", 1).commit();
        assertEquals(12, AppController.getInstance().isUserId());
        account = PmDeletionQueue.currentAccountKey();
        activity = Robolectric.buildActivity(TestActivity.class).setup();
        owner = new ViewModelProvider(activity.get()).get(PmAttachmentOwner.class);
        draft = new PmMessageDialog.Draft();
        draft.text = "Unsent reply";
        activity.saveInstanceState(new Bundle());
        assertTrue(activity.get().getSupportFragmentManager().isStateSaved());
    }

    /** Removes the fallback's forever observer and releases its renderer before destroying the host. */
    @After
    public void tearDown() {
        if (composer != null) composer.dismiss();
        activity.pause().stop().destroy();
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Successful picker/upload completion attaches the correlated file and acknowledges its buffered record. */
    @Test
    public void selectingImageForSavedStateFallbackAttachesFileAndUnblocksSending() {
        openFallback();
        String request = pickImage();
        owner.beginUpload(request, account);
        owner.complete(request, account, "selected.png", PmAttachmentEvent.Outcome.READY, account);
        assertEquals("selected.png", draft.attachment);
        assertNull(draft.attachmentRequest);
        assertTrue(button(R.id.pm_reply_send).isEnabled());
        assertTrue(button(R.id.pm_attach_button).isEnabled());
        assertNull(owner.result(request, account));
    }

    /** Canceling the picker preserves previous content while enabling another attachment and sending. */
    @Test
    public void cancelingSavedStateFallbackPickerUnblocksSendingAndPreservesOldFile() {
        draft.attachment = "existing.png";
        openFallback();
        String request = pickImage();
        owner.complete(request, account, null, PmAttachmentEvent.Outcome.CANCELED, account);
        assertEquals("existing.png", draft.attachment);
        assertEquals("Unsent reply", draft.text);
        assertNull(draft.attachmentRequest);
        assertTrue(button(R.id.pm_reply_send).isEnabled());
        assertTrue(button(R.id.pm_attach_button).isEnabled());
        assertNull(owner.result(request, account));
    }

    /** Transport rejection is consumed and permits a fresh picker request instead of a permanently disabled form. */
    @Test
    public void failedFallbackUploadEnablesManualRetryWithoutLosingText() {
        openFallback();
        String request = pickImage();
        owner.beginUpload(request, account);
        owner.complete(request, account, null, PmAttachmentEvent.Outcome.FAILED, account);
        assertEquals("Unsent reply", draft.text);
        assertTrue(button(R.id.pm_reply_send).isEnabled());
        assertNull(owner.result(request, account));
        String retry = pickImage();
        assertFalse(request.equals(retry));
        assertTrue(owner.isPending(retry, account));
    }

    /** Refusing a picker reservation keeps the form usable and explains why attachment selection did not open. */
    @Test
    public void unavailablePickerShowsNoticeAndDoesNotLeaveFallbackWaiting() {
        pickerAvailable = false;
        openFallback();
        button(R.id.pm_attach_button).performClick();
        assertNull(draft.attachmentRequest);
        assertTrue(button(R.id.pm_reply_send).isEnabled());
        assertTrue(button(R.id.pm_attach_button).isEnabled());
        assertEquals(activity.get().getString(R.string.pm_attachment_unavailable), ShadowToast.getTextOfLatestToast());
        assertNull(owner.pendingPicker());
    }

    /** Buffered files for other requests or accounts must not unlock or contaminate the waiting fallback draft. */
    @Test
    public void fallbackIgnoresOtherRequestAndAccountResults() {
        openFallback();
        String request = pickImage();
        owner.beginUpload(request, account);
        assertTrue(owner.beginPicker("another-request", account));
        owner.complete("another-request", account, "other-draft.png", PmAttachmentEvent.Outcome.READY, account);
        owner.complete(request, "another-account", "private-other-account.png",
                PmAttachmentEvent.Outcome.READY, "another-account");
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        assertEquals(request, draft.attachmentRequest);
        assertNull(draft.attachment);
        assertNotNull(owner.result("another-request", account));
        owner.complete(request, account, "intended.png", PmAttachmentEvent.Outcome.READY, account);
        assertEquals("intended.png", draft.attachment);
        assertTrue(button(R.id.pm_reply_send).isEnabled());
    }

    /** Dismissing releases the observer; reopening the same draft can still apply a later retained completion. */
    @Test
    public void closedFallbackUnsubscribesAndReopenedDraftConsumesBufferedCompletion() {
        openFallback();
        String request = pickImage();
        owner.beginUpload(request, account);
        assertTrue(owner.changes().hasObservers());
        composer.dismiss();
        assertFalse(owner.changes().hasObservers());
        owner.complete(request, account, "completed-after-close.png", PmAttachmentEvent.Outcome.READY, account);
        assertNull(draft.attachment);
        assertNotNull(owner.result(request, account));
        openFallback();
        assertEquals("completed-after-close.png", draft.attachment);
        assertTrue(button(R.id.pm_reply_send).isEnabled());
        assertNull(owner.result(request, account));
    }

    /** Releasing obsolete views cannot keep an activity-wide observer or consume a later result invisibly. */
    @Test
    public void releasingFallbackForRecreationDetachesBufferedResultObserver() {
        openFallback();
        String request = pickImage();
        owner.beginUpload(request, account);
        composer.releaseForRecreation();
        assertFalse(owner.changes().hasObservers());
        owner.complete(request, account, "after-recreation.png", PmAttachmentEvent.Outcome.READY, account);
        assertNull(draft.attachment);
        assertNotNull(owner.result(request, account));
    }

    /** A retained send acknowledgement that immediately closes the sheet must not install a new observer afterward. */
    @Test
    public void acknowledgedDraftDoesNotSubscribeAfterSynchronousDismissal() {
        draft.acknowledged = true;
        openFallback();
        assertFalse(composer.dialogForFragment().isShowing());
        assertFalse(owner.changes().hasObservers());
    }

    /** An account switch rejects a private old-account completion and closes the live fallback sheet. */
    @Test
    public void changedAccountCannotReceiveFallbackAttachment() {
        openFallback();
        String request = pickImage();
        owner.beginUpload(request, account);
        AppController.getInstance().getSharedPreferences().edit()
                .putString("dvc_login", "another-user").putInt("user_id", 99).commit();
        assertEquals(99, AppController.getInstance().isUserId());
        assertFalse(account.equals(PmDeletionQueue.currentAccountKey()));
        owner.complete(request, account, "private-old-account.png",
                PmAttachmentEvent.Outcome.READY, PmDeletionQueue.currentAccountKey());
        assertNull(draft.attachment);
        assertFalse(composer.dialogForFragment().isShowing());
        assertFalse(owner.changes().hasObservers());
    }

    /** Follows the adapter's exact fallback condition and displays its ordinary themed dialog. */
    private void openFallback() {
        FeedPm feed = new FeedPm();
        feed.setId(42);
        feed.setTitle("Original message");
        assertFalse(PmComposerFragment.open(activity.get(), feed, false, draft));
        composer = new PmMessageDialog(activity.get(), feed, false, draft,
                () -> { }, () -> { }, new PickerOperations(), 14);
        composer.show();
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Clicks the real attach button and verifies the form waits for its reserved request. */
    private String pickImage() {
        button(R.id.pm_attach_button).performClick();
        String request = draft.attachmentRequest;
        assertNotNull(request);
        assertFalse(button(R.id.pm_reply_send).isEnabled());
        assertFalse(button(R.id.pm_attach_button).isEnabled());
        return request;
    }

    /** Reads actual button state from the fallback window rather than inspecting only retained models. */
    private Button button(int id) { return composer.dialogForFragment().findViewById(id); }

    /** Uses production picker routing without launching external UI or HTTP in regression tests. */
    private final class PickerOperations implements PmMessageDialog.Operations {
        /** Uses the current verified account so switching accounts exercises the real guard. */
        @Override public String currentAccountKey() { return PmDeletionQueue.currentAccountKey(); }

        /** Leaves sends unused; these tests cover picker results and actual control recovery. */
        @Override public void send(FeedPm feed, boolean member, String text, String attachment,
                                   NetworkUtils.PmOperationCallback callback) { }

        /** Prevents deletion work from entering an attachment-only test fixture. */
        @Override public void enqueueDeletion(FeedPm feed, Runnable onAccepted) { }

        /** Prevents outgoing recipient requests in received-message fixtures. */
        @Override public PmRecipientResolver.Handle resolveRecipient(String name, String account,
                                                                    PmRecipientResolver.Callback callback) {
            return null;
        }

        /** Reserves the same correlated retained record that MainActivity creates before launching its picker. */
        @Override public boolean pickImage(String requestId, String accountKey) {
            return pickerAvailable && owner.beginPicker(requestId, accountKey);
        }
    }

    /** Provides a Material-themed lifecycle owner with a real FragmentManager and SavedStateHandle. */
    public static class TestActivity extends FragmentActivity {
        /** Applies the application theme before creating any dialog views. */
        @Override protected void onCreate(Bundle savedInstanceState) {
            setTheme(R.style.AppTheme);
            super.onCreate(savedInstanceState);
        }
    }

    /** Enables existing account accessors without starting Firebase, Room or WorkManager. */
    public static class TestApp extends AppController {
        /** Initializes account access and clears the static preference cache between Robolectric application instances. */
        @Override public void onCreate() {
            ReflectionHelpers.setStaticField(AppController.class, "sharedPrefs", null);
            ReflectionHelpers.setStaticField(AppController.class, "sInstance", this);
        }
    }
}

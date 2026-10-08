package com.dimonvideo.client.adater;

import android.os.Bundle;
import android.os.Parcel;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.pm.PmRecipientResolver;
import com.dimonvideo.client.util.pm.PmAttachmentEvent;
import com.dimonvideo.client.util.pm.PmAttachmentOwner;
import com.dimonvideo.client.util.pm.PmDeletionQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Runs real fragment/window recreation rather than relying on an attachment event having live subscribers. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = PmComposerFragmentTest.TestApp.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class PmComposerFragmentTest {
    private ActivityController<TestActivity> activity;
    private PmAttachmentOwner owner;
    private String account;

    /** Opens a verified account with no database, Firebase or background application startup. */
    @Before
    public void setUp() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication()).edit()
                .clear().putString("dvc_login", "original-user").putInt("user_id", 12)
                .putInt("auth_state", 1).commit();
        account = PmDeletionQueue.currentAccountKey();
        activity = Robolectric.buildActivity(TestActivity.class).setup();
        owner = new ViewModelProvider(activity.get()).get(PmAttachmentOwner.class);
    }

    /** Removes the retained sheet and releases renderer workers and fragment listeners after every case. */
    @After
    public void tearDown() {
        PmComposerFragment fragment = fragment();
        if (fragment != null) {
            fragment.dismissAllowingStateLoss();
            activity.get().getSupportFragmentManager().executePendingTransactions();
        }
        activity.pause().stop().destroy();
        ShadowLooper.shadowMainLooper().idle();
    }

    /** A picker request, full body, recipient and freshly edited reply all survive actual activity replacement. */
    @Test
    public void recreationDuringPickerRestoresCompleteMessageAndAppliesItsFile() {
        openPending(false);
        input().setText("typed while picker was pending");
        activity.recreate();
        ShadowLooper.shadowMainLooper().idle();
        assertSame(owner, new ViewModelProvider(activity.get()).get(PmAttachmentOwner.class));
        assertEquals("typed while picker was pending", input().getText().toString());
        assertEquals("Original message subject", text(R.id.pm_detail_title));
        assertEquals("Original sender · 8 October", text(R.id.pm_detail_sender));
        PmComposerFragment.State state = new ViewModelProvider(fragment()).get(PmComposerFragment.State.class);
        assertEquals("<b>Complete original body</b>", state.feed.getFullHtml());
        assertEquals("original-recipient", state.feed.getRecipientName());
        assertEquals(42, state.feed.getId());
        assertFalse(send().isEnabled());
        owner.beginUpload("original-request", account);
        owner.complete("original-request", account, "picker-file.png", PmAttachmentEvent.Outcome.READY, account);
        assertEquals("picker-file.png", state.draft.attachment);
        assertNull(state.draft.attachmentRequest);
        assertTrue(send().isEnabled());
        assertNull(owner.result("original-request", account));
    }

    /** Completion while the host is stopped is retained until the recreated member composer can consume it. */
    @Test
    public void uploadCompletingWithoutActiveSheetObserverIsDeliveredAfterRecreation() {
        openPending(true);
        input().setText("member draft after opening");
        owner.beginUpload("original-request", account);
        activity.pause().stop();
        owner.complete("original-request", account, "completed-while-stopped.png",
                PmAttachmentEvent.Outcome.READY, account);
        assertEquals("completed-while-stopped.png", owner.result("original-request", account).filename);
        activity.recreate();
        activity.start().resume();
        ShadowLooper.shadowMainLooper().idle();
        PmComposerFragment.State state = new ViewModelProvider(fragment()).get(PmComposerFragment.State.class);
        assertTrue(state.member);
        assertEquals("member draft after opening", input().getText().toString());
        assertEquals(42, state.feed.getId());
        assertEquals("completed-while-stopped.png", state.draft.attachment);
        assertTrue(send().isEnabled());
        assertNull(owner.result("original-request", account));
    }

    /** An upload still active during recreation remains pending and a wrong request cannot release its controls. */
    @Test
    public void recreationDuringUploadKeepsPendingRequestUntilCorrelatedCompletion() {
        openPending(true);
        owner.beginUpload("original-request", account);
        activity.recreate();
        ShadowLooper.shadowMainLooper().idle();
        assertFalse(send().isEnabled());
        owner.complete("different-request", account, "wrong-file.png", PmAttachmentEvent.Outcome.READY, account);
        assertFalse(send().isEnabled());
        owner.complete("original-request", account, null, PmAttachmentEvent.Outcome.FAILED, account);
        assertTrue(send().isEnabled());
        assertEquals("Initial draft", input().getText().toString());
    }

    /** Android can restore a picker routing record and all text after process death without a live old object. */
    @Test
    public void serializedProcessRestoreKeepsPickerAndDraftWithoutOldViewModel() {
        openPending(true);
        input().setText("process-restored draft");
        Bundle saved = saveAsParcel();
        activity.pause().stop().destroy();
        activity = Robolectric.buildActivity(TestActivity.class).setup(saved);
        ShadowLooper.shadowMainLooper().idle();
        PmAttachmentOwner restored = new ViewModelProvider(activity.get()).get(PmAttachmentOwner.class);
        assertNotSame(owner, restored);
        assertEquals("original-request", restored.pendingPicker().getString("request"));
        assertEquals("process-restored draft", input().getText().toString());
        assertFalse(send().isEnabled());
        restored.beginUpload("original-request", account);
        restored.complete("original-request", account, "restored-picker.png", PmAttachmentEvent.Outcome.READY, account);
        assertTrue(send().isEnabled());
        assertEquals("restored-picker.png", new ViewModelProvider(fragment())
                .get(PmComposerFragment.State.class).draft.attachment);
    }

    /** A process-death upload is not retransmitted; restoring retains text and permits an explicit new attachment. */
    @Test
    public void processDeathDuringUploadUnblocksDraftWithoutReplayingNetworkOperation() {
        openPending(true);
        owner.beginUpload("original-request", account);
        Bundle saved = saveAsParcel();
        activity.pause().stop().destroy();
        activity = Robolectric.buildActivity(TestActivity.class).setup(saved);
        ShadowLooper.shadowMainLooper().idle();
        assertTrue(send().isEnabled());
        assertEquals("Initial draft", input().getText().toString());
        assertNull(new ViewModelProvider(fragment()).get(PmComposerFragment.State.class).draft.attachmentRequest);
    }

    /** Closing after rotation and reopening through a new adapter draft must keep the restored text and file. */
    @Test
    public void recreatedComposerReopensWithAttachmentAndTextFromActivityDraftStore() {
        openPending(true);
        input().setText("keep after closing the restored sheet");
        activity.recreate();
        ShadowLooper.shadowMainLooper().idle();
        owner.complete("original-request", account, "retained-after-close.png",
                PmAttachmentEvent.Outcome.READY, account);
        fragment().dismissNow();
        FeedPm sameRecipient = new FeedPm();
        sameRecipient.setId(42);
        sameRecipient.setTitle("Original recipient");
        assertTrue(PmComposerFragment.open(activity.get(), sameRecipient, true, new PmMessageDialog.Draft()));
        ShadowLooper.shadowMainLooper().idle();
        assertEquals("keep after closing the restored sheet", input().getText().toString());
        assertEquals("retained-after-close.png", new ViewModelProvider(fragment())
                .get(PmComposerFragment.State.class).draft.attachment);
    }

    /** A restored sheet cannot retransmit an outstanding send, and its acknowledgement closes the new window. */
    @Test
    public void recreationDuringSendBlocksDuplicateAndAcknowledgementReachesRestoredSheet() {
        FakeOperations operations = openSendableOutgoing();
        send().performClick();
        assertEquals(1, operations.sends);
        activity.recreate();
        ShadowLooper.shadowMainLooper().idle();
        assertFalse(send().isEnabled());
        send().performClick();
        assertEquals(1, operations.sends);
        operations.sendCallback.onError();
        assertTrue(send().isEnabled());
        send().performClick();
        operations.sendCallback.onSuccess();
        ShadowLooper.shadowMainLooper().idle();
        activity.get().getSupportFragmentManager().executePendingTransactions();
        assertEquals(2, operations.sends);
        assertNull(accountDiagnostic(), fragment());
    }

    /** A late acknowledgement queues deletion and reports its acceptance through the new activity's result channel. */
    @Test
    public void outgoingSendAndDeleteAfterRotationKeepsRecipientAndReachesRecreatedMailbox() {
        FakeOperations operations = openSendableOutgoing();
        ((Button) fragment().requireDialog().findViewById(R.id.pm_reply_send_delete)).performClick();
        assertEquals(0, operations.target.messageId);
        assertEquals(88, operations.target.userId);
        activity.recreate();
        ShadowLooper.shadowMainLooper().idle();
        final int[] acceptedMessage = {0};
        activity.get().getSupportFragmentManager().setFragmentResultListener(
                PmComposerFragment.deletionResultKey(1), activity.get(),
                (key, result) -> acceptedMessage[0] = result.getInt("message"));
        operations.sendCallback.onSuccess();
        ShadowLooper.shadowMainLooper().idle();
        activity.get().getSupportFragmentManager().executePendingTransactions();
        assertNull(fragment());
        assertEquals(0, acceptedMessage[0]);
        operations.deletionAccepted.run();
        assertEquals(42, acceptedMessage[0]);
    }

    /** Late old-account callbacks cannot expose the original body or attach a private file after logout. */
    @Test
    public void accountSwitchDuringRecreationRejectsOriginalSheetAndLateUpload() {
        openPending(true);
        owner.beginUpload("original-request", account);
        AppController.getInstance().getSharedPreferences().edit()
                .putString("dvc_login", "another-user").putInt("user_id", 99).commit();
        assertEquals(99, AppController.getInstance().isUserId());
        assertFalse(account.equals(PmDeletionQueue.currentAccountKey()));
        activity.recreate();
        ShadowLooper.shadowMainLooper().idle();
        activity.get().getSupportFragmentManager().executePendingTransactions();
        assertNull(accountDiagnostic(), fragment());
        owner.complete("original-request", account, "private-old-account.png",
                PmAttachmentEvent.Outcome.READY, PmDeletionQueue.currentAccountKey());
        PmAttachmentEvent result = owner.result("original-request", account);
        assertEquals(PmAttachmentEvent.Outcome.CANCELED, result.outcome);
        assertNull(result.filename);
    }

    /** Reopening a capacity-evicted request releases its waiting controls without erasing the saved draft. */
    @Test
    public void reopeningEvictedAttachmentKeepsDraftAndAllowsSendingAgain() {
        openPending(true);
        input().setText("Retain this unsent message");
        PmComposerFragment.State state = new ViewModelProvider(fragment()).get(PmComposerFragment.State.class);
        state.draft.attachment = "previous-file.png";
        owner.beginUpload("original-request", account);
        fragment().dismissNow();
        owner.complete("original-request", account, "abandoned-file.png", PmAttachmentEvent.Outcome.READY, account);
        for (int index = 0; index < 31; index++) {
            String request = "other-message-" + index;
            assertTrue(owner.beginPicker(request, account));
            owner.beginUpload(request, account);
            owner.complete(request, account, index + ".png", PmAttachmentEvent.Outcome.READY, account);
        }
        assertTrue(owner.beginPicker("new-message", account));
        assertNull(owner.result("original-request", account));
        FeedPm recipient = new FeedPm();
        recipient.setId(42);
        recipient.setTitle("Original recipient");
        assertTrue(PmComposerFragment.open(activity.get(), recipient, true, new PmMessageDialog.Draft()));
        ShadowLooper.shadowMainLooper().idle();
        PmComposerFragment.State restored = new ViewModelProvider(fragment()).get(PmComposerFragment.State.class);
        assertEquals("Retain this unsent message", input().getText().toString());
        assertEquals("previous-file.png", restored.draft.attachment);
        assertNull(restored.draft.attachmentRequest);
        assertTrue(send().isEnabled());
    }

    /** Starts a real lifecycle-owned sheet with an outstanding picker, without invoking external UI or HTTP. */
    private void openPending(boolean member) {
        FeedPm feed = new FeedPm();
        feed.setId(42);
        feed.setTitle("Original message subject");
        feed.setLast_poster_name("Original sender");
        feed.setDate("8 October");
        feed.setFullHtml("<b>Complete original body</b>");
        feed.setRecipientName("original-recipient");
        feed.setSourceFolder(0);
        PmMessageDialog.Draft draft = new PmMessageDialog.Draft();
        draft.text = "Initial draft";
        draft.attachmentRequest = "original-request";
        owner.beginPicker("original-request", account);
        assertTrue(PmComposerFragment.open(activity.get(), feed, member, draft));
        ShadowLooper.shadowMainLooper().idle();
    }

    /** Opens an outgoing composer with a controlled recipient lookup and acknowledged-operation boundaries. */
    private FakeOperations openSendableOutgoing() {
        FeedPm feed = new FeedPm();
        feed.setId(42);
        feed.setTitle("Outgoing message");
        feed.setOutgoing(true);
        feed.setRecipientName("Original recipient");
        feed.setSourceFolder(1);
        PmMessageDialog.Draft draft = new PmMessageDialog.Draft();
        draft.text = "Send to original recipient";
        FakeOperations operations = new FakeOperations();
        assertTrue(PmComposerFragment.open(activity.get(), feed, false, draft, operations));
        ShadowLooper.shadowMainLooper().idle();
        return operations;
    }

    /** Round-trips Android saved state through a Parcel so no retained draft instance can conceal missing fields. */
    private Bundle saveAsParcel() {
        Bundle saved = new Bundle();
        activity.saveInstanceState(saved);
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(saved);
            parcel.setDataPosition(0);
            return parcel.readBundle(TestActivity.class.getClassLoader());
        } finally {
            parcel.recycle();
        }
    }

    /** Finds the current sheet through FragmentManager, including after actual activity recreation. */
    private PmComposerFragment fragment() {
        return (PmComposerFragment) activity.get().getSupportFragmentManager().findFragmentByTag(PmComposerFragment.TAG);
    }

    /** Resolves the actual restored input view. */
    private EditText input() { return fragment().requireDialog().findViewById(R.id.pm_reply_input); }

    /** Resolves the actual send control to verify waiting and recovery states. */
    private Button send() { return fragment().requireDialog().findViewById(R.id.pm_reply_send); }

    /** Reads displayed title/metadata without depending on a saved model-only assertion. */
    private String text(int id) { return ((TextView) fragment().requireDialog().findViewById(id)).getText().toString(); }

    /** Describes lifecycle/account visibility when a rejected sheet unexpectedly remains in FragmentManager. */
    private String accountDiagnostic() {
        PmComposerFragment fragment = fragment();
        if (fragment == null) return "Composer removed";
        PmComposerFragment.State state = new ViewModelProvider(fragment).get(PmComposerFragment.State.class);
        return "added=" + fragment.isAdded() + ", removing=" + fragment.isRemoving()
                + ", lifecycle=" + fragment.getLifecycle().getCurrentState()
                + ", dialogShowing=" + (fragment.getDialog() != null && fragment.getDialog().isShowing())
                + ", sameAccount=" + java.util.Objects.equals(state.account, PmDeletionQueue.currentAccountKey())
                + ", uid=" + AppController.getInstance().isUserId();
    }

    /** Uses the app theme for both the first host and the new instance created by Robolectric. */
    public static class TestActivity extends FragmentActivity {
        /** Applies the Material theme before a restored dialog is inflated. */
        @Override protected void onCreate(Bundle savedInstanceState) {
            setTheme(R.style.AppTheme);
            super.onCreate(savedInstanceState);
        }
    }

    /** Supplies existing preference-backed account access without starting production application services. */
    public static class TestApp extends AppController {
        /** Initializes only the singleton needed by the existing account identity accessor. */
        @Override public void onCreate() { ReflectionHelpers.setStaticField(AppController.class, "sInstance", this); }
    }

    /** Leaves mutations pending so tests can rotate before a real send/deletion boundary is acknowledged. */
    private static final class FakeOperations implements PmMessageDialog.Operations {
        int sends;
        PmSendTarget target;
        NetworkUtils.PmOperationCallback sendCallback;
        Runnable deletionAccepted;

        /** Uses the same verified identity as the production wrapper's account checks. */
        @Override public String currentAccountKey() { return PmDeletionQueue.currentAccountKey(); }

        /** Records the resolved target and retains the single network acknowledgement callback. */
        @Override public void send(FeedPm feed, boolean member, String text, String attachment,
                                   NetworkUtils.PmOperationCallback callback) {
            sends++;
            target = PmSendTarget.from(feed, member);
            sendCallback = callback;
        }

        /** Leaves row removal pending after send success until durable storage is accepted. */
        @Override public void enqueueDeletion(FeedPm feed, Runnable onAccepted) { deletionAccepted = onAccepted; }

        /** Resolves the original recipient without contacting the website. */
        @Override public PmRecipientResolver.Handle resolveRecipient(String name, String account,
                                                                    PmRecipientResolver.Callback callback) {
            callback.onResolved(88);
            return null;
        }

        /** Keeps this send-only fixture from launching the device picker. */
        @Override public boolean pickImage(String requestId, String accountKey) { return false; }
    }
}

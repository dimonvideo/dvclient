package com.dimonvideo.client.adater;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.AsyncHtmlRenderer;
import com.dimonvideo.client.util.BBCodes;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.pm.PmAttachmentEvent;
import com.dimonvideo.client.util.pm.PmDeletionQueue;
import com.dimonvideo.client.util.pm.PmRecipientResolver;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;
import java.util.UUID;

/** Full message view and composer whose draft belongs to a stable message/member ID and account. */
public final class PmMessageDialog {
    private final BottomSheetDialog dialog;
    private final AsyncHtmlRenderer renderer = new AsyncHtmlRenderer();
    private final Context context;
    private final FeedPm feed;
    private final boolean member;
    private final Draft draft;
    private final Operations operations;
    private final String accountKey;
    private final Runnable onSentAndDeleted, onDismiss;
    private final EditText input;
    private final TextView attachmentStatus, recipientStatus;
    private final Button send, sendAndDelete, attach, close, recipientRetry;
    private PmRecipientResolver.Handle recipientLookup;
    private boolean resolvingRecipient;
    private String attachmentRequest;
    private boolean awaitingAttachment;
    private boolean sending;
    private boolean sent;
    private boolean released;
    private final Observer<Integer> draftObserver = ignored -> refreshDraftState();

    /** Stores a view-free draft so rows can be recycled without mixing recipients. */
    static final class Draft {
        String text = "";
        String attachment;
        String attachmentRequest;
        boolean sending;
        boolean acknowledged;
        int resolvedRecipientId;
        final MutableLiveData<Integer> updates = new MutableLiveData<>(0);

        /** Notifies the currently attached sheet after a retained send changes shared draft state. */
        void changed() { updates.setValue(updates.getValue() + 1); }
    }

    /** Separates UI state from network/picker side effects so account and failure paths can be tested. */
    interface Operations {
        /** Returns the current non-secret identity, or null after logout. */
        String currentAccountKey();
        /** Starts one send and reports an acknowledged API result without automatic retransmission. */
        void send(FeedPm feed, boolean member, String text, String attachment,
                  NetworkUtils.PmOperationCallback callback);
        /** Enqueues deletion only after send acknowledgement, then accepts row removal after durable storage. */
        void enqueueDeletion(FeedPm feed, Runnable onAccepted);
        /** Resolves an outgoing username to an exact positive UID without guessing from partial results. */
        PmRecipientResolver.Handle resolveRecipient(String name, String account,
                                                    PmRecipientResolver.Callback callback);
        /** Starts an explicitly PM-routed picker; false indicates it could not be launched. */
        boolean pickImage(String requestId, String accountKey);
    }

    /** Builds an account-bound detail sheet using the application's send and picker operations. */
    PmMessageDialog(Context context, FeedPm feed, boolean member, Draft draft,
                    Runnable onSentAndDeleted, Runnable onDismiss) {
        this(context, feed, member, draft, onSentAndDeleted, onDismiss,
                new RuntimeOperations(context), PmRowViewHolder.fontSizeBase());
    }

    /** Builds a themed, content-sized sheet with a single input label and testable network boundaries. */
    PmMessageDialog(Context context, FeedPm feed, boolean member, Draft draft,
                    Runnable onSentAndDeleted, Runnable onDismiss, Operations operations, float fontSize) {
        this.context = context;
        this.feed = new FeedPm(feed);
        if (draft.resolvedRecipientId > 0) this.feed.setRecipientId(draft.resolvedRecipientId);
        this.member = member;
        this.draft = draft;
        sending = draft.sending;
        attachmentRequest = draft.attachmentRequest;
        awaitingAttachment = attachmentRequest != null;
        this.operations = operations;
        accountKey = operations.currentAccountKey();
        this.onSentAndDeleted = onSentAndDeleted;
        this.onDismiss = onDismiss;
        dialog = new BottomSheetDialog(context, R.style.ThemeOverlay_DVClient_PmDialog);
        Context themedContext = dialog.getContext();
        View content = LayoutInflater.from(themedContext).inflate(
                R.layout.dialog_pm_detail, new FrameLayout(themedContext), false);
        dialog.setContentView(content);
        TextView title = content.findViewById(R.id.pm_detail_title);
        TextView sender = content.findViewById(R.id.pm_detail_sender);
        TextView body = content.findViewById(R.id.pm_detail_body);
        title.setText(member ? context.getString(R.string.pm_write_to, feed.getTitle()) : feed.getTitle());
        title.setTextSize(fontSize + 6);
        String metadata = joinMetadata(feed.getLast_poster_name(), feed.getDate());
        sender.setText(metadata);
        sender.setVisibility(metadata.isEmpty() ? View.GONE : View.VISIBLE);
        body.setTextSize(fontSize + 3);
        body.setVisibility(feed.getFullHtml() == null || feed.getFullHtml().isEmpty()
                ? View.GONE : View.VISIBLE);
        renderer.bind(body, feed.getFullHtml(), true);
        body.setMovementMethod(new TextViewClickMovement() {
            /** Opens links with the same user preferences as other private-message screens. */
            @Override
            public void onLinkClick(String url) {
                AppController controller = AppController.getInstance();
                OpenUrl.open_url(url, controller.isOpenLinks(), controller.isVuploaderPlayListtext(),
                        context, "pm");
            }
        });
        input = content.findViewById(R.id.pm_reply_input);
        TextInputLayout replyLayout = content.findViewById(R.id.pm_reply_layout);
        replyLayout.setHint(themedContext.getString(member
                ? R.string.pm_message_text : R.string.pm_reply_text));
        input.setText(draft.text);
        attachmentStatus = content.findViewById(R.id.pm_attachment_status);
        attachmentStatus.setVisibility(draft.attachment == null ? View.GONE : View.VISIBLE);
        send = content.findViewById(R.id.pm_reply_send);
        sendAndDelete = content.findViewById(R.id.pm_reply_send_delete);
        attach = content.findViewById(R.id.pm_attach_button);
        close = content.findViewById(R.id.pm_detail_close);
        sendAndDelete.setVisibility(member ? View.GONE : View.VISIBLE);
        recipientStatus = content.findViewById(R.id.pm_recipient_status);
        recipientRetry = content.findViewById(R.id.pm_recipient_retry);
        recipientRetry.setOnClickListener(view -> resolveOutgoingRecipient());
        send.setOnClickListener(view -> send(false));
        sendAndDelete.setOnClickListener(view -> send(true));
        attach.setOnClickListener(view -> pickImage());
        close.setOnClickListener(view -> dismiss());
        dialog.setOnDismissListener(ignored -> release());
        dialog.setOnShowListener(ignored -> {
            dialog.getBehavior().setSkipCollapsed(true);
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        // Cap long messages while allowing short messages and member composers to wrap their content.
        dialog.getBehavior().setMaxHeight(Math.round(
                themedContext.getResources().getDisplayMetrics().heightPixels * 0.9f));
        dialog.getBehavior().setMaxWidth(Math.round(
                640 * themedContext.getResources().getDisplayMetrics().density));
    }

    /** Combines optional author and date labels without displaying null values. */
    private static String joinMetadata(String author, String date) {
        String safeAuthor = author == null ? "" : author;
        String safeDate = date == null ? "" : date;
        if (safeAuthor.isEmpty()) return safeDate;
        return safeDate.isEmpty() ? safeAuthor : safeAuthor + " · " + safeDate;
    }

    /** Opens only for the original account and listens to future, correlated upload results. */
    void show() {
        if (released || !ensureAccount()) return;
        dialog.show();
        startForFragment();
    }

    /** Exposes the themed sheet to DialogFragment, which restores its own lifecycle and window. */
    BottomSheetDialog dialogForFragment() { return dialog; }

    /** Activates a shown/restored sheet without opening a second dialog or replacing its dismiss listener. */
    void startForFragment() {
        if (released || !ensureAccount()) return;
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        draft.updates.removeObserver(draftObserver);
        draft.updates.observeForever(draftObserver);
        Window window = dialog.getWindow();
        if (window != null) window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setSendingEnabled(!sending);
        if (!member && feed.isOutgoing()) resolveOutgoingRecipient();
    }

    /** Dismisses the sheet and cleans up even if it was rejected before its first show. */
    void dismiss() {
        dialog.dismiss();
        release();
    }

    /** Saves an unacknowledged draft and releases EventBus/rendering resources exactly once. */
    private void release() {
        if (released) return;
        releaseForRecreation();
        onDismiss.run();
    }

    /** Copies text before saved-state capture without changing the open or pending-upload intent. */
    void saveDraft() {
        if (!sent) draft.text = input.getText().toString();
    }

    /** Detaches obsolete views/subscriptions while leaving view-free state available to the recreated sheet. */
    void releaseForRecreation() {
        if (released) return;
        released = true;
        saveDraft();
        renderer.release();
        if (recipientLookup != null) recipientLookup.cancel();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        draft.updates.removeObserver(draftObserver);
    }

    /** Rejects actions and callbacks for an account different from the one that opened the sheet. */
    private boolean ensureAccount() {
        if (accountKey != null && accountKey.equals(operations.currentAccountKey())) return true;
        Toast.makeText(context.getApplicationContext(), R.string.unsuccess_auth, Toast.LENGTH_SHORT).show();
        dismiss();
        return false;
    }

    /** Resolves the original outgoing recipient; failed lookup leaves only safe full viewing and retry. */
    private void resolveOutgoingRecipient() {
        if (member || !feed.isOutgoing() || released || sending || resolvingRecipient || !ensureAccount()) return;
        resolvingRecipient = true;
        feed.setRecipientId(0);
        recipientStatus.setText(R.string.pm_recipient_loading);
        recipientStatus.setVisibility(View.VISIBLE);
        recipientRetry.setVisibility(View.GONE);
        setSendingEnabled(true);
        recipientLookup = operations.resolveRecipient(feed.getRecipientName(), accountKey,
                new PmRecipientResolver.Callback() {
                    /** Enables new-message sending only after an exact, positive UID is verified. */
                    @Override public void onResolved(int positiveUid) {
                        if (released || !ensureAccount()) return;
                        resolvingRecipient = false;
                        feed.setRecipientId(positiveUid);
                        draft.resolvedRecipientId = positiveUid;
                        recipientStatus.setVisibility(View.GONE);
                        recipientRetry.setVisibility(View.GONE);
                        setSendingEnabled(true);
                    }
                    /** Preserves full HTML and draft while exposing a user-controlled read-only retry. */
                    @Override public void onUnavailable() {
                        if (released || !ensureAccount()) return;
                        resolvingRecipient = false;
                        recipientStatus.setText(R.string.pm_recipient_unavailable);
                        recipientRetry.setVisibility(View.VISIBLE);
                        setSendingEnabled(true);
                    }
                });
    }

    /** Applies only this composer's exact upload result and unblocks sending after cancel/failure. */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAttachmentEvent(PmAttachmentEvent event) {
        if (released || !Objects.equals(attachmentRequest, event.requestId)
                || !Objects.equals(accountKey, event.accountKey) || !ensureAccount()) return;
        attachmentRequest = null;
        draft.attachmentRequest = null;
        awaitingAttachment = false;
        if (event.outcome == PmAttachmentEvent.Outcome.READY && event.filename != null) {
            draft.attachment = event.filename;
            attachmentStatus.setVisibility(View.VISIBLE);
        }
        if (!sending) setSendingEnabled(true);
    }

    /** Starts an account-bound picker whose result cannot be delivered to a different message sheet. */
    private void pickImage() {
        if (sending || awaitingAttachment || !ensureAccount()) return;
        attachmentRequest = UUID.randomUUID().toString();
        draft.attachmentRequest = attachmentRequest;
        awaitingAttachment = true;
        setSendingEnabled(true);
        if (!operations.pickImage(attachmentRequest, accountKey)) {
            attachmentRequest = null;
            draft.attachmentRequest = null;
            awaitingAttachment = false;
            setSendingEnabled(true);
        }
    }

    /** Sends once after uploads finish and keeps the draft/sheet on transport or server rejection. */
    private void send(boolean deleteAfterSend) {
        if (sending || awaitingAttachment || resolvingRecipient || !ensureAccount()) return;
        if (!member && feed.isOutgoing() && feed.getRecipientId() <= 0) return;
        String text = input.getText().toString();
        if (text.trim().isEmpty() && draft.attachment == null) {
            input.setError(context.getString(R.string.pm_empty_reply));
            return;
        }
        draft.text = text;
        final String submittedAttachment = draft.attachment;
        sending = true;
        draft.sending = true;
        draft.acknowledged = false;
        draft.changed();
        setSendingEnabled(false);
        operations.send(feed, member, text, submittedAttachment,
                new NetworkUtils.PmOperationCallback() {
                    /** Clears only acknowledged content; a newer attachment remains available to its author. */
                    @Override
                    public void onSuccess() {
                        if (!ensureAccount()) return;
                        draft.text = "";
                        if (Objects.equals(draft.attachment, submittedAttachment)) draft.attachment = null;
                        draft.sending = false;
                        draft.acknowledged = draft.attachment == null || deleteAfterSend;
                        draft.changed();
                        if (deleteAfterSend) {
                            operations.enqueueDeletion(feed, () -> {
                                if (accountKey.equals(operations.currentAccountKey())) onSentAndDeleted.run();
                            });
                        }
                    }

                    /** Restores controls after failure without erasing the original text or attachment. */
                    @Override
                    public void onError() {
                        sending = false;
                        draft.sending = false;
                        draft.changed();
                        if (!released && ensureAccount()) setSendingEnabled(true);
                    }
                });
    }

    /** Keeps a replacement sheet disabled during the original send and applies its eventual acknowledgement. */
    private void refreshDraftState() {
        if (released) return;
        sending = draft.sending;
        if (draft.acknowledged) {
            sent = true;
            dismiss();
        } else {
            if (!sending && draft.text.isEmpty()) input.setText("");
            setSendingEnabled(!sending);
        }
    }

    /** Prevents duplicate sends, omitted pending uploads, and edits during a send request. */
    private void setSendingEnabled(boolean enabled) {
        boolean resolved = member || !feed.isOutgoing() || feed.getRecipientId() > 0;
        send.setEnabled(enabled && !awaitingAttachment && !resolvingRecipient && resolved);
        sendAndDelete.setEnabled(enabled && !awaitingAttachment && !resolvingRecipient && resolved);
        attach.setEnabled(enabled && !awaitingAttachment);
        input.setEnabled(enabled);
        close.setEnabled(enabled);
        recipientRetry.setEnabled(enabled && !resolvingRecipient);
        dialog.setCancelable(enabled);
    }

    /** Binds the UI boundary to existing production APIs without storing credentials in the draft. */
    private static final class RuntimeOperations implements Operations {
        private final Context context;

        /** Keeps the current themed activity only for the lifetime of its owned sheet. */
        RuntimeOperations(Context context) { this.context = context; }

        /** Returns the non-secret identity already used by the deletion queue. */
        @Override public String currentAccountKey() { return PmDeletionQueue.currentAccountKey(); }

        /** Converts attachments to existing BBCode and sends one account-checked API operation. */
        @Override
        public void send(FeedPm feed, boolean member, String text, String attachment,
                         NetworkUtils.PmOperationCallback callback) {
            PmSendTarget target = PmSendTarget.from(feed, member);
            NetworkUtils.sendPm(context, target.messageId, BBCodes.imageCodes(text, attachment, "13"),
                    0, null, target.userId, callback);
        }

        /** Uses the same account-bound durable queue for send-and-delete as for a swipe deletion. */
        @Override public void enqueueDeletion(FeedPm feed, Runnable onAccepted) {
            PmDeletionQueue.enqueue(context, feed.getId(), feed.getSourceFolder(), feed.getSourcePage(),
                    onAccepted, null);
        }

        /** Starts a bounded exact-name search and returns a handle canceled when the sheet closes. */
        @Override
        public PmRecipientResolver.Handle resolveRecipient(String name, String account,
                                                            PmRecipientResolver.Callback callback) {
            return PmRecipientResolver.resolve(context, name, account, callback);
        }

        /** Resolves the owning MainActivity without changing global sticky navigation/search state. */
        @Override
        public boolean pickImage(String requestId, String accountKey) {
            Context owner = context;
            while (owner instanceof ContextWrapper && !(owner instanceof MainActivity)) {
                Context next = ((ContextWrapper) owner).getBaseContext();
                if (next == owner) break;
                owner = next;
            }
            return owner instanceof MainActivity
                    && ((MainActivity) owner).launchPmImagePicker(requestId, accountKey);
        }
    }
}

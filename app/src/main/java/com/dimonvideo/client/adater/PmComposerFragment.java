package com.dimonvideo.client.adater;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.MutableLiveData;

import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.pm.PmAttachmentEvent;
import com.dimonvideo.client.util.pm.PmAttachmentOwner;
import com.dimonvideo.client.util.pm.PmDeletionQueue;

import java.util.Objects;
import java.lang.ref.WeakReference;

/** Restorable full-message sheet whose draft and uploads survive replacement of the activity and rows. */
public class PmComposerFragment extends DialogFragment {
    public static final String TAG = "pm-composer";
    private State state;
    private PmMessageDialog.Draft initialDraft;
    private PmMessageDialog composer;
    private PmAttachmentOwner attachments;
    private DraftStore draftStore;
    private PmMessageDialog.Operations initialOperations;
    private SharedPreferences preferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener accountListener = (preferences, key) -> {
        if (state != null && !isCurrentAccount()) dismissAllowingStateLoss();
    };

    /** Allows FragmentManager to reconstruct the sheet after configuration change or process death. */
    public PmComposerFragment() { }

    /** Opens one lifecycle-owned sheet while retaining full message, recipient and unsent content. */
    static boolean open(Context context, FeedPm feed, boolean member, PmMessageDialog.Draft draft) {
        return open(context, feed, member, draft, null);
    }

    /** Allows lifecycle tests to control sends and picker callbacks while using the actual restored window. */
    static boolean open(Context context, FeedPm feed, boolean member, PmMessageDialog.Draft draft,
                        PmMessageDialog.Operations operations) {
        FragmentActivity activity = activity(context);
        if (activity == null || activity.getSupportFragmentManager().isStateSaved()) return false;
        PmComposerFragment previous = (PmComposerFragment) activity.getSupportFragmentManager().findFragmentByTag(TAG);
        if (previous != null) previous.dismissNow();
        PmComposerFragment fragment = new PmComposerFragment();
        fragment.initialDraft = draft;
        fragment.initialOperations = operations;
        Bundle arguments = encode(feed, member, PmDeletionQueue.currentAccountKey(), draft);
        fragment.setArguments(arguments);
        fragment.showNow(activity.getSupportFragmentManager(), TAG);
        return true;
    }

    /** Finds the activity behind themed/wrapped adapter contexts without retaining it in saved state. */
    static FragmentActivity activity(Context context) {
        Context owner = context;
        while (owner instanceof ContextWrapper && !(owner instanceof FragmentActivity)) {
            Context next = ((ContextWrapper) owner).getBaseContext();
            if (next == owner) break;
            owner = next;
        }
        return owner instanceof FragmentActivity ? (FragmentActivity) owner : null;
    }

    /** Names an independent result channel for each mailbox, including simultaneously attached tabs. */
    static String deletionResultKey(int folder) { return "pm-composer-deleted-" + folder; }

    /** Obtains view-free retained data and reconnects to the activity's buffered attachment owner. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = new ViewModelProvider(this).get(State.class);
        if (state.feed == null) decode(savedInstanceState != null ? savedInstanceState : requireArguments(), state);
        draftStore = new ViewModelProvider(requireActivity()).get(DraftStore.class);
        draftStore.bind(requireActivity());
        if (initialDraft != null) {
            state.draft = draftStore.draft(state.account, state.feed.getId(), state.member, initialDraft);
            state.draft.acknowledged = false;
        } else draftStore.remember(state.account, state.feed.getId(), state.member, state.draft);
        if (initialOperations != null) state.operations = initialOperations;
        initialDraft = null;
        initialOperations = null;
        attachments = new ViewModelProvider(requireActivity()).get(PmAttachmentOwner.class);
        attachments.changes().observe(this, ignored -> applyAttachmentResult());
        preferences = AppController.getInstance().getSharedPreferences();
        preferences.registerOnSharedPreferenceChangeListener(accountListener);
    }

    /** Builds the original themed content without exposing a previous account's private message. */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!isCurrentAccount()) return new Dialog(requireContext());
        Runnable onDeleted = draftStore.acceptedCallback(state.account, state.feed.getId(), state.feed.getSourceFolder());
        composer = state.operations == null
                ? new PmMessageDialog(requireContext(), state.feed, state.member, state.draft,
                        onDeleted, this::dismissAllowingStateLoss)
                : new PmMessageDialog(requireContext(), state.feed, state.member, state.draft,
                        onDeleted, this::dismissAllowingStateLoss, state.operations, 14);
        return composer.dialogForFragment();
    }

    /** Activates the restored controls before consuming a buffered picker/upload result. */
    @Override
    public void onStart() {
        super.onStart();
        if (!isCurrentAccount()) {
            dismissAllowingStateLoss();
            return;
        }
        if (composer != null) composer.startForFragment();
        applyAttachmentResult();
    }

    /** Rejects an account switch that occurred while the sheet's host was stopped. */
    @Override
    public void onResume() {
        super.onResume();
        if (!isCurrentAccount()) dismissAllowingStateLoss();
    }

    /** Serializes complete view-free content, including text entered since the sheet was opened. */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (composer != null) composer.saveDraft();
        outState.putAll(encode(state.feed, state.member, state.account, state.draft));
        super.onSaveInstanceState(outState);
    }

    /** Releases old activity views and renderer resources without closing the retained composer intent. */
    @Override
    public void onDestroyView() {
        if (composer != null) composer.releaseForRecreation();
        composer = null;
        super.onDestroyView();
    }

    /** Saves a manually dismissed sheet and cancels its view-only lookup/subscription exactly once. */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (composer != null) composer.releaseForRecreation();
        super.onDismiss(dialog);
    }

    /** Removes the account observer once the lifecycle-owned composer is permanently removed. */
    @Override
    public void onDestroy() {
        if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(accountListener);
        preferences = null;
        super.onDestroy();
    }

    /** Applies a completed file only to its original account and request, then acknowledges consumption. */
    private void applyAttachmentResult() {
        if (composer == null || !isCurrentAccount()) return;
        String request = state.draft.attachmentRequest;
        PmAttachmentEvent result = attachments.result(request, state.account);
        if (result != null) {
            composer.onAttachmentEvent(result);
            if (state.draft.attachmentRequest == null) attachments.consume(request, state.account);
        } else if (request != null && !attachments.isPending(request, state.account)) {
            // A network upload cannot continue after process death; keep text/file and permit a manual retry.
            composer.onAttachmentEvent(new PmAttachmentEvent(request, state.account,
                    null, PmAttachmentEvent.Outcome.FAILED));
        }
    }

    /** Requires the exact verified account that supplied both the original body and recipient. */
    private boolean isCurrentAccount() {
        return state != null && state.account != null
                && Objects.equals(state.account, PmDeletionQueue.currentAccountKey());
    }

    /** Copies full API values and a draft to Android's restorable primitive state without credentials. */
    private static Bundle encode(FeedPm feed, boolean member, String account, PmMessageDialog.Draft draft) {
        Bundle state = new Bundle();
        state.putString("account", account);
        state.putBoolean("member", member);
        state.putInt("id", feed.getId());
        state.putString("title", feed.getTitle());
        state.putString("date", feed.getDate());
        state.putString("author", feed.getLast_poster_name());
        state.putString("body", feed.getFullHtml());
        state.putString("preview", feed.getPreviewHtml());
        state.putString("sender", feed.getSenderName());
        state.putString("recipient", feed.getRecipientName());
        state.putInt("recipient_id", feed.getRecipientId());
        state.putBoolean("outgoing", feed.isOutgoing());
        state.putInt("folder", feed.getSourceFolder());
        state.putInt("page", feed.getSourcePage());
        state.putString("text", draft.text);
        state.putString("attachment", draft.attachment);
        state.putString("request", draft.attachmentRequest);
        return state;
    }

    /** Restores the same full message and recipient rather than depending on a reloaded list position. */
    private static void decode(Bundle source, State state) {
        FeedPm feed = new FeedPm();
        feed.setId(source.getInt("id"));
        feed.setTitle(source.getString("title"));
        feed.setDate(source.getString("date"));
        feed.setLast_poster_name(source.getString("author"));
        feed.setFullHtml(source.getString("body"));
        feed.setPreviewHtml(source.getString("preview"));
        feed.setSenderName(source.getString("sender"));
        feed.setRecipientName(source.getString("recipient"));
        feed.setRecipientId(source.getInt("recipient_id"));
        feed.setOutgoing(source.getBoolean("outgoing"));
        feed.setSourceFolder(source.getInt("folder"));
        feed.setSourcePage(source.getInt("page"));
        state.feed = feed;
        state.member = source.getBoolean("member");
        state.account = source.getString("account");
        state.draft.text = source.getString("text", "");
        state.draft.attachment = source.getString("attachment");
        state.draft.attachmentRequest = source.getString("request");
    }

    /** Keeps only non-view message/draft values alive while FragmentManager replaces the activity. */
    public static final class State extends ViewModel {
        FeedPm feed;
        boolean member;
        String account;
        PmMessageDialog.Draft draft = new PmMessageDialog.Draft();
        PmMessageDialog.Operations operations;

        /** Allows Android's default ViewModel factory to create this view-free retained state. */
        public State() { }
    }

    /** Keeps reopened drafts connected to a recreated adapter without retaining adapters, activities or views. */
    public static final class DraftStore extends ViewModel {
        private final LruCache<String, PmMessageDialog.Draft> drafts = new LruCache<>(32);
        private String account;
        private final MutableLiveData<Bundle> acceptedDeletion = new MutableLiveData<>();
        private WeakReference<FragmentActivity> boundActivity = new WeakReference<>(null);

        /** Allows the activity's default ViewModel factory to construct its bounded transient draft cache. */
        public DraftStore() { }

        /** Connects durable removal to the current activity, with lifecycle cleanup and no strong activity retention. */
        void bind(FragmentActivity activity) {
            if (boundActivity.get() == activity) return;
            boundActivity = new WeakReference<>(activity);
            acceptedDeletion.observe(activity, result -> {
                if (result == null) return;
                if (Objects.equals(result.getString("account"), PmDeletionQueue.currentAccountKey())) {
                    activity.getSupportFragmentManager().setFragmentResult(
                            deletionResultKey(result.getInt("folder")), result);
                }
                acceptedDeletion.setValue(null);
            });
        }

        /** Captures only the durable queue's identity so late send success never calls a detached fragment. */
        Runnable acceptedCallback(String account, int id, int folder) {
            return () -> {
                if (!Objects.equals(account, PmDeletionQueue.currentAccountKey())) return;
                Bundle result = new Bundle();
                result.putString("account", account);
                result.putInt("message", id);
                result.putInt("folder", folder);
                acceptedDeletion.setValue(result);
            };
        }

        /** Returns the current account's retained draft, falling back to the adapter's first unsent content. */
        PmMessageDialog.Draft draft(String account, int id, boolean member, PmMessageDialog.Draft initial) {
            synchronize(account);
            String key = member + ":" + id;
            PmMessageDialog.Draft retained = drafts.get(key);
            if (retained == null) {
                retained = initial;
                drafts.put(key, retained);
            }
            return retained;
        }

        /** Reconnects saved or retained fragment data before any newly created adapter can reopen the same item. */
        void remember(String account, int id, boolean member, PmMessageDialog.Draft draft) {
            synchronize(account);
            drafts.put(member + ":" + id, draft);
        }

        /** Evicts every private draft before accepting content supplied by a different verified account. */
        private void synchronize(String account) {
            if (Objects.equals(this.account, account)) return;
            drafts.evictAll();
            this.account = account;
        }
    }
}

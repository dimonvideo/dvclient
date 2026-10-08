package com.dimonvideo.client.adater;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.AsyncHtmlRenderer;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.pm.PmDeletionQueue;

import java.util.ArrayList;
import java.util.List;

/** Compact PM previews with asynchronous diffs/rendering and an independent full-message composer. */
public class AdapterPm extends RecyclerView.Adapter<AdapterPm.ItemViewHolder> {
    private final Context context;
    private final AsyncListDiffer<FeedPm> differ = new AsyncListDiffer<>(this, PmListSnapshot.DIFF);
    private final AsyncHtmlRenderer renderer = new AsyncHtmlRenderer();
    private final PmAccountState accountState = new PmAccountState(PmDeletionQueue::currentAccountKey);
    private SharedPreferences observedPreferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (preferences, key) -> resetAccountState();
    private List<FeedPm> submittedItems = new ArrayList<>();
    private OnMessageRemovedListener removedListener;
    private PmMessageDialog messageDialog;
    private int deletionSourceFolder;

    /** Receives accepted removals by identity so the pager does not reintroduce a deleted message. */
    public interface OnMessageRemovedListener {
        /** Removes this server message ID from the owning fragment's paging data. */
        void onMessageRemoved(int messageId);
    }

    /** Creates stable-ID rows from copied models; HTML and list comparison run off the UI thread. */
    public AdapterPm(List<FeedPm> jsonFeed, Context context) {
        this.context = context;
        setHasStableIds(true);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        updateData(jsonFeed);
    }

    /** Supplies the API folder for verified lookup of a queued deletion before retrying its mutation. */
    public void setDeletionSourceFolder(int folder) { deletionSourceFolder = folder; }

    /** Connects accepted row removal to the fragment's authoritative list. */
    public void setOnMessageRemovedListener(OnMessageRemovedListener listener) {
        removedListener = listener;
    }

    /** Submits an isolated snapshot, preserving locally acknowledged read status during pagination. */
    public void updateData(List<FeedPm> newList) {
        synchronizeAccount();
        if (accountState.accountKey() == null) return;
        List<FeedPm> snapshot = PmListSnapshot.copy(newList);
        for (FeedPm item : snapshot) {
            if (accountState.isRead(item.getId())) item.setIs_new(0);
        }
        submittedItems = snapshot;
        differ.submitList(snapshot);
    }

    /** Dismisses old-account UI and invalidates snapshots when authentication changes. */
    public void resetAccountState() { synchronizeAccount(); }

    /** Removes private UI/rendering state immediately after the bound account identity changes. */
    private boolean synchronizeAccount() {
        if (!accountState.synchronize()) return false;
        if (messageDialog != null) messageDialog.dismiss();
        submittedItems = new ArrayList<>();
        // Null clears the committed list synchronously; an empty-list diff could leave old-account rows clickable.
        differ.submitList(null);
        renderer.release();
        renderer.activate();
        return true;
    }

    /** Inflates the compact row and installs position-safe listeners only once per holder. */
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemViewHolder holder = new ItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row_pm, parent, false));
        holder.itemView.setOnClickListener(view -> {
            FeedPm feed = boundItem(holder);
            if (feed != null) openMessage(feed);
        });
        holder.itemView.setOnLongClickListener(view -> {
            FeedPm feed = boundItem(holder);
            if (feed == null) return false;
            showActions(feed);
            return true;
        });
        return holder;
    }

    /** Binds metadata and cached/background preview HTML without marking messages read or expanding rows. */
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        if (!accountState.isCurrent(accountState.accountKey())) {
            holder.clearMetadata();
            renderer.clear(holder.textViewText);
            return;
        }
        FeedPm feed = differ.getCurrentList().get(position);
        holder.bind(feed, feed.getIs_new() > 0);
        holder.textViewText.setMovementMethod(null);
        renderer.bind(holder.textViewText, feed.getPreviewHtml(), false);
    }

    /** Resolves the current row rather than a stale position captured before a background diff. */
    private FeedPm boundItem(ItemViewHolder holder) {
        if (synchronizeAccount() || !accountState.isCurrent(accountState.accountKey())) return null;
        int position = holder.getBindingAdapterPosition();
        List<FeedPm> current = differ.getCurrentList();
        return position == RecyclerView.NO_POSITION || position >= current.size()
                ? null : current.get(position);
    }

    /** Opens account-specific full HTML and drafts; sent messages resolve recipients instead of replying to self. */
    private void openMessage(FeedPm feed) {
        if (synchronizeAccount()) return;
        final String account = accountState.accountKey();
        if (!accountState.isCurrent(account)) return;
        if (messageDialog != null) messageDialog.dismiss();
        final PmMessageDialog.Draft draft = accountState.draft(feed.getId());
        messageDialog = new PmMessageDialog(context, feed, false, draft,
                () -> { if (accountState.isCurrent(account)) removeAccepted(feed.getId(), account); },
                () -> {
                    accountState.discardEmptyDraft(account, feed.getId(), draft);
                    messageDialog = null;
                });
        messageDialog.show();
        if (!feed.isOutgoing() && feed.getIs_new() > 0 && accountState.beginRead(account, feed.getId())) {
            NetworkUtils.readPm(context, feed.getId(), new NetworkUtils.PmOperationCallback() {
                /** Records acknowledgement only in the originating account's visible list. */
                @Override public void onSuccess() {
                    if (accountState.finishRead(account, feed.getId(), true)) updateData(submittedItems);
                    else resetAccountState();
                }
                /** Releases only this account's in-flight key after a failed read. */
                @Override public void onError() {
                    accountState.finishRead(account, feed.getId(), false);
                    resetAccountState();
                }
            });
        }
    }

    /** Presents actions using a message snapshot so asynchronous list updates cannot change the target. */
    private void showActions(FeedPm feed) {
        final String account = accountState.accountKey();
        if (!accountState.isCurrent(account)) return;
        CharSequence[] actions = {context.getString(R.string.action_open),
                context.getString(R.string.copy_listtext), context.getString(R.string.pm_delete)};
        new AlertDialog.Builder(context).setTitle(feed.getTitle()).setItems(actions, (dialog, item) -> {
            if (synchronizeAccount() || !accountState.isCurrent(account)) return;
            if (item == 0) {
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(Config.WRITE_URL + "/pm/6/" + feed.getId())));
                } catch (RuntimeException exception) {
                    Log.w("AdapterPm", "Cannot open message in browser", exception);
                }
            } else if (item == 1) {
                copyMessage(feed);
            } else {
                changeFolder(feed.getId(), 0);
            }
        }).show();
    }

    /** Decodes complete HTML for the clipboard off the UI thread without loading inline images. */
    private void copyMessage(FeedPm feed) {
        final String account = accountState.accountKey();
        Context applicationContext = context.getApplicationContext();
        AppController.getInstance().getExecutor().execute(() -> {
            String html = feed.getFullHtml() == null ? "" : feed.getFullHtml();
            String plain = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!accountState.isCurrent(account)) return;
                ClipboardManager clipboard = (ClipboardManager)
                        applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("text", plain));
                Toast.makeText(applicationContext, R.string.success, Toast.LENGTH_SHORT).show();
            });
        });
    }

    /** Returns the currently committed row count used by RecyclerView and the pager. */
    @Override
    public int getItemCount() { return differ.getCurrentList().size(); }

    /** Supplies the server message ID so moves/appends preserve row identity. */
    @Override
    public long getItemId(int position) { return differ.getCurrentList().get(position).getId(); }

    /** Cancels work aimed at a recycled row before that holder is reused for another message. */
    @Override
    public void onViewRecycled(@NonNull ItemViewHolder holder) {
        renderer.clear(holder.textViewText);
        holder.clearAvatar();
        super.onViewRecycled(holder);
    }

    /** Recreates rendering resources when RecyclerView reattaches this adapter. */
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        renderer.activate();
        synchronizeAccount();
        AppController controller = AppController.getInstance();
        if (controller != null) {
            observedPreferences = controller.getSharedPreferences();
            observedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
        }
    }

    /** Saves the composer draft and releases rendering/image resources when the list leaves the screen. */
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (observedPreferences != null) {
            observedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
            observedPreferences = null;
        }
        if (messageDialog != null) messageDialog.dismiss();
        renderer.release();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    /** Enqueues deletion before removing the message locally, preserving it if durable storage fails. */
    public void removeItem(int position) { changeFolderAt(position, 0); }

    /** Restores a message from trash, removing this row only after server acknowledgement. */
    public void restoreItem(int position) { changeFolderAt(position, 1); }

    /** Archives a message, removing this row only after server acknowledgement. */
    public void archiveItem(int position) { changeFolderAt(position, 2); }

    /** Restores an archived message, removing this row only after server acknowledgement. */
    public void restoreFromArchiveItem(int position) { changeFolderAt(position, 3); }

    /** Converts the swipe position to a stable server ID before any asynchronous network/storage work. */
    private void changeFolderAt(int position, int operation) {
        if (synchronizeAccount() || !accountState.isCurrent(accountState.accountKey())) return;
        List<FeedPm> current = differ.getCurrentList();
        if (position >= 0 && position < current.size()) {
            changeFolder(current.get(position).getId(), operation);
        }
    }

    /** Requests a folder change and waits for durable queue acceptance or server acknowledgement. */
    private void changeFolder(int messageId, int operation) {
        if (synchronizeAccount()) return;
        final String account = accountState.accountKey();
        if (!accountState.isCurrent(account)) return;
        int sourcePage = 1;
        for (FeedPm item : submittedItems) {
            if (item.getId() == messageId) {
                sourcePage = item.getSourcePage();
                break;
            }
        }
        NetworkUtils.deletePm(context, messageId, operation, deletionSourceFolder, sourcePage,
                () -> { if (accountState.isCurrent(account)) removeAccepted(messageId, account); });
    }

    /** Removes from the latest submitted snapshot by ID, even if another diff/page arrived meanwhile. */
    private void removeAccepted(int messageId, String account) {
        if (!accountState.isCurrent(account)) return;
        List<FeedPm> remaining = new ArrayList<>(submittedItems.size());
        for (FeedPm item : submittedItems) {
            if (item.getId() != messageId) remaining.add(item);
        }
        submittedItems = remaining;
        differ.submitList(remaining);
        accountState.removeDraft(account, messageId);
        if (removedListener != null) removedListener.onMessageRemoved(messageId);
    }

    /** View-only row state; no reply text, position captures, clipboard managers or API models are retained. */
    public static class ItemViewHolder extends PmRowViewHolder {
        /** Resolves compact row metadata once during holder creation. */
        public ItemViewHolder(@NonNull View itemView) { super(itemView); }
    }
}

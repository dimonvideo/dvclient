/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */
package com.dimonvideo.client.adater;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
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
import com.dimonvideo.client.util.AsyncHtmlRenderer;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.pm.PmDeletionQueue;

import java.util.ArrayList;
import java.util.List;

/** Member previews with background diffs/HTML rendering and a composer keyed by recipient ID. */
public class AdapterPmFriends extends RecyclerView.Adapter<AdapterPmFriends.ViewHolder> {
    private final Context context;
    private final AsyncListDiffer<FeedPm> differ = new AsyncListDiffer<>(this, PmListSnapshot.DIFF);
    private final AsyncHtmlRenderer renderer = new AsyncHtmlRenderer();
    private final PmAccountState accountState = new PmAccountState(PmDeletionQueue::currentAccountKey);
    private SharedPreferences observedPreferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (preferences, key) -> resetAccountState();
    private List<FeedPm> submittedItems = new ArrayList<>();
    private PmMessageDialog messageDialog;

    /** Creates stable-ID rows and copies API values before asynchronous comparison. */
    public AdapterPmFriends(List<FeedPm> jsonFeed, Context context) {
        this.context = context;
        setHasStableIds(true);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        updateData(jsonFeed);
    }

    /** Clears local member suppression after a successful first-page refresh from the server. */
    public void resetHiddenItems() { accountState.resetHidden(); }

    /** Submits a copied list and retains locally removed IDs until an explicit refresh. */
    public void updateData(List<FeedPm> newList) {
        synchronizeAccount();
        if (newList.isEmpty()) accountState.resetHidden();
        List<FeedPm> snapshot = new ArrayList<>(newList.size());
        for (FeedPm item : newList) {
            if (!accountState.isHidden(item.getId())) snapshot.add(new FeedPm(item));
        }
        submittedItems = snapshot;
        differ.submitList(snapshot);
    }

    /** Invalidates visible rows, dialogs and caches when the active account changes. */
    public void resetAccountState() { synchronizeAccount(); }

    /** Clears all UI data tied to the previous identity before a new account can bind rows. */
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

    /** Inflates compact metadata only; click listeners resolve the holder's current item on demand. */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row_pm, parent, false));
        holder.itemView.setOnClickListener(view -> {
            FeedPm member = boundItem(holder);
            if (member != null) openComposer(member);
        });
        holder.itemView.setOnLongClickListener(view -> {
            FeedPm member = boundItem(holder);
            if (member == null) return false;
            showActions(member);
            return true;
        });
        return holder;
    }

    /** Binds HTML rank/action previews through the cache without rendering or image fetches in bind. */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!accountState.isVisibleAccount()) {
            holder.clearMetadata();
            renderer.clear(holder.textViewNames);
            renderer.clear(holder.textViewText);
            return;
        }
        FeedPm feed = differ.getCurrentList().get(position);
        holder.bind(feed, false);
        holder.textViewDate.setText(joinLastSeen(feed));
        holder.textViewTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        holder.textViewText.setMovementMethod(null);
        renderer.bind(holder.textViewNames, feed.getFullHtml(), false);
        renderer.bind(holder.textViewText, feed.getPreviewHtml(), false);
    }

    /** Combines the member's last-visit status and date without allocating formatter/calendar objects. */
    private static String joinLastSeen(FeedPm feed) {
        String status = feed.getLast_poster_name() == null ? "" : feed.getLast_poster_name();
        String date = feed.getDate() == null ? "" : feed.getDate();
        return status.isEmpty() ? date : status + " " + date;
    }

    /** Resolves a valid current binding instead of a position captured before a list update. */
    private FeedPm boundItem(ViewHolder holder) {
        if (synchronizeAccount() || !accountState.isCurrent(accountState.accountKey())) return null;
        int position = holder.getBindingAdapterPosition();
        List<FeedPm> current = differ.getCurrentList();
        return position == RecyclerView.NO_POSITION || position >= current.size()
                ? null : current.get(position);
    }

    /** Opens a composer using an account-qualified recipient draft rather than a globally shared user ID. */
    private void openComposer(FeedPm member) {
        if (synchronizeAccount()) return;
        final String account = accountState.accountKey();
        if (!accountState.isCurrent(account)) return;
        if (messageDialog != null) messageDialog.dismiss();
        final PmMessageDialog.Draft draft = accountState.draft(member.getId());
        messageDialog = new PmMessageDialog(context, member, true, draft, () -> { }, () -> {
            accountState.discardEmptyDraft(account, member.getId(), draft);
            messageDialog = null;
        });
        messageDialog.show();
    }

    /** Keeps profile/friend/ignore actions tied to the selected member ID during asynchronous updates. */
    private void showActions(FeedPm member) {
        final String account = accountState.accountKey();
        if (!accountState.isCurrent(account)) return;
        CharSequence[] actions = {context.getString(R.string.action_open),
                context.getString(R.string.action_like_member), context.getString(R.string.copy_name),
                context.getString(R.string.add_friend), context.getString(R.string.add_ignor),
                context.getString(R.string.remove_all)};
        new AlertDialog.Builder(context).setTitle(member.getTitle()).setItems(actions, (dialog, item) -> {
            if (synchronizeAccount() || !accountState.isCurrent(account)) return;
            try {
                if (item == 0) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.WRITE_URL
                            + "/0/name/" + Uri.encode(member.getTitle() == null ? "" : member.getTitle()))));
                } else if (item == 1) {
                    ButtonsActions.like_member(context, member.getId(), member.getTitle(), 5);
                } else if (item == 2) {
                    ClipboardManager clipboard = (ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) clipboard.setPrimaryClip(
                            ClipData.newPlainText("text", member.getTitle()));
                    Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show();
                } else {
                    ButtonsActions.add_to_fav_user(context, member.getId(), item);
                    removeById(member.getId());
                }
            } catch (RuntimeException exception) {
                Log.w("AdapterPmFriends", "Cannot complete member action", exception);
            }
        }).show();
    }

    /** Removes the latest submitted member snapshot by ID and prevents a subsequent page restoring it. */
    private void removeById(int memberId) {
        accountState.hide(memberId);
        List<FeedPm> remaining = new ArrayList<>(submittedItems.size());
        for (FeedPm item : submittedItems) {
            if (item.getId() != memberId) remaining.add(item);
        }
        submittedItems = remaining;
        differ.submitList(remaining);
    }

    /** Returns the committed number of member rows. */
    @Override
    public int getItemCount() { return differ.getCurrentList().size(); }

    /** Supplies the stable API user ID for RecyclerView row identity. */
    @Override
    public long getItemId(int position) { return differ.getCurrentList().get(position).getId(); }

    /** Returns isolated data so callers cannot mutate a snapshot while a diff is running. */
    public List<FeedPm> getData() { return PmListSnapshot.copy(submittedItems); }

    /** Invalidates pending HTML delivery and clears avatar loading before the row is reused. */
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        renderer.clear(holder.textViewNames);
        renderer.clear(holder.textViewText);
        holder.clearAvatar();
        super.onViewRecycled(holder);
    }

    /** Activates a fresh rendering worker when the adapter is attached or reattached. */
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

    /** Saves any active draft and releases cached/rendering resources when leaving the member list. */
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

    /** Holds only compact member metadata; reply controls are owned by the detail sheet. */
    public static class ViewHolder extends PmRowViewHolder {
        /** Resolves row views once at holder creation. */
        public ViewHolder(@NonNull View itemView) { super(itemView); }
    }
}

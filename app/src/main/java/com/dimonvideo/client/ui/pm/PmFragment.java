/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */
package com.dimonvideo.client.ui.pm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterPm;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.pm.PmDeletionEvent;
import com.dimonvideo.client.util.pm.PmDeletionQueue;
import com.dimonvideo.client.util.pm.PmHttpTransport;
import com.dimonvideo.client.util.pm.PmNotifications;
import com.google.android.material.snackbar.Snackbar;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Displays a continuous mailbox with guarded pagination and durable deletion filtering. */
public class PmFragment extends Fragment {
    private FragmentHomeBinding binding;
    private AdapterPm adapter;
    private LinearLayoutManager layoutManager;
    private ItemTouchHelper touchHelper;
    private final PmPagination pagination = new PmPagination();
    private final LinkedHashMap<Integer, FeedPm> messages = new LinkedHashMap<>();
    private final Set<Integer> hiddenIds = new HashSet<>();
    private final Object requestTag = new Object();
    private int generation;
    private int folder;
    private boolean waitingForQueue;
    private boolean loadFailed;
    private boolean replaceFirstPage;
    private String mailboxAccount;
    private Snackbar retrySnackbar;

    /** Inflates a binding scoped to the fragment's current view. */
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /** Attaches the list and its controls before starting the first request. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        folder = resolveFolder();
        mailboxAccount = PmDeletionQueue.currentAccountKey();
        messages.clear();
        adapter = new AdapterPm(new ArrayList<>(), requireContext());
        adapter.setDeletionSourceFolder(folder);
        adapter.setOnMessageRemovedListener(this::onMessageRemoved);
        layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
        RecyclerView.ItemAnimator animator = binding.recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            /** Prefetches near the end without accumulating delayed scroll callbacks. */
            @Override public void onScrolled(@NonNull RecyclerView list, int dx, int dy) {
                if (binding == null) return;
                boolean showTop = AppController.getInstance().isOnTop()
                        && layoutManager.findFirstVisibleItemPosition() > 3 && dy <= 0;
                binding.fabTop.setVisibility(showTop ? View.VISIBLE : View.GONE);
                maybeLoadMore();
            }
        });
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            /** Rechecks the viewport after asynchronous list replacement. */
            @Override public void onChanged() { onListChanged(); }
            /** Fills the viewport after insertion of a short initial page. */
            @Override public void onItemRangeInserted(int start, int count) { onListChanged(); }
            /** Updates the empty state after accepted removals. */
            @Override public void onItemRangeRemoved(int start, int count) { onListChanged(); }
        });
        binding.fabTop.setOnClickListener(v -> binding.recyclerView.smoothScrollToPosition(0));
        binding.swipeLayout.setOnRefreshListener(this::refresh);
        binding.emptyView.setOnClickListener(v -> retry());
        attachSwipeActions();
        PmNotifications.dismissAll(requireContext());
        refresh();
    }

    /** Resolves the API folder, including the outgoing messages tab. */
    private int resolveFolder() {
        String title = getArguments() == null ? null : getArguments().getString("tab");
        if (getString(R.string.tab_trash).equals(title)) return 5;
        if (getString(R.string.tab_outbox).equals(title)) return 1;
        if (getString(R.string.tab_arhiv).equals(title)) return 2;
        if (getString(R.string.tab_ish).equals(title) || getString(R.string.pm_send).equals(title)) return 3;
        return 0;
    }

    /** Cancels old requests and reads durable pending IDs before refreshing the first page. */
    private void refresh() {
        if (binding == null) return;
        dismissRetry();
        generation++;
        PmHttpTransport.queue(requireContext()).cancelAll(requestTag);
        pagination.reset();
        loadFailed = false;
        waitingForQueue = true;
        replaceFirstPage = true;
        hiddenIds.clear();
        String currentAccount = PmDeletionQueue.currentAccountKey();
        if (!Objects.equals(mailboxAccount, currentAccount)) {
            messages.clear();
            adapter.updateData(new ArrayList<>());
        }
        mailboxAccount = currentAccount;
        if (mailboxAccount == null) {
            waitingForQueue = false;
            finishLoading();
            updateEmptyState();
            binding.emptyView.setText(R.string.unsuccess_auth);
            return;
        }
        final int token = generation;
        binding.progressbar.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        binding.swipeLayout.setRefreshing(!messages.isEmpty());
        updateEmptyState();
        PmDeletionQueue.getPendingIds(requireContext(), pending -> {
            if (binding == null || token != generation) return;
            if (folder != 5) hiddenIds.addAll(pending);
            waitingForQueue = false;
            loadNextPage();
        });
    }

    /** Acquires the loading guard and starts one uncached request for the current page. */
    private void loadNextPage() {
        if (binding == null || waitingForQueue || loadFailed || mailboxAccount == null) return;
        int page = pagination.begin();
        if (page == 0) return;
        final int token = generation;
        AppController controller = AppController.getInstance();
        String url = Config.PM_URL + page + "&pm=" + folder
                + "&login_name=" + encode(controller.userName(""))
                + "&login_password=" + encode(controller.userPassword());
        binding.ProgressBarBottom.setVisibility(messages.isEmpty() ? View.GONE : View.VISIBLE);
        JsonArrayRequest request = new JsonArrayRequest(url,
                response -> acceptPage(token, page, response), error -> failPage(token));
        request.setTag(requestTag);
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
        PmHttpTransport.queue(controller).add(request);
    }

    /** Applies a valid whole page, preserving order and deduplicating rows by message ID. */
    private void acceptPage(int token, int page, JSONArray response) {
        if (!isCurrentResponse(token)) return;
        List<FeedPm> parsed = new ArrayList<>();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject json = response.getJSONObject(i);
                FeedPm message = new FeedPm();
                message.setId(json.getInt(Config.TAG_ID));
                message.setSourcePage(page);
                message.setTitle(json.getString(Config.TAG_TITLE));
                message.setImageUrl(json.optString(Config.TAG_CATEGORY));
                message.setDate(json.getString(Config.TAG_DATE));
                String sender = json.optString(Config.TAG_LAST_POSTER_NAME);
                String recipient = json.optString(Config.TAG_USER);
                boolean outgoing = sender.trim().equalsIgnoreCase(
                        AppController.getInstance().userName("").trim());
                message.setSenderName(sender);
                message.setRecipientName(recipient);
                message.setOutgoing(outgoing);
                message.setSourceFolder(folder);
                message.setLast_poster_name(outgoing ? recipient : sender);
                message.setIs_new(outgoing ? 0 : json.optInt(Config.TAG_HITS));
                message.setFullHtml(json.getString(Config.TAG_TEXT));
                message.setPreviewHtml(json.getString(Config.TAG_FULL_TEXT));
                if (message.getId() > 0 && !hiddenIds.contains(message.getId())) parsed.add(message);
            }
        } catch (JSONException malformed) {
            failPage(token);
            return;
        }
        if (replaceFirstPage) {
            messages.clear();
            replaceFirstPage = false;
        }
        for (FeedPm message : parsed) messages.put(message.getId(), message);
        pagination.complete(response.length());
        finishLoading();
        adapter.updateData(new ArrayList<>(messages.values()));
        onListChanged();
    }

    /** Rejects callbacks from an old refresh, a destroyed view, or a different authenticated mailbox. */
    private boolean isCurrentResponse(int token) {
        if (binding == null || token != generation) return false;
        if (!Objects.equals(mailboxAccount, PmDeletionQueue.currentAccountKey())) {
            refresh();
            return false;
        }
        return true;
    }

    /** Preserves the failed page and current rows, exposing an explicit retry action. */
    private void failPage(int token) {
        if (!isCurrentResponse(token)) return;
        pagination.fail();
        loadFailed = true;
        finishLoading();
        dismissRetry();
        retrySnackbar = Snackbar.make(binding.getRoot(), R.string.pm_load_more_failed, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.pm_retry, v -> retry());
        retrySnackbar.show();
        updateEmptyState();
    }

    /** Retries the same page instead of repeatedly hitting an unavailable network. */
    private void retry() {
        if (binding == null) return;
        dismissRetry();
        loadFailed = false;
        loadNextPage();
    }

    /** Releases retry actions before refresh or view teardown so a Snackbar cannot keep the old view alive. */
    private void dismissRetry() {
        if (retrySnackbar != null) retrySnackbar.dismiss();
        retrySnackbar = null;
    }

    /** Clears both request indicators after success or failure. */
    private void finishLoading() {
        binding.progressbar.setVisibility(View.GONE);
        binding.ProgressBarBottom.setVisibility(View.GONE);
        binding.swipeLayout.setRefreshing(false);
    }

    /** Prefetches three rows before the end and fills an initially unscrollable list. */
    private void maybeLoadMore() {
        if (binding == null || waitingForQueue || pagination.isLoading()
                || pagination.isEndReached() || loadFailed) return;
        int count = adapter.getItemCount();
        if (count == 0 || layoutManager.findLastVisibleItemPosition() >= count - 3) loadNextPage();
    }

    /** Defers viewport checks until RecyclerView finishes dispatching a background diff. */
    private void onListChanged() {
        if (binding == null) return;
        updateEmptyState();
        binding.recyclerView.post(this::maybeLoadMore);
    }

    /** Displays an empty mailbox or an actionable loading error only after the request completes. */
    private void updateEmptyState() {
        if (binding == null) return;
        boolean empty = messages.isEmpty() && !waitingForQueue && !pagination.isLoading();
        binding.linearEmpty.setVisibility(View.GONE);
        binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.emptyView.setText(loadFailed ? R.string.pm_load_more_failed : R.string.no_data_available);
    }

    /** Removes accepted IDs from the source list so later page submissions cannot resurrect them. */
    private void onMessageRemoved(int id) {
        if (binding == null) return;
        messages.remove(id);
        hiddenIds.add(id);
        // This also covers acknowledged archive/restore operations, which shift API offsets.
        pagination.onRemoteRemoval();
        onListChanged();
    }

    /** Reconciles shifted server offsets or re-exposes an expired deletion intent. */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeletionEvent(PmDeletionEvent event) {
        if (binding == null || !event.accountKey.equals(mailboxAccount)) return;
        if (event.outcome == PmDeletionEvent.Outcome.EXPIRED) {
            hiddenIds.remove(event.messageId);
            refresh();
            if (isResumed()) Snackbar.make(binding.getRoot(), R.string.pm_delete_expired, Snackbar.LENGTH_LONG).show();
        } else {
            pagination.onRemoteRemoval();
            if (folder == 5) refresh();
            else {
                // A deletion initiated from a notification did not pass through this adapter.
                messages.remove(event.messageId);
                hiddenIds.add(event.messageId);
                adapter.updateData(new ArrayList<>(messages.values()));
                onListChanged();
            }
        }
    }

    /** Refreshes folders after server-confirmed moves without replaying sticky events on attachment. */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if ("restored".equals(event.action) || "archived".equals(event.action)) refresh();
    }

    /** Adds swipe actions; only durable or server acceptance lets the adapter remove a row. */
    private void attachSwipeActions() {
        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            /** Offers only folder moves supported by the API for the message's actual sender. */
            @Override public int getSwipeDirs(@NonNull RecyclerView list,
                                               @NonNull RecyclerView.ViewHolder holder) {
                if (adapter == null || !Objects.equals(mailboxAccount,
                        PmDeletionQueue.currentAccountKey())) return 0;
                int position = holder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= adapter.getItemCount()) return 0;
                FeedPm message = messages.get((int) adapter.getItemId(position));
                if (message == null) return 0;
                // Own deleted rows cannot be restored by pm.php. Archiving an outgoing row
                // moves it to a recipient-only folder, making it inaccessible to the sender.
                if (message.isOutgoing()) return folder == 5 ? 0 : ItemTouchHelper.LEFT;
                return super.getSwipeDirs(list, holder);
            }
            /** Message rows are not reorderable. */
            @Override public boolean onMove(@NonNull RecyclerView list, @NonNull RecyclerView.ViewHolder source,
                                             @NonNull RecyclerView.ViewHolder target) { return false; }
            /** Disables pull-to-refresh while a swipe gesture is active. */
            @Override public void onSelectedChanged(@Nullable RecyclerView.ViewHolder holder, int state) {
                super.onSelectedChanged(holder, state);
                if (binding != null) binding.swipeLayout.setEnabled(state != ItemTouchHelper.ACTION_STATE_SWIPE);
            }
            /** Draws themed delete and archive backgrounds behind the displaced message. */
            @Override public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView list,
                    @NonNull RecyclerView.ViewHolder holder, float dx, float dy, int state, boolean active) {
                View row = holder.itemView;
                ColorDrawable background = new ColorDrawable(ContextCompat.getColor(list.getContext(),
                        dx < 0 ? R.color.colorDelete : R.color.colorPmSend));
                background.setBounds(dx < 0 ? row.getRight() + (int) dx : row.getLeft(), row.getTop(),
                        dx < 0 ? row.getRight() : row.getLeft() + (int) dx, row.getBottom());
                background.draw(canvas);
                Drawable icon = ContextCompat.getDrawable(list.getContext(), dx < 0
                        ? R.drawable.ic_delete : R.drawable.baseline_inventory_2_white_20);
                if (icon != null && dx != 0) {
                    int margin = (row.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int left = dx < 0 ? row.getRight() - margin - icon.getIntrinsicWidth() : row.getLeft() + margin;
                    int top = row.getTop() + margin;
                    icon.setBounds(left, top, left + icon.getIntrinsicWidth(), top + icon.getIntrinsicHeight());
                    icon.draw(canvas);
                }
                super.onChildDraw(canvas, list, holder, dx, dy, state, active);
            }
            /** Resets the visual swipe while persistence or a server operation is still pending. */
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                int position = holder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || binding == null) return;
                if (!Objects.equals(mailboxAccount, PmDeletionQueue.currentAccountKey())) {
                    refresh();
                    return;
                }
                if (direction == ItemTouchHelper.LEFT) {
                    if (folder == 5) adapter.restoreItem(position);
                    else adapter.removeItem(position);
                } else {
                    if (folder == 2) adapter.restoreFromArchiveItem(position);
                    else adapter.archiveItem(position);
                }
                adapter.notifyItemChanged(position);
            }
        });
        touchHelper.attachToRecyclerView(binding.recyclerView);
    }

    /** Encodes the existing API credentials without logging request URLs or message contents. */
    private static String encode(String value) {
        try { return URLEncoder.encode(value, "UTF-8"); }
        catch (UnsupportedEncodingException impossible) { throw new IllegalStateException(impossible); }
    }

    /** Registers live-view events and reconciles an account changed while the tab was offscreen. */
    @Override public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        if (binding != null && !Objects.equals(PmDeletionQueue.currentAccountKey(), mailboxAccount)) refresh();
    }

    /** Stops receiving foreground events when the fragment is no longer started. */
    @Override public void onStop() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onStop();
    }

    /** Cancels this view's requests and detaches adapters to release HTML targets and dialogs. */
    @Override public void onDestroyView() {
        dismissRetry();
        generation++;
        PmHttpTransport.queue(requireContext()).cancelAll(requestTag);
        if (touchHelper != null) touchHelper.attachToRecyclerView(null);
        if (binding != null) {
            binding.recyclerView.clearOnScrollListeners();
            binding.recyclerView.setAdapter(null);
        }
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        binding = null;
        adapter = null;
        layoutManager = null;
        touchHelper = null;
        super.onDestroyView();
    }
}

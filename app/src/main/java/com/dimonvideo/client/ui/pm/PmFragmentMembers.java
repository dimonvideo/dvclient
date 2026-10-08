/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.ui.pm;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.ParseError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterPmFriends;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.pm.PmDeletionQueue;
import com.dimonvideo.client.util.pm.PmHttpTransport;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PmFragmentMembers extends Fragment {

    private static final int PAGE_SIZE = 10;
    private final List<FeedPm> listFeed = new ArrayList<>();
    private final Set<Integer> loadedIds = new HashSet<>();
    private FragmentHomeBinding binding;
    private AdapterPmFriends adapter;
    private AppController controller;
    private int requestCount = 1;
    private int requestGeneration;
    private boolean loading;
    private boolean endReached;
    private boolean loadFailed;
    private String story;
    private String tabTitle;
    private String accountKey;
    private JsonArrayRequest activeRequest;
    private RecyclerView.OnScrollListener scrollListener;
    private Snackbar retrySnackbar;
    private final Runnable hideTopButton = this::hideTopButton;

    /** Creates the empty fragment required for framework recreation. */
    public PmFragmentMembers() {
    }

    /** Seeds the initial search term without changing a query that is already being paged. */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        Bundle arguments = getArguments();
        if ("13".equals(event.razdel)
                && adapter == null
                && (arguments == null || !arguments.containsKey(Config.TAG_STORY))) {
            story = event.story;
        }
    }

    /** Creates a binding owned only by this fragment's current view. */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /** Sets up the member list and requests its first page once. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        Bundle arguments = getArguments();
        if (arguments != null) {
            story = arguments.getString(Config.TAG_STORY);
            tabTitle = arguments.getString("tab");
        }
        EventBus.getDefault().postSticky(new MessageEvent("13", story, null, null, null, null));

        controller = AppController.getInstance();
        accountKey = PmDeletionQueue.currentAccountKey();
        listFeed.clear();
        loadedIds.clear();
        requestCount = 1;
        endReached = false;
        loadFailed = false;
        adapter = new AdapterPmFriends(listFeed, requireContext());
        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        recyclerView.setAdapter(adapter);
        scrollListener = new RecyclerView.OnScrollListener() {
            /** Maintains one top-button timer and fills the viewport with guarded page loads. */
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (binding == null) return;
                if (dy != 0) recyclerView.removeCallbacks(hideTopButton);
                if (dy > 0) {
                    recyclerView.postDelayed(hideTopButton, 6000);
                } else if (dy < 0) {
                    binding.fabTop.setVisibility(controller.isOnTop() ? View.VISIBLE : View.GONE);
                }
                if (isLastItemDisplaying(recyclerView)) getData();
            }

            /** Loads a further member page only when the list reaches its end. */
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (isLastItemDisplaying(recyclerView)) getData();
            }
        };
        recyclerView.addOnScrollListener(scrollListener);
        binding.fabTop.setOnClickListener(button -> recyclerView.smoothScrollToPosition(0));
        binding.swipeLayout.setOnRefreshListener(this::update);
        getData();
    }

    /** Hides the top button only while its view still exists. */
    private void hideTopButton() {
        if (binding != null) binding.fabTop.setVisibility(View.GONE);
    }

    /** Cancels an older load and refreshes page one without discarding visible rows on failure. */
    private void update() {
        if (binding == null) return;
        reconcileAccount();
        cancelRequest();
        requestCount = 1;
        endReached = false;
        loadFailed = false;
        if (MainActivity.binding != null) {
            MainActivity.binding.appBarMain.fabBadge.setVisibility(View.GONE);
        }
        getData();
    }

    /** Clears private rows and dismisses old-account composers before accepting a new session. */
    private boolean reconcileAccount() {
        if (binding == null) return false;
        String currentAccount = PmDeletionQueue.currentAccountKey();
        if (Objects.equals(accountKey, currentAccount)) return false;
        cancelRequest();
        if (retrySnackbar != null) retrySnackbar.dismiss();
        retrySnackbar = null;
        accountKey = currentAccount;
        listFeed.clear();
        loadedIds.clear();
        requestCount = 1;
        endReached = false;
        loadFailed = false;
        binding.recyclerView.setAdapter(null);
        adapter = new AdapterPmFriends(listFeed, requireContext());
        binding.recyclerView.setAdapter(adapter);
        binding.emptyView.setVisibility(View.GONE);
        return true;
    }

    /** Encodes a query value once while retaining the original navigation search term. */
    private String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is unavailable", exception);
        }
    }

    /** Returns whether the selected friends or ignore endpoint contains a single full page. */
    private boolean isSinglePage() {
        return tabTitle != null && (tabTitle.equalsIgnoreCase(getString(R.string.tab_friends))
                || tabTitle.equalsIgnoreCase(getString(R.string.tab_ignore)));
    }

    /** Builds the documented member, search, friends, or ignore URL for one page. */
    private String getRequestUrl(int page) {
        String credentials = "&login_name="
                + encodeQueryValue(controller.userName(getString(R.string.nav_header_title)))
                + "&login_password=" + encodeQueryValue(controller.userPassword());
        if (isSinglePage()) {
            int folder = tabTitle.equalsIgnoreCase(getString(R.string.tab_friends)) ? 6 : 7;
            return Config.PM_URL + page + "&pm=" + folder + credentials;
        }
        String endpoint = TextUtils.isEmpty(story) ? Config.MEMBERS_URL : Config.MEMBERS_SEARCH_URL;
        String search = TextUtils.isEmpty(story) ? "" : "&story=" + encodeQueryValue(story);
        return endpoint + page + "&pm=6" + credentials + search;
    }

    /** Starts one session-bound request and advances the page only after its response is accepted. */
    private void getData() {
        if (binding == null) return;
        reconcileAccount();
        if (loading || endReached || loadFailed) return;
        if (isSinglePage() && accountKey == null) {
            endReached = true;
            finishLoading();
            binding.emptyView.setText(R.string.unsuccess_auth);
            binding.emptyView.setVisibility(View.VISIBLE);
            return;
        }
        if (retrySnackbar != null) retrySnackbar.dismiss();
        retrySnackbar = null;
        loading = true;
        binding.emptyView.setVisibility(View.GONE);
        binding.progressbar.setVisibility(listFeed.isEmpty() ? View.VISIBLE : View.GONE);
        binding.ProgressBarBottom.setVisibility(listFeed.isEmpty() ? View.GONE : View.VISIBLE);
        final int page = requestCount;
        final int generation = requestGeneration;
        final String requestAccount = accountKey;
        final FragmentHomeBinding requestBinding = binding;
        JsonArrayRequest request = new JsonArrayRequest(getRequestUrl(page),
                response -> {
                    if (!isCurrentRequest(requestBinding, generation, requestAccount)) return;
                    applyResponse(response, page);
                    finishLoading();
                }, error -> {
                    if (!isCurrentRequest(requestBinding, generation, requestAccount)) return;
                    finishLoading();
                    if (isEmptyLastPage(error, page)) {
                        endReached = true;
                        return;
                    }
                    loadFailed = true;
                    retrySnackbar = Snackbar.make(binding.getRoot(), R.string.pm_members_load_failed,
                                    Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.pm_retry, button -> retryPage());
                    retrySnackbar.show();
                });
        activeRequest = request;
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1f));
        PmHttpTransport.queue(controller).add(request);
    }

    /** Retries the failed page explicitly, without letting scroll events restart failed loads. */
    private void retryPage() {
        loadFailed = false;
        getData();
    }

    /** Rejects old-view callbacks and reloads after a late response from a different account. */
    private boolean isCurrentRequest(FragmentHomeBinding requestBinding, int generation,
                                     String requestAccount) {
        if (binding != requestBinding || requestGeneration != generation) return false;
        if (!Objects.equals(requestAccount, PmDeletionQueue.currentAccountKey())) {
            reconcileAccount();
            getData();
            return false;
        }
        return Objects.equals(accountKey, requestAccount);
    }

    /** Recognizes the documented empty HTTP response after the final search page. */
    private boolean isEmptyLastPage(VolleyError error, int page) {
        return page > 1 && error instanceof ParseError && error.networkResponse != null
                && error.networkResponse.statusCode == 200 && error.networkResponse.data != null
                && new String(error.networkResponse.data, StandardCharsets.UTF_8).trim().isEmpty();
    }

    /** Applies one page while filtering repeated IDs and handling the API's no-results sentinel. */
    private void applyResponse(JSONArray response, int page) {
        if (page == 1) {
            listFeed.clear();
            loadedIds.clear();
        }
        int added = 0;
        boolean noResults = false;
        for (int index = 0; index < response.length(); index++) {
            JSONObject json = response.optJSONObject(index);
            if (json == null) continue;
            int id = json.optInt(Config.TAG_ID, 0);
            noResults |= id == 0 && json.optInt("plus", 0) == 1;
            if (id <= 0 || !loadedIds.add(id)) continue;
            String title = json.optString(Config.TAG_TITLE, "");
            FeedPm item = new FeedPm();
            item.setTitle(title);
            item.setImageUrl(json.optString(Config.TAG_IMAGE_URL, ""));
            item.setId(id);
            item.setDate(json.optString(Config.TAG_DATE, ""));
            item.setLast_poster_name(json.optString(Config.TAG_USER, ""));
            item.setFullHtml(json.optString(Config.TAG_FULL_TEXT, ""));
            item.setPreviewHtml(json.optString(Config.TAG_TEXT, ""));
            item.setTime(json.isNull(Config.TAG_TIME) ? 0L : json.optLong(Config.TAG_TIME, 0L));
            listFeed.add(item);
            added++;
        }
        endReached = isSinglePage() || response.length() < PAGE_SIZE || added == 0 || noResults;
        requestCount = page + 1;
        if (page == 1) adapter.resetHiddenItems();
        adapter.updateData(listFeed);
        binding.emptyView.setText(noResults ? R.string.dialog_no_result : R.string.no_data_available);
        binding.emptyView.setVisibility(listFeed.isEmpty() ? View.VISIBLE : View.GONE);
        if (page == 1) binding.recyclerView.scrollToPosition(0);
    }

    /** Clears request indicators after the current response or failure. */
    private void finishLoading() {
        activeRequest = null;
        loading = false;
        binding.progressbar.setVisibility(View.GONE);
        binding.ProgressBarBottom.setVisibility(View.GONE);
        binding.swipeLayout.setRefreshing(false);
    }

    /** Cancels only this fragment's active request and invalidates its callbacks. */
    private void cancelRequest() {
        requestGeneration++;
        if (activeRequest != null) activeRequest.cancel();
        activeRequest = null;
        loading = false;
    }

    /** Reports whether the visible member list has reached its final row. */
    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        RecyclerView.Adapter<?> currentAdapter = recyclerView.getAdapter();
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (currentAdapter == null || currentAdapter.getItemCount() == 0
                || !(manager instanceof LinearLayoutManager)) return false;
        int lastVisible = ((LinearLayoutManager) manager).findLastCompletelyVisibleItemPosition();
        return lastVisible != RecyclerView.NO_POSITION
                && lastVisible == currentAdapter.getItemCount() - 1;
    }

    /** Registers navigation events and replaces rows if the account changed while this tab was stopped. */
    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        if (reconcileAccount()) getData();
    }

    /** Unregisters navigation events while the fragment is stopped. */
    @Override
    public void onStop() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onStop();
    }

    /** Releases requests, callbacks, the adapter, and all references to the destroyed view. */
    @Override
    public void onDestroyView() {
        cancelRequest();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        if (retrySnackbar != null) retrySnackbar.dismiss();
        retrySnackbar = null;
        if (binding != null) {
            binding.recyclerView.removeCallbacks(hideTopButton);
            if (scrollListener != null) binding.recyclerView.removeOnScrollListener(scrollListener);
            binding.recyclerView.setAdapter(null);
            binding.swipeLayout.setOnRefreshListener(null);
        }
        scrollListener = null;
        adapter = null;
        binding = null;
        super.onDestroyView();
    }
}

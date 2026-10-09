/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */
package com.dimonvideo.client.ui.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterMainRazdel;
import com.dimonvideo.client.databinding.FragmentFeedBinding;
import com.dimonvideo.client.db.AppDatabase;
import com.dimonvideo.client.db.FeedPageEntity;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.GetRazdelName;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.UpdatePm;
import com.dimonvideo.client.util.feed.FeedCodec;
import com.dimonvideo.client.util.feed.FeedQuery;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Displays the current section and restores its exact saved query when the network is unavailable. */
public class MainFragmentContent extends Fragment {
    private final List<Feed> listFeed = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FragmentFeedBinding binding;
    private AdapterMainRazdel adapter;
    private AppController controller;
    private AppDatabase database;
    private SharedPreferences sharedPrefs;
    private String razdel, story, tabTitle, tabMode, categoryTitle;
    private int cid;
    private FeedQuery query;
    private JsonArrayRequest activeRequest;
    private int viewGeneration, requestGeneration, nextPage = 1, failedPage = 1;
    private boolean loading, cacheLoaded, freshFirstPage, loadFailed, endReached;

    /** Provides the public empty constructor required when Android restores the fragment. */
    public MainFragmentContent() { }

    /** Uses legacy navigation events only before the view has fixed its own immutable query. */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (query == null && (getArguments() == null || !getArguments().containsKey(Config.TAG_CATEGORY))) {
            razdel = event.razdel;
            story = event.story;
        }
    }

    /** Inflates a feed-specific layout so other lists retain their existing empty-state behavior. */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /** Starts independent disk and network reads without allowing either to overwrite a newer result. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        controller = AppController.getInstance();
        database = controller.getDatabase();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(Config.TAG_CATEGORY)) razdel = args.getString(Config.TAG_CATEGORY);
            if (args.containsKey(Config.TAG_STORY)) story = args.getString(Config.TAG_STORY);
            tabTitle = args.getString("tab");
            tabMode = args.getString("feed_tab");
            cid = args.getInt(Config.TAG_ID);
            categoryTitle = args.getString(Config.TAG_RAZDEL);
        }
        adapter = new AdapterMainRazdel(listFeed, requireContext(),
                (AppCompatActivity) requireActivity(), database);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setItemViewCacheSize(10);
        if (binding.recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) binding.recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        binding.recyclerView.setAdapter(adapter);
        binding.swipeLayout.setOnRefreshListener(this::refresh);
        binding.retryButton.setOnClickListener(v -> refresh());
        binding.retrySavedButton.setOnClickListener(v -> requestPage(failedPage));
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            /** Requests the next successful page only after live page one establishes its ordering. */
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (freshFirstPage && !loading && !loadFailed && !endReached && isLastItemDisplaying()) {
                    requestPage(nextPage);
                }
            }

            /** Keeps the optional scroll-to-top control consistent with the selected preference. */
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) handler.postDelayed(() -> {
                    if (binding != null) binding.fabTop.setVisibility(View.GONE);
                }, 12000);
                else if (dy < 0) binding.fabTop.setVisibility(controller.isOnTop() ? View.VISIBLE : View.GONE);
            }
        });
        binding.fabTop.setOnClickListener(v -> markReadAndScrollToTop());
        startQuery(createQuery());
        if (MainActivity.binding != null) {
            Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
            NetworkUtils.loadAvatar(requireContext(), toolbar);
            toolbar.setSubtitle(TextUtils.isEmpty(categoryTitle) ? null : categoryTitle);
            if (cid > 0) toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
            UpdatePm.update(requireActivity(), razdel, MainActivity.binding.getRoot());
        }
    }

    /** Re-evaluates filter and account changes before exposing a restored favorites list. */
    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && query != null) {
            FeedQuery current = createQuery();
            if (!query.scopeKey().equals(current.scopeKey())) startQuery(current);
        }
    }

    /** Captures section, tab, search, category preferences and favorites account in a stable identity. */
    private FeedQuery createQuery() {
        String mode = tabMode;
        if (mode == null) {
            mode = FeedQuery.LATEST;
            if (getString(R.string.tab_details).equalsIgnoreCase(tabTitle)
                    || getString(R.string.tab_waiting).equalsIgnoreCase(tabTitle)) mode = FeedQuery.DETAILS;
            else if (getString(R.string.tab_favorites).equalsIgnoreCase(tabTitle)) mode = FeedQuery.FAVORITES;
        }
        String sectionKey = GetRazdelName.getRazdelName(razdel, 0);
        return new FeedQuery(razdel, cid, story, mode, controller.isMoreOdob(),
                sharedPrefs.getStringSet("dvc_" + sectionKey + "_cat", null), controller.userName(""));
    }

    /** Cancels obsolete work and restores only data belonging to the newly selected query. */
    private void startQuery(FeedQuery selected) {
        if (activeRequest != null) activeRequest.cancel();
        viewGeneration++;
        requestGeneration++;
        query = selected;
        listFeed.clear();
        adapter.updateFeed(new ArrayList<>(listFeed));
        nextPage = 1;
        loading = false;
        cacheLoaded = false;
        freshFirstPage = false;
        loadFailed = false;
        endReached = false;
        loadSavedData(viewGeneration, selected);
        requestPage(1);
    }

    /** Loads contiguous saved pages in the background, including legacy unfiltered section rows. */
    private void loadSavedData(int generation, FeedQuery selected) {
        controller.getExecutor().execute(() -> {
            List<Feed> saved = new ArrayList<>();
            try {
                List<FeedPageEntity> pages = database.feedPageDao().loadPages(selected.scopeKey());
                int expectedPage = 1;
                for (FeedPageEntity page : pages) {
                    if (page.page != expectedPage) break;
                    JSONArray json = new JSONArray(page.payload);
                    saved.addAll(FeedCodec.parse(json, selected.isDetails()));
                    expectedPage++;
                    if (json.length() == 0) break;
                }
                if (pages.isEmpty() && selected.isLegacyCacheEligible()) {
                    saved.addAll(FeedCodec.fromLegacy(database.feedDao().getAllRows(selected.sectionKey(), 20), false));
                }
            } catch (Exception error) {
                // Do not expose saved response bodies, account names or request URLs in diagnostics.
                Log.w("MainFragmentContent", "Saved feed unavailable");
            }
            List<Feed> result = distinct(saved);
            handler.post(() -> {
                if (!isCurrentView(generation, selected)) return;
                cacheLoaded = true;
                if (!freshFirstPage) replaceFeed(result);
                renderState();
            });
        });
    }

    /** Refreshes page one while keeping the visible list available until its replacement succeeds. */
    private void refresh() {
        if (activeRequest != null) activeRequest.cancel();
        requestGeneration++;
        loading = false;
        requestPage(1);
    }

    /** Sends one request at a time and advances pagination only after a validated response. */
    private void requestPage(int page) {
        if (binding == null || loading) return;
        FeedQuery selected = query;
        if (!selected.scopeKey().equals(createQuery().scopeKey())) {
            startQuery(createQuery());
            return;
        }
        int viewToken = viewGeneration;
        int requestToken = ++requestGeneration;
        loading = true;
        loadFailed = false;
        renderState();
        activeRequest = new JsonArrayRequest(selected.url(page), response -> {
            if (!isCurrentRequest(viewToken, requestToken, selected)) return;
            final List<Feed> feeds;
            try {
                feeds = FeedCodec.parse(response, selected.isDetails());
            } catch (JSONException malformed) {
                handleFailure(page);
                return;
            }
            loading = false;
            activeRequest = null;
            freshFirstPage = true;
            loadFailed = false;
            nextPage = page + 1;
            // Some sections filter SQL rows after applying LIMIT, so a short page is not terminal.
            endReached = feeds.isEmpty() || (feeds.size() == 1 && feeds.get(0).getId() == 0);
            if (page == 1) {
                replaceFeed(distinct(feeds));
                binding.recyclerView.scrollToPosition(0);
            } else {
                List<Feed> combined = new ArrayList<>(listFeed);
                combined.addAll(feeds);
                replaceFeed(distinct(combined));
            }
            persistPage(selected, page, response.toString(), feeds);
            renderState();
        }, error -> {
            if (isCurrentRequest(viewToken, requestToken, selected)) handleFailure(page);
        });
        controller.addToRequestQueue(activeRequest);
    }

    /** Records a retryable page failure without clearing cached cards or skipping the failed page. */
    private void handleFailure(int page) {
        loading = false;
        activeRequest = null;
        loadFailed = true;
        failedPage = page;
        Log.w("MainFragmentContent", "Feed request failed");
        renderState();
    }

    /** Persists complete API pages separately from the old section index used by read markers. */
    private void persistPage(FeedQuery selected, int page, String payload, List<Feed> feeds) {
        controller.getExecutor().execute(() -> {
            try {
                database.feedPageDao().savePage(selected.scopeKey(), page, payload, System.currentTimeMillis());
                if (selected.isLatestIndexEligible() && !feeds.isEmpty()) {
                    database.feedDao().insertAll(FeedCodec.entities(feeds));
                }
            } catch (Exception error) {
                Log.w("MainFragmentContent", "Could not save feed");
            }
        });
    }

    /** Rejects callbacks for a destroyed view, another query, changed filters or another account. */
    private boolean isCurrentView(int generation, FeedQuery selected) {
        return binding != null && isAdded() && generation == viewGeneration
                && query.scopeKey().equals(selected.scopeKey())
                && selected.scopeKey().equals(createQuery().scopeKey());
    }

    /** Adds a request token so an older refresh cannot overwrite the latest response or loading state. */
    private boolean isCurrentRequest(int generation, int requestToken, FeedQuery selected) {
        return isCurrentView(generation, selected) && requestToken == requestGeneration;
    }

    /** Updates the adapter's independent snapshot instead of mutating only the fragment's list. */
    private void replaceFeed(List<Feed> feeds) {
        listFeed.clear();
        listFeed.addAll(feeds);
        adapter.updateFeed(new ArrayList<>(listFeed));
    }

    /** Removes overlapping API rows using both section and item identifier while retaining order. */
    private static List<Feed> distinct(List<Feed> feeds) {
        Map<String, Feed> unique = new LinkedHashMap<>();
        for (Feed feed : feeds) unique.put(feed.getRazdel() + ":" + feed.getId(), feed);
        return new ArrayList<>(unique.values());
    }

    /** Shows progress, saved-data notice, a genuine empty result or an actionable network empty state. */
    private void renderState() {
        if (binding == null) return;
        boolean empty = listFeed.isEmpty();
        boolean initialPending = !cacheLoaded && !freshFirstPage;
        binding.progressbar.setVisibility(empty && (loading || initialPending) ? View.VISIBLE : View.GONE);
        binding.progressBottom.setVisibility(!empty && loading ? View.VISIBLE : View.GONE);
        binding.swipeLayout.setRefreshing(loading && !empty);
        binding.offlineEmpty.setVisibility(empty && loadFailed && !initialPending && !loading ? View.VISIBLE : View.GONE);
        binding.emptyView.setVisibility(empty && !loadFailed && !loading && !initialPending ? View.VISIBLE : View.GONE);
        binding.savedNotice.setVisibility(!empty && loadFailed ? View.VISIBLE : View.GONE);
    }

    /** Detects the loaded end without allowing RecyclerView layout events to duplicate pending calls. */
    private boolean isLastItemDisplaying() {
        LinearLayoutManager manager = (LinearLayoutManager) binding.recyclerView.getLayoutManager();
        return !listFeed.isEmpty() && manager != null
                && manager.findLastCompletelyVisibleItemPosition() == listFeed.size() - 1;
    }

    /** Optionally marks the active section read in Room and posts UI work only to its surviving view. */
    private void markReadAndScrollToTop() {
        binding.recyclerView.scrollToPosition(0);
        if (!controller.isOnTopMark()) return;
        int generation = viewGeneration;
        FeedQuery selected = query;
        controller.getExecutor().execute(() -> {
            database.readMarkDao().markAllRead(selected.sectionKey(), database.feedDao());
            handler.post(() -> {
                if (!isCurrentView(generation, selected)) return;
                adapter.markAllReadInUi();
                Toast.makeText(requireContext(), R.string.success, Toast.LENGTH_LONG).show();
            });
        });
    }

    /** Reconnects legacy navigation observation when this fragment becomes active. */
    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    /** Stops receiving global section changes while the immutable view is inactive. */
    @Override
    public void onStop() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onStop();
    }

    /** Invalidates disk/network callbacks and releases the adapter before Android destroys the view. */
    @Override
    public void onDestroyView() {
        viewGeneration++;
        requestGeneration++;
        if (activeRequest != null) activeRequest.cancel();
        activeRequest = null;
        handler.removeCallbacksAndMessages(null);
        if (adapter != null) adapter.cleanup();
        binding = null;
        query = null;
        listFeed.clear();
        super.onDestroyView();
    }

    /** Removes the last navigation subscription when the fragment itself is discarded. */
    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}

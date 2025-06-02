package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterMainRazdel;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.db.AppDatabase;
import com.dimonvideo.client.db.FeedEntity;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.GetRazdelName;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.UpdatePm;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainFragmentContent extends Fragment {

    private final List<Feed> listFeed = new CopyOnWriteArrayList<>();
    private RecyclerView recyclerView;
    private AdapterMainRazdel adapter;
    private SwipeRefreshLayout swipLayout;
    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    private String razdel;
    private int cid = 0;
    private String url = Config.COMMENTS_URL;
    private String search_url = Config.COMMENTS_SEARCH_URL;
    private String key = "comments";
    private SharedPreferences sharedPrefs;
    private String story, f_name, s_url = "", tab_title;
    private FragmentHomeBinding binding;
    private boolean is_more_odob;
    private String st_url = "";
    private AppController controller;
    private AppDatabase database;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public MainFragmentContent() {
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        razdel = event.razdel;
        story = event.story;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        controller = AppController.getInstance();
        database = controller.getDatabase();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        is_more_odob = controller.isMoreOdob();

        if (getArguments() != null) {
            tab_title = getArguments().getString("tab");
            cid = getArguments().getInt(Config.TAG_ID);
            story = getArguments().getString(Config.TAG_STORY);
            f_name = getArguments().getString(Config.TAG_RAZDEL);
            EventBus.getDefault().postSticky(new MessageEvent(razdel, story, null, null, null, null));
        }

        Log.d("MainFragmentContent", "Cid: " + cid);

        key = GetRazdelName.getRazdelName(razdel, 0);
        search_url = GetRazdelName.getRazdelName(razdel, 1);
        url = GetRazdelName.getRazdelName(razdel, 2);

        LinearLayout emptyLayout = binding.linearEmpty;
        emptyLayout.setVisibility(View.GONE);
        recyclerView = binding.recyclerView;

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = binding.ProgressBarBottom;
        ProgressBarBottom.setVisibility(View.GONE);

        adapter = new AdapterMainRazdel(listFeed, requireContext(), (AppCompatActivity) requireActivity(), database);
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(requireContext(), R.drawable.divider);
        if (horizontalDivider != null) {
            horizontalDecoration.setDrawable(horizontalDivider);
        }
        recyclerView.addItemDecoration(horizontalDecoration);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(true);
        ((SimpleItemAnimator) Objects.requireNonNull(recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
        recyclerView.setAdapter(adapter);

        // Загрузка данных из Room
        controller.getExecutor().execute(() -> {
            List<FeedEntity> entities = database.feedDao().getAllRows(key, 20); // Увеличен лимит
            List<Feed> feeds = new ArrayList<>();
            for (FeedEntity entity : entities) {
                Feed feed = new Feed();
                feed.setId(entity.lid);
                feed.setTitle(entity.title);
                feed.setText(entity.description);
                feed.setFull_text(entity.fullText);
                feed.setDate(entity.date);
                feed.setCategory(entity.category);
                feed.setImageUrl(entity.img);
                feed.setRazdel(entity.razdel);
                feed.setSize(entity.size);
                feed.setLink(entity.url);
                feed.setState(entity.state);
                feeds.add(feed);
                String cacheKey = entity.lid + "_" + entity.razdel;
                int status = database.readMarkDao().getStatus(entity.lid, entity.razdel);
                adapter.addToCache(cacheKey, status);
            }
            requireActivity().runOnUiThread(() -> {
                synchronized (listFeed) {
                    listFeed.addAll(feeds);
                    adapter.notifyDataSetChanged();
                }
            });
        });

        getData();

        try {
            Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
            NetworkUtils.loadAvatar(requireContext(), toolbar);
            toolbar.setSubtitle(TextUtils.isEmpty(f_name) ? null : f_name);
            if (cid > 0) {
                toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
            }
        } catch (Exception e) {
            Log.e("MainFragmentContent", "Toolbar setup error", e);
        }

        FloatingActionButton fab = binding.fabTop;
        boolean is_top = controller.isOnTop();
        boolean is_top_mark = controller.isOnTopMark();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    handler.postDelayed(() -> fab.setVisibility(View.GONE), 6000);
                } else if (dy < 0) {
                    fab.setVisibility(is_top ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (isLastItemDisplaying(recyclerView)) {
                    getData();
                }
            }
        });
        fab.setOnClickListener(v -> {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(0));
            if (is_top_mark) {
                controller.getExecutor().execute(() -> {
                    database.readMarkDao().markAllRead(key);
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), getString(R.string.success), Toast.LENGTH_LONG).show());
                });
            }
        });

        swipLayout = binding.swipeLayout;
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            synchronized (listFeed) {
                listFeed.clear();
            }
            getData();
            swipLayout.setRefreshing(false);
        });

        try {
            UpdatePm.update(requireActivity(), razdel, MainActivity.binding.getRoot());
        } catch (Exception e) {
            Log.e("MainFragmentContent", "UpdatePm error", e);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {
        Set<String> selections = sharedPrefs.getStringSet("dvc_" + key + "_cat", null);
        String categoryString = selections != null ? TextUtils.join(",", selections.toArray(new String[0])) : "all";
        final String loginName = controller.userName(getString(R.string.nav_header_title));

        if (cid > 0) s_url = "&where=" + cid;
        if (!TextUtils.isEmpty(story)) {
            url = search_url;
            try {
                story = URLEncoder.encode(story, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e("MainFragmentContent", "URL encoding error", e);
            }
            s_url = "&story=" + story;
        }

        String urlFinal = url + requestCount + "&c=limit,10," + categoryString + s_url; // Добавлен limit=10
        if (tab_title != null && tab_title.equalsIgnoreCase(getString(R.string.tab_details))) {
            if (is_more_odob) st_url = "&st=2";
            urlFinal = url + requestCount + "&c=limit,10," + categoryString + s_url + st_url;
        }
        if (tab_title != null && tab_title.equalsIgnoreCase(getString(R.string.tab_favorites))) {
            urlFinal = url + requestCount + s_url + "&fav=1&login=" + loginName;
        }

        Activity activity = getActivity();
        Log.d("MainFragmentContent", "URL: " + urlFinal);

        return new JsonArrayRequest(urlFinal,
                response -> {
                    if (activity == null) return;

                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);

                    if (requestCount == 1) {
                        synchronized (listFeed) {
                            listFeed.clear();
                        }
                        recyclerView.post(() -> recyclerView.scrollToPosition(0));
                    }

                    List<FeedEntity> entitiesToInsert = new ArrayList<>();
                    synchronized (listFeed) {
                        for (int i = 0; i < response.length(); i++) {
                            Feed feed = new Feed();
                            FeedEntity entity = new FeedEntity();
                            try {
                                JSONObject json = response.getJSONObject(i);
                                feed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                                feed.setTitle(json.getString(Config.TAG_TITLE));
                                feed.setText(tab_title != null && tab_title.equalsIgnoreCase(getString(R.string.tab_details))
                                        ? json.getString(Config.TAG_FULL_TEXT) : json.getString(Config.TAG_TEXT));
                                feed.setFull_text(json.getString(Config.TAG_FULL_TEXT));
                                feed.setDate(json.getString(Config.TAG_DATE));
                                feed.setComments(json.getInt(Config.TAG_COMMENTS));
                                feed.setHits(json.getInt(Config.TAG_HITS));
                                feed.setRazdel(json.getString(Config.TAG_RAZDEL));
                                feed.setLink(json.getString(Config.TAG_LINK));
                                feed.setMod(json.getString(Config.TAG_MOD));
                                feed.setCategory(json.getString(Config.TAG_CATEGORY));
                                feed.setHeaders(json.getString(Config.TAG_HEADERS));
                                feed.setUser(json.getString(Config.TAG_USER));
                                feed.setSize(json.getString(Config.TAG_SIZE));
                                feed.setTime(json.getLong(Config.TAG_TIME));
                                feed.setId(json.getInt(Config.TAG_ID));
                                feed.setMin(json.getInt(Config.TAG_MIN));
                                feed.setPlus(json.getInt(Config.TAG_PLUS));
                                feed.setFav(json.getInt(Config.TAG_FAV));
                                feed.setState(json.getInt(Config.TAG_STATUS));

                                if (tab_title != null && tab_title.equalsIgnoreCase(getString(R.string.tab_last))) {
                                    entity.lid = json.getInt(Config.TAG_ID);
                                    entity.title = json.getString(Config.TAG_TITLE);
                                    entity.date = json.getString(Config.TAG_DATE);
                                    entity.timestamp = json.getLong(Config.TAG_TIME);
                                    entity.description = json.getString(Config.TAG_TEXT);
                                    entity.fullText = json.getString(Config.TAG_FULL_TEXT);
                                    entity.category = json.getString(Config.TAG_CATEGORY);
                                    entity.img = json.getString(Config.TAG_IMAGE_URL);
                                    entity.razdel = json.getString(Config.TAG_RAZDEL);
                                    entity.size = json.getString(Config.TAG_SIZE);
                                    entity.url = json.getString(Config.TAG_LINK);
                                    entity.state = json.getInt(Config.TAG_STATUS);
                                    entitiesToInsert.add(entity);
                                }
                                listFeed.add(feed);
                            } catch (JSONException e) {
                                Log.e("MainFragmentContent", "JSON parsing error at index " + i, e);
                            }
                        }
                        recyclerView.post(() -> adapter.updateFeed(new ArrayList<>(listFeed)));
                    }

                    if (!entitiesToInsert.isEmpty()) {
                        controller.getExecutor().execute(() -> database.feedDao().insertAll(entitiesToInsert));
                    }
                },
                error -> {
                    if (activity != null) {
                        progressBar.setVisibility(View.GONE);
                        ProgressBarBottom.setVisibility(View.GONE);
                    }
                    Log.e("MainFragmentContent", "Volley error", error);
                });

    }

    private void getData() {
        ProgressBarBottom.setVisibility(View.VISIBLE);
        controller.addToRequestQueue(getDataFromServer(requestCount));
        requestCount++;
    }

    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        if (Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() != 0) {
            int lastVisibleItemPosition = ((LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).findLastCompletelyVisibleItemPosition();
            return lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        adapter.cleanup();
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
        binding = null;
    }
}
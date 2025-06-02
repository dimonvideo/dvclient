package com.dimonvideo.client.ui.pm;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterPmFriends;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;
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

public class PmFragmentMembers extends Fragment {

    private List<FeedPm> listFeed;
    private RecyclerView recyclerView;
    private AdapterPmFriends adapter;
    private SwipeRefreshLayout swipLayout;
    private int requestCount = 1;
    private ProgressBar progressBar, progressBarBottom;
    private String razdel = "13";
    private String url = Config.MEMBERS_URL;
    private String story, tab_title;
    private String s_url = "";
    private FragmentHomeBinding binding;
    private AppController controller;

    public PmFragmentMembers() {
        // Required empty public constructor
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        razdel = "13";
        story = event.story;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (this.getArguments() != null) {
            story = getArguments().getString(Config.TAG_STORY);
            tab_title = getArguments().getString("tab");
        }

        EventBus.getDefault().postSticky(new MessageEvent(razdel, story, null, null, null, null));

        recyclerView = binding.recyclerView;
        controller = AppController.getInstance();
        listFeed = new ArrayList<>();
        adapter = new AdapterPmFriends(listFeed, getContext());

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        progressBarBottom = binding.ProgressBarBottom;
        progressBarBottom.setVisibility(View.GONE);

        // Разделитель позиций
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(requireContext(), R.drawable.divider);
        assert horizontalDivider != null;
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        ((SimpleItemAnimator) Objects.requireNonNull(recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
        recyclerView.setAdapter(adapter);

        // Показ кнопки наверх
        FloatingActionButton fab = binding.fabTop;
        boolean isTop = controller.isOnTop();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) { // down
                    new Handler().postDelayed(() -> fab.setVisibility(View.GONE), 6000);
                } else if (dy < 0) { // up
                    fab.setVisibility(View.VISIBLE);
                    if (!isTop) fab.setVisibility(View.GONE);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (isLastItemDisplaying(recyclerView)) {
                    getData();
                }
            }
        });

        fab.setOnClickListener(views -> recyclerView.post(() -> recyclerView.smoothScrollToPosition(0)));

        // Обновление
        swipLayout = binding.swipeLayout;
        swipLayout.setOnRefreshListener(this::update);

        // Получение данных
        recyclerView.post(this::getData);
    }

    private void update() {
        requestCount = 1;
        listFeed.clear();
        adapter.updateData(listFeed);
        getData();
        TextView fabBadge = MainActivity.binding.appBarMain.fabBadge;
        fabBadge.setVisibility(View.GONE);
        swipLayout.setRefreshing(false);
    }

    private JsonArrayRequest getDataFromServer(int requestCount) {
        String loginName = controller.userName(getString(R.string.nav_header_title));
        String pass = controller.userPassword();
        try {
            pass = URLEncoder.encode(pass, "utf-8");
            loginName = URLEncoder.encode(loginName, "utf-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("PmFragmentMembers", "Encoding error", e);
        }
        String finalPass = pass;
        String finalLogin = loginName;

        if (!TextUtils.isEmpty(story)) {
            url = Config.MEMBERS_SEARCH_URL;
            try {
                story = URLEncoder.encode(story, "utf-8");
            } catch (UnsupportedEncodingException e) {
                Log.e("PmFragmentMembers", "Story encoding error", e);
            }
            s_url = "&story=" + story;
            Log.d("PmFragmentMembers", "story real - " + story);
        }

        String finalUrl = url + requestCount + "&pm=6&login_name=" + finalLogin + "&login_password=" + finalPass + s_url;

        if (tab_title != null && tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_friends))) {
            finalUrl = Config.PM_URL + requestCount + "&pm=6&login_name=" + finalLogin + "&login_password=" + finalPass;
        } else if (tab_title != null && tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_ignore))) {
            finalUrl = Config.PM_URL + requestCount + "&pm=7&login_name=" + finalLogin + "&login_password=" + finalPass;
        }

        return new JsonArrayRequest(finalUrl,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    progressBarBottom.setVisibility(View.GONE);

                    if (response.length() == 0) {
                        swipLayout.setRefreshing(false);
                        return;
                    }

                    if (requestCount == 1) {
                        listFeed.clear();
                        recyclerView.post(() -> recyclerView.scrollToPosition(0));
                    }

                    List<FeedPm> newItems = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        FeedPm jsonFeed = new FeedPm();
                        try {
                            JSONObject json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setTime(json.getLong(Config.TAG_TIME));
                            jsonFeed.setLast_poster_name(json.getString(Config.TAG_USER));
                            jsonFeed.setFullText(json.getString(Config.TAG_FULL_TEXT));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            newItems.add(jsonFeed);
                        } catch (JSONException e) {
                            Log.e("PmFragmentMembers", "JSON parsing error", e);
                        }
                    }
                    listFeed.addAll(newItems);
                    recyclerView.post(() -> adapter.updateData(listFeed));
                    swipLayout.setRefreshing(false);
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    progressBarBottom.setVisibility(View.GONE);
                    swipLayout.setRefreshing(false);
                    Log.e("PmFragmentMembers", "Volley error", error);
                });
    }

    private void getData() {
        progressBarBottom.setVisibility(View.VISIBLE);
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
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
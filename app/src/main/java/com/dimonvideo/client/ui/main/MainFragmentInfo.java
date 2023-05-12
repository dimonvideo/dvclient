package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

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
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterMainRazdelInfo;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.GetRazdelName;
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
import java.util.Set;

public class MainFragmentInfo extends Fragment implements SwipeRefreshLayout.OnRefreshListener   {

    private List<Feed> listFeed;
    private RecyclerView recyclerView;
    private AdapterMainRazdelInfo adapter;
    private SwipeRefreshLayout swipLayout;
    private ProgressBar progressBar, ProgressBarBottom;
    private int requestCount = 1;
    private String razdel = "10";
    private String story = null;
    private SharedPreferences sharedPrefs;
    private String s_url = "";
    private String st_url = "";
    private FragmentHomeBinding binding;
    boolean is_more_odob;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (this.getArguments() != null) {
            razdel = getArguments().getString(Config.TAG_CATEGORY);
        }

        sharedPrefs = AppController.getInstance().getSharedPreferences();
        is_more_odob = AppController.getInstance().isMoreOdob();

        recyclerView = binding.recyclerView;

        listFeed = new ArrayList<>();
        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = binding.ProgressBarBottom;
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();

        adapter = new AdapterMainRazdelInfo(listFeed, getContext());

        // разделитель позиций
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(requireContext(), R.drawable.divider);
        assert horizontalDivider != null;
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setItemViewCacheSize(10);
        ((SimpleItemAnimator) Objects.requireNonNull(recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
        recyclerView.setAdapter(adapter);

        // показ кнопки наверх
        FloatingActionButton fab = binding.fabTop;
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) { // down
                    new Handler().postDelayed(() -> fab.setVisibility(View.GONE), 6000);
                } else if (dy < 0) { // up
                    fab.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // подгрузка ленты
                if (isLastItemDisplaying(recyclerView)) {
                    getData();
                }
            }
        });
        fab.setOnClickListener(views -> {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(0));
        });


        // обновление
        swipLayout = binding.swipeLayout;
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            listFeed.clear();
            getData();
            recyclerView.setAdapter(adapter);
            swipLayout.setRefreshing(false);
        });
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
        story = event.story;
    }
    // запрос к серверу апи

    private JsonArrayRequest getDataFromServer(int requestCount) {

        String key = GetRazdelName.getRazdelName(razdel, 0);
        String search_url = GetRazdelName.getRazdelName(razdel, 1);
        String url = GetRazdelName.getRazdelName(razdel, 2);

        Set<String> selections = sharedPrefs.getStringSet("dvc_"+ key +"_cat", null);
        String category_string = "all";
        if (selections != null) {
            String[] selected = selections.toArray(new String[]{});
            category_string = TextUtils.join(",", selected);
        }
        if (!TextUtils.isEmpty(story)) {
            url = search_url;

            try {
                story = URLEncoder.encode(story, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            s_url = "&story=" + story;
        }

        if (is_more_odob) st_url = "&st=2";

        String url_final = url + requestCount + "&c=placeholder," + category_string + s_url + st_url;

        Log.e("---", url_final);
        return new JsonArrayRequest(url_final,
                response -> {

                    if (requestCount == 1) {
                        listFeed.clear();
                        adapter.notifyItemRangeChanged(0, 10);
                        recyclerView.post(() -> recyclerView.scrollToPosition(0));
                    }

                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                    Log.e("---", "response info: "+response);
                    for (int i = 0; i < response.length(); i++) {
                            Feed jsonFeed = new Feed();
                            JSONObject json;
                            try {
                                json = response.getJSONObject(i);
                                jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                                jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                                jsonFeed.setText(json.getString(Config.TAG_FULL_TEXT));
                                jsonFeed.setDate(json.getString(Config.TAG_DATE));
                                jsonFeed.setComments(json.getInt(Config.TAG_COMMENTS));
                                jsonFeed.setHits(json.getInt(Config.TAG_HITS));
                                jsonFeed.setRazdel(json.getString(Config.TAG_RAZDEL));
                                jsonFeed.setLink(json.getString(Config.TAG_LINK));
                                jsonFeed.setMod(json.getString(Config.TAG_MOD));
                                jsonFeed.setCategory(json.getString(Config.TAG_CATEGORY));
                                jsonFeed.setHeaders(json.getString(Config.TAG_HEADERS));
                                jsonFeed.setUser(json.getString(Config.TAG_USER));
                                jsonFeed.setSize(json.getString(Config.TAG_SIZE));
                                jsonFeed.setTime(json.getLong(Config.TAG_TIME));
                                jsonFeed.setId(json.getInt(Config.TAG_ID));
                                jsonFeed.setMin(json.getInt(Config.TAG_MIN));
                                jsonFeed.setPlus(json.getInt(Config.TAG_PLUS));
                                jsonFeed.setStatus(json.getInt(Config.TAG_STATUS));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            listFeed.add(jsonFeed);
                        }
                    adapter.notifyItemRangeChanged(0, 10);

                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        ProgressBarBottom.setVisibility(View.VISIBLE);
        AppController.getInstance().addToRequestQueue(getDataFromServer(requestCount));
        requestCount++;
    }

    // опредление последнего элемента
    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        if (Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() != 0) {
            int lastVisibleItemPosition = ((LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).findLastCompletelyVisibleItemPosition();
            return lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1;
        }
        return false;
    }
    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }
    // обновление
    @Override
    public void onRefresh() {

    }
    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
        binding = null;
    }
}
package com.dimonvideo.client.ui.pm;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterPmFriends;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PmIgnorFragment extends Fragment {

    private List<FeedPm> listFeed;
    public RecyclerView recyclerView;
    public AdapterPmFriends adapter;
    SwipeRefreshLayout swipLayout;
    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    String url = Config.PM_URL;
    private FragmentHomeBinding binding;


    public PmIgnorFragment() {
        // Required empty public constructor
    }

    @SuppressLint({"DetachAndAttachSameFragment", "NotifyDataSetChanged"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        requestCount = 1;

        recyclerView = binding.recyclerView;

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
        listFeed = new ArrayList<>();

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = binding.ProgressBarBottom;
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new AdapterPmFriends(listFeed);

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

        // обновление
        swipLayout = binding.swipeLayout;
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            listFeed.clear();
            getData();
            swipLayout.setRefreshing(false);
        });
    }

    // запрос к серверу апи
    private JsonArrayRequest getDataFromServer(int requestCount) {

        String login_name = AppController.getInstance().userName(getString(R.string.nav_header_title));
        String pass = AppController.getInstance().userPassword();
        try {
            pass = URLEncoder.encode(pass, "utf-8");
            login_name = URLEncoder.encode(login_name, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String finalPass = pass;
        String finalLogin = login_name;

        return new JsonArrayRequest(url + requestCount + "&pm=7&login_name=" + finalLogin + "&login_password=" + finalPass,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);

                    if (requestCount == 1) {
                        listFeed.clear();
                        adapter.notifyItemRangeChanged(0, 10);
                        recyclerView.post(() -> recyclerView.scrollToPosition(0));
                    }

                    for (int i = 0; i < response.length(); i++) {
                        FeedPm jsonFeed = new FeedPm();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setImageUrl(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setTime(json.getLong(Config.TAG_TIME));
                            jsonFeed.setLast_poster_name(json.getString(Config.TAG_LAST_POSTER_NAME));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setFullText(json.getString(Config.TAG_FULL_TEXT));

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
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

}
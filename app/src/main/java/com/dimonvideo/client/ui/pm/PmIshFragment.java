package com.dimonvideo.client.ui.pm;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.PmAdapter;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PmIshFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener  {

    public RecyclerView recyclerView;
    SwipeRefreshLayout swipLayout;
    private List<FeedPm> listFeed;

    private ProgressBar progressBar, progressBarBottom;
    String url = Config.PM_URL;
    private FragmentHomeBinding binding;
    public PmAdapter adapter;
    private int requestCount = 1;


    public PmIshFragment() {
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

        recyclerView = binding.recyclerView;
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        listFeed = new ArrayList<>();

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (isLastItemDisplaying(recyclerView)) {
                    getData();
                }

            }
        });

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        progressBarBottom = binding.ProgressBarBottom;
        progressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new PmAdapter(listFeed);

        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setHasFixedSize(true);

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
    @SuppressLint("NotifyDataSetChanged")
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

        return new JsonArrayRequest(url + requestCount + "&pm=3&login_name=" + finalLogin + "&login_password=" + finalPass,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    progressBarBottom.setVisibility(View.GONE);
                    for (int i = 0; i < response.length(); i++) {
                        FeedPm jsonFeed = new FeedPm();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setImageUrl(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setIs_new(json.getInt(Config.TAG_HITS));
                            jsonFeed.setLast_poster_name(json.getString(Config.TAG_LAST_POSTER_NAME));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setFullText(json.getString(Config.TAG_FULL_TEXT));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listFeed.add(jsonFeed);
                    }
                    adapter.notifyDataSetChanged();

                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    progressBarBottom.setVisibility(View.GONE);
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        progressBarBottom.setVisibility(View.VISIBLE);
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

    // обновление
    @Override
    public void onRefresh() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

}
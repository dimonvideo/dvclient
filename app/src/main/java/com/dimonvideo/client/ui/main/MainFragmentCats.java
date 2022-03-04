package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.MainCategoryAdapter;
import com.dimonvideo.client.model.FeedCats;
import com.dimonvideo.client.util.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MainFragmentCats extends Fragment implements SwipeRefreshLayout.OnRefreshListener  {

    private List<FeedCats> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;
    SharedPreferences sharedPrefs;

    private RequestQueue requestQueue;

    private ProgressBar progressBar, ProgressBarBottom;
    static int razdel = 10;
    String url = Config.CATEGORY_URL;
    String key = "comments";
    public MainFragmentCats() {
        // Required empty public constructor
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
    }

    @SuppressLint("DetachAndAttachSameFragment")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        recyclerView = root.findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());

        progressBar = root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = root.findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new MainCategoryAdapter(listFeed, getContext());

        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);

        recyclerView.setAdapter(adapter);

        // обновление
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(() -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .detach(MainFragmentCats.this)
                    .attach(MainFragmentCats.this)
                    .commit();
            swipLayout.setRefreshing(false);
        });

        return root;
    }


    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer() {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        if (razdel == 1) key = Config.GALLERY_RAZDEL;
        if (razdel == 2) key = Config.UPLOADER_RAZDEL;
        if (razdel == 3) key = Config.VUPLOADER_RAZDEL;
        if (razdel == 4) key = Config.NEWS_RAZDEL;
        if (razdel == 5) key = Config.MUZON_RAZDEL;
        if (razdel == 6) key = Config.BOOKS_RAZDEL;
        if (razdel == 7) key = Config.ARTICLES_RAZDEL;
        if (razdel == 11) key = Config.ANDROID_RAZDEL;
        if (razdel == 14) key = Config.TRACKER_RAZDEL;
        if (razdel == 15) key = Config.BLOG_RAZDEL;

        Log.e("mainFragmentCats", ""+url+key);

        if (this.getArguments() != null) {
            EventBus.getDefault().postSticky(new MessageEvent(razdel, null));
        }
        return new JsonArrayRequest(url + key,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                    for (int i = 0; i < response.length(); i++) {
                        FeedCats jsonFeed = new FeedCats();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setRazdel(json.getString(Config.TAG_RAZDEL));
                            jsonFeed.setCid(json.getInt(Config.TAG_ID));
                            jsonFeed.setCount(json.getInt(Config.TAG_COUNT));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listFeed.add(jsonFeed);
                    }
                    adapter.notifyDataSetChanged();

                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        ProgressBarBottom.setVisibility(View.VISIBLE);
        requestQueue.add(getDataFromServer());
    }

    // обновление
    @Override
    public void onRefresh() {

    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
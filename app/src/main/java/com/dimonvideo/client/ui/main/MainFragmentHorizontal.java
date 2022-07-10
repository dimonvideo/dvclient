package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
import com.dimonvideo.client.adater.MainAdapter;
import com.dimonvideo.client.adater.MainAdapterFull;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.MessageEvent;

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

public class MainFragmentHorizontal extends Fragment implements SwipeRefreshLayout.OnRefreshListener   {

    private List<Feed> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;

    private RequestQueue requestQueue;
    private ProgressBar progressBar, ProgressBarBottom;

    private int requestCount = 1;
    static int razdel = 10;
    String url = Config.COMMENTS_URL;
    String search_url = Config.COMMENTS_SEARCH_URL;
    static String story = null;
    String key = "comments";
    SharedPreferences sharedPrefs;
    String s_url = "";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = root.findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
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
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());
        progressBar = root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = root.findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();

        adapter = new MainAdapterFull(listFeed, getContext());

        recyclerView.setAdapter(adapter);

        // обновление
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            listFeed = new ArrayList<>();
            requestQueue = Volley.newRequestQueue(requireActivity());
            getData();
            adapter = new MainAdapter(listFeed, getContext());
            recyclerView.setAdapter(adapter);
            swipLayout.setRefreshing(false);
        });

        return root;
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
        story = event.story;
    }
    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        if (razdel == 1) {
            url = Config.GALLERY_URL;
            search_url = Config.GALLERY_SEARCH_URL;
            key = Config.GALLERY_RAZDEL;
        }
        if (razdel == 2) {
            url = Config.UPLOADER_URL;
            search_url = Config.UPLOADER_SEARCH_URL;
            key = Config.UPLOADER_RAZDEL;

        }
        if (razdel == 3) {
            url = Config.VUPLOADER_URL;
            search_url = Config.VUPLOADER_SEARCH_URL;
            key = Config.VUPLOADER_RAZDEL;

        }
        if (razdel == 4) {
            url = Config.NEWS_URL;
            search_url = Config.NEWS_SEARCH_URL;
            key = Config.NEWS_RAZDEL;

        }
        if (razdel == 5) {
            url = Config.MUZON_URL;
            search_url = Config.MUZON_SEARCH_URL;
            key = Config.MUZON_RAZDEL;

        }
        if (razdel == 6) {
            url = Config.BOOKS_URL;
            search_url = Config.BOOKS_SEARCH_URL;
            key = Config.BOOKS_RAZDEL;

        }
        if (razdel == 7) {
            url = Config.ARTICLES_URL;
            search_url = Config.ARTICLES_SEARCH_URL;
            key = Config.ARTICLES_RAZDEL;
        }
        if (razdel == 11) {
            url = Config.ANDROID_URL;
            search_url = Config.ANDROID_SEARCH_URL;
            key = Config.ANDROID_RAZDEL;
        }
        if (razdel == 14) {
            url = Config.TRACKER_URL;
            search_url = Config.TRACKER_SEARCH_URL;
            key = Config.TRACKER_RAZDEL;
        }
        if (razdel == 15) {
            url = Config.BLOG_URL;
            search_url = Config.BLOG_SEARCH_URL;
            key = Config.BLOG_RAZDEL;
        }
        Set<String> selections = sharedPrefs.getStringSet("dvc_"+key+"_cat", null);
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
        return new JsonArrayRequest(url + requestCount + "&c=placeholder," + category_string + s_url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
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
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            listFeed.add(jsonFeed);
                        }
                    new Handler().postDelayed(() -> adapter.notifyDataSetChanged(), 50);

                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        ProgressBarBottom.setVisibility(View.VISIBLE);
        requestQueue.add(getDataFromServer(requestCount));
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
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
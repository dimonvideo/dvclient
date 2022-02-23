package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.dimonvideo.client.adater.MainAdapter;
import com.dimonvideo.client.model.Feed;
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
public class MainFragmentFav extends Fragment implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener  {

    private List<Feed> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    static int razdel = 10;
    int cid = 0;
    String url = Config.COMMENTS_URL;
    String search_url = Config.COMMENTS_SEARCH_URL;
    static String story = null;
    String s_url = "";
    String key = "comments";
    SharedPreferences sharedPrefs;

    public MainFragmentFav() {
        // Required empty public constructor
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
        story = event.story;
        if (TextUtils.isEmpty(story)) story = null;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        String main_razdel = sharedPrefs.getString("dvc_main_razdel", "10");
        if (razdel == 10) {
            if (Integer.parseInt(main_razdel) != 10) razdel = Integer.parseInt(main_razdel);
        }
    }

    @SuppressLint("DetachAndAttachSameFragment")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        requestCount = 1;
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (this.getArguments() != null) {
            cid = getArguments().getInt(Config.TAG_ID);
        }

        recyclerView = root.findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnScrollChangeListener(this);
        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());

        progressBar = root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = root.findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new MainAdapter(listFeed, getContext());

        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);

        // обновление
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            getParentFragmentManager()
                    .beginTransaction()
                    .detach(MainFragmentFav.this)
                    .attach(MainFragmentFav.this)
                    .commit();
            swipLayout.setRefreshing(false);
        });

        recyclerView.setAdapter(adapter);
        return root;
    }



    // запрос к серверу апи
    private JsonArrayRequest getDataFromServer(int requestCount) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        String main_razdel = sharedPrefs.getString("dvc_main_razdel", "10");
        final String login_name = sharedPrefs.getString("dvc_login", getString(R.string.nav_header_title));
        if (razdel == 10) {
            if (Integer.parseInt(main_razdel) != 10) razdel = Integer.parseInt(main_razdel);
        }
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
        if (!TextUtils.isEmpty(story)) {
            url = search_url;
        }
        if (!TextUtils.isEmpty(story)) {
            s_url = "&story=" + story;
        }
        return new JsonArrayRequest(url + requestCount + s_url+"&fav=1&login_name="+ login_name,
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
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
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
                            jsonFeed.setFav(json.getInt(Config.TAG_FAV));
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

    // получение следующей страницы при скролле
    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (isLastItemDisplaying(recyclerView)) {
            getData();
        }
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
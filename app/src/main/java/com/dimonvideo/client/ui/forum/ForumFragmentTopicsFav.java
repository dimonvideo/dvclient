package com.dimonvideo.client.ui.forum;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.ForumAdapter;
import com.dimonvideo.client.model.FeedForum;
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
public class ForumFragmentTopicsFav extends Fragment implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener   {

    private List<FeedForum> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    String url = Config.FORUM_FEED_URL;
    String story = null;
    String s_url = "";
    String f_name;
    int id = 0;
    int razdel = 8; // forum fragment

    public ForumFragmentTopicsFav() {
        // Required empty public constructor
    }

    @SuppressLint({"DetachAndAttachSameFragment", "NotifyDataSetChanged"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        if (this.getArguments() != null) {
            id = getArguments().getInt(Config.TAG_ID);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            f_name = getArguments().getString(Config.TAG_CATEGORY);
        }

        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());

        progressBar = root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = root.findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new ForumAdapter(listFeed, getContext());

        recyclerView = root.findViewById(R.id.recycler_view);

        // разделитель позиций
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(requireContext(), R.drawable.divider);
        assert horizontalDivider != null;
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setOnScrollChangeListener(this);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setItemViewCacheSize(30);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        // обновление
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(() -> {
            FragmentManager fragmentManager = getParentFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, new ForumFragmentTopicsFav())
                    .addToBackStack(null)
                    .commit();
            swipLayout.setRefreshing(false);
            adapter.notifyDataSetChanged();
        });

        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (!TextUtils.isEmpty(f_name)) toolbar.setTitle(f_name);

        return root;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
        story = event.story;
    }

    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        final String login_name = sharedPrefs.getString("dvc_login", getString(R.string.nav_header_title));

        if (!TextUtils.isEmpty(story)) {
            s_url = "&story=" + story;
        }

        if (id>0) {
            s_url = "&id=" + id;
        }
        Log.e("forum", url + requestCount + s_url+"&fav=1&login_name="+ login_name);
        return new JsonArrayRequest(url + requestCount + s_url+"&fav=1&login_name="+ login_name,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                    for (int i = 0; i < response.length(); i++) {
                        FeedForum jsonFeed = new FeedForum();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setLast_poster_name(json.getString(Config.TAG_LAST_POSTER_NAME));
                            jsonFeed.setUser(json.getString(Config.TAG_USER));
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setCategory(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setState(json.getString(Config.TAG_STATE));
                            jsonFeed.setPinned(json.getString(Config.TAG_PINNED));
                            jsonFeed.setComments(json.getInt(Config.TAG_COMMENTS));
                            jsonFeed.setTime(json.getLong(Config.TAG_TIME));
                            jsonFeed.setFav(json.getInt(Config.TAG_FAV));
                            jsonFeed.setHits(json.getInt(Config.TAG_HITS));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listFeed.add(jsonFeed);
                    }
                    new Handler().postDelayed(() -> adapter.notifyDataSetChanged(), 300);

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
            new Handler().postDelayed(this::getData, 300);
        }
    }

    // обновление
    @SuppressLint("DetachAndAttachSameFragment")
    @Override
    public void onRefresh() {
        requestCount = 1;
        getParentFragmentManager()
                .beginTransaction()
                .detach(ForumFragmentTopicsFav.this)
                .attach(ForumFragmentTopicsFav.this)
                .commit();
    }
    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
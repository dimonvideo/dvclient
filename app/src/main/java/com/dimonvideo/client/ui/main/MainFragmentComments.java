package com.dimonvideo.client.ui.main;

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
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
import com.dimonvideo.client.adater.CommentsAdapter;
import com.dimonvideo.client.adater.MainAdapter;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.ui.pm.PmFragment;
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

@RequiresApi(api = Build.VERSION_CODES.M)
public class MainFragmentComments extends Fragment implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener  {

    private List<FeedForum> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;
    LinearLayout emptyLayout;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    static int razdel = 10;
    String url = Config.COMMENTS_READS_URL;
    String key = "comments";
    SharedPreferences sharedPrefs;
    static String story;

    public MainFragmentComments() {
        // Required empty public constructor
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
    }

    @SuppressLint({"DetachAndAttachSameFragment", "NotifyDataSetChanged"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        requestCount = 1;

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        if (this.getArguments() != null) {
            razdel = getArguments().getInt(Config.TAG_CATEGORY);
            EventBus.getDefault().postSticky(new MessageEvent(razdel, null));
        }

        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());
        emptyLayout = root.findViewById(R.id.linearEmpty);

        progressBar = root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = root.findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new CommentsAdapter(listFeed, getContext());
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
            requestCount = 1;
            FragmentManager fragmentManager = getParentFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, new MainFragmentComments())
                    .addToBackStack(null)
                    .commit();
            swipLayout.setRefreshing(false);
            adapter.notifyDataSetChanged();
        });

        return root;
    }



    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());




        if (razdel == 1) {
            key = Config.GALLERY_RAZDEL;
        }
        if (razdel == 2) {
            key = Config.UPLOADER_RAZDEL;

        }
        if (razdel == 3) {
            key = Config.VUPLOADER_RAZDEL;

        }
        if (razdel == 4) {
            key = Config.NEWS_RAZDEL;

        }
        if (razdel == 5) {
            key = Config.MUZON_RAZDEL;

        }
        if (razdel == 6) {
            key = Config.BOOKS_RAZDEL;

        }
        if (razdel == 7) {
            key = Config.ARTICLES_RAZDEL;
        }
        if (razdel == 11) {
            key = Config.ANDROID_RAZDEL;
        }
        if (razdel == 14) {
            key = Config.TRACKER_RAZDEL;
        }
        if (razdel == 15) {
            key = Config.BLOG_RAZDEL;
        }





        return new JsonArrayRequest(url + key + "&min=" + requestCount  + "&lid=0",
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                    for (int i = 0; i < response.length(); i++) {
                        FeedForum jsonFeed = new FeedForum();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setUser(json.getString(Config.TAG_USER));
                            jsonFeed.setRazdel(json.getString(Config.TAG_RAZDEL));
                            jsonFeed.setCategory(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setState(json.getString(Config.TAG_RAZDEL));
                            jsonFeed.setTime(json.getLong(Config.TAG_TIME));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setPost_id(json.getInt(Config.TAG_POST_ID));
                            jsonFeed.setMin(json.getInt(Config.TAG_MIN));
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

    // получение следующей страницы при скролле
    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (isLastItemDisplaying(recyclerView)) {
            new Handler().postDelayed(this::getData, 100);
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
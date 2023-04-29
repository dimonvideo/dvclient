package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.MainAdapter;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.db.Provider;
import com.dimonvideo.client.db.Table;
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

public class MainFragmentContent extends Fragment {

    private List<Feed> listFeed;
    public RecyclerView recyclerView;
    public MainAdapter adapter;
    SwipeRefreshLayout swipLayout;
    LinearLayout emptyLayout;
    private Context mContext;
    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    int razdel = 10;
    int cid = 0;
    String url = Config.COMMENTS_URL;
    String search_url = Config.COMMENTS_SEARCH_URL;
    String key = "comments";
    SharedPreferences sharedPrefs;
    String story, f_name, s_url = "";
    private FragmentHomeBinding binding;
    Toolbar toolbar;

    public MainFragmentContent() {
        // Required empty public constructor
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        razdel = event.razdel;
        story = event.story;
    }

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

        Log.e(Config.TAG, "MainFragmentContent razdel: " + razdel);

        mContext = requireContext();

        sharedPrefs = AppController.getInstance().getSharedPreferences();

        if (this.getArguments() != null) {
            cid = getArguments().getInt(Config.TAG_ID);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            f_name = getArguments().getString(Config.TAG_RAZDEL);
            EventBus.getDefault().postSticky(new MessageEvent(razdel, story, null));
        }


        key = GetRazdelName.getRazdelName(razdel, 0);
        search_url = GetRazdelName.getRazdelName(razdel, 1);
        url = GetRazdelName.getRazdelName(razdel, 2);

        listFeed = new ArrayList<>();

        // запоминание просмотренного
        try {

            Cursor cursor = Provider.getAllRows(key);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {

                        Feed jsonFeedList = new Feed();
                        jsonFeedList.setId(cursor.getInt(1));
                        jsonFeedList.setTitle(cursor.getString(3));
                        jsonFeedList.setText(cursor.getString(4));
                        jsonFeedList.setFull_text(cursor.getString(5));
                        jsonFeedList.setDate(cursor.getString(6));
                        jsonFeedList.setCategory(cursor.getString(8));
                        jsonFeedList.setImageUrl(cursor.getString(9));
                        jsonFeedList.setRazdel(cursor.getString(10));
                        jsonFeedList.setSize(cursor.getString(11));
                        jsonFeedList.setLink(cursor.getString(12));

                        listFeed.add(jsonFeedList);

                    } while (cursor.moveToNext());

                }
                cursor.close();
            }
        } catch (Throwable ignored) {
        }

        emptyLayout = binding.linearEmpty;
        emptyLayout.setVisibility(View.GONE);
        recyclerView = binding.recyclerView;

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = binding.ProgressBarBottom;
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new MainAdapter(listFeed, mContext);

        // разделитель позиций
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(mContext, R.drawable.divider);
        assert horizontalDivider != null;
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (isLastItemDisplaying(recyclerView)) {
                    getData();
                }

            }
        });
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);


        try {
            toolbar = MainActivity.binding.appBarMain.toolbar;
            NetworkUtils.loadAvatar(requireContext(), toolbar);
        } catch (Exception ignored) {

        }
        if ((!TextUtils.isEmpty(f_name)) && (toolbar != null)) {
            toolbar.setSubtitle(f_name);
        } else if (toolbar != null) {
            toolbar.setSubtitle(null);
        }

        if ((cid > 0) && (toolbar != null)) {
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }

        // обновление
        swipLayout = binding.swipeLayout;
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            listFeed.clear();
            getData();
            swipLayout.setRefreshing(false);
        });


        UpdatePm.update(requireActivity());
    }


    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {
        Set<String> selections = null;
        try {
            selections = sharedPrefs.getStringSet("dvc_" + key + "_cat", null);
        } catch(Exception ignored) {

        }

        String category_string = "all";
        if (selections != null) {
            String[] selected = selections.toArray(new String[]{});
            category_string = TextUtils.join(",", selected);
        }


        if (cid > 0) s_url = "&where=" + cid;

        if (!TextUtils.isEmpty(story)) {
            url = search_url;

            try {
                story = URLEncoder.encode(story, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            s_url = "&story=" + story;
        }

        String url_final = url + requestCount + "&c=placeholder," + category_string + s_url;

        return new JsonArrayRequest(url_final,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);

                    if (requestCount == 1) {
                        listFeed.clear();
                        adapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(0);
                    }


                    for (int i = 0; i < response.length(); i++) {
                        Feed jsonFeed = new Feed();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setFull_text(json.getString(Config.TAG_FULL_TEXT));
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


                            // сохраняем в базу результат для оффлайн просмотра
                            String unique = razdel + String.valueOf(json.getInt(Config.TAG_ID));
                            ContentValues values = new ContentValues();
                            values.put(Table.COLUMN_ID, unique);
                            values.put(Table.COLUMN_LID, json.getInt(Config.TAG_ID));
                            values.put(Table.COLUMN_STATUS, 0);
                            values.put(Table.COLUMN_TITLE, json.getString(Config.TAG_TITLE));
                            values.put(Table.COLUMN_DATE, json.getString(Config.TAG_DATE));
                            values.put(Table.COLUMN_TIMESTAMP, json.getString(Config.TAG_TIME));
                            values.put(Table.COLUMN_TEXT, json.getString(Config.TAG_TEXT));
                            values.put(Table.COLUMN_FULL_TEXT, json.getString(Config.TAG_FULL_TEXT));
                            values.put(Table.COLUMN_CATEGORY, json.getString(Config.TAG_CATEGORY));
                            values.put(Table.COLUMN_IMG, json.getString(Config.TAG_IMAGE_URL));
                            values.put(Table.COLUMN_RAZDEL, json.getString(Config.TAG_RAZDEL));
                            values.put(Table.COLUMN_SIZE, json.getString(Config.TAG_SIZE));
                            values.put(Table.COLUMN_URL, json.getString(Config.TAG_LINK));

                            try {
                                mContext.getContentResolver().insert(Provider.CONTENT_URI, values);
                            } catch (Throwable ignored) {
                            }

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
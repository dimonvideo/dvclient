package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterMainRazdel;
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
    private RecyclerView recyclerView;
    private AdapterMainRazdel adapter;
    private SwipeRefreshLayout swipLayout;
    private Context mContext;
    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    private String razdel;
    private int cid = 0;
    private String url = Config.COMMENTS_URL;
    private String search_url = Config.COMMENTS_SEARCH_URL;
    private String key = "comments";
    private SharedPreferences sharedPrefs;
    private String story, f_name, s_url = "", tab_title;
    private FragmentHomeBinding binding;
    private boolean is_more_odob;
    private String st_url = "";

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


        mContext = requireContext();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        is_more_odob = AppController.getInstance().isMoreOdob();

        if (this.getArguments() != null) {
            tab_title = getArguments().getString("tab");
            cid = getArguments().getInt(Config.TAG_ID);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            f_name = getArguments().getString(Config.TAG_RAZDEL);
            EventBus.getDefault().postSticky(new MessageEvent(razdel, story, null, null, null, null));
        }

        Log.e("---", "MainFragmentCid: "+cid);

        key = GetRazdelName.getRazdelName(razdel, 0);
        search_url = GetRazdelName.getRazdelName(razdel, 1);
        url = GetRazdelName.getRazdelName(razdel, 2);

        listFeed = new ArrayList<>();

        // запоминание просмотренного, показ из БД старых данных при запуске
        try {
            Cursor cursor = Provider.getAllRows(key);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {

                        Feed jsonFeedList = new Feed();
                        jsonFeedList.setId(cursor.getInt(1));
                        jsonFeedList.setStatus(cursor.getInt(2));
                        jsonFeedList.setTitle(cursor.getString(3));
                        jsonFeedList.setText(cursor.getString(4));
                        jsonFeedList.setFull_text(cursor.getString(5));
                        jsonFeedList.setDate(cursor.getString(6));
                        jsonFeedList.setCategory(cursor.getString(8));
                        jsonFeedList.setImageUrl(cursor.getString(9));
                        jsonFeedList.setRazdel(cursor.getString(10));
                        jsonFeedList.setSize(cursor.getString(11));
                        jsonFeedList.setLink(cursor.getString(12));
                        jsonFeedList.setState(1);

                        listFeed.add(jsonFeedList);

                    } while (cursor.moveToNext());

                }
                cursor.close();
                Provider.sqlDB.close();
            }
        } catch (Throwable ignored) {
        }

        LinearLayout emptyLayout = binding.linearEmpty;
        emptyLayout.setVisibility(View.GONE);
        recyclerView = binding.recyclerView;

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = binding.ProgressBarBottom;
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new AdapterMainRazdel(listFeed, mContext);

        // разделитель позиций
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(requireContext(), R.drawable.divider);
        if (horizontalDivider != null) {
            horizontalDecoration.setDrawable(horizontalDivider);
        }
        recyclerView.addItemDecoration(horizontalDecoration);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(true);
        ((SimpleItemAnimator) Objects.requireNonNull(recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
        recyclerView.setAdapter(adapter);


        try {
            Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
            NetworkUtils.loadAvatar(requireContext(), toolbar);

            if (!TextUtils.isEmpty(f_name)) {
                toolbar.setSubtitle(f_name);
            } else {
                toolbar.setSubtitle(null);
            }

            if (cid > 0) {
                toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
            }

        } catch (Exception ignored) {

        }

        // показ кнопки наверх
        FloatingActionButton fab = binding.fabTop;
        boolean is_top = AppController.getInstance().isOnTop();
        boolean is_top_mark = AppController.getInstance().isOnTopMark();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) { // down
                    new Handler().postDelayed(() -> fab.setVisibility(View.GONE), 6000);
                } else if (dy < 0) { // up
                    fab.setVisibility(View.VISIBLE);
                    if (!is_top) fab.setVisibility(View.GONE);
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
            String key = GetRazdelName.getRazdelName(razdel, 0);
            if (is_top_mark) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Provider.markAllRead(key);
                    Toast.makeText(getContext(), this.getString(R.string.success), Toast.LENGTH_LONG).show();
                });
            }

        });


        // обновление
        swipLayout = binding.swipeLayout;
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            listFeed.clear();
            getData();
            swipLayout.setRefreshing(false);
        });


        try{
            UpdatePm.update(requireActivity(), razdel, MainActivity.binding.getRoot());
        } catch (Exception ignored) {
        }
    }


    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {
        Set<String> selections = null;
        try {
            selections = sharedPrefs.getStringSet("dvc_" + key + "_cat", null);
        } catch (Exception ignored) {

        }

        String category_string = "all";
        if (selections != null) {
            String[] selected = selections.toArray(new String[]{});
            category_string = TextUtils.join(",", selected);
        }

        final String login_name = AppController.getInstance().userName(getString(R.string.nav_header_title));

        if (cid > 0) s_url = "&where=" + cid;

        if (!TextUtils.isEmpty(story)) {
            url = search_url;

            try {
                story = URLEncoder.encode(story, "utf-8");
            } catch (UnsupportedEncodingException ignored) {

            }
            s_url = "&story=" + story;
        }

        if (TextUtils.isEmpty(category_string)) category_string = "all";
        String url_final = url + requestCount + "&c=placeholder," + category_string + s_url;
        if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_details)))) {
            if (is_more_odob) st_url = "&st=2";
            url_final = url + requestCount + "&c=placeholder," + category_string + s_url + st_url;
        }
        if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_favorites)))) {
            url_final = url + requestCount + s_url + "&fav=1&login_name=" + login_name;
        }

        Activity activity = getActivity();

        Log.e("---", "url_final: "+url_final);


        return new JsonArrayRequest(url_final,
                response -> {

                    if (activity != null) {
                        progressBar.setVisibility(View.GONE);
                        ProgressBarBottom.setVisibility(View.GONE);
                    }

                    if (requestCount == 1) {
                        listFeed.clear();
                        recyclerView.post(() -> recyclerView.scrollToPosition(0));
                    }

                    Log.e("---", "response: "+response);


                    for (int i = 0; i < response.length(); i++) {
                        Feed jsonFeed = new Feed();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));

                            if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_details)))) {
                                jsonFeed.setText(json.getString(Config.TAG_FULL_TEXT));
                            } else jsonFeed.setText(json.getString(Config.TAG_TEXT));

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
                            jsonFeed.setStatus(0);
                            jsonFeed.setFav(json.getInt(Config.TAG_FAV));
                            jsonFeed.setState(json.getInt(Config.TAG_STATUS));


                            // сохраняем в базу результат для оффлайн просмотра
                            if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_last))) && (activity != null)) {
                                ContentValues values = new ContentValues();
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
                                values.put(Table.COLUMN_STATE, json.getInt(Config.TAG_STATUS));
                                try {
                                    mContext.getContentResolver().insert(Provider.CONTENT_URI, values);
                                } catch (Throwable ignored) {
                                }
                            }

                        } catch (JSONException ignored) {

                        }
                        if (activity != null) listFeed.add(jsonFeed);

                    }
                    if (activity != null)
                        recyclerView.post(() -> {
                            adapter.notifyDataSetChanged();
                        });

                },
                error -> {
                    if(activity != null) {
                        progressBar.setVisibility(View.GONE);
                        ProgressBarBottom.setVisibility(View.GONE);
                    }
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
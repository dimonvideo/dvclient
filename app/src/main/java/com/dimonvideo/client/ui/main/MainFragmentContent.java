package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.MainAdapter;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.ui.forum.ForumFragment;
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
public class MainFragmentContent extends Fragment implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener  {

    private List<Feed> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;
    LinearLayout emptyLayout;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    static int razdel = 10;
    int cid = 0;
    String url = Config.COMMENTS_URL;
    String search_url = Config.COMMENTS_SEARCH_URL;
    String s_url = "";
    String key = "comments";
    SharedPreferences sharedPrefs;
    static String story;
    String f_name;

    public MainFragmentContent() {
        // Required empty public constructor
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
        story = event.story;
    }

    @SuppressLint("DetachAndAttachSameFragment")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        requestCount = 1;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        final String image_url = sharedPrefs.getString("auth_foto", Config.BASE_URL + "/images/noavatar.png");

        if (this.getArguments() != null) {
            cid = getArguments().getInt(Config.TAG_ID);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            f_name = getArguments().getString(Config.TAG_RAZDEL);
            EventBus.getDefault().postSticky(new MessageEvent(razdel, story));
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        recyclerView = root.findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnScrollChangeListener(this);
        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());
        emptyLayout = root.findViewById(R.id.linearEmpty);

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

        recyclerView.setAdapter(adapter);

        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (!TextUtils.isEmpty(f_name)) toolbar.setSubtitle(f_name); else toolbar.setSubtitle(null);

        if (cid > 0) {
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);

            toolbar.setNavigationOnClickListener(v -> {
                toolbar.setSubtitle(null);
                requireActivity().onBackPressed();
            });

        } else {
            Glide.with(this)
                    .asDrawable()
                    .load(image_url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(new CustomTarget<Drawable>() {

                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable @org.jetbrains.annotations.Nullable com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                            try {
                                toolbar.setNavigationIcon(resource);
                            } catch (Throwable ignored) {
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }

        // обновление
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            getParentFragmentManager()
                    .beginTransaction()
                    .detach(MainFragmentContent.this)
                    .attach(MainFragmentContent.this)
                    .commit();
            swipLayout.setRefreshing(false);
        });



        return root;
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

        Log.e("tagURL--->>>>", url + requestCount + "&c=placeholder," + category_string + s_url);

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
/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.ui.forum;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterForumPosts;
import com.dimonvideo.client.databinding.CommentsListBinding;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.BBCodes;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.ProgressHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ForumFragmentPosts extends BottomSheetDialogFragment {

    private List<FeedForum> listFeed;
    private AdapterForumPosts adapter;
    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    private String s_url = "", t_name;
    private String tid = "1728146606";
    private ImageView imagePick;
    @SuppressLint("StaticFieldLeak")
    public static CommentsListBinding binding;
    private String image_uploaded, razdel;
    private EditText textInput;
    private RecyclerView recyclerView;
    private AppController controller;

    public ForumFragmentPosts(){
        // Required empty public constructor
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        razdel = event.razdel;
        Bitmap bitmap = event.bitmap;
        image_uploaded = event.image_uploaded;
        String cit = event.action;

        // если скриншот загружен на сервер
        if (bitmap != null) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            imagePick.setBackground(bitmapDrawable);
            imagePick.setBackgroundTintList(null);
        }
        // если скриншот загружен на сервер
        if (cit != null) {
            textInput.setText(cit);
            textInput.setSelection(textInput.getText().length());
            textInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(textInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = CommentsListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        controller = AppController.getInstance();

        final int auth_state = controller.isAuth();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
        toolbar.setNavigationIcon(R.drawable.ic_back_button);


        LinearLayout post_layout = binding.post.linearLayout1;
        imagePick = binding.post.imgBtn;

        imagePick.setOnClickListener(v -> {
            MainActivity.pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        if (this.getArguments() != null) {
            tid = getArguments().getString(Config.TAG_ID);
            t_name = getArguments().getString(Config.TAG_TITLE);
        }

        recyclerView = binding.recyclerView;
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());

        recyclerView.setLayoutManager(layoutManager);

        listFeed = new ArrayList<>();
        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = binding.ProgressBarBottom;
        ProgressBarBottom.setVisibility(View.GONE);
        ImageView search_icon = binding.searchIcon;
        EditText searchText = binding.search;

        // получение данных
        getData();
        adapter = new AdapterForumPosts(listFeed);
        TextView title = binding.title;
        if (t_name != null) title.setText(t_name);
        // разделитель позиций
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(requireContext(), R.drawable.divider);
        assert horizontalDivider != null;
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);

        recyclerView.setAdapter(adapter);

        // показ кнопки наверх
        FloatingActionButton fab = binding.fabTop;
        boolean is_top = controller.isOnTop();
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
        });

        if (auth_state > 0) post_layout.setVisibility(View.VISIBLE);
        // отправка ответа
        ImageButton btnSend = binding.post.btnSend;
        textInput = binding.post.textInput;

        btnSend.setOnClickListener(v -> {
            String text = textInput.getText().toString();
            NetworkUtils.sendPm(getContext(), Integer.parseInt(tid), BBCodes.imageCodes(text, image_uploaded, razdel), 2, null, 0);
            textInput.getText().clear();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
            imagePick.setVisibility(View.GONE);
            requestCount = 1;
            getData();
        });

        ImageView imageDismiss = binding.dismiss;
        imageDismiss.setOnClickListener(v -> {
            if (searchText.isShown()){
                searchText.setVisibility(View.GONE);
                search_icon.setVisibility(View.VISIBLE);
            } else dismiss();
        });

        // поиск по теме
        search_icon.setOnClickListener(v -> {
            searchText.setVisibility(View.VISIBLE);
            search_icon.setVisibility(View.GONE);
            searchText.setFocusableInTouchMode(true);
            searchText.setOnFocusChangeListener((v1, hasFocus) -> searchText.post(() -> {
                InputMethodManager inputMethodManager= (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(searchText, InputMethodManager.SHOW_IMPLICIT);
            }));
            searchText.requestFocus();
            searchText.setOnEditorActionListener((v12, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String text =  searchText.getText().toString();
                    dismiss();
                    ForumFragmentPostSearch fragment = new ForumFragmentPostSearch();
                    Bundle bundle = new Bundle();
                    bundle.putString(Config.TAG_TITLE, t_name);
                    bundle.putString(Config.TAG_ID, tid);
                    bundle.putString(Config.TAG_STORY, text);
                    fragment.setArguments(bundle);
                    fragment.show(requireActivity().getSupportFragmentManager(), "ForumFragmentPostSearch");
                    return true;
                }
                return false;
            });

        });
    }

    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {

        if (!TextUtils.isEmpty(tid)) {
            s_url = "&id=" + tid;
        }

        String url = Config.FORUM_POSTS_URL;
        return new JsonArrayRequest(url + requestCount + s_url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                    if (requestCount == 1) {
                        listFeed.clear();
                        recyclerView.post(() -> recyclerView.scrollToPosition(0));
                    }
                    for (int i = 0; i < response.length(); i++) {
                        FeedForum jsonFeed = new FeedForum();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
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
                            jsonFeed.setHits(json.getInt(Config.TAG_HITS));
                            jsonFeed.setNewtopic(json.getInt(Config.TAG_NEW_TOPIC));
                            jsonFeed.setTopic_id(json.getInt(Config.TAG_TOPIC_ID));
                            jsonFeed.setMin(json.getInt(Config.TAG_MIN));
                        } catch (JSONException ignored) {

                        }
                        listFeed.add(jsonFeed);
                    }
                    recyclerView.post(() -> {
                        adapter.notifyDataSetChanged();
                    });
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        progressBar.setVisibility(View.VISIBLE);
        controller.addToRequestQueue(getDataFromServer(requestCount));
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
        if (ProgressHelper.isDialogVisible()) ProgressHelper.dismissDialog();
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
    public void onDetach() {
        super.onDetach();
    }

}
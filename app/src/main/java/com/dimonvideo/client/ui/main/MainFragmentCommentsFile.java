package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterComments;
import com.dimonvideo.client.databinding.CommentsListBinding;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.BBCodes;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainFragmentCommentsFile extends BottomSheetDialogFragment {
    private List<FeedForum> listFeed;
    private AdapterComments adapter;
    private String comm_url;
    private String file_title;
    private String image_uploaded;
    private String razdel, lid;
    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    @SuppressLint("StaticFieldLeak")
    public static CommentsListBinding binding;
    private String story = null;
    private ImageView imagePick;
    private EditText textInput;
    private RecyclerView recyclerView;

    public MainFragmentCommentsFile(){
        // Required empty public constructor
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
        story = event.story;
        image_uploaded = event.image_uploaded;
        Bitmap bitmap = event.bitmap;
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        final int auth_state = AppController.getInstance().isAuth();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (this.getArguments() != null) {
            file_title = (String) getArguments().getString(Config.TAG_TITLE);
            comm_url = (String) getArguments().getString(Config.TAG_LINK);
            razdel = (String) getArguments().getString(Config.TAG_RAZDEL);
            lid = (String) getArguments().getString(Config.TAG_ID);
        }

        EventBus.getDefault().postSticky(new MessageEvent(razdel, story, image_uploaded, null, null, null));


        recyclerView = binding.recyclerView;
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());

        recyclerView.setLayoutManager(layoutManager);
        listFeed = new ArrayList<>();

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = binding.ProgressBarBottom;
        ProgressBarBottom.setVisibility(View.GONE);
        TextView title = binding.title;
        if (file_title != null) title.setText(file_title);
        ImageView search_icon = binding.searchIcon;
        search_icon.setVisibility(View.GONE);


        // получение данных
        getData();
        adapter = new AdapterComments(listFeed, getContext());
        // разделитель позиций
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(requireContext(), R.drawable.divider);
        assert horizontalDivider != null;
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);

        recyclerView.setAdapter(adapter);

        LinearLayout post_layout = binding.post.linearLayout1;
        if (auth_state > 0) post_layout.setVisibility(View.VISIBLE);
        // отправка ответа
        ImageButton btnSend = binding.post.btnSend;
        textInput = binding.post.textInput;
        textInput.requestFocus();

        btnSend.setOnClickListener(v -> {
            String text = textInput.getText().toString();
            NetworkUtils.sendPm(getContext(), Integer.parseInt(lid), BBCodes.imageCodes(text, image_uploaded, razdel), 20, razdel, 0);
            textInput.getText().clear();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
            imagePick.setVisibility(View.GONE);
            requestCount = 1;
            getData();
        });

        imagePick = binding.post.imgBtn;
        imagePick.setOnClickListener(v -> {
            MainActivity.pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        ImageView imageDismiss = binding.dismiss;
        imageDismiss.setOnClickListener(v -> {
            dismiss();
        });


                }
    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {

        return new JsonArrayRequest(comm_url + requestCount,
                response -> {
                    if (requestCount == 1) {
                        listFeed.clear();
                        adapter.notifyDataSetChanged();
                        recyclerView.post(() -> recyclerView.scrollToPosition(0));
                    }
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                    for (int i = 0; i < response.length(); i++) {
                        FeedForum jsonFeed = new FeedForum();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                            jsonFeed.setTitle(file_title);
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setUser(json.getString(Config.TAG_USER));
                            jsonFeed.setCategory(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setState(json.getString(Config.TAG_RAZDEL));
                            jsonFeed.setRazdel(json.getString(Config.TAG_RAZDEL));
                            jsonFeed.setTime(json.getLong(Config.TAG_TIME));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setPost_id(json.getInt(Config.TAG_POST_ID));
                            jsonFeed.setMin(json.getInt(Config.TAG_MIN));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listFeed.add(jsonFeed);
                    }
                    adapter.notifyDataSetChanged();
                    Log.e("---", "MainFragmentCommentsFile: "+response);

                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        progressBar.setVisibility(View.VISIBLE);
        AppController.getInstance().addToRequestQueue(getDataFromServer(requestCount));
        requestCount++;
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
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


}
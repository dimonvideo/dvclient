/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.toolbox.JsonArrayRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.databinding.BottomDetailBinding;
import com.dimonvideo.client.db.ReadMarkEntity;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.ProgressHelper;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.XMLReader;

public class MainFragmentViewFileByApi extends BottomSheetDialogFragment {

    private BottomDetailBinding binding;
    private String razdel;
    private String lid;
    private String file_title;
    private String logourl;
    private String mod;
    private String link;
    private Context context;
    private AppController controller;

    public MainFragmentViewFileByApi() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = BottomDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View views, @Nullable Bundle savedInstanceState) {
        context = requireContext();
        controller = AppController.getInstance();

        if (this.getArguments() != null) {
            razdel = getArguments().getString(Config.TAG_RAZDEL);
            lid = getArguments().getString(Config.TAG_ID);
        }

        if (lid != null) {
            controller.getExecutor().execute(() -> {
                ReadMarkEntity readMark = new ReadMarkEntity();
                readMark.lid = Integer.parseInt(lid);
                readMark.razdel = razdel;
                readMark.status = 1;
                controller.getDatabase().readMarkDao().insert(readMark);
            });
        }

        getData(views);
    }

    @SuppressLint("SetTextI18n")
    private void buildFile(@NonNull View views, Context context, Feed jsonFeed) {
        file_title = jsonFeed.getTitle();
        String category = jsonFeed.getCategory();
        String date = jsonFeed.getDate();
        int plus = jsonFeed.getPlus();
        String user = jsonFeed.getUser();
        String text = jsonFeed.getText();
        logourl = jsonFeed.getImageUrl();
        mod = jsonFeed.getMod();
        int comments = jsonFeed.getComments();
        int status = jsonFeed.getStatus();
        link = jsonFeed.getLink();
        String size = jsonFeed.getSize();

        final boolean is_open_link = controller.isOpenLinks();
        final boolean is_vuploader_play_listtext = controller.isVuploaderPlayListtext();
        final boolean is_share_btn = controller.isShareBtn();

        TextView textViewTitle = binding.title;
        TextView textViewDate = binding.date;
        TextView textViewCategory = binding.category;
        TextView txt_plus = binding.txtPlus; // Обновлено: txtPlus
        TextView textViewAuthor = binding.byName; // Обновлено: byName
        TextView textViewText = binding.text;

        // Массив всех нужных TextView
        TextView[] textViews = {
                textViewTitle,
                textViewText,
                textViewDate,
                textViewCategory,
                txt_plus,
                textViewAuthor,
                textViewText
        };

        // Массивы размеров для каждого режима
        float[] sizesSmallest = {14, 13, 12, 12, 12, 12, 12};
        float[] sizesSmall = {16, 15, 14, 14, 14, 14, 14};
        float[] sizesNormal = {18, 17, 16, 16, 16, 16, 16};
        float[] sizesLarge = {20, 19, 18, 18, 18, 18, 18};
        float[] sizesLargest = {24, 23, 22, 22, 22, 22, 22};

        float[] selectedSizes;

        switch (AppController.getInstance().isFontSize()) {
            case "smallest":
                selectedSizes = sizesSmallest;
                break;
            case "small":
                selectedSizes = sizesSmall;
                break;
            case "large":
                selectedSizes = sizesLarge;
                break;
            case "largest":
                selectedSizes = sizesLargest;
                break;
            default:
                selectedSizes = sizesNormal;
                break;
        }
        for (int i = 0; i < textViews.length; i++) {
            textViews[i].setTextSize(selectedSizes[i]);
        }

        textViewTitle.setText(file_title);
        textViewDate.setText(date);
        textViewCategory.setText(category);
        txt_plus.setText(String.valueOf(plus));
        textViewAuthor.setText(String.valueOf(user));

        MaterialButton dismiss2 = binding.dismiss2; // Обновлено: MaterialButton
        MaterialButton btn_comms = binding.btnComment; // Обновлено: btnComment
        MaterialButton btn_download = binding.btnDownload; // Обновлено: btnDownload
        MaterialButton btn_mod = binding.btnMod; // Обновлено: btnMod
        MaterialButton btn_mp4 = binding.btnMp4; // Обновлено: btnMp4
        MaterialButton btn_share = binding.btnShare; // Обновлено: btnShare
        MaterialButton btn_odob = binding.btnOdob; // Обновлено: btnOdob
        ImageView starButton = binding.starButton; // Добавлено: starButton
        ImageView likeButton = binding.thumbButton; // Добавлено: thumbButton
        ImageView imageView = binding.logo;
        ImageView imageDismiss = binding.dismiss;

        btn_odob.setVisibility(View.GONE);
        Log.e("---", "status: " + status);
        if ((status == 0) && (controller.isUserGroup() <= 2)) {
            btn_odob.setVisibility(View.VISIBLE);
            btn_odob.setOnClickListener(v -> NetworkUtils.getOdob(razdel, Integer.parseInt(lid)));
        }

        // HTML textview
        try {
            URLImageParser parser = new URLImageParser(textViewText);
            Spanned spanned = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY, parser, new TagHandler());
            textViewText.setText(spanned);
            textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context, razdel);
                }
            });
        } catch (Throwable ignored) {
        }

        Glide.with(context).load(logourl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new FitCenter(), new RoundedCorners(15))
                .into(imageView);

        imageView.setOnClickListener(v -> ButtonsActions.loadScreen(context, logourl));

        imageDismiss.setOnClickListener(v -> dismiss());
        dismiss2.setOnClickListener(v -> dismiss());

        btn_comms.setOnClickListener(v -> {
            String comm_url = Config.COMMENTS_READS_URL + razdel + "&lid=" + lid + "&min=";
            MainFragmentCommentsFile fragment = new MainFragmentCommentsFile();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_TITLE, file_title);
            bundle.putString(Config.TAG_ID, String.valueOf(lid));
            bundle.putString(Config.TAG_LINK, comm_url);
            bundle.putString(Config.TAG_RAZDEL, razdel);
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "MainFragmentCommentsFile");
            dismiss();
        });

        // Комментарии
        String comText;
        if (comments > 0) {
            comText = context.getResources().getString(R.string.Comments) + " " + comments;
        } else {
            comText = context.getResources().getString(R.string.Comments) + " " + 0;
        }
        btn_comms.setText(comText);

        // Смотреть онлайн
        if ((razdel != null) && (razdel.equals(Config.VUPLOADER_RAZDEL))) {
            btn_mp4.setVisibility(View.VISIBLE);
            btn_mp4.setOnClickListener(v -> ButtonsActions.PlayVideo(context, link));
        }

        // Слушать онлайн
        if ((razdel != null) && (razdel.equals(Config.MUZON_RAZDEL))) {
            btn_mp4.setVisibility(View.VISIBLE);
            btn_mp4.setText(R.string.listen_online);
            btn_mp4.setOnClickListener(v -> ButtonsActions.PlayVideo(context, link));
        }

        // Если нет размера файла
        if ((size == null) || (size.startsWith("0"))) {
            btn_download.setVisibility(View.GONE);
            btn_mod.setVisibility(View.GONE);
        } else {
            btn_download.setText(context.getString(R.string.download) + " " + size);
            btn_download.setVisibility(View.VISIBLE);
        }

        // Если есть mod
        if ((mod != null) && (!mod.startsWith("null"))) {
            btn_mod.setVisibility(View.VISIBLE);
            btn_mod.setOnClickListener(v -> DownloadFile.download(context, mod, razdel));
        }

        // Поделиться
        try {
            if (!is_share_btn) btn_share.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }

        btn_share.setOnClickListener(v -> {
            String url = Config.WRITE_URL + "/" + razdel + "/" + lid;
            if (razdel != null && razdel.equals(Config.COMMENTS_RAZDEL))
                url = Config.WRITE_URL + "/" + lid + "-news.html";

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, file_title);
            dismiss();
            try {
                context.startActivity(shareIntent);
            } catch (Throwable ignored) {
            }
        });

        // Скачать
        btn_download.setOnClickListener(v -> DownloadFile.download(context, link, razdel));

        // Лайк и избранное
        starButton.setOnClickListener(v -> {
            Snackbar.make(views, context.getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
            ButtonsActions.add_to_fav_file(context, razdel, Integer.parseInt(lid), 1);
        });

        likeButton.setOnClickListener(v -> {
            Snackbar.make(views, context.getString(R.string.like), Snackbar.LENGTH_LONG).show();
            ButtonsActions.like_file(context, razdel, Integer.parseInt(lid), 1);
            txt_plus.setText(String.valueOf(plus + 1));
        });

        // Скрытие лишнего
        if ((razdel != null) && (razdel.equals(Config.TRACKER_RAZDEL))) {
            btn_comms.setVisibility(View.GONE);
            btn_share.setVisibility(View.GONE);
        } else {
            btn_comms.setVisibility(View.VISIBLE);
            btn_share.setVisibility(View.VISIBLE);
        }

        if (is_share_btn) btn_share.setVisibility(View.GONE);
    }

    // Запрос к серверу API
    private JsonArrayRequest getDataFromServer(View views) {
        String url_final = Config.SINGLE_FILE_URL + razdel + "&lid=" + lid;

        Log.e("---", url_final);
        ProgressHelper.showDialog(requireContext(), getString(R.string.please_wait));

        return new JsonArrayRequest(url_final,
                response -> {
                    Feed jsonFeed = new Feed();
                    JSONObject json;
                    try {
                        json = response.getJSONObject(0);
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
                        jsonFeed.setStatus(json.getInt(Config.TAG_STATUS));
                    } catch (JSONException ignored) {
                    }

                    if (ProgressHelper.isDialogVisible()) ProgressHelper.dismissDialog();

                    buildFile(views, context, jsonFeed);
                },
                error -> {
                    if (ProgressHelper.isDialogVisible()) ProgressHelper.dismissDialog();
                });
    }

    // Получение данных
    private void getData(View views) {
        controller.addToRequestQueue(getDataFromServer(views));
    }

    public static class TagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (!opening && tag.equals("ul")) {
                output.append("\n");
            }
            if (opening && tag.equals("li")) {
                output.append("\n•");
            }
        }
    }

    @Override
    public void onDestroy() {
        controller.getRequestQueueV().cancelAll(this);
        if (ProgressHelper.isDialogVisible()) ProgressHelper.dismissDialog();
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
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
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterMainRazdel;
import com.dimonvideo.client.databinding.BottomDetailBinding;
import com.dimonvideo.client.db.Provider;
import com.dimonvideo.client.ui.main.MainFragmentCommentsFile;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;

import org.xml.sax.XMLReader;

public class MainFragmentViewFile extends BottomSheetDialogFragment {

    private BottomDetailBinding binding;
    private String razdel, lid, file_title, date, category, user, text, logourl, mod, link, size;
    private int plus, comments, status;

    public MainFragmentViewFile(){
        // Required empty public constructor
    }

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

        if (this.getArguments() != null) {
            file_title = getArguments().getString(Config.TAG_TITLE);
            razdel = getArguments().getString(Config.TAG_RAZDEL);
            category = getArguments().getString(Config.TAG_CATEGORY);
            date = getArguments().getString(Config.TAG_DATE);
            plus = getArguments().getInt(Config.TAG_PLUS);
            lid = getArguments().getString(Config.TAG_ID);
            user = getArguments().getString(Config.TAG_USER);
            text = getArguments().getString(Config.TAG_TEXT);
            logourl = getArguments().getString(Config.TAG_IMAGE_URL);
            mod = getArguments().getString(Config.TAG_MOD);
            comments = getArguments().getInt(Config.TAG_COMMENTS);
            status = getArguments().getInt(Config.TAG_STATUS);
            link = getArguments().getString(Config.TAG_LINK);
            size = getArguments().getString(Config.TAG_SIZE);
        }

        if (lid != null) {
            Provider.updateStatus(Integer.parseInt(lid), razdel, 1);
        }

        Context context = requireContext();


        final boolean is_open_link = AppController.getInstance().isOpenLinks();
        final boolean is_vuploader_play_listtext = AppController.getInstance().isVuploaderPlayListtext();
        final boolean is_share_btn = AppController.getInstance().isShareBtn();

        TextView textViewTitle = binding.title;
        textViewTitle.setText(file_title);
        TextView textViewDate = binding.date;
        textViewDate.setText(date);
        TextView textViewCategory = binding.category;
        textViewCategory.setText(category);
        TextView txt_plus = binding.txtPlus;
        txt_plus.setText(String.valueOf(plus));
        TextView textViewAuthor = binding.byName;
        textViewAuthor.setText(String.valueOf(user));

        Button dismiss2 = binding.dismiss2;
        Button btn_comms, btn_download, btn_mod, btn_mp4, btn_share, btn_odob;

        LikeButton likeButton, starButton;
        likeButton = binding.thumbButton;

        starButton = binding.starButton;

        btn_comms = binding.btnComment;
        btn_download = binding.btnDownload;
        btn_mod = binding.btnMod;
        btn_share = binding.btnShare;
        btn_mp4 = binding.btnMp4;
        btn_odob = binding.btnOdob;
        btn_odob.setVisibility(View.GONE);

        Log.e("---", "status: "+status);
        if ((status == 0) && (AppController.getInstance().isUserGroup() <= 2)){
            btn_odob.setVisibility(View.VISIBLE);
            btn_odob.setOnClickListener(v -> {
                NetworkUtils.getOdob(razdel, Integer.parseInt(lid));
            });
        }

        // html textview
        TextView textViewText = binding.text;
        try {
            URLImageParser parser = new URLImageParser(textViewText);
            Spanned spanned = Html.fromHtml(text, parser, new TagHandler());
            textViewText.setText(spanned);
            textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context, razdel);
                }
            });
        } catch (Throwable ignored) {
        }

        ImageView imageView = binding.logo;
        ImageView imageDismiss = binding.dismiss;

        Glide.with(context).load(logourl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new FitCenter(), new RoundedCorners(15))
                .into(imageView);

        imageView.setOnClickListener(v -> ButtonsActions.loadScreen(context, logourl));

        imageDismiss.setOnClickListener(v -> {
            dismiss();
        });
        dismiss2.setOnClickListener(v -> {
            dismiss();
        });


        btn_comms.setOnClickListener(v -> {
            String comm_url = Config.COMMENTS_READS_URL + razdel + "&lid=" + lid + "&min=";
            MainFragmentCommentsFile fragment = new MainFragmentCommentsFile();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_TITLE, file_title);
            bundle.putString(Config.TAG_ID, String.valueOf(lid));
            bundle.putString(Config.TAG_LINK, comm_url);
            bundle.putString(Config.TAG_RAZDEL, razdel);
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity)context).getSupportFragmentManager(), "MainFragmentCommentsFile");
            dismiss();
        });
        // комментарии
        if (comments > 0) {
            String comText = context.getResources().getString(R.string.Comments) + " " + comments;
            btn_comms.setText(comText);
        } else {
            String comText = context.getResources().getString(R.string.Comments) + " " + 0;
            btn_comms.setText(comText);
        }
        // смотреть онлайн
        if ((razdel != null) && (razdel.equals(Config.VUPLOADER_RAZDEL))) {
            btn_mp4.setVisibility(View.VISIBLE);
            btn_mp4.setOnClickListener(v -> ButtonsActions.PlayVideo(context, link));
        }
        // слушать онлайн
        if ((razdel != null) && (razdel.equals(Config.MUZON_RAZDEL))) {
            btn_mp4.setVisibility(View.VISIBLE);
            btn_mp4.setText(R.string.listen_online);
            btn_mp4.setOnClickListener(v -> ButtonsActions.PlayVideo(context, link));
        }
        // если нет размера файла
        if ((size == null) || (size.startsWith("0"))) {
            btn_download.setVisibility(View.GONE);
            btn_mod.setVisibility(View.GONE);
        } else {
            btn_download.setText(context.getString(R.string.download) + " " + size);
            btn_download.setVisibility(View.VISIBLE);
        }
        // если нет mod
        if ((mod != null) && (!mod.startsWith("null"))) {
            btn_mod.setVisibility(View.VISIBLE);
            btn_mod.setOnClickListener(v -> DownloadFile.download(context, mod, razdel));
        }

        // поделится
        try {
            if (!is_share_btn) btn_share.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }

        btn_share.setOnClickListener(v -> {

            String url = Config.BASE_URL + "/" + razdel + "/" + lid;
            if (razdel != null && razdel.equals(Config.COMMENTS_RAZDEL))
                url = Config.BASE_URL + "/" + lid + "-news.html";

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

        // скачать
        btn_download.setOnClickListener(v -> DownloadFile.download(context, link, razdel));

        // лайк и избранное
        starButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton starButton) {
                Snackbar.make(views, context.getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, razdel, Integer.parseInt(lid), 1); // в избранное
            }

            @Override
            public void unLiked(LikeButton starButton) {
                Snackbar.make(views, context.getString(R.string.unfavorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, razdel, Integer.parseInt(lid), 2); // из избранного
            }
        });

        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Snackbar.make(views, context.getString(R.string.like), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, razdel, Integer.parseInt(lid), 1);
                txt_plus.setText(String.valueOf(plus + 1));

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Snackbar.make(views, context.getString(R.string.unlike), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, razdel, Integer.parseInt(lid), 2);
                txt_plus.setText(String.valueOf(plus - 1));
            }
        });

        // скрытие лишнего
        if ((razdel != null) && (razdel.equals(Config.TRACKER_RAZDEL)))
            btn_comms.setVisibility(View.GONE);
        else btn_comms.setVisibility(View.VISIBLE);
        if ((razdel != null) && (razdel.equals(Config.TRACKER_RAZDEL)))
            btn_share.setVisibility(View.GONE);
        else btn_share.setVisibility(View.VISIBLE);

        if (is_share_btn) btn_share.setVisibility(View.GONE);

    }

    public static class TagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag,
                              Editable output, XMLReader xmlReader) {
            if (!opening && tag.equals("ul")) {
                output.append("\n");
            }
            if (opening && tag.equals("li")) {
                output.append("\n\u2022");
            }
        }
    }

    @Override
    public void onDestroy() {
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
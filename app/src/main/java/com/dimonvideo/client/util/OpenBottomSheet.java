package com.dimonvideo.client.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterMainRazdel;
import com.dimonvideo.client.db.Provider;
import com.dimonvideo.client.ui.main.MainFragmentCommentsFile;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;

public class OpenBottomSheet {// подробный вывод файла

    public static void openFile(String razdel, View v, int lid, int comments, String title,
                                String user, int plus, String link, String mod, String size, String logourl, String text, String date, String category) {

        Provider.updateStatus(lid, razdel, 1);

        Context context = v.getContext();
        @SuppressLint("InflateParams") View views = LayoutInflater.from(context).inflate(R.layout.bottom_detail, null);

        final boolean is_open_link = AppController.getInstance().isOpenLinks();
        final boolean is_vuploader_play_listtext = AppController.getInstance().isVuploaderPlayListtext();
        final boolean is_share_btn = AppController.getInstance().isShareBtn();

        TextView textViewTitle = views.findViewById(R.id.title);
        textViewTitle.setText(title);
        TextView textViewDate = views.findViewById(R.id.date);
        textViewDate.setText(date);
        TextView textViewCategory = views.findViewById(R.id.category);
        textViewCategory.setText(category);
        TextView txt_plus = views.findViewById(R.id.txt_plus);
        txt_plus.setText(String.valueOf(plus));
        TextView textViewAuthor = views.findViewById(R.id.by_name);
        textViewAuthor.setText(String.valueOf(user));

        Button dismiss2 = views.findViewById(R.id.dismiss2);
        Button btn_comms, btn_download, btn_mod, btn_mp4, btn_share;

        LikeButton likeButton, starButton;
        likeButton = views.findViewById(R.id.thumb_button);

        starButton = views.findViewById(R.id.star_button);

        btn_comms = views.findViewById(R.id.btn_comment);
        btn_download = views.findViewById(R.id.btn_download);
        btn_mod = views.findViewById(R.id.btn_mod);
        btn_share = views.findViewById(R.id.btn_share);
        btn_mp4 = views.findViewById(R.id.btn_mp4);

        // html textview
        TextView textViewText = views.findViewById(R.id.text);
        try {
            URLImageParser parser = new URLImageParser(textViewText);
            Spanned spanned = Html.fromHtml(text, parser, new AdapterMainRazdel.TagHandler());
            textViewText.setText(spanned);
            textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context, razdel);
                }
            });
        } catch (Throwable ignored) {
        }

        ImageView imageView = views.findViewById(R.id.logo);
        ImageView imageDismiss = views.findViewById(R.id.dismiss);

        Glide.with(context).load(logourl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new FitCenter(), new RoundedCorners(15))
                .into(imageView);

        imageView.setOnClickListener(view -> ButtonsActions.loadScreen(context, logourl));

        AdapterMainRazdel.dialog = new BottomSheetDialog(context);
        AdapterMainRazdel.dialog.setCancelable(true);
        AdapterMainRazdel.dialog.setCanceledOnTouchOutside(true);
        AdapterMainRazdel.dialog.setContentView(views);
        AdapterMainRazdel.dialog.show();

        imageDismiss.setOnClickListener(view -> {
            AdapterMainRazdel.dialog.dismiss();
        });
        dismiss2.setOnClickListener(view -> {
            AdapterMainRazdel.dialog.dismiss();
        });


        btn_comms.setOnClickListener(view -> {
            String comm_url = Config.COMMENTS_READS_URL + razdel + "&lid=" + lid + "&min=";
            MainFragmentCommentsFile fragment = new MainFragmentCommentsFile();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_TITLE, title);
            bundle.putString(Config.TAG_ID, String.valueOf(lid));
            bundle.putString(Config.TAG_LINK, comm_url);
            bundle.putString(Config.TAG_RAZDEL, razdel);
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity)context).getSupportFragmentManager(), "MainFragmentCommentsFile");
            AdapterMainRazdel.dialog.dismiss();
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
            btn_mp4.setOnClickListener(view -> ButtonsActions.PlayVideo(context, link));
        }
        // слушать онлайн
        if ((razdel != null) && (razdel.equals(Config.MUZON_RAZDEL))) {
            btn_mp4.setVisibility(View.VISIBLE);
            btn_mp4.setText(R.string.listen_online);
            btn_mp4.setOnClickListener(view -> ButtonsActions.PlayVideo(context, link));
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
            btn_mod.setOnClickListener(view -> DownloadFile.download(context, mod, razdel));
        }

        // поделится
        try {
            if (!is_share_btn) btn_share.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }

        btn_share.setOnClickListener(view -> {

            String url = Config.BASE_URL + "/" + razdel + "/" + lid;
            if (razdel != null && razdel.equals(Config.COMMENTS_RAZDEL))
                url = Config.BASE_URL + "/" + lid + "-news.html";

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, title);
            AdapterMainRazdel.dialog.dismiss();
            try {
                context.startActivity(shareIntent);
            } catch (Throwable ignored) {
            }

        });

        // скачать
        btn_download.setOnClickListener(view -> DownloadFile.download(context, link, razdel));

        // лайк и избранное
        starButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton starButton) {
                Snackbar.make(views, context.getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, razdel, lid, 1); // в избранное
            }

            @Override
            public void unLiked(LikeButton starButton) {
                Snackbar.make(views, context.getString(R.string.unfavorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, razdel, lid, 2); // из избранного
            }
        });

        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Snackbar.make(views, context.getString(R.string.like), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, razdel, lid, 1);
                txt_plus.setText(String.valueOf(plus + 1));

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Snackbar.make(views, context.getString(R.string.unlike), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, razdel, lid, 2);
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
}
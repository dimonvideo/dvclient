package com.dimonvideo.client.adater;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.dimonvideo.client.ui.main.Comments;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;

import java.util.List;

public class MainAdapterFull extends RecyclerView.Adapter<MainAdapterFull.ViewHolder> {

    private final Context context;

    //List to store all
    List<Feed> jsonFeed;

    //Constructor of this class
    public MainAdapterFull(List<Feed> jsonFeed, Context context){
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_full, parent, false);


        return new ViewHolder(v);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final Feed Feed =  jsonFeed.get(position);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean is_vuploader_play = sharedPrefs.getBoolean("dvc_vuploader_play",true);
        final boolean is_muzon_play = sharedPrefs.getBoolean("dvc_muzon_play",true);
        final boolean is_open_link = sharedPrefs.getBoolean("dvc_open_link", false);
        final boolean is_share_btn = sharedPrefs.getBoolean("dvc_btn_share", false);

        Glide.with(context).load(Feed.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.imageView);

        holder.imageView.setOnClickListener(v -> ButtonsActions.loadScreen(context, Feed.getImageUrl()));
        try {
        if (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL) && is_vuploader_play) holder.imageView.setOnClickListener(v -> ButtonsActions.PlayVideo(context, Feed.getLink()));
        } catch (Exception ignored) {
        }
        try {
            if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.MUZON_RAZDEL) && is_muzon_play))
                holder.imageView.setOnClickListener(v -> ButtonsActions.PlayVideo(context, Feed.getLink()));
        } catch (Exception ignored) {
        }

        holder.headersTitle.setText(Feed.getTitle());
        holder.subHeadersTitle.setText(Feed.getDate());
        holder.subHeadersTitle.append(" " + context.getString(R.string.by) + " " + Feed.getUser());
        try {
            holder.textViewText.setText(Html.fromHtml(Feed.getText(), null,  new MainAdapter.TagHandler()));
            holder.textViewText.setMovementMethod(LinkMovementMethod.getInstance());
        } catch (Throwable ignored) {
        }
        holder.downloadBtn.setVisibility(View.VISIBLE);
        holder.modBtn.setVisibility(View.VISIBLE);
        holder.txt_plus.setText(String.valueOf(Feed.getPlus()));

        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL))) {
            holder.mp4Btn.setVisibility(View.VISIBLE);
            holder.mp4Btn.setOnClickListener(view -> ButtonsActions.PlayVideo(context, Feed.getLink()));

        }
        // если нет размера файла
        if ((Feed.getSize() != null) || (Feed.getSize().startsWith("0"))) {
            holder.downloadBtn.setVisibility(View.GONE);
            holder.modBtn.setVisibility(View.GONE);
        } else holder.downloadBtn.setText(context.getString(R.string.download) + " " + Feed.getSize());

        // если нет mod
        if ((Feed.getMod() != null) && (Feed.getMod().startsWith("null"))) {
            holder.modBtn.setVisibility(View.GONE);
        }

        holder.downloadBtn.setOnClickListener(view -> DownloadFile.download(context, Feed.getLink(), com.dimonvideo.client.model.Feed.getRazdel()));

        holder.modBtn.setOnClickListener(view -> DownloadFile.download(context, Feed.getMod(), com.dimonvideo.client.model.Feed.getRazdel()));

        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.TRACKER_RAZDEL))) holder.commentsBtn.setVisibility(View.GONE);

        if (Feed.getComments() > 0) {
            String comText = context.getResources().getString(R.string.Comments) + ": " + Feed.getComments();
            holder.commentsBtn.setText(comText);
        }

        holder.commentsBtn.setOnClickListener(view -> {
            String comm_url = Config.COMMENTS_READS_URL + com.dimonvideo.client.model.Feed.getRazdel() + "&lid=" + Feed.getId() + "&min=";
            Intent intent = new Intent(context, Comments.class);
            intent.putExtra(Config.TAG_TITLE, Feed.getTitle());
            intent.putExtra(Config.TAG_LINK, comm_url);
            intent.putExtra(Config.TAG_ID, String.valueOf(Feed.getId()));
            intent.putExtra(Config.TAG_RAZDEL, com.dimonvideo.client.model.Feed.getRazdel());
            context.startActivity(intent);
        });

        holder.likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.like), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1);
                holder.txt_plus.setText(String.valueOf(Feed.getPlus()+1));

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.unlike), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 2);
                holder.txt_plus.setText(String.valueOf(Feed.getPlus()-1));
            }
        });

        holder.starButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton starButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1); // в избранное
            }

            @Override
            public void unLiked(LikeButton starButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.unfavorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 2); // из избранного
            }
        });
        // dialog menu
        holder.itemView.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });
        holder.textViewText.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });

        // share menu
        holder.btn_share.setVisibility(View.VISIBLE);
        holder.btn_share.setOnClickListener(view -> {

            holder.url = Config.BASE_URL + "/" + com.dimonvideo.client.model.Feed.getRazdel() + "/" + Feed.getId();
            if (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
                holder.url = Config.BASE_URL + "/" + Feed.getId() + "-news.html";

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, holder.url);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, Feed.getTitle());

            try {
                context.startActivity(shareIntent);
            } catch (Throwable ignored) {
            }

        });

        if (is_share_btn) holder.btn_share.setVisibility(View.GONE);


    }

    // dialog
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void show_dialog(ViewHolder holder, final int position, Context context){
        final CharSequence[] items = {context.getString(R.string.menu_share_title), context.getString(R.string.action_open),
                context.getString(R.string.menu_fav), context.getString(R.string.action_like),
                context.getString(R.string.action_screen), context.getString(R.string.download), context.getString(R.string.copy_listtext)};
        final Feed Feed =  jsonFeed.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.BASE_URL + "/" + com.dimonvideo.client.model.Feed.getRazdel() + "/" + Feed.getId();
        if (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
            holder.url = Config.BASE_URL + "/" + Feed.getId() + "-news.html";

        holder.myClipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(Feed.getTitle());
        builder.setItems(items, (dialog, item) -> {
            if (item == 0) { // share
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, holder.url);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, Feed.getTitle());
                try {
                    context.startActivity(shareIntent);
                } catch (Throwable ignored) {
                }
            }
            if (item == 1) { // browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                try {
                    context.startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
            }
            if (item == 2) { // fav
                ButtonsActions.add_to_fav_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1);
            }
            if (item == 3) { // like
                ButtonsActions.like_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1);
            }
            if (item == 4) { // screen
                ButtonsActions.loadScreen(context, Feed.getImageUrl());
            }
            if (item == 5) { // download
                DownloadFile.download(context, Feed.getLink(), com.dimonvideo.client.model.Feed.getRazdel());
            }
            if (item == 6) { // copy text
                try { holder.myClip = ClipData.newPlainText("text", Html.fromHtml(Feed.getFull_text()).toString());
                holder.myClipboard.setPrimaryClip(holder.myClip);
                } catch (Throwable ignored) {
                }
                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
            }

        });
        builder.show();
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public ImageView imageView;
        public TextView headersTitle, subHeadersTitle, txt_plus;
        public TextView textViewText;
        public Button downloadBtn, modBtn, commentsBtn, mp4Btn, btn_share;
        public LikeButton likeButton, starButton;
        public ProgressBar progressBar;
        public String url;
        public ClipboardManager myClipboard;
        public ClipData myClip;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.main_imageview_placeholder);
            headersTitle = itemView.findViewById(R.id.headers_title);
            subHeadersTitle = itemView.findViewById(R.id.sub_headers_title);
            textViewText = itemView.findViewById(R.id.listtext);
            downloadBtn = itemView.findViewById(R.id.btn_download);
            modBtn = itemView.findViewById(R.id.btn_mod);
            commentsBtn = itemView.findViewById(R.id.btn_comment);
            mp4Btn = itemView.findViewById(R.id.btn_mp4);
            likeButton = itemView.findViewById(R.id.thumb_button);
            starButton = itemView.findViewById(R.id.star_button);
            txt_plus = itemView.findViewById(R.id.txt_plus);
            progressBar = itemView.findViewById(R.id.progressBar);
            btn_share = itemView.findViewById(R.id.btn_share);

        }

    }
}

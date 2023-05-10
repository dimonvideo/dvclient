package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.db.Provider;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.ui.main.MainFragmentCommentsFile;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.ui.main.MainFragmentViewFile;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;
import com.like.LikeButton;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class AdapterMainRazdelInfo extends RecyclerView.Adapter<AdapterMainRazdelInfo.ViewHolder> {

    private Context context;

    //List to store all
    private final List<Feed> jsonFeed;

    //Constructor of this class
    public AdapterMainRazdelInfo(List<Feed> jsonFeed, Context context) {
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == 1) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_gallery, parent, false);
            context = parent.getContext();
            return new ViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row, parent, false);
        context = parent.getContext();
        return new ViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {

        Feed feed = jsonFeed.get(position);

        if ((feed.getRazdel() != null) && ((feed.getRazdel().equals(Config.GALLERY_RAZDEL)) || (feed.getRazdel().equals(Config.VUPLOADER_RAZDEL)))) {
            return 1;
        } else {
            return 0;
        }

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        Feed feed = jsonFeed.get(position);
        final boolean is_vuploader_play = AppController.getInstance().isVuploaderPlay();
        final boolean is_muzon_play = AppController.getInstance().isMuzonPlay();
        final boolean is_open_link = AppController.getInstance().isOpenLinks();
        final boolean is_vuploader_play_listtext = AppController.getInstance().isVuploaderPlayListtext();
        final boolean is_share_btn = AppController.getInstance().isShareBtn();

        String razdel = feed.getRazdel();
        int lid = feed.getId();

        holder.status_logo.setImageResource(R.drawable.ic_status_green);
        try {
            int status;
            Cursor cursor = Provider.getOneData(String.valueOf(feed.getId()), feed.getRazdel());
            if (cursor != null) {
                status = cursor.getInt(2);
                if (status == 1) holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                cursor.close();
            }

        } catch (Throwable ignored) {
        }

        //Loading image from url
        Glide.with(context).load(feed.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.bitmapTransform(new RoundedCorners(14))).into(holder.imageView);

        holder.textViewTitle.setText(feed.getTitle());
        // html textview
        try {
            URLImageParser parser = new URLImageParser(holder.textViewText);
            Spanned spanned = Html.fromHtml(feed.getText(), parser, new AdapterMainRazdel.TagHandler());
            holder.textViewText.setText(spanned);
            holder.textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context, feed.getRazdel());
                }
            });
        } catch (Throwable ignored) {
        }

        holder.textViewDate.setText(feed.getDate());
        holder.textViewCategory.setText(feed.getCategory());
        holder.textViewComments.setText(String.valueOf(feed.getComments()));
        holder.textViewComments.setVisibility(View.VISIBLE);
        holder.rating_logo.setVisibility(View.VISIBLE);
        holder.textViewName.setText(feed.getUser());
        holder.textViewComments.setOnClickListener(view -> {
            String comm_url = Config.COMMENTS_READS_URL + feed.getRazdel() + "&lid=" + feed.getId() + "&min=";
            MainFragmentCommentsFile fragment = new MainFragmentCommentsFile();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_TITLE, feed.getTitle());
            bundle.putString(Config.TAG_ID, String.valueOf(lid));
            bundle.putString(Config.TAG_LINK, comm_url);
            bundle.putString(Config.TAG_RAZDEL, razdel);
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity)context).getSupportFragmentManager(), "MainFragmentCommentsFile");


        });
        if (feed.getComments() == 0) {
            holder.textViewComments.setVisibility(View.INVISIBLE);
            holder.rating_logo.setVisibility(View.INVISIBLE);
        }
        if (feed.getFav() > 0) {
            holder.fav_star.setVisibility(View.VISIBLE);
            holder.fav_star.setOnClickListener(v -> removeFav(position));
        }
        holder.textViewHits.setText(String.valueOf(feed.getHits()));

        holder.itemView.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            openFile(razdel, lid, feed.getComments(), feed.getTitle(),
                    feed.getUser(), feed.getPlus(), feed.getLink(), feed.getMod(), feed.getSize(), feed.getImageUrl(), feed.getText(), feed.getDate(), feed.getCategory(), feed.getStatus());


        });

        holder.textViewText.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            openFile(razdel, lid, feed.getComments(), feed.getTitle(),
                    feed.getUser(), feed.getPlus(), feed.getLink(), feed.getMod(), feed.getSize(), feed.getImageUrl(), feed.getText(), feed.getDate(), feed.getCategory(), feed.getStatus());
        });

        holder.imageView.setOnClickListener(v -> ButtonsActions.loadScreen(context, feed.getImageUrl()));

        try {
            if ((feed.getRazdel() != null) && (feed.getRazdel().equals(Config.VUPLOADER_RAZDEL) && is_vuploader_play))
                holder.imageView.setOnClickListener(v -> ButtonsActions.PlayVideo(context, feed.getLink()));
        } catch (Exception ignored) {
        }
        try {
            if ((feed.getRazdel() != null) && (feed.getRazdel().equals(Config.MUZON_RAZDEL) && is_muzon_play))
                holder.imageView.setOnClickListener(v -> ButtonsActions.PlayVideo(context, feed.getLink()));
        } catch (Exception ignored) {
        }
        if (feed.getMin() > 0) {
            holder.btn_comms.setVisibility(View.GONE);
            holder.txt_plus.setVisibility(View.GONE);
            holder.likeButton.setVisibility(View.GONE);
            holder.starButton.setVisibility(View.GONE);
            holder.btn_mp4.setVisibility(View.GONE);
            holder.btn_download.setVisibility(View.GONE);
            holder.btn_share.setVisibility(View.GONE);
            holder.btn_mod.setVisibility(View.GONE);
            holder.btn_odob.setVisibility(View.GONE);
        }

        if ((feed.getStatus() == 0) && (AppController.getInstance().isUserGroup() <= 2)){
            holder.btn_odob.setVisibility(View.VISIBLE);
            holder.btn_odob.setOnClickListener(v -> {
                NetworkUtils.getOdob(razdel, lid);
                jsonFeed.remove(position);
                notifyItemRemoved(position);
            });
        }

        // dialog menu
        holder.itemView.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });
        holder.imageView.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });
        holder.textViewText.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });

        // скачать
        holder.small_download.setOnClickListener(view -> DownloadFile.download(context, feed.getLink(), razdel));
        // если нет размера файла
        if ((feed.getSize() == null) || (feed.getSize().startsWith("0"))) {
            holder.small_download.setVisibility(View.GONE);
        } else {
            holder.small_download.setVisibility(View.VISIBLE);
        }
        // поделится
        try {
            if (!is_share_btn) holder.small_share.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }

        holder.small_share.setOnClickListener(view -> {

            String url = Config.BASE_URL + "/" + razdel + "/" + lid;
            if (razdel.equals(Config.COMMENTS_RAZDEL))
                url = Config.BASE_URL + "/" + lid + "-news.html";

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, feed.getTitle());

            try {
                context.startActivity(shareIntent);
            } catch (Throwable ignored) {
            }

        });

    }

    private void openFile(String razdel, int lid, int comments, String title, String user, int plus, String link, String mod, String size, String imageUrl,
                          String text, String date, String category, int status) {
            MainFragmentViewFile fragment = new MainFragmentViewFile();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_RAZDEL, razdel);
            bundle.putString(Config.TAG_TITLE, title);
            bundle.putString(Config.TAG_ID, String.valueOf(lid));
            bundle.putString(Config.TAG_DATE, date);
            bundle.putString(Config.TAG_CATEGORY, category);
            bundle.putInt(Config.TAG_PLUS, plus);
            bundle.putString(Config.TAG_USER, user);
            bundle.putString(Config.TAG_TEXT, text);
            bundle.putString(Config.TAG_IMAGE_URL, imageUrl);
            bundle.putString(Config.TAG_MOD, mod);
            bundle.putInt(Config.TAG_COMMENTS, comments);
            bundle.putInt(Config.TAG_STATUS, status);
            bundle.putString(Config.TAG_LINK, link);
            bundle.putString(Config.TAG_SIZE, size);
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity)context).getSupportFragmentManager(), "MainFragmentViewFile");

    }

    // dialog
    private void show_dialog(ViewHolder holder, final int position, Context context) {
        Feed feed = jsonFeed.get(position);
        final CharSequence[] items = {context.getString(R.string.menu_share_title), context.getString(R.string.action_open),
                context.getString(R.string.menu_fav), context.getString(R.string.action_like), context.getString(R.string.action_screen),
                context.getString(R.string.download), context.getString(R.string.copy_listtext)};
        final Feed Feed = jsonFeed.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.BASE_URL + "/" + feed.getRazdel() + "/" + Feed.getId();
        if (feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
            holder.url = Config.BASE_URL + "/" + Feed.getId() + "-news.html";

        holder.myClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

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
                ButtonsActions.add_to_fav_file(context, feed.getRazdel(), Feed.getId(), 1);
            }
            if (item == 3) { // like
                ButtonsActions.like_file(context, feed.getRazdel(), Feed.getId(), 1);
            }
            if (item == 4) { // screen
                ButtonsActions.loadScreen(context, feed.getImageUrl());
            }
            if (item == 5) { // download
                DownloadFile.download(context, feed.getLink(), feed.getRazdel());
            }
            if (item == 6) { // copy text
                try { holder.myClip = ClipData.newPlainText("text", Html.fromHtml(feed.getFull_text()).toString());
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

    // swipe to remove favorites
    @SuppressLint("NotifyDataSetChanged")
    public void removeFav(int position) {
        Feed feed = jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyDataSetChanged();
        ButtonsActions.add_to_fav_file(context, feed.getRazdel(), feed.getId(), 2);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewComments, textViewCategory, textViewHits, txt_plus, textViewName;
        public ImageView imageView, rating_logo, status_logo, fav_star, small_share, small_download;
        public TextView textViewText;
        public String url;
        public Button btn_comms, btn_download, btn_mod, btn_mp4, btn_share, btn_odob;
        public ProgressBar progressBar;
        public LikeButton likeButton, starButton;
        public LinearLayout name;
        public ClipboardManager myClipboard;
        public ClipData myClip;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
            rating_logo = itemView.findViewById(R.id.rating_logo);
            fav_star = itemView.findViewById(R.id.fav);
            status_logo = itemView.findViewById(R.id.status);
            textViewTitle = itemView.findViewById(R.id.title);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewName = itemView.findViewById(R.id.by_name);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewHits = itemView.findViewById(R.id.views_count);
            btn_comms = itemView.findViewById(R.id.btn_comment);
            btn_download = itemView.findViewById(R.id.btn_download);
            btn_mod = itemView.findViewById(R.id.btn_mod);
            btn_share = itemView.findViewById(R.id.btn_share);
            btn_mp4 = itemView.findViewById(R.id.btn_mp4);
            btn_odob = itemView.findViewById(R.id.btn_odob);
            progressBar = itemView.findViewById(R.id.progressBar);
            likeButton = itemView.findViewById(R.id.thumb_button);
            starButton = itemView.findViewById(R.id.star_button);
            txt_plus = itemView.findViewById(R.id.txt_plus);
            name = itemView.findViewById(R.id.name_layout);
            small_share = itemView.findViewById(R.id.small_share);
            small_download = itemView.findViewById(R.id.small_download);
        }

    }

}

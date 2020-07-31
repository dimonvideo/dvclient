package com.dimonvideo.client.adater;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dimonvideo.client.AllContent;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.NetworkUtils;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.htmltextview.OnClickATagListener;

import java.util.Calendar;
import java.util.List;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private Context context;

    //List to store all
    List<Feed> jsonFeed;

    //Constructor of this class
    public MainAdapter(List<Feed> jsonFeed, Context context) {
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row, parent, false);


        return new ViewHolder(v);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final Feed Feed = jsonFeed.get(position);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean is_vuploader_play = sharedPrefs.getBoolean("dvc_vuploader_play", true);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        if (Feed.getTime() > cal.getTimeInMillis() / 1000L) {
            holder.status_logo.setImageResource(R.drawable.ic_status_green);
        }
        //Loading image from url
        Glide.with(context).load(Feed.getImageUrl()).into(holder.imageView);

        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewText.setHtml(Feed.getText(), new HtmlHttpImageGetter(holder.textViewText));
        holder.textViewDate.setText(Feed.getDate());
        holder.textViewCategory.setText(Feed.getCategory());
        holder.textViewComments.setText(String.valueOf(Feed.getComments()));
        holder.textViewComments.setVisibility(View.VISIBLE);
        holder.rating_logo.setVisibility(View.VISIBLE);
        if (Feed.getComments() == 0) {
            holder.textViewComments.setVisibility(View.INVISIBLE);
            holder.rating_logo.setVisibility(View.INVISIBLE);
        }
        if (Feed.getFav() > 0) {
            holder.fav_star.setVisibility(View.VISIBLE);
            holder.fav_star.setOnClickListener(v -> removeFav(position));
        }
        holder.textViewHits.setText(String.valueOf(Feed.getHits()));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AllContent.class);
            intent.putExtra(Config.TAG_TITLE, Feed.getTitle());
            intent.putExtra(Config.TAG_ID, String.valueOf(Feed.getId()));
            intent.putExtra(Config.TAG_PLUS, String.valueOf(Feed.getPlus()));
            intent.putExtra(Config.TAG_DATE, Feed.getDate());
            intent.putExtra(Config.TAG_HEADERS, Feed.getHeaders());
            intent.putExtra(Config.TAG_IMAGE_URL, Feed.getImageUrl());
            intent.putExtra(Config.TAG_CATEGORY, Feed.getCategory());
            intent.putExtra(Config.TAG_RAZDEL, Feed.getRazdel());
            intent.putExtra(Config.TAG_USER, Feed.getUser());
            intent.putExtra(Config.TAG_SIZE, Feed.getSize());
            intent.putExtra(Config.TAG_LINK, Feed.getLink());
            intent.putExtra(Config.TAG_MOD, Feed.getMod());
            intent.putExtra(Config.TAG_HITS, String.valueOf(Feed.getHits()));
            intent.putExtra(Config.TAG_COMMENTS, String.valueOf(Feed.getComments()));
            context.startActivity(intent);
        });

        holder.imageView.setOnClickListener(v -> ButtonsActions.loadScreen(context, Feed.getImageUrl()));
        if (Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL) && is_vuploader_play)
            holder.imageView.setOnClickListener(v -> ButtonsActions.PlayVideo(context, Feed.getLink()));

        if (Feed.getMin() > 0) {
            holder.btn_comms.setVisibility(View.GONE);
            holder.txt_plus.setVisibility(View.GONE);
            holder.likeButton.setVisibility(View.GONE);
            holder.starButton.setVisibility(View.GONE);
            holder.btn_mp4.setVisibility(View.GONE);
            holder.btn_download.setVisibility(View.GONE);
            holder.btn_mod.setVisibility(View.GONE);
        }

        holder.textViewText.setOnClickListener(v -> {
            holder.txt_plus.setVisibility(View.VISIBLE);
            holder.likeButton.setVisibility(View.VISIBLE);
            holder.starButton.setVisibility(View.VISIBLE);
            holder.txt_plus.setText(String.valueOf(Feed.getPlus()));
            holder.textViewText.setHtml(Feed.getFull_text(), new HtmlHttpImageGetter(holder.textViewText));

            // comments
            holder.btn_comms.setVisibility(View.VISIBLE);
            holder.btn_comms.setOnClickListener(view -> {
                String comm_url = Config.COMMENTS_READS_URL + Feed.getRazdel() + "&lid=" + Feed.getId();
                ButtonsActions.loadComments(context, comm_url, holder.progressBar);
            });
            if (Feed.getComments() > 0) {
                String comText = context.getResources().getString(R.string.Comments) + " " + Feed.getComments();
                holder.btn_comms.setText(comText);
            } else {
                String comText = context.getResources().getString(R.string.Comments) + " " + 0;
                holder.btn_comms.setText(comText);
            }
            // смотреть онлайн
            if (Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL)) {
                holder.btn_mp4.setVisibility(View.VISIBLE);
                holder.btn_mp4.setOnClickListener(view -> ButtonsActions.PlayVideo(context, Feed.getLink()));
            }
            // если нет размера файла
            if (Feed.getSize().startsWith("0")) {
                holder.btn_download.setVisibility(View.GONE);
                holder.btn_mod.setVisibility(View.GONE);
            } else {
                holder.btn_download.setText(context.getString(R.string.download) + " " + Feed.getSize());
                holder.btn_download.setVisibility(View.VISIBLE);
            }
            // если нет mod
            if (!Feed.getMod().startsWith("null")) {
                holder.btn_mod.setVisibility(View.VISIBLE);
                holder.btn_mod.setOnClickListener(view -> DownloadFile.download(context, Feed.getMod()));
            }
        });

        // download
        holder.btn_download.setOnClickListener(view -> DownloadFile.download(context, Feed.getLink()));

        // like and favorites
        holder.starButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton starButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, Feed.getRazdel(), Feed.getId(), 1); // в избранное
            }

            @Override
            public void unLiked(LikeButton starButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.unfavorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, Feed.getRazdel(), Feed.getId(), 2); // из избранного
            }
        });

        holder.likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.like), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, Feed.getRazdel(), Feed.getId(), 1);
                holder.txt_plus.setText(String.valueOf(Feed.getPlus() + 1));

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.unlike), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, Feed.getRazdel(), Feed.getId(), 2);
                holder.txt_plus.setText(String.valueOf(Feed.getPlus() - 1));
            }
        });

        holder.textViewText.setOnLongClickListener(view -> {
            final CharSequence[] items = {context.getString(R.string.menu_share_title), context.getString(R.string.action_open), context.getString(R.string.menu_fav), context.getString(R.string.action_like), context.getString(R.string.action_screen), context.getString(R.string.download)};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            holder.url = Config.BASE_URL + "/" + Feed.getRazdel() + "/" + Feed.getId();
            if (Feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
                holder.url = Config.BASE_URL + "/" + Feed.getId() + "-news.html";

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
                    ButtonsActions.add_to_fav_file(context, Feed.getRazdel(), Feed.getId(), 1);
                }
                if (item == 3) { // like
                    ButtonsActions.like_file(context, Feed.getRazdel(), Feed.getId(), 1);
                }
                if (item == 4) { // screen
                    ButtonsActions.loadScreen(context, Feed.getImageUrl());
                }
                if (item == 5) { // download
                    DownloadFile.download(context, Feed.getLink());
                }
            });
            builder.show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    // swipe to remove favorites
    public void removeFav(int position) {
        final Feed Feed = jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyDataSetChanged();
        ButtonsActions.add_to_fav_file(context, Feed.getRazdel(), Feed.getId(), 2);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewComments, textViewCategory, textViewHits, txt_plus;
        public ImageView imageView, rating_logo, status_logo, fav_star;
        public HtmlTextView textViewText;
        public String url;
        public Button btn_comms, btn_download, btn_mod, btn_mp4;
        public ProgressBar progressBar;
        public LikeButton likeButton, starButton;

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
            textViewComments = itemView.findViewById(R.id.rating);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewHits = itemView.findViewById(R.id.views_count);
            btn_comms = itemView.findViewById(R.id.btn_comment);
            btn_download = itemView.findViewById(R.id.btn_download);
            btn_mod = itemView.findViewById(R.id.btn_mod);
            btn_mp4 = itemView.findViewById(R.id.btn_mp4);
            progressBar = itemView.findViewById(R.id.progressBar);
            likeButton = itemView.findViewById(R.id.thumb_button);
            starButton = itemView.findViewById(R.id.star_button);
            txt_plus = itemView.findViewById(R.id.txt_plus);
        }

    }
}

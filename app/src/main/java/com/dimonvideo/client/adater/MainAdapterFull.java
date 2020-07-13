package com.dimonvideo.client.adater;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.List;

public class MainAdapterFull extends RecyclerView.Adapter<MainAdapterFull.ViewHolder> {

    private Context context;

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

        Glide.with(context).load(Feed.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.imageView);

        holder.imageView.setOnClickListener(v -> ButtonsActions.loadScreen(context, Feed.getImageUrl()));
        if (Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL)) holder.imageView.setOnClickListener(v -> ButtonsActions.PlayVideo(context, Feed.getLink()));

        holder.headersTitle.setText(Feed.getTitle());
        holder.subHeadersTitle.setText(Feed.getDate());
        holder.subHeadersTitle.append(" " + context.getString(R.string.by) + " " + Feed.getUser());
        holder.textViewText.setHtml(Feed.getText(), new HtmlHttpImageGetter(holder.textViewText));
        holder.downloadBtn.setVisibility(View.VISIBLE);
        holder.modBtn.setVisibility(View.VISIBLE);
        holder.txt_plus.setText(String.valueOf(Feed.getPlus()));

        if (Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL)) {
            holder.mp4Btn.setVisibility(View.VISIBLE);
            holder.mp4Btn.setOnClickListener(view -> ButtonsActions.PlayVideo(context, Feed.getLink()));

        }
        // если нет размера файла
        if (Feed.getSize().startsWith("0")) {
            holder.downloadBtn.setVisibility(View.GONE);
            holder.modBtn.setVisibility(View.GONE);
        } else holder.downloadBtn.setText(context.getString(R.string.download) + " " + Feed.getSize());

        // если нет mod
        if (Feed.getMod().startsWith("null")) {
            holder.modBtn.setVisibility(View.GONE);
        }

        holder.downloadBtn.setOnClickListener(view -> {

            DownloadFile.download(context, Feed.getLink());

        });

        holder.modBtn.setOnClickListener(view -> {

            DownloadFile.download(context, Feed.getMod());

        });

        if (Feed.getComments() > 0) {
            String comText = context.getResources().getString(R.string.Comments) + ": " + Feed.getComments();
            holder.commentsBtn.setText(comText);
        }

        holder.commentsBtn.setOnClickListener(view -> {
            String comm_url = Config.COMMENTS_READS_URL + Feed.getRazdel() + "&lid=" + Feed.getId();
            ButtonsActions.loadComments(context, comm_url);
        });

        holder.likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.like), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, Feed.getRazdel(), Feed.getId(), 1);
                holder.txt_plus.setText(String.valueOf(Feed.getPlus()+1));

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.unlike), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, Feed.getRazdel(), Feed.getId(), 2);
                holder.txt_plus.setText(String.valueOf(Feed.getPlus()-1));
            }
        });

        holder.starButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton starButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void unLiked(LikeButton starButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.unfavorites_btn), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public ImageView imageView;
        public TextView headersTitle, subHeadersTitle, txt_plus;
        public HtmlTextView textViewText;
        public Button downloadBtn, modBtn, commentsBtn, mp4Btn;
        public LikeButton likeButton, starButton;

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

        }

    }
}

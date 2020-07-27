package com.dimonvideo.client.adater;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.dimonvideo.client.AllContent;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.ui.forum.ForumFragmentPosts;
import com.dimonvideo.client.ui.main.MainFragmentHorizontal;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.CustomVolleyRequest;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.NetworkUtils;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.Calendar;
import java.util.List;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private Context context;

    //List to store all
    List<Feed> jsonFeed;

    //Constructor of this class
    public MainAdapter(List<Feed> jsonFeed, Context context){
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
        final Feed Feed =  jsonFeed.get(position);
        int min = Feed.getMin();
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
        //Imageloader to load image
        ImageLoader imageLoader = CustomVolleyRequest.getInstance(context).getImageLoader();
        imageLoader.get(Feed.getImageUrl(), ImageLoader.getImageListener(holder.imageView, R.drawable.ic_menu_gallery, android.R.drawable.ic_dialog_alert));
        holder.imageView.setImageUrl(Feed.getImageUrl(), imageLoader);
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
        holder.textViewHits.setText(String.valueOf(Feed.getHits()));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AllContent.class);
            intent.putExtra(Config.TAG_TITLE, Feed.getTitle());
            intent.putExtra(Config.TAG_ID, String.valueOf(Feed.getId()));
            intent.putExtra(Config.TAG_PLUS, String.valueOf(Feed.getPlus()));
            intent.putExtra(Config.TAG_DATE,Feed.getDate());
            intent.putExtra(Config.TAG_HEADERS,Feed.getHeaders());
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
        holder.textViewText.setOnClickListener(v -> {
            Intent intent = new Intent(context, AllContent.class);
            intent.putExtra(Config.TAG_TITLE, Feed.getTitle());
            intent.putExtra(Config.TAG_ID, String.valueOf(Feed.getId()));
            intent.putExtra(Config.TAG_PLUS, String.valueOf(Feed.getPlus()));
            intent.putExtra(Config.TAG_DATE,Feed.getDate());
            intent.putExtra(Config.TAG_HEADERS,Feed.getHeaders());
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
        holder.textViewText.setOnLongClickListener(view -> {
            final CharSequence[] items = {context.getString(R.string.action_open), context.getString(R.string.action_like), context.getString(R.string.action_screen), context.getString(R.string.download)};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            holder.url = Config.BASE_URL + "/" + Feed.getRazdel() + "/" + Feed.getId();
            if (Feed.getRazdel().equals(Config.COMMENTS_RAZDEL)) holder.url = Config.BASE_URL + "/" + Feed.getId() + "-news.html";

            builder.setTitle(Feed.getTitle());
            builder.setItems(items, (dialog, item) -> {
                if (item == 0) { // browser
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                    try {
                        context.startActivity(browserIntent);
                    } catch (Throwable ignored) {
                    }
                }
                if (item == 1) { // like
                    ButtonsActions.like_file(context, Feed.getRazdel(), Feed.getId(), 1);
                }
                if (item == 2) { // screen
                    ButtonsActions.loadScreen(context, Feed.getImageUrl());
                }
                if (item == 3) { // download
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
        final Feed Feed =  jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyDataSetChanged();
        ButtonsActions.add_to_fav_file(context, Feed.getRazdel(), Feed.getId(), 2);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public NetworkImageView imageView;
        public TextView textViewTitle, textViewDate, textViewComments, textViewCategory, textViewHits;
        public ImageView rating_logo, status_logo;
        public HtmlTextView textViewText;
        public String url;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
            rating_logo = itemView.findViewById(R.id.rating_logo);
            status_logo = itemView.findViewById(R.id.status);
            textViewTitle = itemView.findViewById(R.id.title);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewHits = itemView.findViewById(R.id.views_count);

        }

    }
}

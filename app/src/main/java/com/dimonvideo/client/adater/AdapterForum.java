/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.ui.forum.ForumFragmentPosts;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;

import java.util.Calendar;
import java.util.List;

public class AdapterForum extends RecyclerView.Adapter<AdapterForum.ViewHolder> {

    private final Context context;

    //List to store all
    List<FeedForum> jsonFeed;

    //Constructor of this class
    public AdapterForum(List<FeedForum> jsonFeed, Context context){
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_topics, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final FeedForum Feed =  jsonFeed.get(holder.getBindingAdapterPosition());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        if (Feed.getTime() > cal.getTimeInMillis() / 1000L) {
            holder.status_logo.setImageResource(R.drawable.ic_status_green);
        }

        if (Feed.getComments() == 0) {
            holder.textViewComments.setVisibility(View.INVISIBLE);
            holder.rating_logo.setVisibility(View.INVISIBLE);
        }

        holder.textViewHits.setText(String.valueOf(Feed.getHits()));
        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewText.setText(Feed.getText());
        holder.textViewDate.setText(Feed.getDate());
        holder.textViewName.setText(Feed.getLast_poster_name());
        holder.textViewCategory.setText(Feed.getCategory());
        holder.textViewComments.setText(String.valueOf(Feed.getComments()));

        // Массив всех нужных TextView
        TextView[] textViews = {
                holder.textViewTitle,
                holder.textViewText,
                holder.textViewDate,
                holder.textViewCategory,
                holder.textViewComments,
                holder.textViewName,
                holder.textViewHits
        };

        // Массивы размеров для каждого режима
        float[] sizesSmallest = {14, 13, 12, 12, 12, 12, 12};
        float[] sizesSmall    = {16, 15, 14, 14, 14, 14, 14};
        float[] sizesNormal   = {18, 17, 16, 16, 16, 16, 16};
        float[] sizesLarge    = {20, 19, 18, 18, 18, 18, 18};
        float[] sizesLargest  = {24, 23, 22, 22, 22, 22, 22};

        float[] selectedSizes;

        switch (AppController.getInstance().isFontSize()) {
            case "smallest": selectedSizes = sizesSmallest; break;
            case "small":    selectedSizes = sizesSmall;    break;
            case "large":    selectedSizes = sizesLarge;    break;
            case "largest":  selectedSizes = sizesLargest;  break;
            default:         selectedSizes = sizesNormal;   break;
        }
        for (int i = 0; i < textViews.length; i++) {
            textViews[i].setTextSize(selectedSizes[i]);
        }



        holder.textViewText.setTextColor(context.getColor(R.color.year));
        holder.textViewName.setTextColor(context.getColor(R.color.year));
        holder.textViewComments.setVisibility(View.VISIBLE);
        holder.rating_logo.setVisibility(View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            ForumFragmentPosts fragment = new ForumFragmentPosts();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_TITLE, Feed.getTitle());
            bundle.putString(Config.TAG_ID, String.valueOf(Feed.getId()));
            bundle.putString(Config.TAG_RAZDEL, "8");
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity)context).getSupportFragmentManager(), "ForumFragmentPosts");

        });
        if (Feed.getFav() > 0) {
            holder.fav_star.setVisibility(View.VISIBLE);
            holder.fav_star.setOnClickListener(v -> removeFav(holder.getBindingAdapterPosition()));
        }
        holder.itemView.setOnLongClickListener(view -> {
            final CharSequence[] items = {context.getString(R.string.menu_share_title), context.getString(R.string.action_open), context.getString(R.string.menu_fav)};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            holder.url = Config.WRITE_URL + "/forum/topic_" + Feed.getId();


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
                    ButtonsActions.add_to_fav_file(context, "forum", Feed.getId(), 1);
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
    @SuppressLint("NotifyDataSetChanged")
    public void removeFav(int position) {
        final FeedForum Feed = jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyDataSetChanged();
        ButtonsActions.add_to_fav_file(context, "forum", Feed.getId(), 2);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewText, textViewDate, textViewComments, textViewCategory, textViewHits, textViewName;
        public ImageView rating_logo, status_logo, fav_star;
        public String url;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            rating_logo = itemView.findViewById(R.id.rating_logo);
            status_logo = itemView.findViewById(R.id.status);
            textViewTitle = itemView.findViewById(R.id.title);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewHits = itemView.findViewById(R.id.views_count);
            textViewName = itemView.findViewById(R.id.names);
            fav_star = itemView.findViewById(R.id.fav);

        }

    }
}

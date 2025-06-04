/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.adater;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.ui.forum.ForumFragment;
import com.dimonvideo.client.ui.forum.ForumFragmentTopics;
import com.dimonvideo.client.util.AppController;

import java.util.Calendar;
import java.util.List;

public class AdapterForumCategory extends RecyclerView.Adapter<AdapterForumCategory.ViewHolder> {

    private final Context context;

    //List to store all
    List<FeedForum> jsonFeed;

    //Constructor of this class
    public AdapterForumCategory(List<FeedForum> jsonFeed, Context context){
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

        //Getting the particular item from the list
        final FeedForum Feed =  jsonFeed.get(holder.getBindingAdapterPosition());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        try {
            if (Feed.getTime() > cal.getTimeInMillis() / 1000L) holder.status_logo.setImageResource(R.drawable.ic_status_green);
            } catch (Throwable ignored) {

        }

        if (Feed.getComments() == 0) {
            holder.textViewComments.setVisibility(View.INVISIBLE);
            holder.rating_logo.setVisibility(View.INVISIBLE);
        }

        holder.textViewHits.setText(String.valueOf(Feed.getHits()));
        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewDate.setText(Feed.getDate());
        holder.textViewCategory.setText(Feed.getCategory());
        holder.textViewComments.setText(String.valueOf(Feed.getComments()));

        // Массив всех нужных TextView
        TextView[] textViews = {
                holder.textViewTitle,
                holder.textViewText,
                holder.textViewDate,
                holder.textViewCategory,
                holder.textViewComments,
                holder.textViewHits
        };

        // Массивы размеров для каждого режима
        float[] sizesSmallest = {14, 13, 12, 12, 12, 12};
        float[] sizesSmall    = {16, 15, 14, 14, 14, 14};
        float[] sizesNormal   = {18, 17, 16, 16, 16, 16};
        float[] sizesLarge    = {20, 19, 18, 18, 18, 18};
        float[] sizesLargest  = {24, 23, 22, 22, 22, 22};

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
        holder.textViewNames.setTextColor(context.getColor(R.color.year));
        holder.textViewText.setVisibility(View.GONE);
        holder.textViewNames.setVisibility(View.GONE);
        holder.textViewComments.setVisibility(View.VISIBLE);
        holder.rating_logo.setVisibility(View.VISIBLE);



        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = new ForumFragmentTopics();
            Bundle bundle = new Bundle();
            bundle.putInt(Config.TAG_ID, Feed.getId());
            bundle.putString(Config.TAG_CATEGORY, Feed.getTitle());
            bundle.putString(Config.TAG_RAZDEL, "8");
            fragment.setArguments(bundle);
            FragmentManager fragmentManager = ((AppCompatActivity)context).getSupportFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.addToBackStack(fragment.toString());
            ft.add(R.id.container_frag, fragment);
            ForumFragment.viewPager.setCurrentItem(0, true);
            ft.commit();
        });

    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewText, textViewDate, textViewComments, textViewCategory, textViewHits, textViewNames;
        public ImageView rating_logo, status_logo;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            rating_logo = itemView.findViewById(R.id.rating_logo);
            status_logo = itemView.findViewById(R.id.status);
            textViewTitle = itemView.findViewById(R.id.title);
            textViewCategory = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewText = itemView.findViewById(R.id.category);
            textViewHits = itemView.findViewById(R.id.views_count);
            textViewNames = itemView.findViewById(R.id.names);

        }

    }
}

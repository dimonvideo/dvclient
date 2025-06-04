/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.adater;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedCats;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class AdapterMainCategories extends RecyclerView.Adapter<AdapterMainCategories.ViewHolder> {

    private final Context context;
    List<FeedCats> jsonFeed;
    private String razdel;

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
    }

    public AdapterMainCategories(List<FeedCats> jsonFeed, Context context){
        super();
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_categories, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final FeedCats feed =  jsonFeed.get(holder.getBindingAdapterPosition());

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        EventBus.getDefault().postSticky(new MessageEvent(razdel, null, null, null, null, null));

        holder.textViewTitle.setText(feed.getTitle());
        holder.textViewCategory.setText(String.valueOf(feed.getCount()));

        // Массив всех нужных TextView
        TextView[] textViews = {
                holder.textViewTitle,
                holder.textViewCategory
        };

        // Массивы размеров для каждого режима
        float[] sizesSmallest = {14, 13};
        float[] sizesSmall    = {16, 15};
        float[] sizesNormal   = {18, 17};
        float[] sizesLarge    = {20, 19};
        float[] sizesLargest  = {24, 23};

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

        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = new MainFragmentContent();
            Bundle bundle = new Bundle();
            bundle.putInt(Config.TAG_ID, feed.getCid());
            bundle.putString(Config.TAG_RAZDEL, feed.getTitle());
            fragment.setArguments(bundle);
            FragmentManager fragmentManager = ((AppCompatActivity)context).getSupportFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.addToBackStack(fragment.toString());
            ft.add(R.id.container_frag, fragment);
            MainFragment.viewPager.setCurrentItem(0, true);
            ft.commit();
            Log.e("---", "Category cid: "+feed.getCid());
            Log.e("---", "Category razdel: "+razdel);

        });

    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewCategory;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.title);
            textViewCategory = itemView.findViewById(R.id.category);
        }

    }
}

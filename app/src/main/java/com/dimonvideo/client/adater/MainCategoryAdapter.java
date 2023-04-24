package com.dimonvideo.client.adater;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedCats;
import com.dimonvideo.client.ui.forum.ForumFragment;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.ui.main.MainFragmentContent;

import java.util.List;

public class MainCategoryAdapter extends RecyclerView.Adapter<MainCategoryAdapter.ViewHolder> {

    private final Context context;

    //List to store all
    List<FeedCats> jsonFeed;

    //Constructor of this class
    public MainCategoryAdapter(List<FeedCats> jsonFeed, Context context){
        super();
        //Getting all feed
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
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final FeedCats Feed =  jsonFeed.get(position);

        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewCategory.setText(String.valueOf(Feed.getCount()));
        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = new MainFragmentContent();
            Bundle bundle = new Bundle();
            bundle.putInt(Config.TAG_ID, Feed.getCid());
            bundle.putString(Config.TAG_RAZDEL, Feed.getTitle());
            fragment.setArguments(bundle);
            FragmentManager fragmentManager = ((AppCompatActivity)context).getSupportFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.addToBackStack(fragment.toString());
            ft.add(R.id.container_frag, fragment);
            MainFragment.viewPager.setCurrentItem(0, true);
            ft.commit();
        });

    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
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

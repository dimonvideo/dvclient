package com.dimonvideo.client.adater;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.dimonvideo.client.AllContent;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.util.CustomVolleyRequest;

import java.util.Calendar;
import java.util.List;

public class ForumAdapter extends RecyclerView.Adapter<ForumAdapter.ViewHolder> {

    private Context context;

    //List to store all
    List<FeedForum> jsonFeed;

    //Constructor of this class
    public ForumAdapter(List<FeedForum> jsonFeed, Context context){
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
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final FeedForum Feed =  jsonFeed.get(position);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        if (Feed.getTime() > cal.getTimeInMillis() / 1000L) {
            holder.status_logo.setImageResource(R.drawable.ic_status_green);
        }

        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewText.setText(Feed.getText());
        holder.textViewDate.setText(Feed.getDate());
        holder.textViewNames.setText(Feed.getLast_poster_name());
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
            intent.putExtra(Config.TAG_DATE,Feed.getDate());
            intent.putExtra(Config.TAG_LAST_POSTER_NAME,Feed.getLast_poster_name());
            intent.putExtra(Config.TAG_CATEGORY, Feed.getCategory());
            intent.putExtra(Config.TAG_PINNED, Feed.getPinned());
            intent.putExtra(Config.TAG_USER, Feed.getUser());
            intent.putExtra(Config.TAG_STATE, Feed.getState());
            intent.putExtra(Config.TAG_HITS, String.valueOf(Feed.getHits()));
            intent.putExtra(Config.TAG_COMMENTS, String.valueOf(Feed.getComments()));
          //  context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewText, textViewDate, textViewComments, textViewCategory, textViewHits, textViewNames;
        public ImageView rating_logo, status_logo;

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
            textViewNames = itemView.findViewById(R.id.names);

        }

    }
}

package com.dimonvideo.client.adater;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.ui.forum.ForumFragmentPosts;
import com.dimonvideo.client.util.CustomVolleyRequest;
import com.dimonvideo.client.util.NetworkUtils;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.net.Uri.encode;

public class PmAdapter extends RecyclerView.Adapter<PmAdapter.ViewHolder> {

    private Context context;

    //List to store all
    List<FeedPm> jsonFeed;

    //Constructor of this class
    public PmAdapter(List<FeedPm> jsonFeed, Context context){
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_pm, parent, false);


        return new ViewHolder(v);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final FeedPm Feed =  jsonFeed.get(position);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);
        ImageLoader imageLoader = CustomVolleyRequest.getInstance(context).getImageLoader();
        imageLoader.get(Feed.getImageUrl(), ImageLoader.getImageListener(holder.imageView, R.drawable.ic_menu_gallery, android.R.drawable.ic_dialog_alert));
        holder.imageView.setImageUrl(Feed.getImageUrl(), imageLoader);
        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewDate.setText(Feed.getDate());
        holder.textViewNames.setText(Feed.getLast_poster_name());

        holder.textViewText.setHtml(Feed.getFullText(), new HtmlHttpImageGetter(holder.textViewText));

        if (Feed.getIs_new() > 0) holder.status_logo.setImageResource(R.drawable.ic_status_green);

        holder.itemView.setOnClickListener(v -> {

            holder.textViewText.setHtml(Feed.getText(), new HtmlHttpImageGetter(holder.textViewText));
            holder.btns.setVisibility(View.VISIBLE);
            NetworkUtils.readPm(context, Feed.getId());
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        });
        holder.textViewText.setOnClickListener(v -> {

            holder.textViewText.setHtml(Feed.getText(), new HtmlHttpImageGetter(holder.textViewText));
            holder.btns.setVisibility(View.VISIBLE);
            NetworkUtils.readPm(context, Feed.getId());
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        });
        holder.send.setOnClickListener(v -> {

            holder.textViewText.setHtml(Feed.getFullText(), new HtmlHttpImageGetter(holder.textViewText));
            holder.btns.setVisibility(View.GONE);
            NetworkUtils.sendPm(context, Feed.getId(), holder.textInput.getText().toString(), 0);

        });
        holder.send.setOnLongClickListener(v -> {

            holder.textViewText.setHtml(Feed.getFullText(), new HtmlHttpImageGetter(holder.textViewText));
            holder.btns.setVisibility(View.GONE);
            NetworkUtils.sendPm(context, Feed.getId(), holder.textInput.getText().toString(), 1);

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    // swipe to delete
    public void removeItem(int position) {
        final FeedPm Feed =  jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyItemRemoved(position);
        NetworkUtils.deletePm(context, Feed.getId());

    }

    public List<FeedPm> getData() {
        return jsonFeed;
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewNames;
        public ImageView status_logo;
        public NetworkImageView imageView;
        public HtmlTextView textViewText;
        public LinearLayout btns;
        public Button send;
        public EditText textInput;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (NetworkImageView) itemView.findViewById(R.id.thumbnail);
            status_logo = itemView.findViewById(R.id.status);
            textViewTitle = itemView.findViewById(R.id.title);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewNames = itemView.findViewById(R.id.name);
            btns = itemView.findViewById(R.id.linearLayout1);
            send = itemView.findViewById(R.id.btnSend);
            textInput = itemView.findViewById(R.id.textInput);

        }

    }
}

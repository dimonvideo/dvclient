package com.dimonvideo.client.adater;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.NetworkUtils;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.Calendar;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private final Context context;

    //List to store all
    List<FeedPm> jsonFeed;

    //Constructor of this class
    public FriendsAdapter(List<FeedPm> jsonFeed, Context context){
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

        Glide.with(context).load(Feed.getImageUrl()).apply(RequestOptions.circleCropTransform()).into(holder.imageView);

        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewDate.setText(Feed.getDate());
        holder.textViewNames.setText(Feed.getLast_poster_name());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        try { holder.textViewText.setHtml(Feed.getFullText(), new HtmlHttpImageGetter(holder.textViewText));
        } catch (Throwable ignored) {
        }

        try {
            if (Feed.getTime() > cal.getTimeInMillis() / 1000L) {
                holder.status_logo.setImageResource(R.drawable.ic_status_green);
            }
        } catch (Exception ignored) {
        }

        holder.itemView.setOnClickListener(v -> {

            holder.textViewText.setHtml(Feed.getText(), new HtmlHttpImageGetter(holder.textViewText));
            holder.btns.setVisibility(View.VISIBLE);
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            holder.itemView.setBackgroundColor(0x00000000);

        });
        holder.textViewText.setOnClickListener(v -> {

            holder.textViewText.setHtml(Feed.getText(), new HtmlHttpImageGetter(holder.textViewText));
            holder.btns.setVisibility(View.VISIBLE);
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            holder.itemView.setBackgroundColor(0x00000000);

        });

        holder.send.setOnClickListener(v -> {

            holder.textViewText.setHtml(Feed.getFullText(), new HtmlHttpImageGetter(holder.textViewText));
            holder.btns.setVisibility(View.GONE);
            NetworkUtils.sendPm(context, 0, holder.textInput.getText().toString(), 0, null, Feed.getId());

        });

    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    public List<FeedPm> getData() {
        return jsonFeed;
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewNames;
        public ImageView status_logo, imageView;
        public HtmlTextView textViewText;
        public LinearLayout btns;
        public Button send;
        public EditText textInput;
        public String url;
        public ClipboardManager myClipboard;
        public ClipData myClip;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
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

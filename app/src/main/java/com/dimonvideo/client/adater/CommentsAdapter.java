package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.ui.main.Comments;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.NetworkUtils;

import java.util.Calendar;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private final Context context;

    //List to store all
    List<FeedForum> jsonFeed;

    //Constructor of this class
    public CommentsAdapter(List<FeedForum> jsonFeed, Context context) {
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_posts, parent, false);


        return new ViewHolder(v);
    }

    @SuppressLint({"SetJavaScriptEnabled", "NotifyDataSetChanged"})
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final FeedForum Feed = jsonFeed.get(position);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        try {
            if (Feed.getTime() > cal.getTimeInMillis() / 1000L)
                holder.status_logo.setImageResource(R.drawable.ic_status_green);
        } catch (Throwable ignored) {

        }
        Glide.with(context).load(Feed.getImageUrl()).apply(RequestOptions.circleCropTransform()).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.imageView);
        SharedPreferences sharedPrefs = AppController.getInstance().getSharedPreferences();
        final String password = sharedPrefs.getString("dvc_password", "null");
        final int auth_state = sharedPrefs.getInt("auth_state", 0);

        holder.textViewCategory.setText(Feed.getCategory());
        holder.textViewNames.setVisibility(View.GONE);

        if ((Feed.getNewtopic() == 1) && (!password.equals("null")))
            holder.post_layout.setVisibility(View.GONE);

        // отправка ответа
        holder.btnSend.setOnClickListener(v -> {
            NetworkUtils.sendPm(context, Feed.getId(), holder.textInput.getText().toString(), 20, Feed.getState(), 0);
            holder.post_layout.setVisibility(View.GONE);
            notifyDataSetChanged();

        });

        try {
            holder.textViewText.setText(Html.fromHtml(Feed.getText(), null,  null));
        } catch (Throwable ignored) {
        }
        holder.textViewTitle.setText("#"+ (Feed.getMin() + position + 1) +" ");
        holder.textViewTitle.append(Feed.getUser());
        holder.textViewDate.setText(Feed.getDate());
        holder.textViewComments.setVisibility(View.GONE);
        holder.rating_logo.setVisibility(View.GONE);
        holder.textViewHits.setText(Feed.getTitle());


        // цитирование
        holder.textViewText.setOnClickListener(view -> {
            if ((auth_state > 0) && (Feed.getId() > 0)) {
                postComment(holder, Feed);
            } else {
                openComments(Feed);
            }

        });
        holder.itemView.setOnClickListener(view -> {
            if ((auth_state > 0) && (Feed.getId() > 0)) {
                postComment(holder, Feed);
            } else {
                openComments(Feed);
            }

        });


        if (Feed.getMin()>0) {
            holder.post_layout.setVisibility(View.GONE);
        }

        // show dialog
        holder.itemView.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });
        holder.textViewText.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });
    }

    private static void postComment(ViewHolder holder, FeedForum Feed) {
        if (holder.post_layout.getVisibility()==View.VISIBLE) holder.post_layout.setVisibility(View.GONE); else holder.post_layout.setVisibility(View.VISIBLE);
        holder.textInput.setText("[b]" + Feed.getUser() + "[/b], ");
        holder.textInput.setSelection(holder.textInput.getText().length());
        holder.textInput.setFocusableInTouchMode(true);
        holder.textInput.requestFocus();
    }

    private void openComments(FeedForum Feed) {
        String comm_url = Config.COMMENTS_READS_URL + Feed.getState() + "&lid=" + Feed.getPost_id() + "&min=";
        Intent intent = new Intent(context, Comments.class);
        intent.putExtra(Config.TAG_TITLE, Feed.getTitle());
        intent.putExtra(Config.TAG_LINK, comm_url);
        intent.putExtra(Config.TAG_ID, String.valueOf(Feed.getId()));
        intent.putExtra(Config.TAG_RAZDEL, Feed.getState());
        context.startActivity(intent);
    }


    // dialog
    private void show_dialog(ViewHolder holder, final int position, Context context){
        final CharSequence[] items = {context.getString(R.string.copy_listtext), context.getString(R.string.action_open)};
        FeedForum Feed = jsonFeed.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.myClipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        holder.url = Config.BASE_URL + "/" + com.dimonvideo.client.model.Feed.getRazdel() + "/" + Feed.getPost_id();
        if (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
            holder.url = Config.BASE_URL + "/" + Feed.getPost_id() + "-news.html";

        builder.setTitle(Feed.getTitle());
        builder.setItems(items, (dialog, item) -> {

            if (item == 0) { // copy text
                holder.myClip = ClipData.newPlainText("text", Html.fromHtml(Feed.getText()).toString());
                holder.myClipboard.setPrimaryClip(holder.myClip);
                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
            }
            if (item == 1) { // browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                try {
                    context.startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
            }
        });
        builder.show();
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewComments, textViewHits, textViewNames, textViewCategory;
        public ImageView rating_logo, status_logo, imageView, views_logo;
        public TextView textViewText;
        public LinearLayout post_layout;
        public Button btnSend;
        public EditText textInput;
        public String url;
        public ClipboardManager myClipboard;
        public ClipData myClip;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
            rating_logo = itemView.findViewById(R.id.rating_logo);
            views_logo = itemView.findViewById(R.id.views_logo);
            textViewHits = itemView.findViewById(R.id.views_count);
            status_logo = itemView.findViewById(R.id.status);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewTitle = itemView.findViewById(R.id.title);
            post_layout = itemView.findViewById(R.id.post);
            btnSend = itemView.findViewById(R.id.btnSend);
            textInput = itemView.findViewById(R.id.textInput);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewNames = itemView.findViewById(R.id.names);
        }

    }
}

package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
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
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;

import java.util.Calendar;
import java.util.List;

public class ForumPostsAdapter extends RecyclerView.Adapter<ForumPostsAdapter.ViewHolder> {

    private final Context context;

    //List to store all
    List<FeedForum> jsonFeed;

    //Constructor of this class
    public ForumPostsAdapter(List<FeedForum> jsonFeed, Context context) {
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
        Glide.with(context).load(Feed.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.circleCropTransform()).into(holder.imageView);
        holder.textViewTitle.setText(Feed.getTitle());
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int auth_state = sharedPrefs.getInt("auth_state", 0);
        final boolean is_open_link = sharedPrefs.getBoolean("dvc_open_link", false);
        final boolean is_vuploader_play_listtext = sharedPrefs.getBoolean("dvc_vuploader_play_listtext", false);

        // отправка ответа на форум
        holder.btnSend.setOnClickListener(v -> {
            NetworkUtils.sendPm(context, Feed.getTopic_id(), holder.textInput.getText().toString(), 2, null, 0);
            holder.post_layout.setVisibility(View.GONE);
            notifyDataSetChanged();
        });


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
        try {
            URLImageParser parser = new URLImageParser(holder.textViewText, context, position);
            Spanned spanned = Html.fromHtml(Feed.getText(), parser, new MainAdapter.TagHandler());
            holder.textViewText.setText(spanned);
            holder.textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    // open links from listtext
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context);
                }
            });
        } catch (Throwable ignored) {
        }
        // цитирование
        holder.textViewText.setOnClickListener(view -> {
            if (auth_state > 0) {
                if (holder.post_layout.getVisibility()==View.VISIBLE) holder.post_layout.setVisibility(View.GONE); else holder.post_layout.setVisibility(View.VISIBLE);
                holder.textInput.setText("[b]" + Feed.getLast_poster_name() + "[/b], ");
                holder.textInput.setSelection(holder.textInput.getText().length());
            }
        });
        holder.itemView.setOnClickListener(view -> {
            if (auth_state > 0) {
                if (holder.post_layout.getVisibility()==View.VISIBLE) holder.post_layout.setVisibility(View.GONE); else holder.post_layout.setVisibility(View.VISIBLE);
                holder.textInput.setText("[b]" + Feed.getLast_poster_name() + "[/b], ");
                holder.textInput.setSelection(holder.textInput.getText().length());
            }
        });

        // меню по долгому нажатию
        holder.itemView.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });
        holder.textViewText.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });


    }

    // dialog
    private void show_dialog(ViewHolder holder, final int position, Context context){
        final CharSequence[] items = {context.getString(R.string.menu_share_title), context.getString(R.string.action_open),
                context.getString(R.string.action_like), context.getString(R.string.copy_listtext)};
        FeedForum Feed = jsonFeed.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.BASE_URL + "/forum/post_" + Feed.getId();

        holder.myClipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);

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
            if (item == 2) { // like
                ButtonsActions.like_forum_post(context, Feed.getId(), 1);
            }
            if (item == 3) { // copy text
                try { holder.myClip = ClipData.newPlainText("text", Html.fromHtml(Feed.getText()).toString());
                holder.myClipboard.setPrimaryClip(holder.myClip);
                } catch (Throwable ignored) {
                }
                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
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
        public TextView textViewTitle, textViewDate, textViewComments, textViewCategory, textViewHits, textViewNames;
        public ImageView rating_logo, status_logo, imageView;
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
            status_logo = itemView.findViewById(R.id.status);
            textViewNames = itemView.findViewById(R.id.title);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewHits = itemView.findViewById(R.id.views_count);
            textViewTitle = itemView.findViewById(R.id.names);
            post_layout = itemView.findViewById(R.id.post);
            btnSend = itemView.findViewById(R.id.btnSend);
            textInput = itemView.findViewById(R.id.textInput);

        }

    }
}

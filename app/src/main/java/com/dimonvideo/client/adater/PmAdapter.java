package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
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
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;

import org.xml.sax.XMLReader;

import java.util.List;

public class PmAdapter extends RecyclerView.Adapter<PmAdapter.ViewHolder> {

    private final Context context;

    //List to store all
    List<FeedPm> jsonFeed;

    //Constructor of this class
    public PmAdapter(List<FeedPm> jsonFeed, Context context){
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;

    }

    public static class TagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag,
                              Editable output, XMLReader xmlReader) {
            if (!opening && tag.equals("ul")) {
                output.append("\n");
            }
            if (opening && tag.equals("li")) {
                output.append("\n\u2022");
            }
        }
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

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean is_open_link = sharedPrefs.getBoolean("dvc_open_link", false);
        final boolean is_vuploader_play_listtext = sharedPrefs.getBoolean("dvc_vuploader_play_listtext", false);

        //Getting the particular item from the list
        final FeedPm Feed =  jsonFeed.get(position);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        Glide.with(context).load(Feed.getImageUrl()).apply(RequestOptions.circleCropTransform()).into(holder.imageView);

        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewDate.setText(Feed.getDate());
        holder.textViewNames.setText(Feed.getLast_poster_name());

        try {
            Spanned spanned = Html.fromHtml(Feed.getFullText(), null, null);
            holder.textViewText.setText(spanned);
        } catch (Throwable ignored) {
        }
        holder.itemView.setBackgroundColor(0x00000000);

        if (Feed.getIs_new() > 0) {
            holder.status_logo.setImageResource(R.drawable.ic_status_green);
            holder.itemView.setBackgroundColor(Color.parseColor("#992301"));
            holder.textViewText.setTypeface(null, Typeface.BOLD);
        }

        holder.itemView.setOnClickListener(v -> {

            showFullText(holder, is_open_link, is_vuploader_play_listtext, Feed, position);

            holder.btns.setVisibility(View.VISIBLE);
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            holder.itemView.setBackgroundColor(0x00000000);

        });
        holder.textViewText.setOnClickListener(v -> {

            showFullText(holder, is_open_link, is_vuploader_play_listtext, Feed, position);

            holder.btns.setVisibility(View.VISIBLE);
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            holder.itemView.setBackgroundColor(0x00000000);

        });
        holder.send.setOnClickListener(v -> {

            showFullText(holder, is_open_link, is_vuploader_play_listtext, Feed, position);
            holder.btns.setVisibility(View.GONE);
            NetworkUtils.sendPm(context, Feed.getId(), holder.textInput.getText().toString(), 0, null, 0);

        });
        holder.send.setOnLongClickListener(v -> {

            showFullText(holder, is_open_link, is_vuploader_play_listtext, Feed, position);

            holder.btns.setVisibility(View.GONE);
            NetworkUtils.sendPm(context, Feed.getId(), holder.textInput.getText().toString(), 1, null, 0);
            jsonFeed.remove(position);
            return true;
        });
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

    // show full
    private void showFullText(ViewHolder holder, boolean is_open_link, boolean is_vuploader_play_listtext, FeedPm Feed, int position) {
        try {
            URLImageParser parser = new URLImageParser(holder.textViewText, context, position);
            Spanned spanned = Html.fromHtml(Feed.getText(), parser, new TagHandler());
            holder.textViewText.setText(spanned);

            if (Feed.getIs_new() > 0) NetworkUtils.readPm(context, Feed.getId());

            holder.textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    // dialog
    private void show_dialog(ViewHolder holder, final int position, Context context){
        final CharSequence[] items = {context.getString(R.string.action_open), context.getString(R.string.copy_listtext)};
        final FeedPm Feed =  jsonFeed.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.BASE_URL + "/pm/6/" + Feed.getId();

        holder.myClipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(Feed.getTitle());
        builder.setItems(items, (dialog, item) -> {

            if (item == 0) { // browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                try {
                    context.startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
            }
            if (item == 1) { // copy text
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

    // swipe to delete
    @SuppressLint("NotifyDataSetChanged")
    public void removeItem(int position) {
        final FeedPm Feed =  jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyDataSetChanged();
        NetworkUtils.deletePm(context, Feed.getId(), 0);

    }

    // swipe to delete
    @SuppressLint("NotifyDataSetChanged")
    public void restoreItem(int position) {
        final FeedPm Feed =  jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyDataSetChanged();
        NetworkUtils.deletePm(context, Feed.getId(), 1);

    }
    public List<FeedPm> getData() {
        return jsonFeed;
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewNames;
        public ImageView status_logo, imageView;
        public TextView textViewText;
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

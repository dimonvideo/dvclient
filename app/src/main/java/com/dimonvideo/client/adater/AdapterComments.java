/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.ui.main.MainFragmentCommentsFile;
import com.dimonvideo.client.ui.main.MainFragmentViewFileByApi;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;

import org.xml.sax.XMLReader;

import java.util.Calendar;
import java.util.List;

public class AdapterComments extends RecyclerView.Adapter<AdapterComments.ViewHolder> {

    Context context;
    private final List<FeedForum> jsonFeed;

    public static class TagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag,
                              Editable output, XMLReader xmlReader) {
            if (!opening && tag.equals("ul")) {
                output.append("\n");
            }
            if (opening && tag.equals("li")) {
                output.append("\n•");
            }
        }
    }

    //Constructor of this class
    public AdapterComments(List<FeedForum> jsonFeed, Context context) {
        super();
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_posts, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        onBindViewHold(holder, position);
    }


    @SuppressLint("SetTextI18n")
    public void onBindViewHold(ViewHolder holder, int position) {

        final FeedForum feed = jsonFeed.get(position);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        try {
            if (feed.getTime() > cal.getTimeInMillis() / 1000L)
                holder.status_logo.setImageResource(R.drawable.ic_status_green);
        } catch (Throwable ignored) {

        }

        Glide.with(holder.itemView.getContext()).clear(holder.imageView);

        Glide.with(holder.itemView.getContext())
                .load(feed.getImageUrl())
                .apply(RequestOptions.circleCropTransform())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.baseline_image_20)
                .error(R.drawable.baseline_image_20)
                .apply(new RequestOptions().override(80, 80))
                .into(holder.imageView);

        holder.textViewNames.setVisibility(View.GONE);

        final boolean is_open_link = AppController.getInstance().isOpenLinks();
        final boolean is_vuploader_play_listtext = AppController.getInstance().isVuploaderPlayListtext();

        // html textview
        TextView textViewText = holder.textViewText;
        try {
            URLImageParser parser = new URLImageParser(textViewText);
            Spanned spanned = Html.fromHtml(feed.getText(), Html.FROM_HTML_MODE_LEGACY, parser, new TagHandler());
            textViewText.setText(spanned);
            textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context, feed.getRazdel());
                }
            });
        } catch (Throwable ignored) {
        }


        holder.textViewCategory.setText(feed.getCategory());
        holder.textViewTitle.setText("#"+ (position + 1)+" ");
        holder.textViewDate.setText(feed.getDate());
        holder.textViewHits.setText(feed.getTitle());

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


        holder.textViewTitle.append(feed.getUser());
        holder.textViewComments.setVisibility(View.GONE);
        holder.rating_logo.setVisibility(View.GONE);

        // просмотр описания файла
        holder.textViewHits.setOnClickListener(view -> {
            MainFragmentViewFileByApi fragment = new MainFragmentViewFileByApi();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_RAZDEL, feed.getRazdel());
            bundle.putString(Config.TAG_ID, String.valueOf(feed.getPost_id()));
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity)context).getSupportFragmentManager(), "MainFragmentViewFileByApi");
        });

        holder.itemView.setOnClickListener(view -> {
            openComments(feed.getPost_id(), feed.getTitle(), feed.getRazdel());
        });

        holder.textViewText.setOnClickListener(view -> {
            openComments(feed.getPost_id(), feed.getTitle(), feed.getRazdel());
        });

        // show dialog
        holder.itemView.setOnLongClickListener(view -> {
            show_dialog(holder, holder.getBindingAdapterPosition(), context);
            return true;
        });
        holder.textViewText.setOnLongClickListener(view -> {
            show_dialog(holder, holder.getBindingAdapterPosition(), context);
            return true;
        });
    }

    private void openComments(int lid, String title, String razdel) {
        String comm_url = Config.COMMENTS_READS_URL + razdel + "&lid=" + lid + "&min=";
        MainFragmentCommentsFile fragment = new MainFragmentCommentsFile();
        Bundle bundle = new Bundle();
        bundle.putString(Config.TAG_TITLE, title);
        bundle.putString(Config.TAG_ID, String.valueOf(lid));
        bundle.putString(Config.TAG_LINK, comm_url);
        bundle.putString(Config.TAG_RAZDEL, razdel);
        fragment.setArguments(bundle);
        fragment.show(((AppCompatActivity)context).getSupportFragmentManager(), "MainFragmentCommentsFile");
    }


    // dialog
    private void show_dialog(ViewHolder holder, final int position, Context context){
        final CharSequence[] items = {context.getString(R.string.copy_listtext), context.getString(R.string.action_open)};
        FeedForum feed = jsonFeed.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.myClipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        holder.url = Config.WRITE_URL + "/" + feed.getRazdel() + "/" + feed.getPost_id();
        if (feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
            holder.url = Config.WRITE_URL + "/" + feed.getPost_id() + "-news.html";

        builder.setTitle(feed.getTitle());
        builder.setItems(items, (dialog, item) -> {

            if (item == 0) { // copy text

                holder.myClip = ClipData.newPlainText("text", Html.fromHtml(feed.getText(), Html.FROM_HTML_MODE_LEGACY).toString());
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewComments, textViewHits, textViewNames, textViewCategory;
        public ImageView rating_logo, status_logo, imageView, views_logo, imagePick;
        public TextView textViewText;
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
            textInput = itemView.findViewById(R.id.textInput);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewNames = itemView.findViewById(R.id.names);
            imagePick = itemView.findViewById(R.id.img_btn);
        }

    }
}

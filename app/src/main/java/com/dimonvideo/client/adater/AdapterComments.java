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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xml.sax.XMLReader;

import java.util.Calendar;
import java.util.List;

public class AdapterComments extends RecyclerView.Adapter<AdapterComments.ViewHolder> {

    private final Context context;
    private String image_uploaded;
    private String razdel;
    private final List<FeedForum> jsonFeed;

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
        image_uploaded = event.image_uploaded;
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

    //Constructor of this class
    public AdapterComments(List<FeedForum> jsonFeed, Context context) {
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

    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final FeedForum feed = jsonFeed.get(holder.getBindingAdapterPosition());

        razdel = feed.getRazdel();
        int lid = feed.getPost_id();

        EventBus.getDefault().postSticky(new MessageEvent(razdel, null, image_uploaded, null, null, null));

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

        Glide.with(context).load(feed.getImageUrl()).apply(RequestOptions.circleCropTransform()).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.imageView);

        final int auth_state = AppController.getInstance().isAuth();

        holder.textViewCategory.setText(feed.getCategory());
        holder.textViewNames.setVisibility(View.GONE);

        final boolean is_open_link = AppController.getInstance().isOpenLinks();
        final boolean is_vuploader_play_listtext = AppController.getInstance().isVuploaderPlayListtext();

        // html textview
        TextView textViewText = holder.textViewText;
        try {
            URLImageParser parser = new URLImageParser(textViewText);
            Spanned spanned = Html.fromHtml(feed.getText(), parser, new AdapterComments.TagHandler());
            textViewText.setText(spanned);
            textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context, razdel);
                }
            });
        } catch (Throwable ignored) {
        }


        holder.textViewTitle.setText("#"+ (feed.getMin() + holder.getBindingAdapterPosition() + 1) +" ");
        holder.textViewTitle.append(feed.getUser());
        holder.textViewDate.setText(feed.getDate());
        holder.textViewComments.setVisibility(View.GONE);
        holder.rating_logo.setVisibility(View.GONE);
        holder.textViewHits.setText(feed.getTitle());

        // просмотр описания файла
        holder.textViewHits.setOnClickListener(view -> {
            MainFragmentViewFileByApi fragment = new MainFragmentViewFileByApi();
            Bundle bundle = new Bundle();
            bundle.putString(Config.TAG_RAZDEL, razdel);
            bundle.putString(Config.TAG_ID, String.valueOf(lid));
            fragment.setArguments(bundle);
            fragment.show(((AppCompatActivity)context).getSupportFragmentManager(), "MainFragmentViewFileByApi");
        });

        // цитирование
        holder.textViewText.setOnClickListener(view -> {
            if ((auth_state > 0) && (feed.getId() > 0)) {
                postComment(feed);
            } else {
                openComments(lid, feed.getTitle(), razdel);
            }

        });

        holder.itemView.setOnClickListener(view -> {
            if ((auth_state > 0) && (feed.getId() > 0)) {
                postComment(feed);
            } else {
                openComments(lid, feed.getTitle(), razdel);
            }

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

    @SuppressLint("SetTextI18n")
    private static void postComment(FeedForum feed) {
        EventBus.getDefault().post(new MessageEvent(feed.getRazdel(), null, null, null, "[b]" + feed.getUser() + "[/b], ", null));

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
        holder.url = Config.BASE_URL + "/" + feed.getRazdel() + "/" + feed.getPost_id();
        if (feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
            holder.url = Config.BASE_URL + "/" + feed.getPost_id() + "-news.html";

        builder.setTitle(feed.getTitle());
        builder.setItems(items, (dialog, item) -> {

            if (item == 0) { // copy text
                holder.myClip = ClipData.newPlainText("text", Html.fromHtml(feed.getText()).toString());
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

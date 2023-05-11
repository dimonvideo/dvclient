package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.ui.forum.ForumFragmentPosts;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;
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

public class AdapterForumPosts extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    String image_uploaded;
    int razdel = 8;

    //List to store all
    List<FeedForum> jsonFeed;

    public AdapterForumPosts(List<FeedForum> jsonFeed) {
        super();
        this.jsonFeed = jsonFeed;
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
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

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_posts, parent, false);
        mContext = parent.getContext();
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        populateItemRows((ItemViewHolder) holder, holder.getBindingAdapterPosition());

    }

    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    private void populateItemRows(ItemViewHolder holder, int position) {

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

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
        Glide.with(mContext).load(Feed.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.circleCropTransform()).into(holder.imageView);
        holder.textViewTitle.setText(Feed.getTitle());

        final int auth_state = AppController.getInstance().isAuth();
        final boolean is_open_link = AppController.getInstance().isOpenLinks();
        final boolean is_vuploader_play_listtext = AppController.getInstance().isVuploaderPlayListtext();

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

        holder.textViewHits.setVisibility(View.INVISIBLE);
        holder.views_logo.setVisibility(View.INVISIBLE);

        try {
            URLImageParser parser = new URLImageParser(holder.textViewText);
            Spanned spanned = Html.fromHtml(Feed.getText(), parser, new AdapterMainRazdel.TagHandler());
            holder.textViewText.setText(spanned);
            holder.textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    // open links from listtext
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, mContext, Feed.getRazdel());
                }
            });
        } catch (Throwable ignored) {
        }
        // цитирование
        holder.textViewText.setOnClickListener(view -> {
            if (auth_state > 0) {
                EventBus.getDefault().post(new MessageEvent("8", null, null, null, "[b]" + Feed.getLast_poster_name() + "[/b], ", null));
            }
        });
        holder.itemView.setOnClickListener(view -> {
            if (auth_state > 0) {
                EventBus.getDefault().post(new MessageEvent("8", null, null, null, "[b]" + Feed.getLast_poster_name() + "[/b], ", null));
            }
        });

        // меню по долгому нажатию
        holder.itemView.setOnLongClickListener(view -> {
            show_dialog(holder, position);
            return true;
        });
        holder.textViewText.setOnLongClickListener(view -> {
            show_dialog(holder, position);
            return true;
        });


    }

    // dialog
    private void show_dialog(ItemViewHolder holder, int position){
        final CharSequence[] items = {mContext.getString(R.string.menu_share_title), mContext.getString(R.string.action_open),
                mContext.getString(R.string.action_like), mContext.getString(R.string.copy_listtext)};
        FeedForum Feed = jsonFeed.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        holder.url = Config.BASE_URL + "/forum/post_" + Feed.getId();

        holder.myClipboard = (ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(Feed.getTitle());
        builder.setItems(items, (dialog, item) -> {

            if (item == 0) { // share
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, holder.url);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, Feed.getTitle());
                try {
                    mContext.startActivity(shareIntent);
                } catch (Throwable ignored) {
                }
            }
            if (item == 1) { // browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                try {
                    mContext.startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
            }
            if (item == 2) { // like
                ButtonsActions.like_forum_post(mContext, Feed.getId(), 1);
            }
            if (item == 3) { // copy text
                try { holder.myClip = ClipData.newPlainText("text", Html.fromHtml(Feed.getText()).toString());
                holder.myClipboard.setPrimaryClip(holder.myClip);
                } catch (Throwable ignored) {
                }
                Toast.makeText(mContext, mContext.getString(R.string.success), Toast.LENGTH_SHORT).show();
            }

        });
        builder.show();
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewComments, textViewCategory, textViewHits, textViewNames;
        public ImageView rating_logo, status_logo, imageView, views_logo, imagePick;
        public TextView textViewText;
        public EditText textInput;
        public String url;
        public ClipboardManager myClipboard;
        public ClipData myClip;

        //Initializing Views
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
            rating_logo = itemView.findViewById(R.id.rating_logo);
            status_logo = itemView.findViewById(R.id.status);
            views_logo = itemView.findViewById(R.id.views_logo);
            textViewNames = itemView.findViewById(R.id.title);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewHits = itemView.findViewById(R.id.views_count);
            textViewTitle = itemView.findViewById(R.id.names);
            textInput = itemView.findViewById(R.id.textInput);
            imagePick = itemView.findViewById(R.id.img_btn);

        }

    }
}

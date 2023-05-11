package com.dimonvideo.client.adater;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
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

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.BBCodes;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xml.sax.XMLReader;

import java.util.List;

public class AdapterPm extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    String image_uploaded;

    //List to store all
    List<FeedPm> jsonFeed;

    //Constructor of this class
    public AdapterPm(List<FeedPm> JsonFeed){
        super();
        this.jsonFeed = JsonFeed;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_pm, parent, false);
        mContext = parent.getContext();
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        populateItemRows((ItemViewHolder) holder, position);

    }

    @Override
    public int getItemCount() {
        return jsonFeed == null ? 0 : jsonFeed.size();
    }

    public int getItemViewType(int position) {
        int VIEW_TYPE_ITEM = 0;
        int VIEW_TYPE_LOADING = 1;
        return jsonFeed.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle, textViewDate, textViewNames;
        public ImageView status_logo, imageView;
        public TextView textViewText;
        public LinearLayout btns;
        public ImageButton send;
        public EditText textInput;
        public String url;
        public ClipboardManager myClipboard;
        public ClipData myClip;
        public ImageView imagePick;

        public ItemViewHolder(@NonNull View itemView) {
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
            imagePick = itemView.findViewById(R.id.img_btn);
        }
    }

    private void populateItemRows(ItemViewHolder holder, int position) {

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        final boolean is_open_link = AppController.getInstance().isOpenLinks();
        final boolean is_vuploader_play_listtext = AppController.getInstance().isVuploaderPlayListtext();

        final FeedPm Feed =  jsonFeed.get(holder.getBindingAdapterPosition());

        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        Glide.with(mContext).load(Feed.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.circleCropTransform()).into(holder.imageView);

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

            if (holder.btns.getVisibility()==View.VISIBLE) holder.btns.setVisibility(View.GONE); else holder.btns.setVisibility(View.VISIBLE);

            showFullText(holder, is_open_link, is_vuploader_play_listtext, Feed);

            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            holder.itemView.setBackgroundColor(0x00000000);

        });
        holder.textViewText.setOnClickListener(v -> {

            if (holder.btns.getVisibility()==View.VISIBLE) holder.btns.setVisibility(View.GONE); else holder.btns.setVisibility(View.VISIBLE);

            showFullText(holder, is_open_link, is_vuploader_play_listtext, Feed);

            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            holder.itemView.setBackgroundColor(0x00000000);

        });

        holder.imagePick.setOnClickListener(v -> {
            MainActivity.pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());

        });

        holder.send.setOnClickListener(v -> {

            showFullText(holder, is_open_link, is_vuploader_play_listtext, Feed);
            holder.btns.setVisibility(View.GONE);
            String text = holder.textInput.getText().toString();
            NetworkUtils.sendPm(mContext, Feed.getId(), BBCodes.imageCodes(text, image_uploaded, "13"), 0, null, 0);

        });
        holder.send.setOnLongClickListener(v -> {

            showFullText(holder, is_open_link, is_vuploader_play_listtext, Feed);
            holder.btns.setVisibility(View.GONE);
            String text = holder.textInput.getText().toString();
            NetworkUtils.sendPm(mContext, Feed.getId(), BBCodes.imageCodes(text, image_uploaded, "13"), 1, null, 0);
            try {
                jsonFeed.remove(holder.getBindingAdapterPosition());
            } catch (Throwable ignored) {}
            return true;
        });
        // show dialog
        holder.itemView.setOnLongClickListener(view -> {
            show_dialog(holder, holder.getBindingAdapterPosition());
            return true;
        });
        holder.textViewText.setOnLongClickListener(view -> {
            show_dialog(holder, holder.getBindingAdapterPosition());
            return true;
        });


    }



    // show full
    private void showFullText(ItemViewHolder holder, boolean is_open_link, boolean is_vuploader_play_listtext, FeedPm Feed) {
        try {
            URLImageParser parser = new URLImageParser(holder.textViewText);
            Spanned spanned = Html.fromHtml(Feed.getText(), parser, new TagHandler());
            holder.textViewText.setText(spanned);

            if (Feed.getIs_new() > 0) NetworkUtils.readPm(mContext, Feed.getId());

            holder.textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, mContext, "pm");
                }
            });
        } catch (Throwable ignored) {
        }
    }

    // dialog
    private void show_dialog(ItemViewHolder holder, final int position){
        final CharSequence[] items = {mContext.getString(R.string.action_open), mContext.getString(R.string.copy_listtext), mContext.getString(R.string.pm_delete)};
        final FeedPm Feed =  jsonFeed.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        holder.url = Config.BASE_URL + "/pm/6/" + Feed.getId();

        holder.myClipboard = (ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(Feed.getTitle());
        builder.setItems(items, (dialog, item) -> {

            if (item == 0) { // browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                try {
                    mContext.startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
            }
            if (item == 1) { // copy text
                try { holder.myClip = ClipData.newPlainText("text", Html.fromHtml(Feed.getText()).toString());
                    holder.myClipboard.setPrimaryClip(holder.myClip);
                } catch (Throwable ignored) {
                }
                Toast.makeText(mContext, mContext.getString(R.string.success), Toast.LENGTH_SHORT).show();
            }
            if (item == 2) { // delete
                try {
                    removeItem(position);
                    Toast.makeText(mContext, mContext.getString(R.string.msg_removed), Toast.LENGTH_SHORT).show();

                } catch (Throwable ignored) {
                }
                Toast.makeText(mContext, mContext.getString(R.string.success), Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    // swipe to delete
    public void removeItem(int position) {
        try {
            if (position >= 0) {
                FeedPm Feed = jsonFeed.get(position);
                jsonFeed.remove(position);
                NetworkUtils.deletePm(mContext, Feed.getId(), 0);
                notifyItemRemoved(position);
            }
        } catch (Throwable ignored) {}

    }

    // swipe to restore
    public void restoreItem(int position) {
        try {
            if (position >= 0) {
                FeedPm Feed = jsonFeed.get(position);
                jsonFeed.remove(position);
                NetworkUtils.deletePm(mContext, Feed.getId(), 1);
                notifyItemRemoved(position);
            }
        } catch (Throwable ignored) {}


    }

    // swipe to archive
    public void archiveItem(int position) {
        try {
            if (position >= 0) {
                FeedPm Feed = jsonFeed.get(position);
                jsonFeed.remove(position);
                NetworkUtils.deletePm(mContext, Feed.getId(), 2);
                notifyItemRemoved(position);
            }
        } catch (Throwable ignored) {}


    }


    // swipe to restore from archive
    public void restoreFromArchiveItem(int position) {
        try {
            if (position >= 0) {
                FeedPm Feed = jsonFeed.get(position);
                jsonFeed.remove(position);
                NetworkUtils.deletePm(mContext, Feed.getId(), 3);
                notifyItemRemoved(position);
            }
        } catch (Throwable ignored) {}


    }

}

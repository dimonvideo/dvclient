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
import android.util.Log;
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
import androidx.recyclerview.widget.DiffUtil;
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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xml.sax.XMLReader;

import java.util.ArrayList;
import java.util.List;

public class AdapterPm extends RecyclerView.Adapter<AdapterPm.ItemViewHolder> {

    private final Context context;
    private List<FeedPm> jsonFeed;
    private String image_uploaded;

    public AdapterPm(List<FeedPm> jsonFeed, Context context) {
        this.jsonFeed = new ArrayList<>(jsonFeed);
        this.context = context;
    }

    public void updateData(List<FeedPm> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return jsonFeed.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return jsonFeed.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return jsonFeed.get(oldItemPosition).equals(newList.get(newItemPosition));
            }
        });
        jsonFeed = new ArrayList<>(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        image_uploaded = event.image_uploaded;
    }

    public static class TagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (!opening && tag.equals("ul")) {
                output.append("\n");
            }
            if (opening && tag.equals("li")) {
                output.append("\n•");
            }
        }
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_pm, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        FeedPm feed = jsonFeed.get(position);
        final boolean isOpenLink = AppController.getInstance().isOpenLinks();
        final boolean isVuploaderPlayListtext = AppController.getInstance().isVuploaderPlayListtext();

        // Установка основных данных
        holder.textViewTitle.setText(feed.getTitle());
        holder.textViewDate.setText(feed.getDate());
        holder.textViewNames.setText(feed.getLast_poster_name());
        holder.textViewText.setText(feed.getSpannedFullText());
        holder.status_logo.setImageResource(feed.getIs_new() > 0 ? R.drawable.ic_status_green : R.drawable.ic_status_gray);
        holder.itemView.setBackgroundColor(feed.getIs_new() > 0 ? Color.parseColor("#992301") : 0x00000000);
        holder.textViewText.setTypeface(null, feed.getIs_new() > 0 ? Typeface.BOLD : Typeface.NORMAL);

        // Загрузка изображения
        Glide.with(context)
                .load(feed.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.baseline_image_20)
                .error(R.drawable.baseline_image_20)
                .apply(RequestOptions.circleCropTransform())
                .override(100, 100)
                .into(holder.imageView);

        // Обработчики событий
        holder.itemView.setOnClickListener(v -> {
            if (holder.btns.getVisibility() == View.VISIBLE) {
                holder.btns.setVisibility(View.GONE);
            } else {
                holder.btns.setVisibility(View.VISIBLE);
                showFullText(holder, isOpenLink, isVuploaderPlayListtext, feed);
                holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                holder.itemView.setBackgroundColor(0x00000000);
            }
        });

        holder.imagePick.setOnClickListener(v -> MainActivity.pickMedia.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));

        holder.send.setOnClickListener(v -> {
            showFullText(holder, isOpenLink, isVuploaderPlayListtext, feed);
            holder.btns.setVisibility(View.GONE);
            String text = holder.textInput.getText().toString();
            NetworkUtils.sendPm(context, feed.getId(), BBCodes.imageCodes(text, image_uploaded, "13"), 0, null, 0);
        });

        holder.send.setOnLongClickListener(v -> {
            showFullText(holder, isOpenLink, isVuploaderPlayListtext, feed);
            holder.btns.setVisibility(View.GONE);
            String text = holder.textInput.getText().toString();
            NetworkUtils.sendPm(context, feed.getId(), BBCodes.imageCodes(text, image_uploaded, "13"), 1, null, 0);
            removeItem(holder.getBindingAdapterPosition());
            return true;
        });

        holder.itemView.setOnLongClickListener(v -> {
            showDialog(holder, holder.getBindingAdapterPosition());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return jsonFeed == null ? 0 : jsonFeed.size();
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

    private void showFullText(ItemViewHolder holder, boolean isOpenLink, boolean isVuploaderPlayListtext, FeedPm feed) {
        try {
            holder.textViewText.setText(feed.getSpannedText());
            if (feed.getIs_new() > 0) {
                NetworkUtils.readPm(context, feed.getId());
            }
            holder.textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, isOpenLink, isVuploaderPlayListtext, context, "pm");
                }
            });
        } catch (Exception e) {
            Log.e("AdapterPm", "Error showing full text", e);
        }
    }

    private void showDialog(ItemViewHolder holder, final int position) {
        final CharSequence[] items = {
                context.getString(R.string.action_open),
                context.getString(R.string.copy_listtext),
                context.getString(R.string.pm_delete)
        };
        final FeedPm feed = jsonFeed.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.WRITE_URL + "/pm/6/" + feed.getId();
        holder.myClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(feed.getTitle());
        builder.setItems(items, (dialog, item) -> {
            if (item == 0) { // browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                try {
                    context.startActivity(browserIntent);
                } catch (Exception e) {
                    Log.e("AdapterPm", "Error opening browser", e);
                }
            } else if (item == 1) { // copy text
                try {
                    holder.myClip = ClipData.newPlainText("text", Html.fromHtml(feed.getText(), Html.FROM_HTML_MODE_LEGACY).toString());
                    holder.myClipboard.setPrimaryClip(holder.myClip);
                    Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("AdapterPm", "Error copying text", e);
                }
            } else if (item == 2) { // delete
                try {
                    removeItem(position);
                    Toast.makeText(context, context.getString(R.string.msg_removed), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("AdapterPm", "Error deleting item", e);
                }
            }
        });
        builder.show();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < jsonFeed.size()) {
            FeedPm feed = jsonFeed.get(position);
            jsonFeed = new ArrayList<>(jsonFeed);
            jsonFeed.remove(position);
            NetworkUtils.deletePm(context, feed.getId(), 0);
            notifyItemRemoved(position);
        } else {
            Log.e("AdapterPm", "Invalid position for removeItem: " + position);
        }
    }

    public void restoreItem(int position) {
        if (position >= 0 && position < jsonFeed.size()) {
            FeedPm feed = jsonFeed.get(position);
            jsonFeed = new ArrayList<>(jsonFeed);
            jsonFeed.remove(position);
            NetworkUtils.deletePm(context, feed.getId(), 1);
            notifyItemRemoved(position);
        } else {
            Log.e("AdapterPm", "Invalid position for restoreItem: " + position);
        }
    }

    public void archiveItem(int position) {
        if (position >= 0 && position < jsonFeed.size()) {
            FeedPm feed = jsonFeed.get(position);
            jsonFeed = new ArrayList<>(jsonFeed);
            jsonFeed.remove(position);
            NetworkUtils.deletePm(context, feed.getId(), 2);
            notifyItemRemoved(position);
        } else {
            Log.e("AdapterPm", "Invalid position for archiveItem: " + position);
        }
    }

    public void restoreFromArchiveItem(int position) {
        if (position >= 0 && position < jsonFeed.size()) {
            FeedPm feed = jsonFeed.get(position);
            jsonFeed = new ArrayList<>(jsonFeed);
            jsonFeed.remove(position);
            NetworkUtils.deletePm(context, feed.getId(), 3);
            notifyItemRemoved(position);
        } else {
            Log.e("AdapterPm", "Invalid position for restoreFromArchiveItem: " + position);
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
}
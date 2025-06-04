/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.adater;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
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
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AdapterPmFriends extends RecyclerView.Adapter<AdapterPmFriends.ViewHolder> {

    private final Context context;
    private List<FeedPm> jsonFeed;
    private String image_uploaded;

    public AdapterPmFriends(List<FeedPm> jsonFeed, Context context) {
        this.context = context;
        this.jsonFeed = new ArrayList<>(jsonFeed);
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_pm, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final FeedPm feed = jsonFeed.get(position);

        holder.status_logo.setImageResource(R.drawable.ic_status_gray);
        Glide.with(context)
                .load(feed.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.baseline_image_20)
                .apply(RequestOptions.circleCropTransform())
                .override(100, 100)
                .into(holder.imageView);

        holder.textViewTitle.setText(feed.getTitle());
        holder.textViewDate.setText(String.format("%s %s", feed.getLast_poster_name(), feed.getDate()));
        holder.textViewNames.setText(feed.getFullText());
        holder.textViewText.setText(feed.getSpannedText(holder.textViewText));

        // Массив всех нужных TextView
        TextView[] textViews = {
                holder.textViewTitle,
                holder.textViewText,
                holder.textViewDate,
                holder.textViewNames
        };

        // Массивы размеров для каждого режима
        float[] sizesSmallest = {14, 13, 12, 12};
        float[] sizesSmall    = {16, 15, 14, 14};
        float[] sizesNormal   = {18, 17, 16, 16};
        float[] sizesLarge    = {20, 19, 18, 18};
        float[] sizesLargest  = {24, 23, 22, 22};

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

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        try {
            if (feed.getTime() > cal.getTimeInMillis() / 1000L) {
                holder.status_logo.setImageResource(R.drawable.ic_status_green);
            }
        } catch (Exception e) {
            Log.e("AdapterPmFriends", "Error checking time", e);
        }

        holder.itemView.setOnClickListener(v -> {
            if (holder.btns.getVisibility() == View.VISIBLE) {
                holder.btns.setVisibility(View.GONE);
            } else {
                holder.btns.setVisibility(View.VISIBLE);
            }
        });

        holder.imagePick.setOnClickListener(v -> {
            MainActivity.pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        holder.send.setOnClickListener(v -> {
            holder.textViewText.setText(feed.getSpannedText(holder.textViewText));
            holder.textViewText.setMovementMethod(LinkMovementMethod.getInstance());
            holder.btns.setVisibility(View.GONE);
            String text = holder.textInput.getText().toString();
            NetworkUtils.sendPm(context, 0, BBCodes.imageCodes(text, image_uploaded, "13"), 0, null, feed.getId());
        });

        holder.itemView.setOnLongClickListener(view -> {
            showDialog(holder, position, context);
            return true;
        });
    }

    private void showDialog(ViewHolder holder, final int position, Context context) {
        final CharSequence[] items = {
                context.getString(R.string.action_open),
                context.getString(R.string.action_like_member),
                context.getString(R.string.copy_name),
                context.getString(R.string.add_friend),
                context.getString(R.string.add_ignor),
                context.getString(R.string.remove_all)
        };
        final FeedPm feed = jsonFeed.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.WRITE_URL + "/0/name/" + feed.getTitle();
        holder.myClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(feed.getTitle());
        builder.setItems(items, (dialog, item) -> {
            try {
                if (item == 0) { // browser
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                    context.startActivity(browserIntent);
                } else if (item == 1) { // like
                    ButtonsActions.like_member(context, feed.getId(), feed.getTitle(), 5);
                } else if (item == 2) { // copy text
                    holder.myClip = ClipData.newPlainText("text", feed.getTitle());
                    holder.myClipboard.setPrimaryClip(holder.myClip);
                    Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
                } else if (item == 3) { // add to friends
                    ButtonsActions.add_to_fav_user(context, feed.getId(), 3);
                    removeItem(position);
                } else if (item == 4) { // add to ignore
                    ButtonsActions.add_to_fav_user(context, feed.getId(), 4);
                    removeItem(position);
                } else if (item == 5) { // clear
                    ButtonsActions.add_to_fav_user(context, feed.getId(), 5);
                    removeItem(position);
                }
            } catch (Exception e) {
                Log.e("AdapterPmFriends", "Error in dialog action", e);
            }
        });
        builder.show();
    }

    private void removeItem(int position) {
        if (position >= 0 && position < jsonFeed.size()) {
            jsonFeed = new ArrayList<>(jsonFeed);
            jsonFeed.remove(position);
            notifyItemRemoved(position);
        } else {
            Log.e("AdapterPmFriends", "Invalid position for removeItem: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    public List<FeedPm> getData() {
        return jsonFeed;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
            imagePick = itemView.findViewById(R.id.img_btn);
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
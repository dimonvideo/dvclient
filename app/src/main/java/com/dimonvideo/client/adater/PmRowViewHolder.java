package com.dimonvideo.client.adater;

import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.google.android.material.card.MaterialCardView;

/** Shared compact row binding without composing controls or HTML work in the bind path. */
class PmRowViewHolder extends RecyclerView.ViewHolder {
    final TextView textViewTitle, textViewDate, textViewNames, textViewText;
    final ImageView imageView;
    final View statusDot;
    final MaterialCardView card;

    /** Resolves row views once so rebinding allocates no arrays or click listeners. */
    PmRowViewHolder(@NonNull View itemView) {
        super(itemView);
        card = (MaterialCardView) itemView;
        imageView = itemView.findViewById(R.id.thumbnail);
        statusDot = itemView.findViewById(R.id.status_dot);
        textViewTitle = itemView.findViewById(R.id.title);
        textViewDate = itemView.findViewById(R.id.date);
        textViewNames = itemView.findViewById(R.id.name);
        textViewText = itemView.findViewById(R.id.listtext);
    }

    /** Binds scalar metadata and a density-correct avatar; the caller renders HTML separately. */
    void bind(FeedPm feed, boolean unread) {
        textViewTitle.setText(feed.getTitle());
        textViewDate.setText(feed.getDate());
        textViewNames.setText(feed.getLast_poster_name());
        statusDot.setVisibility(unread ? View.VISIBLE : View.GONE);
        textViewTitle.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
        float baseSize = fontSizeBase();
        textViewTitle.setTextSize(baseSize + 2);
        textViewText.setTextSize(baseSize);
        textViewNames.setTextSize(baseSize - 1);
        textViewDate.setTextSize(baseSize - 3);
        int avatarSize = Math.round(48 * imageView.getResources().getDisplayMetrics().density);
        Glide.with(imageView)
                .load(feed.getImageUrl())
                .placeholder(R.drawable.baseline_image_20)
                .error(R.drawable.baseline_image_20)
                .circleCrop()
                .override(avatarSize, avatarSize)
                .into(imageView);
    }

    /** Returns the preview size selected in the application settings without per-bind arrays. */
    static float fontSizeBase() {
        String size = AppController.getInstance().isFontSize();
        if ("smallest".equals(size)) return 12;
        if ("small".equals(size)) return 13;
        if ("large".equals(size)) return 16;
        if ("largest".equals(size)) return 20;
        return 14;
    }

    /** Removes another account's row content before it can be rebound or clicked. */
    void clearMetadata() {
        textViewTitle.setText("");
        textViewDate.setText("");
        textViewNames.setText("");
        textViewText.setText("");
        statusDot.setVisibility(View.GONE);
        clearAvatar();
    }

    /** Cancels the avatar request when RecyclerView recycles or detaches this row. */
    void clearAvatar() {
        Glide.with(imageView.getContext().getApplicationContext()).clear(imageView);
    }
}

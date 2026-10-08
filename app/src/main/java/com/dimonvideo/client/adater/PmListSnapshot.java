package com.dimonvideo.client.adater;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.dimonvideo.client.model.FeedPm;

import java.util.ArrayList;
import java.util.List;

/** Immutable-by-convention API snapshots shared by the two asynchronous PM adapters. */
final class PmListSnapshot {
    static final DiffUtil.ItemCallback<FeedPm> DIFF = new DiffUtil.ItemCallback<FeedPm>() {
        /** Matches a message/member by its stable server identifier. */
        @Override
        public boolean areItemsTheSame(@NonNull FeedPm oldItem, @NonNull FeedPm newItem) {
            return oldItem.getId() == newItem.getId();
        }

        /** Skips a rebind when every API value displayed by the row is unchanged. */
        @Override
        public boolean areContentsTheSame(@NonNull FeedPm oldItem, @NonNull FeedPm newItem) {
            return oldItem.equals(newItem);
        }
    };

    /** Prevents instantiation of the snapshot utility. */
    private PmListSnapshot() { }

    /** Copies both the list and its models before a background diff starts. */
    static List<FeedPm> copy(List<FeedPm> source) {
        List<FeedPm> result = new ArrayList<>(source.size());
        for (FeedPm item : source) result.add(new FeedPm(item));
        return result;
    }
}

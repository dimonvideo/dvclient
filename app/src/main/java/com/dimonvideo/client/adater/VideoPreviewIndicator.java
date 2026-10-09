package com.dimonvideo.client.adater;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.model.Feed;

/** Identifies playable MP4 files from API file links rather than their JPEG screenshot URLs. */
final class VideoPreviewIndicator {
    /** Prevents constructing this stateless indicator policy. */
    private VideoPreviewIndicator() { }

    /** Recognizes MP4 files in the video section, ignoring URL parameters and extension case. */
    static boolean isMp4Video(@Nullable Feed feed) {
        if (feed == null || !Config.VUPLOADER_RAZDEL.equals(feed.getRazdel())
                || feed.getLink() == null) return false;
        String path = Uri.parse(feed.getLink()).getPath();
        return path != null && path.regionMatches(true, path.length() - 4, ".mp4", 0, 4);
    }
}

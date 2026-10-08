package com.dimonvideo.client.adater;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Verifies the API extension policy and recycled preview controls using real card layouts. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class VideoPreviewIndicatorTest {
    private Context context;
    private ActivityController<Activity> touchActivity;

    /** Applies the app's Material theme without starting application services or network requests. */
    @Before
    public void setUp() {
        context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme);
    }

    /** Releases the real window used by the touch regression, including its posted click callbacks. */
    @After
    public void tearDown() {
        if (touchActivity != null) touchActivity.pause().stop().destroy();
    }

    /** The real file extension controls the icon even when the separate thumbnail is a JPEG. */
    @Test
    public void sourceFileDeterminesIndicatorRatherThanScreenshot() {
        Feed feed = feed("https://example.org/video.mp4");
        feed.setImageUrl("https://example.org/screenshot.jpg");
        assertTrue(VideoPreviewIndicator.isMp4Video(feed));

        feed.setLink("https://example.org/archive.zip");
        feed.setImageUrl("https://example.org/screenshot.mp4");
        assertFalse(VideoPreviewIndicator.isMp4Video(feed));
    }

    /** Signed URLs, fragments, upper-case extensions and encoded path characters remain supported. */
    @Test
    public void recognizesDecodedPathWithoutQueryOrFragment() {
        assertTrue(VideoPreviewIndicator.isMp4Video(feed("https://example.org/video.MP4?token=abc#preview")));
        assertTrue(VideoPreviewIndicator.isMp4Video(feed("https://example.org/video.%6Dp4")));
        assertTrue(VideoPreviewIndicator.isMp4Video(feed("/files/video.mp4")));
    }

    /** Empty links, directories, query parameters and non-video sections must never gain an icon. */
    @Test
    public void rejectsMissingOrUnrelatedFiles() {
        assertFalse(VideoPreviewIndicator.isMp4Video(null));
        assertFalse(VideoPreviewIndicator.isMp4Video(feed(null)));
        assertFalse(VideoPreviewIndicator.isMp4Video(feed("")));
        assertFalse(VideoPreviewIndicator.isMp4Video(feed("https://example.mp4")));
        assertFalse(VideoPreviewIndicator.isMp4Video(feed("https://example.org/video.mp4/")));
        assertFalse(VideoPreviewIndicator.isMp4Video(feed("https://example.org/download?file=video.mp4")));
        assertFalse(VideoPreviewIndicator.isMp4Video(feed("https://example.org/video.mp4.zip")));
        Feed gallery = feed("https://example.org/video.mp4");
        gallery.setRazdel(com.dimonvideo.client.Config.GALLERY_RAZDEL);
        assertFalse(VideoPreviewIndicator.isMp4Video(gallery));
    }

    /** Reusing a gallery holder must clear both the video badge and its accessible preview label. */
    @Test
    public void recycledHolderClearsVideoState() {
        AdapterMainRazdel.ViewHolder holder = holder(R.layout.list_row_gallery);
        holder.bindVideoPreview(feed("https://example.org/video.mp4"));
        assertEquals(View.VISIBLE, holder.videoPlayIndicator.getVisibility());
        assertEquals(context.getString(R.string.video_preview), holder.imageView.getContentDescription());

        holder.bindVideoPreview(feed("https://example.org/archive.zip"));
        assertEquals(View.GONE, holder.videoPlayIndicator.getVisibility());
        assertEquals(context.getString(R.string.action_screen), holder.imageView.getContentDescription());

        holder.bindVideoPreview(feed("https://example.org/video.mp4"));
        holder.bindVideoPreview(null);
        assertEquals(View.GONE, holder.videoPlayIndicator.getVisibility());
    }

    /** Touching the decorative overlay must still invoke the existing thumbnail action underneath. */
    @Test
    public void overlayDoesNotInterceptThumbnailClicks() {
        AdapterMainRazdel.ViewHolder holder = holder(R.layout.list_row_gallery);
        holder.bindVideoPreview(feed("https://example.org/video.mp4"));
        AtomicInteger clicks = new AtomicInteger();
        AtomicInteger longClicks = new AtomicInteger();
        holder.imageView.setOnClickListener(view -> clicks.incrementAndGet());
        holder.imageView.setOnLongClickListener(view -> {
            longClicks.incrementAndGet();
            return true;
        });
        assertFalse(holder.videoPlayIndicator.isClickable());
        assertFalse(holder.videoPlayIndicator.isFocusable());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                holder.videoPlayIndicator.getImportantForAccessibility());

        ViewGroup row = (ViewGroup) holder.itemView;
        touchActivity = Robolectric.buildActivity(Activity.class);
        touchActivity.get().setTheme(R.style.AppTheme);
        touchActivity.setup().visible();
        touchActivity.get().setContentView(row);
        assertTrue(row.isAttachedToWindow());
        assertTrue(holder.imageView.isAttachedToWindow());
        row.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST));
        row.layout(0, 0, row.getMeasuredWidth(), row.getMeasuredHeight());
        Rect bounds = new Rect(0, 0, holder.videoPlayIndicator.getWidth(), holder.videoPlayIndicator.getHeight());
        row.offsetDescendantRectToMyCoords(holder.videoPlayIndicator, bounds);
        Rect thumbnailBounds = new Rect(0, 0, holder.imageView.getWidth(), holder.imageView.getHeight());
        row.offsetDescendantRectToMyCoords(holder.imageView, thumbnailBounds);
        assertTrue(thumbnailBounds.contains(bounds.centerX(), bounds.centerY()));
        long touchTime = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(touchTime, touchTime, MotionEvent.ACTION_DOWN,
                bounds.centerX(), bounds.centerY(), 0);
        MotionEvent up = MotionEvent.obtain(touchTime, touchTime + 1, MotionEvent.ACTION_UP,
                bounds.centerX(), bounds.centerY(), 0);
        try {
            assertTrue(row.dispatchTouchEvent(down));
            assertTrue(holder.imageView.isPressed());
            assertTrue(row.dispatchTouchEvent(up));
        } finally {
            down.recycle();
            up.recycle();
        }
        ShadowLooper.shadowMainLooper().idle();
        assertEquals(1, clicks.get());
        holder.imageView.performLongClick();
        assertEquals(1, longClicks.get());
    }

    /** Regular rows have no overlay and continue binding safely without a video-specific view. */
    @Test
    public void standardRowDoesNotRequireVideoOverlay() {
        AdapterMainRazdel.ViewHolder holder = holder(R.layout.list_row);
        assertNull(holder.videoPlayIndicator);
        holder.bindVideoPreview(feed("https://example.org/video.mp4"));
        holder.bindVideoPreview(null);
    }

    /** Supplies a video record with its independent source-file URL from the API. */
    private static Feed feed(String fileLink) {
        Feed feed = new Feed();
        feed.setRazdel(com.dimonvideo.client.Config.VUPLOADER_RAZDEL);
        feed.setLink(fileLink);
        return feed;
    }

    /** Inflates the same themed card and holder used by RecyclerView without constructing an adapter. */
    private AdapterMainRazdel.ViewHolder holder(int layout) {
        FrameLayout parent = new FrameLayout(context);
        return new AdapterMainRazdel.ViewHolder(LayoutInflater.from(context).inflate(layout, parent, false));
    }
}

/*
 * Copyright (c) 2026. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */
package com.dimonvideo.client.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.LruCache;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dimonvideo.client.Config;

import org.xml.sax.XMLReader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Parses list HTML off the UI thread and caches templates without retaining views or image requests.
 * All public methods must be called on the main thread; recycled views must be cleared by their owner.
 */
public final class AsyncHtmlRenderer {
    private static final String TAG = "AsyncHtmlRenderer";
    private static final int CACHE_CHARACTER_BUDGET = 256 * 1024;
    private static final int QUEUED_PARSE_LIMIT = 24;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final WeakHashMap<TextView, Binding> bindings = new WeakHashMap<>();
    private final Set<String> inFlight = new HashSet<>();
    private final Set<ImageTarget> imageTargets = new HashSet<>();
    private final LruCache<String, Spanned> cache = new LruCache<String, Spanned>(CACHE_CHARACTER_BUDGET) {
        /** Accounts for source text, rendered text and spans rather than only the number of rows. */
        @Override
        protected int sizeOf(@NonNull String key, @NonNull Spanned value) {
            return Math.max(1, key.length() + value.length()
                    + value.getSpans(0, value.length(), Object.class).length * 64);
        }
    };
    private ThreadPoolExecutor executor;
    private boolean active;
    private int generation;

    /** Creates a renderer ready to bind views, with a lazily started, bounded worker queue. */
    public AsyncHtmlRenderer() {
        activate();
    }

    /** Recreates background resources when an adapter is attached after a previous release. */
    public void activate() {
        if (active) {
            return;
        }
        active = true;
        generation++;
        executor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUED_PARSE_LIMIT), runnable -> {
                    Thread thread = new Thread(runnable, "dv-html-render");
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(true);
    }

    /**
     * Binds HTML without exposing raw tags while parsing. Formatting, entities, links and lists are
     * preserved; inline image requests are created separately for each view only when requested.
     */
    public void bind(@NonNull TextView view, @Nullable String html, boolean loadImages) {
        clear(view);
        if (!active) {
            view.setText("");
            return;
        }
        String source = html == null ? "" : html;
        Binding binding = new Binding(view, source, loadImages);
        bindings.put(view, binding);
        Spanned cached = cache.get(source);
        if (cached != null) {
            apply(binding, cached);
        } else {
            view.setText("");
            schedule(source);
        }
    }

    /** Cancels the image requests and invalidates delivery for a recycled or dismissed view. */
    public void clear(@NonNull TextView view) {
        Binding previous = bindings.remove(view);
        if (previous != null) {
            clearImages(previous);
        }
    }

    /** Stops parsing and delivery, releases cached text, and clears every outstanding image request. */
    public void release() {
        active = false;
        generation++;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        mainHandler.removeCallbacksAndMessages(null);
        inFlight.clear();
        for (ImageTarget target : new ArrayList<>(imageTargets)) {
            Glide.with(target.binding.applicationContext).clear(target);
        }
        imageTargets.clear();
        bindings.clear();
        cache.evictAll();
    }

    /** Queues one parse per exact source, leaving rejected requests pending until a worker completes. */
    private boolean schedule(String source) {
        if (!active || inFlight.contains(source)) {
            return true;
        }
        final int taskGeneration = generation;
        inFlight.add(source);
        try {
            executor.execute(() -> {
                Spanned parsed;
                try {
                    parsed = parse(source);
                } catch (RuntimeException exception) {
                    Log.w(TAG, "Cannot parse list HTML", exception);
                    parsed = new SpannedString("");
                }
                final Spanned result = parsed;
                mainHandler.post(() -> complete(source, result, taskGeneration));
            });
            return true;
        } catch (RejectedExecutionException exception) {
            inFlight.remove(source);
            return false;
        }
    }

    /** Parses an immutable template; image placeholders do not contain a view or start network work. */
    private static Spanned parse(String source) {
        return new SpannedString(Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY,
                imageSource -> new ColorDrawable(Color.TRANSPARENT), new ListTagHandler()));
    }

    /** Delivers shared work to every matching view, then drains requests deferred by the bounded queue. */
    private void complete(String source, Spanned result, int taskGeneration) {
        if (!active || generation != taskGeneration) {
            return;
        }
        inFlight.remove(source);
        cache.put(source, result);
        for (Binding binding : new ArrayList<>(bindings.values())) {
            if (!binding.delivered && source.equals(binding.source)) {
                apply(binding, result);
            }
        }
        for (Binding binding : new ArrayList<>(bindings.values())) {
            if (!binding.delivered && !schedule(binding.source)) {
                break;
            }
        }
    }

    /** Copies image spans per target so cached templates cannot share mutable drawables between rows. */
    private void apply(Binding binding, Spanned template) {
        TextView view = binding.view.get();
        if (view == null || bindings.get(view) != binding) {
            return;
        }
        binding.delivered = true;
        SpannableStringBuilder text = new SpannableStringBuilder(template);
        ImageSpan[] spans = text.getSpans(0, text.length(), ImageSpan.class);
        for (int index = spans.length - 1; index >= 0; index--) {
            ImageSpan span = spans[index];
            int start = text.getSpanStart(span);
            int end = text.getSpanEnd(span);
            int flags = text.getSpanFlags(span);
            text.removeSpan(span);
            if (binding.loadImages) {
                InlineDrawable placeholder = new InlineDrawable();
                text.setSpan(new ImageSpan(placeholder, span.getSource()), start, end, flags);
                ImageTarget target = new ImageTarget(binding, placeholder, span.getSource());
                binding.images.add(target);
                imageTargets.add(target);
            } else {
                text.delete(start, end);
            }
        }
        view.setText(text);
        for (ImageTarget target : binding.images) {
            Glide.with(view.getContext().getApplicationContext()).asDrawable()
                    .load(resolveImageUrl(target.source)).into(target);
        }
    }

    /** Resolves relative site image URLs, including the emoticons returned by the API. */
    private static String resolveImageUrl(String source) {
        if (source == null) {
            return "";
        }
        if (source.startsWith("//")) {
            return "https:" + source;
        }
        return source.startsWith("/") ? Config.WRITE_URL + source : source;
    }

    /** Clears Glide targets after invalidating their binding, without retaining an Activity context. */
    private void clearImages(Binding binding) {
        for (ImageTarget target : binding.images) {
            Glide.with(binding.applicationContext).clear(target);
            imageTargets.remove(target);
        }
        binding.images.clear();
    }

    /** Tracks one current view binding through a weak reference and per-view image requests. */
    private static final class Binding {
        final WeakReference<TextView> view;
        final Context applicationContext;
        final String source;
        final boolean loadImages;
        final ArrayList<ImageTarget> images = new ArrayList<>();
        boolean delivered;

        /** Captures only the view weakly so queued HTML work cannot keep a screen alive. */
        Binding(TextView view, String source, boolean loadImages) {
            this.view = new WeakReference<>(view);
            this.applicationContext = view.getContext().getApplicationContext();
            this.source = source;
            this.loadImages = loadImages;
        }
    }

    /** Loads one inline image while checking that its view still displays the owning binding. */
    private final class ImageTarget extends CustomTarget<Drawable> {
        private final Binding binding;
        private final InlineDrawable placeholder;
        private final String source;

        /** Associates a mutable drawable with one view rather than the immutable HTML cache. */
        ImageTarget(Binding binding, InlineDrawable placeholder, String source) {
            super(512, 512);
            this.binding = binding;
            this.placeholder = placeholder;
            this.source = source;
        }

        /** Resizes a downloaded image to the current text width and redraws only its current binding. */
        @Override
        public void onResourceReady(@NonNull Drawable resource,
                                    @Nullable Transition<? super Drawable> transition) {
            TextView view = binding.view.get();
            if (!active || view == null || bindings.get(view) != binding) {
                return;
            }
            int width = Math.max(1, resource.getIntrinsicWidth());
            int height = Math.max(1, resource.getIntrinsicHeight());
            int available = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
            if (available > 0 && width > available) {
                height = Math.max(1, Math.round(height * (available / (float) width)));
                width = available;
            }
            resource.setBounds(0, 0, width, height);
            placeholder.drawable = resource;
            placeholder.setBounds(resource.getBounds());
            view.setText(view.getText());
        }

        /** Removes the loaded drawable when the owning row or dialog releases its request. */
        @Override
        public void onLoadCleared(@Nullable Drawable placeholderDrawable) {
            placeholder.drawable = null;
        }
    }

    /** Provides an initially small, per-view image span drawable without allocating placeholder bitmaps. */
    private static final class InlineDrawable extends Drawable {
        private Drawable drawable;

        /** Reserves an inline slot until the asynchronous image request completes. */
        InlineDrawable() {
            setBounds(0, 0, 1, 1);
        }

        /** Draws the image once Glide has populated this placeholder. */
        @Override
        public void draw(@NonNull Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }

        /** Forwards alpha changes to the loaded image. */
        @Override
        public void setAlpha(int alpha) {
            if (drawable != null) {
                drawable.setAlpha(alpha);
            }
        }

        /** Forwards optional text image color filters to the loaded image. */
        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            if (drawable != null) {
                drawable.setColorFilter(colorFilter);
            }
        }

        /** Reports transparency while this drawable may still be an empty placeholder. */
        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /** Preserves the custom list bullet behavior used by the existing forum and comment adapters. */
    private static final class ListTagHandler implements Html.TagHandler {
        /** Adds list separators for platform versions that do not provide their own list formatting. */
        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader reader) {
            if (!opening && "ul".equals(tag)) {
                output.append('\n');
            } else if (opening && "li".equals(tag)) {
                output.append("\n•");
            }
        }
    }
}

/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.dimonvideo.client.Config;

import java.lang.ref.WeakReference;

public class URLImageParser implements Html.ImageGetter {
    private final WeakReference<TextView> container;
    private final Context context; // Добавлено для получения Resources
    private final boolean matchParentWidth;
    private final HtmlImagesHandler imagesHandler;
    private float density = 1.0f;

    public URLImageParser(TextView textView) {
        this(textView, false, false, null);
    }

    public URLImageParser(TextView textView, boolean matchParentWidth, HtmlImagesHandler imagesHandler) {
        this(textView, matchParentWidth, false, imagesHandler);
    }

    public URLImageParser(TextView textView, boolean matchParentWidth, boolean densityAware,
                          @Nullable HtmlImagesHandler imagesHandler) {
        this.container = new WeakReference<>(textView);
        this.context = textView != null ? textView.getContext().getApplicationContext() : null;
        this.matchParentWidth = matchParentWidth;
        this.imagesHandler = imagesHandler;
        if (densityAware && textView != null) {
            density = textView.getResources().getDisplayMetrics().density;
        }
    }

    @Override
    public Drawable getDrawable(String source) {
        String finalUrl;

        if (source.contains("emoticons")) {
            finalUrl = Config.WRITE_URL + source;
        } else {
            finalUrl = source;
        }

        if (imagesHandler != null) {
            imagesHandler.addImage(finalUrl);
        }

        BitmapDrawablePlaceholder drawable = new BitmapDrawablePlaceholder();

        TextView textView = container.get();
        if (textView != null) {
            textView.post(() -> Glide.with(textView.getContext())
                    .asBitmap()
                    .load(finalUrl)
                    .into(drawable));
        } else {
            Log.w("URLImageParser", "TextView is null, skipping image load for URL: " + finalUrl);
        }

        return drawable;
    }

    private class BitmapDrawablePlaceholder extends BitmapDrawable implements Target<Bitmap> {

        protected Drawable drawable;

        BitmapDrawablePlaceholder() {
            super(context != null ? context.getResources() : null, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
            if (context == null) {
                Log.w("URLImageParser", "Context is null, using fallback bitmap");
            }
        }

        @Override
        public void draw(final Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }

        private void setDrawable(Drawable drawable) {
            this.drawable = drawable;
            TextView textView = container.get();
            if (textView == null) {
                Log.w("URLImageParser", "TextView is null, cannot set drawable bounds");
                return;
            }

            int drawableWidth = (int) (drawable.getIntrinsicWidth() * density);
            int drawableHeight = (int) (drawable.getIntrinsicHeight() * density);
            int maxWidth = textView.getMeasuredWidth();
            if ((drawableWidth > maxWidth && maxWidth > 0) || matchParentWidth) {
                int calculatedHeight = maxWidth * drawableHeight / drawableWidth;
                drawable.setBounds(0, 0, maxWidth, calculatedHeight);
                setBounds(0, 0, maxWidth, calculatedHeight);
            } else {
                drawable.setBounds(0, 0, drawableWidth, drawableHeight);
                setBounds(0, 0, drawableWidth, drawableHeight);
            }

            textView.setText(textView.getText());
        }

        @Override
        public void onLoadStarted(@Nullable Drawable placeholderDrawable) {
            if (placeholderDrawable != null) {
                setDrawable(placeholderDrawable);
            }
        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
            if (errorDrawable != null) {
                setDrawable(errorDrawable);
            }
        }

        @Override
        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
            TextView textView = container.get();
            if (textView != null) {
                setDrawable(new BitmapDrawable(textView.getResources(), bitmap));
            } else {
                Log.w("URLImageParser", "TextView is null, cannot set bitmap drawable");
            }
        }

        @Override
        public void onLoadCleared(@Nullable Drawable placeholderDrawable) {
            if (placeholderDrawable != null) {
                setDrawable(placeholderDrawable);
            }
        }

        @Override
        public void getSize(@NonNull SizeReadyCallback cb) {
            cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
        }

        @Override
        public void removeCallback(@NonNull SizeReadyCallback cb) {}

        @Override
        public void setRequest(@Nullable Request request) {}

        @Nullable
        @Override
        public Request getRequest() {
            return null;
        }

        @Override
        public void onStart() {}

        @Override
        public void onStop() {}

        @Override
        public void onDestroy() {}
    }

    public interface HtmlImagesHandler {
        void addImage(String uri);
    }
}
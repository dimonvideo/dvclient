/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    public static class ImageResult {
        public final Bitmap bitmap;
        public final String uploadedUrl;

        public ImageResult(Bitmap bitmap, String uploadedUrl) {
            this.bitmap = bitmap;
            this.uploadedUrl = uploadedUrl;
        }
    }

    public interface ImagePickCallback {
        void onImagePicked(ImageResult result);
        void onError(Exception e);
    }

    public static ActivityResultLauncher<PickVisualMediaRequest> registerImagePicker(
            FragmentActivity activity, ImagePickCallback callback, String razdel) {
        return activity.registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                try {
                    Bitmap bitmap = loadAndProcessImage(activity, uri);
                    String uploadedUrl = NetworkUtils.uploadBitmap(bitmap, activity, razdel);
                    callback.onImagePicked(new ImageResult(bitmap, uploadedUrl));
                } catch (Exception e) {
                    Log.e("ImageUtils", "Error processing image", e);
                    callback.onError(e);
                }
            } else {
                callback.onError(new IllegalStateException("No image selected"));
            }
        });
    }

    public static void launchImagePicker(ActivityResultLauncher<PickVisualMediaRequest> launcher) {
        launcher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    public static Bitmap loadAndProcessImage(Context context, Uri uri) throws IOException {
        // Загружаем Bitmap с учётом масштабирования
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, options);
        }

        // Вычисляем масштабирование для уменьшения памяти
        int targetSize = 960;
        options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize);
        options.inJustDecodeBounds = false;

        Bitmap bitmap;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }

        if (bitmap == null) {
            throw new IOException("Failed to decode bitmap");
        }

        // Корректируем ориентацию
        bitmap = rotateImageIfRequired(context, bitmap, uri);

        // Масштабируем до 960x960
        Matrix matrix = new Matrix();
        matrix.setRectToRect(
                new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                new RectF(0, 0, targetSize, targetSize),
                Matrix.ScaleToFit.CENTER
        );
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        bitmap.recycle(); // Не обязательно, но оставлено для совместимости
        return scaledBitmap;
    }

    public static Bitmap rotateImageIfRequired(Context context, Bitmap bitmap, Uri uri) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Cannot open input stream for URI: " + uri);
            }
            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(bitmap, 270);
                default:
                    return bitmap;
            }
        }
    }

    private static Bitmap rotateImage(Bitmap bitmap, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle(); // Не обязательно, но оставлено для совместимости
        return rotatedBitmap;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
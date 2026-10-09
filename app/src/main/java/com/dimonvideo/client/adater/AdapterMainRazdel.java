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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.db.AppDatabase;
import com.dimonvideo.client.db.ReadMarkEntity;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.ui.main.MainFragmentCommentsFile;
import com.dimonvideo.client.ui.main.MainFragmentViewFile;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.AsyncHtmlRenderer;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class AdapterMainRazdel extends RecyclerView.Adapter<AdapterMainRazdel.ViewHolder> {

    private final Context context;
    private final AppCompatActivity activity;
    private final List<Feed> jsonFeed;
    private final AppDatabase database;
    private final Handler mainHandler;
    private final AppController appController;
    private final Executor executor;
    private final LruCache<String, Integer> statusCache = new LruCache<>(300);
    private final AsyncHtmlRenderer htmlRenderer = new AsyncHtmlRenderer();
    private final Set<String> requestedStatusKeys = ConcurrentHashMap.newKeySet();
    private volatile boolean released;
    private volatile int statusGeneration;
    private final List<StatusUpdate> pendingStatusUpdates = new ArrayList<>();
    private static final Object PAYLOAD_STATUS_ONLY = new Object();

    /** Marks every loaded item as read immediately while preserving pending database writes. */
    public void markAllReadInUi() {
        flushStatusUpdates();

        for (Feed feed : jsonFeed) {
            String cacheKey = feed.getId() + "_" + feed.getRazdel();
            statusCache.put(cacheKey, 1);
        }

        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_STATUS_ONLY);
    }


    /** Initializes the feed snapshot and asynchronously preloads its bounded read-status cache. */
    public AdapterMainRazdel(List<Feed> jsonFeed, Context context, AppCompatActivity activity, AppDatabase database) {
        this.jsonFeed = new ArrayList<>(jsonFeed);
        this.context = context;
        this.activity = activity;
        this.database = database;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.appController = AppController.getInstance();
        this.executor = appController.getExecutor();
        setHasStableIds(true);
        preloadStatuses(jsonFeed);
    }

    /** Applies list changes and refreshes only read-status indicators after the database preload. */
    public void updateFeed(List<Feed> newFeed) {
        FeedDiffCallback diffCallback = new FeedDiffCallback(jsonFeed, newFeed);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        jsonFeed.clear();
        jsonFeed.addAll(newFeed);
        diffResult.dispatchUpdatesTo(this);
        preloadStatuses(newFeed);
    }

    /** Stores a read indicator in the bounded, thread-safe cache without clearing unrelated entries. */
    public void addToCache(String key, int status) {
        statusCache.put(key, status);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == 1 ? R.layout.list_row_gallery : R.layout.list_row;
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        Feed feed = jsonFeed.get(position);
        if (feed.getRazdel() != null && (feed.getRazdel().equals(Config.GALLERY_RAZDEL) || feed.getRazdel().equals(Config.VUPLOADER_RAZDEL))) {
            return 1;
        }
        return 0;
    }
    /** Returns a cached read mark and reloads evicted entries asynchronously for long lists. */
    private int getCachedStatus(@NonNull Feed feed) {
        String cacheKey = feed.getId() + "_" + feed.getRazdel();
        Integer status = statusCache.get(cacheKey);
        if (status == null && !released) {
            preloadStatuses(Collections.singletonList(feed));
        }
        return status != null ? status : 0;
    }


    /** Applies read-status payloads without parsing HTML or restarting row image requests. */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains(PAYLOAD_STATUS_ONLY)) {
            Feed feed = jsonFeed.get(position);
            int status = getCachedStatus(feed);
            holder.status_logo.setImageResource(status == 1 ? R.drawable.ic_status_gray : R.drawable.ic_status_green);
            return;
        }
        // если payload не наш — делаем полный bind
        onBindViewHolder(holder, position);
    }


    /** Binds feed metadata while HTML is formatted off the main thread. */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Feed feed = jsonFeed.get(position);

        holder.bindVideoPreview(feed);

        final boolean is_vuploader_play = appController.isVuploaderPlay();
        final boolean is_muzon_play = appController.isMuzonPlay();
        final boolean is_share_btn = appController.isShareBtn();

        int status = getCachedStatus(feed);
        holder.status_logo.setImageResource(
                status == 1 ? R.drawable.ic_status_gray : R.drawable.ic_status_green
        );



        if (feed.getState() == 0 && appController.isUserGroup() <= 2) {
            holder.btn_odob.setVisibility(View.VISIBLE);
            holder.btn_odob.setOnClickListener(v -> {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION) {
                    return;
                }
                NetworkUtils.getOdob(feed.getRazdel(), feed.getId());
                jsonFeed.remove(currentPosition);
                notifyItemRemoved(currentPosition);
                executor.execute(() -> database.readMarkDao().delete(feed.getId(), feed.getRazdel()));
            });
        } else {
            holder.btn_odob.setVisibility(View.GONE);
        }

        Glide.with(holder.itemView.getContext()).clear(holder.imageView);
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.baseline_image_20)
                .error(R.drawable.baseline_image_20)
                .transform(new CenterCrop(), new RoundedCorners(15))
                .override(getItemViewType(position) == 1 ? 250 : 80);
        Glide.with(holder.itemView.getContext())
                .load(feed.getImageUrl())
                .apply(options)
                .into(holder.imageView);

        holder.textViewTitle.setText(feed.getTitle());

        bindHtmlTextAsync(holder, feed);

        holder.textViewDate.setText(feed.getDate());
        holder.textViewCategory.setText(feed.getCategory());
        holder.textViewComments.setText(String.valueOf(feed.getComments()));
        holder.textViewName.setText(feed.getUser());
        holder.textViewHits.setText(String.valueOf(feed.getHits()));

        // Массив всех нужных TextView
        TextView[] textViews = {
                holder.textViewTitle,
                holder.textViewText,
                holder.textViewDate,
                holder.textViewCategory,
                holder.textViewComments,
                holder.textViewName,
                holder.textViewHits
        };

        // Массивы размеров для каждого режима
        float[] sizesSmallest = {14, 13, 12, 12, 12, 12, 12};
        float[] sizesSmall    = {16, 15, 14, 14, 14, 14, 14};
        float[] sizesNormal   = {18, 17, 16, 16, 16, 16, 16};
        float[] sizesLarge    = {20, 19, 18, 18, 18, 18, 18};
        float[] sizesLargest  = {24, 23, 22, 22, 22, 22, 22};

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


        holder.textViewComments.setVisibility(feed.getComments() == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.rating_logo.setVisibility(feed.getComments() == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.fav_star.setVisibility(feed.getFav() > 0 ? View.VISIBLE : View.GONE);
        holder.fav_star.setOnClickListener(v -> removeFav(holder.getBindingAdapterPosition()));

        View.OnClickListener commentsListener = view -> openComments(feed, feed.getId());
        holder.textViewComments.setOnClickListener(commentsListener);
        holder.rating_logo.setOnClickListener(commentsListener);

        holder.itemView.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            queueStatusUpdate(feed.getId(), feed.getRazdel());
            openFile(feed);
        });

        holder.imageView.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            queueStatusUpdate(feed.getId(), feed.getRazdel());
            ButtonsActions.loadScreen(context, feed.getImageUrl());
        });

        if (feed.getRazdel() != null) {
            if (feed.getRazdel().equals(Config.VUPLOADER_RAZDEL) && is_vuploader_play) {
                holder.imageView.setOnClickListener(view -> {
                    queueStatusUpdate(feed.getId(), feed.getRazdel());
                    holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                    ButtonsActions.PlayVideo(context, feed.getLink());
                });
            } else if (feed.getRazdel().equals(Config.MUZON_RAZDEL) && is_muzon_play) {
                holder.imageView.setOnClickListener(view -> {
                    queueStatusUpdate(feed.getId(), feed.getRazdel());
                    holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                    ButtonsActions.PlayVideo(context, feed.getLink());
                });
            }
        }

        View.OnLongClickListener dialogListener = view -> {
            show_dialog(holder, holder.getBindingAdapterPosition());
            return true;
        };
        holder.itemView.setOnLongClickListener(dialogListener);
        holder.imageView.setOnLongClickListener(dialogListener);

        holder.small_download.setOnClickListener(view -> DownloadFile.download(context, feed.getLink(), feed.getRazdel()));
        holder.small_download.setVisibility((feed.getSize() == null || feed.getSize().startsWith("0")) ? View.GONE : View.VISIBLE);

        holder.small_share.setEnabled(!is_share_btn);
        holder.small_share.setOnClickListener(view -> {
            String url = Config.WRITE_URL + "/" + feed.getRazdel() + "/" + feed.getId();
            if (feed.getRazdel().equals(Config.COMMENTS_RAZDEL)) {
                url = Config.WRITE_URL + "/" + feed.getId() + "-news.html";
            }
            Intent sendIntent = new Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, url)
                    .setType("text/plain");
            context.startActivity(Intent.createChooser(sendIntent, feed.getTitle()));
        });
    }

    /** Loads a stable feed snapshot off-thread and updates status payloads without rebinding HTML. */
    private void preloadStatuses(List<Feed> feeds) {
        if (released) {
            return;
        }
        final int loadGeneration = statusGeneration;
        List<Feed> snapshot = new ArrayList<>();
        for (Feed feed : feeds) {
            String cacheKey = feed.getId() + "_" + feed.getRazdel();
            if (statusCache.get(cacheKey) == null
                    && requestedStatusKeys.add(loadGeneration + "_" + cacheKey)) {
                snapshot.add(feed);
            }
        }
        if (snapshot.isEmpty()) {
            return;
        }
        executor.execute(() -> {
            try {
                for (Feed feed : snapshot) {
                    if (released || loadGeneration != statusGeneration) {
                        return;
                    }
                    String cacheKey = feed.getId() + "_" + feed.getRazdel();
                    if (statusCache.get(cacheKey) == null) {
                        int status = database.readMarkDao().getStatus(feed.getId(), feed.getRazdel());
                        synchronized (statusCache) {
                            if (!released && loadGeneration == statusGeneration
                                    && statusCache.get(cacheKey) == null) {
                                statusCache.put(cacheKey, status);
                            }
                        }
                    }
                }
                mainHandler.post(() -> {
                    if (!released && loadGeneration == statusGeneration) {
                        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_STATUS_ONLY);
                    }
                });
            } finally {
                for (Feed feed : snapshot) {
                    requestedStatusKeys.remove(loadGeneration + "_" + feed.getId() + "_" + feed.getRazdel());
                }
            }
        });
    }

    /** Delegates HTML rendering to the bounded cache without showing source tags while parsing. */
    private void bindHtmlTextAsync(@NonNull ViewHolder holder, @NonNull Feed feed) {
        htmlRenderer.bind(holder.textViewText, feed.getText(), false);
    }

    /** Updates the cached read indicator immediately and batches its eventual database persistence. */
    private void queueStatusUpdate(int lid, String razdel) {
        statusCache.put(lid + "_" + razdel, 1);
        synchronized (pendingStatusUpdates) {
            pendingStatusUpdates.add(new StatusUpdate(lid, razdel, 1));
            if (pendingStatusUpdates.size() >= 10) {
                flushStatusUpdates();
            }
        }
    }

    /** Persists the pending read marks even when the list is leaving, avoiding stale UI callbacks. */
    private void flushStatusUpdates() {
        List<StatusUpdate> updates;
        synchronized (pendingStatusUpdates) {
            if (pendingStatusUpdates.isEmpty()) return;
            updates = new ArrayList<>(pendingStatusUpdates);
            pendingStatusUpdates.clear();
        }
        final int updateGeneration = statusGeneration;
        executor.execute(() -> {
            List<ReadMarkEntity> readMarks = new ArrayList<>();
            for (StatusUpdate update : updates) {
                ReadMarkEntity readMark = new ReadMarkEntity();
                readMark.lid = update.lid;
                readMark.razdel = update.razdel;
                readMark.status = update.status;
                readMarks.add(readMark);
                statusCache.put(update.lid + "_" + update.razdel, update.status);
            }
            database.readMarkDao().insertAll(readMarks);
            mainHandler.post(() -> {
                if (!released && updateGeneration == statusGeneration) {
                    notifyItemRangeChanged(0, getItemCount(), PAYLOAD_STATUS_ONLY);
                }
            });
        });
    }

    private void openFile(Feed feed) {
        MainFragmentViewFile fragment = new MainFragmentViewFile();
        Bundle bundle = new Bundle();
        bundle.putString(Config.TAG_RAZDEL, feed.getRazdel());
        bundle.putString(Config.TAG_ID, String.valueOf(feed.getId()));
        bundle.putString(Config.TAG_TITLE, feed.getTitle());
        bundle.putString(Config.TAG_DATE, feed.getDate());
        bundle.putString(Config.TAG_CATEGORY, feed.getCategory());
        bundle.putInt(Config.TAG_PLUS, feed.getPlus());
        bundle.putString(Config.TAG_USER, feed.getUser());
        bundle.putString(Config.TAG_TEXT, feed.getFull_text());
        bundle.putString(Config.TAG_IMAGE_URL, feed.getImageUrl());
        bundle.putString(Config.TAG_MOD, feed.getMod());
        bundle.putInt(Config.TAG_STATUS, feed.getStatus());
        bundle.putInt(Config.TAG_COMMENTS, feed.getComments());
        bundle.putString(Config.TAG_LINK, feed.getLink());
        bundle.putString(Config.TAG_SIZE, feed.getSize());
        bundle.putInt(Config.TAG_FAV, feed.getFav());
        fragment.setArguments(bundle);
        fragment.show(activity.getSupportFragmentManager(), "MainFragmentViewFile");
    }

    private void openComments(Feed feed, int lid) {
        String comm_url = Config.COMMENTS_READS_URL + feed.getRazdel() + "&lid=" + lid + "&min=";
        MainFragmentCommentsFile fragment = new MainFragmentCommentsFile();
        Bundle bundle = new Bundle();
        bundle.putString(Config.TAG_TITLE, feed.getTitle());
        bundle.putString(Config.TAG_ID, String.valueOf(lid));
        bundle.putString(Config.TAG_LINK, comm_url);
        bundle.putString(Config.TAG_RAZDEL, feed.getRazdel());
        fragment.setArguments(bundle);
        fragment.show(activity.getSupportFragmentManager(), "MainFragmentCommentsFile");
    }

    /** Shows actions for a current holder, ignoring positions invalidated by list removal. */
    private void show_dialog(ViewHolder holder, int position) {
        if (position == RecyclerView.NO_POSITION || position >= jsonFeed.size()) {
            return;
        }
        final Feed feed = jsonFeed.get(position);
        String message = feed.getFav() > 0 ? context.getString(R.string.menu_unfav) : context.getString(R.string.menu_fav);
        final CharSequence[] items = {
                context.getString(R.string.menu_share_title),
                context.getString(R.string.action_open),
                message,
                context.getString(R.string.action_like),
                context.getString(R.string.action_screen),
                context.getString(R.string.download),
                context.getString(R.string.copy_listtext),
                context.getString(R.string.put_to_news)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.WRITE_URL + "/" + feed.getRazdel() + "/" + feed.getId();
        if (feed.getRazdel().equals(Config.COMMENTS_RAZDEL)) {
            holder.url = Config.WRITE_URL + "/" + feed.getId() + "-news.html";
        }

        holder.myClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(feed.getTitle());
        builder.setItems(items, (dialog, item) -> {
            switch (item) {
                case 0: // share
                    Intent sendIntent = new Intent(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_TEXT, holder.url)
                            .setType("text/plain");
                    context.startActivity(Intent.createChooser(sendIntent, feed.getTitle()));
                    break;
                case 1: // browser
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url)));
                    break;
                case 2: // fav
                    int invertedFav = (feed.getFav() == 1) ? 2 : 1;
                    ButtonsActions.add_to_fav_file(context, feed.getRazdel(), feed.getId(), invertedFav);
                    break;
                case 3: // like
                    ButtonsActions.like_file(context, feed.getRazdel(), feed.getId(), 1);
                    break;
                case 4: // screen
                    ButtonsActions.loadScreen(context, feed.getImageUrl());
                    break;
                case 5: // download
                    DownloadFile.download(context, feed.getLink(), feed.getRazdel());
                    break;
                case 6: // copy text
                    holder.myClip = ClipData.newPlainText("text", Html.fromHtml(feed.getText(), Html.FROM_HTML_MODE_LEGACY).toString());
                    holder.myClipboard.setPrimaryClip(holder.myClip);
                    Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
                    break;
                case 7: // to news
                    NetworkUtils.putToNews(feed.getRazdel(), feed.getId());
                    break;
            }
        });
        builder.show();
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    /** Combines the section and server identifier without truncating both into a 32-bit string hash. */
    @Override
    public long getItemId(int position) {
        Feed feed = jsonFeed.get(position);
        return ((long) String.valueOf(feed.getRazdel()).hashCode() << 32)
                | (feed.getId() & 0xffffffffL);
    }

    /** Recreates HTML resources and restores read status loading after a list is attached again. */
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        released = false;
        htmlRenderer.activate();
        preloadStatuses(jsonFeed);
    }

    /** Cancels row-specific HTML and image work before a holder enters the recycled pool. */
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.bindVideoPreview(null);
        htmlRenderer.clear(holder.textViewText);
        Glide.with(holder.itemView.getContext()).clear(holder.imageView);
        super.onViewRecycled(holder);
    }

    /** Persists read marks and releases view-bound HTML work when the RecyclerView detaches. */
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        cleanup();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    /** Removes the current favorite row without acting on an already recycled holder. */
    public void removeFav(int position) {
        if (position == RecyclerView.NO_POSITION || position >= jsonFeed.size()) {
            return;
        }
        Feed feed = jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyItemRemoved(position);
        String cacheKey = feed.getId() + "_" + feed.getRazdel();
        statusCache.remove(cacheKey); // Удаляем из кэша
        ButtonsActions.add_to_fav_file(context, feed.getRazdel(), feed.getId(), 2);
    }

    // Класс для хранения обновлений статусов
    private static class StatusUpdate {
        final int lid;
        final String razdel;
        final int status;

        StatusUpdate(int lid, String razdel, int status) {
            this.lid = lid;
            this.razdel = razdel;
            this.status = status;
        }
    }

    // DiffUtil для эффективного обновления списка
    private static class FeedDiffCallback extends DiffUtil.Callback {
        private final List<Feed> oldList;
        private final List<Feed> newList;

        FeedDiffCallback(List<Feed> oldList, List<Feed> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId() &&
                    oldList.get(oldItemPosition).getRazdel().equals(newList.get(newItemPosition).getRazdel());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle, textViewDate, textViewComments, textViewCategory, textViewHits, textViewName;
        public ImageView imageView, rating_logo, status_logo, fav_star, small_share, small_download;
        final ImageView videoPlayIndicator;
        public TextView textViewText;
        public String url;
        public ProgressBar progressBar;
        public LinearLayout name;
        public ClipboardManager myClipboard;
        public ClipData myClip;
        public Button btn_odob;

        /** Resolves row controls once, including the optional gallery/video preview indicator. */
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
            videoPlayIndicator = itemView.findViewById(R.id.video_play_indicator);
            rating_logo = itemView.findViewById(R.id.rating_logo);
            fav_star = itemView.findViewById(R.id.fav);
            status_logo = itemView.findViewById(R.id.status);
            textViewTitle = itemView.findViewById(R.id.title);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewName = itemView.findViewById(R.id.by_name);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewHits = itemView.findViewById(R.id.views_count);
            progressBar = itemView.findViewById(R.id.progressBar);
            name = itemView.findViewById(R.id.name_layout);
            small_share = itemView.findViewById(R.id.small_share);
            small_download = itemView.findViewById(R.id.small_download);
            btn_odob = itemView.findViewById(R.id.btn_odob);
        }

        /** Resets recycled preview state without changing the thumbnail's existing click actions. */
        void bindVideoPreview(@Nullable Feed feed) {
            if (videoPlayIndicator == null) return;
            boolean video = VideoPreviewIndicator.isMp4Video(feed);
            videoPlayIndicator.setVisibility(video ? View.VISIBLE : View.GONE);
            imageView.setContentDescription(itemView.getContext().getString(
                    video ? R.string.video_preview : R.string.action_screen));
        }
    }

    /** Flushes pending database writes and invalidates every callback tied to the departing view. */
    public void cleanup() {
        released = true;
        statusGeneration++;
        flushStatusUpdates();
        mainHandler.removeCallbacksAndMessages(null);
        requestedStatusKeys.clear();
        statusCache.evictAll();
        htmlRenderer.release();
    }
}

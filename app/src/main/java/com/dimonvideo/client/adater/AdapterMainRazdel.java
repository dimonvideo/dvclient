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
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class AdapterMainRazdel extends RecyclerView.Adapter<AdapterMainRazdel.ViewHolder> {

    private final Context context;
    private final AppCompatActivity activity;
    private final List<Feed> jsonFeed;
    private final AppDatabase database;
    private final Handler mainHandler;
    private final AppController appController;
    private final Executor executor;
    private final Map<String, Integer> statusCache = new HashMap<>();
    private final List<StatusUpdate> pendingStatusUpdates = new ArrayList<>();
    private static final int MAX_CACHE_SIZE = 300;

    public AdapterMainRazdel(List<Feed> jsonFeed, Context context, AppCompatActivity activity, AppDatabase database) {
        this.jsonFeed = new ArrayList<>(jsonFeed);
        this.context = context;
        this.activity = activity;
        this.database = database;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.appController = AppController.getInstance();
        this.executor = appController.getExecutor();
        preloadStatuses(jsonFeed);
    }

    public void updateFeed(List<Feed> newFeed) {
        FeedDiffCallback diffCallback = new FeedDiffCallback(jsonFeed, newFeed);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        jsonFeed.clear();
        jsonFeed.addAll(newFeed);
        diffResult.dispatchUpdatesTo(this);
        preloadStatuses(newFeed);
    }

    public void addToCache(String key, int status) {
        if (statusCache.size() >= MAX_CACHE_SIZE) {
            statusCache.clear();
        }
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Feed feed = jsonFeed.get(position);

        final boolean is_vuploader_play = appController.isVuploaderPlay();
        final boolean is_muzon_play = appController.isMuzonPlay();
        final boolean is_share_btn = appController.isShareBtn();

        String cacheKey = feed.getId() + "_" + feed.getRazdel();
        Integer cachedStatus = statusCache.getOrDefault(cacheKey, 0);
        holder.status_logo.setImageResource(cachedStatus == 1 ? R.drawable.ic_status_gray : R.drawable.ic_status_green);

        if (feed.getState() == 0 && appController.isUserGroup() <= 2) {
            holder.btn_odob.setVisibility(View.VISIBLE);
            holder.btn_odob.setOnClickListener(v -> {
                NetworkUtils.getOdob(feed.getRazdel(), feed.getId());
                jsonFeed.remove(position);
                notifyItemRemoved(position);
                statusCache.remove(cacheKey);
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
        holder.textViewText.setText(Html.fromHtml(feed.getText(), Html.FROM_HTML_MODE_LEGACY));
        holder.textViewDate.setText(feed.getDate());
        holder.textViewCategory.setText(feed.getCategory());
        holder.textViewComments.setText(String.valueOf(feed.getComments()));
        holder.textViewComments.setVisibility(feed.getComments() == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.rating_logo.setVisibility(feed.getComments() == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.textViewName.setText(feed.getUser());
        holder.textViewHits.setText(String.valueOf(feed.getHits()));

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

    private void preloadStatuses(List<Feed> feeds) {
        executor.execute(() -> {
            List<Feed> feedsToLoad = new ArrayList<>();
            for (Feed feed : feeds) {
                String cacheKey = feed.getId() + "_" + feed.getRazdel();
                if (!statusCache.containsKey(cacheKey)) {
                    feedsToLoad.add(feed);
                }
            }
            for (Feed feed : feedsToLoad) {
                String cacheKey = feed.getId() + "_" + feed.getRazdel();
                int status = database.readMarkDao().getStatus(feed.getId(), feed.getRazdel());
                statusCache.put(cacheKey, status);
            }
            mainHandler.post(this::notifyDataSetChanged);
        });
    }

    private void queueStatusUpdate(int lid, String razdel) {
        synchronized (pendingStatusUpdates) {
            pendingStatusUpdates.add(new StatusUpdate(lid, razdel, 1));
            if (pendingStatusUpdates.size() >= 10) {
                flushStatusUpdates();
            }
        }
    }

    private void flushStatusUpdates() {
        List<StatusUpdate> updates;
        synchronized (pendingStatusUpdates) {
            if (pendingStatusUpdates.isEmpty()) return;
            updates = new ArrayList<>(pendingStatusUpdates);
            pendingStatusUpdates.clear();
        }
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
            mainHandler.post(this::notifyDataSetChanged);
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
        bundle.putInt(Config.TAG_STATUS, feed.getState());
        bundle.putInt(Config.TAG_COMMENTS, feed.getComments());
        bundle.putString(Config.TAG_LINK, feed.getLink());
        bundle.putString(Config.TAG_SIZE, feed.getSize());
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

    private void show_dialog(ViewHolder holder, int position) {
        final Feed feed = jsonFeed.get(position);
        final CharSequence[] items = {
                context.getString(R.string.menu_share_title),
                context.getString(R.string.action_open),
                context.getString(R.string.menu_fav),
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
                    ButtonsActions.add_to_fav_file(context, feed.getRazdel(), feed.getId(), 1);
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

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void removeFav(int position) {
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
        public TextView textViewText;
        public String url;
        public ProgressBar progressBar;
        public LinearLayout name;
        public ClipboardManager myClipboard;
        public ClipData myClip;
        public Button btn_odob;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
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
    }

    // Очистка ресурсов
    public void cleanup() {
        flushStatusUpdates();
        statusCache.clear();
    }
}
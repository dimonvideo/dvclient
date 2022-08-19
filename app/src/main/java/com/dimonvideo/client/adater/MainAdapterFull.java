package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.db.Provider;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.ui.main.Comments;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;

import org.xml.sax.XMLReader;

import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainAdapterFull extends RecyclerView.Adapter<MainAdapterFull.ViewHolder> {

    private final Context context;

    //List to store all
    List<Feed> jsonFeed;

    //Constructor of this class
    public MainAdapterFull(List<Feed> jsonFeed, Context context) {
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if ((Feed.getRazdel() != null) && ((Feed.getRazdel().equals(Config.GALLERY_RAZDEL)) || (Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL)))) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_gallery, parent, false);
            return new ViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row, parent, false);
            return new ViewHolder(v);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final Feed Feed = jsonFeed.get(position);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean is_vuploader_play = sharedPrefs.getBoolean("dvc_vuploader_play", true);
        final boolean is_muzon_play = sharedPrefs.getBoolean("dvc_muzon_play", true);
        final boolean is_open_link = sharedPrefs.getBoolean("dvc_open_link", false);
        final boolean is_share_btn = sharedPrefs.getBoolean("dvc_btn_share", false);
        final boolean is_vuploader_play_listtext = sharedPrefs.getBoolean("dvc_vuploader_play_listtext", false);

        holder.status_logo.setImageResource(R.drawable.ic_status_green);
        try {
            int status;
            Cursor cursor = Provider.getOneData(String.valueOf(Feed.getId()), com.dimonvideo.client.model.Feed.getRazdel());
            if (cursor != null) {
                status = cursor.getInt(2);
                if (status == 1) holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                Log.i("---", "select: " + String.valueOf(cursor.getInt(0)));
                cursor.close();
            }

        } catch (Throwable ignored) {
        }

        //Loading image from url
        Glide.with(context).load(Feed.getImageUrl()).apply(RequestOptions.bitmapTransform(new RoundedCorners(14))).into(holder.imageView);

        holder.textViewTitle.setText(Feed.getTitle());
        try {
            Spanned spanned = Html.fromHtml(Feed.getText(), null, new MainAdapter.TagHandler());
            holder.textViewText.setText(spanned);
            holder.textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    // open links from listtext
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context);
                }
            });
        } catch (Throwable ignored) {
        }


        holder.textViewDate.setText(Feed.getDate());
        holder.textViewCategory.setText(Feed.getCategory());
        holder.textViewComments.setText(String.valueOf(Feed.getComments()));
        holder.textViewComments.setVisibility(View.VISIBLE);
        holder.rating_logo.setVisibility(View.VISIBLE);
        holder.textViewName.setText(Feed.getUser());
        holder.textViewComments.setOnClickListener(view -> {
            String comm_url = Config.COMMENTS_READS_URL + com.dimonvideo.client.model.Feed.getRazdel() + "&lid=" + Feed.getId() + "&min=";
            Intent intent = new Intent(context, Comments.class);
            intent.putExtra(Config.TAG_TITLE, Feed.getTitle());
            intent.putExtra(Config.TAG_LINK, comm_url);
            intent.putExtra(Config.TAG_ID, String.valueOf(Feed.getId()));
            intent.putExtra(Config.TAG_RAZDEL, com.dimonvideo.client.model.Feed.getRazdel());
            context.startActivity(intent);

        });
        if (Feed.getComments() == 0) {
            holder.textViewComments.setVisibility(View.INVISIBLE);
            holder.rating_logo.setVisibility(View.INVISIBLE);
        }
        if (Feed.getFav() > 0) {
            holder.fav_star.setVisibility(View.VISIBLE);
            holder.fav_star.setOnClickListener(v -> removeFav(position));
        }
        holder.textViewHits.setText(String.valueOf(Feed.getHits()));

        holder.itemView.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            if (holder.btn_comms.getVisibility() == View.VISIBLE) {
                hide_content(holder, position);
            } else {

                try { open_content(holder, position, context, is_share_btn);
                } catch (Throwable ignored) {
                }
            }

        });

        holder.textViewText.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            if (holder.btn_comms.getVisibility() != View.VISIBLE) {
                try { open_content(holder, position, context, is_share_btn);
                } catch (Throwable ignored) {
                }
            }
        });

        holder.imageView.setOnClickListener(v -> ButtonsActions.loadScreen(context, Feed.getImageUrl()));

        try {
            if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL) && is_vuploader_play))
                holder.imageView.setOnClickListener(v -> ButtonsActions.PlayVideo(context, Feed.getLink()));
        } catch (Exception ignored) {
        }
        try {
            if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.MUZON_RAZDEL) && is_muzon_play))
                holder.imageView.setOnClickListener(v -> ButtonsActions.PlayVideo(context, Feed.getLink()));
        } catch (Exception ignored) {
        }
        if (Feed.getMin() > 0) {
            holder.btn_comms.setVisibility(View.GONE);
            holder.txt_plus.setVisibility(View.GONE);
            holder.likeButton.setVisibility(View.GONE);
            holder.starButton.setVisibility(View.GONE);
            holder.btn_mp4.setVisibility(View.GONE);
            holder.btn_download.setVisibility(View.GONE);
            holder.btn_share.setVisibility(View.GONE);
            holder.btn_mod.setVisibility(View.GONE);
        }

        // dialog menu
        holder.itemView.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });
        holder.imageView.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });
        holder.textViewText.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });



    }
    public static class TagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag,
                              Editable output, XMLReader xmlReader) {
            if (!opening && tag.equals("ul")) {
                output.append("\n");
            }
            if (opening && tag.equals("li")) {
                output.append("\n\u2022");
            }
        }
    }

    // dialog
    private void show_dialog(ViewHolder holder, final int position, Context context) {
        final CharSequence[] items = {context.getString(R.string.menu_share_title), context.getString(R.string.action_open),
                context.getString(R.string.menu_fav), context.getString(R.string.action_like), context.getString(R.string.action_screen),
                context.getString(R.string.download), context.getString(R.string.copy_listtext)};
        final Feed Feed = jsonFeed.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.BASE_URL + "/" + com.dimonvideo.client.model.Feed.getRazdel() + "/" + Feed.getId();
        if (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
            holder.url = Config.BASE_URL + "/" + Feed.getId() + "-news.html";

        holder.myClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(Feed.getTitle());
        builder.setItems(items, (dialog, item) -> {
            if (item == 0) { // share
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, holder.url);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, Feed.getTitle());
                try {
                    context.startActivity(shareIntent);
                } catch (Throwable ignored) {
                }
            }
            if (item == 1) { // browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                try {
                    context.startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
            }
            if (item == 2) { // fav
                ButtonsActions.add_to_fav_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1);
            }
            if (item == 3) { // like
                ButtonsActions.like_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1);
            }
            if (item == 4) { // screen
                ButtonsActions.loadScreen(context, Feed.getImageUrl());
            }
            if (item == 5) { // download
                DownloadFile.download(context, Feed.getLink(), com.dimonvideo.client.model.Feed.getRazdel());
            }
            if (item == 6) { // copy text
                try { holder.myClip = ClipData.newPlainText("text", Html.fromHtml(Feed.getFull_text()).toString());
                    holder.myClipboard.setPrimaryClip(holder.myClip);
                } catch (Throwable ignored) {
                }
                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    // подробный вывод файла
    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void open_content(ViewHolder holder, final int position, Context context, Boolean is_share_btn) {
        final Feed Feed = jsonFeed.get(position);
        holder.txt_plus.setVisibility(View.VISIBLE);
        holder.likeButton.setVisibility(View.VISIBLE);
        holder.starButton.setVisibility(View.VISIBLE);
        holder.name.setVisibility(View.VISIBLE);
        holder.txt_plus.setText(String.valueOf(Feed.getPlus()));
        try {
            URLImageParser parser = new URLImageParser(holder.textViewText, context, position);
            Spanned spanned = Html.fromHtml(Feed.getText(), parser, new MainAdapter.TagHandler());
            holder.textViewText.setText(spanned);
            holder.textViewText.setMovementMethod(LinkMovementMethod.getInstance());
        } catch (Throwable ignored) {
        }
        holder.btn_comms.setOnClickListener(view -> {
            String comm_url = Config.COMMENTS_READS_URL + com.dimonvideo.client.model.Feed.getRazdel() + "&lid=" + Feed.getId() + "&min=";
            Intent intent = new Intent(context, Comments.class);
            intent.putExtra(Config.TAG_TITLE, Feed.getTitle());
            intent.putExtra(Config.TAG_LINK, comm_url);
            intent.putExtra(Config.TAG_ID, String.valueOf(Feed.getId()));
            intent.putExtra(Config.TAG_RAZDEL, com.dimonvideo.client.model.Feed.getRazdel());
            context.startActivity(intent);

        });
        if (Feed.getComments() > 0) {
            String comText = context.getResources().getString(R.string.Comments) + " " + Feed.getComments();
            holder.btn_comms.setText(comText);
        } else {
            String comText = context.getResources().getString(R.string.Comments) + " " + 0;
            holder.btn_comms.setText(comText);
        }
        // смотреть онлайн
        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL))) {
            holder.btn_mp4.setVisibility(View.VISIBLE);
            holder.btn_mp4.setOnClickListener(view -> ButtonsActions.PlayVideo(context, Feed.getLink()));
        }
        // слушать онлайн
        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.MUZON_RAZDEL))) {
            holder.btn_mp4.setVisibility(View.VISIBLE);
            holder.btn_mp4.setText(R.string.listen_online);
            holder.btn_mp4.setOnClickListener(view -> ButtonsActions.PlayVideo(context, Feed.getLink()));
        }
        // если нет размера файла
        if ((Feed.getSize() == null) || (Feed.getSize().startsWith("0"))) {
            holder.btn_download.setVisibility(View.GONE);
            holder.btn_mod.setVisibility(View.GONE);
        } else {
            holder.btn_download.setText(context.getString(R.string.download) + " " + Feed.getSize());
            holder.btn_download.setVisibility(View.VISIBLE);
        }
        // если нет mod
        if ((Feed.getMod() != null) && (!Feed.getMod().startsWith("null"))) {
            holder.btn_mod.setVisibility(View.VISIBLE);
            holder.btn_mod.setOnClickListener(view -> DownloadFile.download(context, Feed.getMod(), com.dimonvideo.client.model.Feed.getRazdel()));
        }

        // share menu
        try {
            if (!is_share_btn) holder.btn_share.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }

        holder.btn_share.setOnClickListener(view -> {

            holder.url = Config.BASE_URL + "/" + com.dimonvideo.client.model.Feed.getRazdel() + "/" + Feed.getId();
            if (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
                holder.url = Config.BASE_URL + "/" + Feed.getId() + "-news.html";

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, holder.url);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, Feed.getTitle());

            try {
                context.startActivity(shareIntent);
            } catch (Throwable ignored) {
            }

        });

        // download
        holder.btn_download.setOnClickListener(view -> DownloadFile.download(context, Feed.getLink(), com.dimonvideo.client.model.Feed.getRazdel()));

        // like and favorites
        holder.starButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton starButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1); // в избранное
            }

            @Override
            public void unLiked(LikeButton starButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.unfavorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 2); // из избранного
            }
        });

        holder.likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.like), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1);
                holder.txt_plus.setText(String.valueOf(Feed.getPlus() + 1));

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Snackbar.make(holder.itemView, context.getString(R.string.unlike), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 2);
                holder.txt_plus.setText(String.valueOf(Feed.getPlus() - 1));
            }
        });

        // comments
        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.TRACKER_RAZDEL))) holder.btn_comms.setVisibility(View.GONE); else holder.btn_comms.setVisibility(View.VISIBLE);
        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.TRACKER_RAZDEL))) holder.btn_share.setVisibility(View.GONE); else holder.btn_share.setVisibility(View.VISIBLE);

        if (is_share_btn) holder.btn_share.setVisibility(View.GONE);

        holder.textViewText.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            if (holder.btn_comms.getVisibility() == View.VISIBLE) {
                hide_content(holder, position);
            } else {
                open_content(holder, position, context, is_share_btn);
            }

        });

    }

    // подробный вывод файла - close
    @SuppressLint("NotifyDataSetChanged")
    private void hide_content(ViewHolder holder, final int position) {
        final Feed Feed = jsonFeed.get(position);
        holder.txt_plus.setVisibility(View.GONE);
        holder.likeButton.setVisibility(View.GONE);
        holder.starButton.setVisibility(View.GONE);
        holder.name.setVisibility(View.GONE);
        holder.txt_plus.setText(String.valueOf(Feed.getPlus()));
        try {
            holder.textViewText.setText(Html.fromHtml(Feed.getFull_text(), null,  new TagHandler()));
            holder.textViewText.setMovementMethod(LinkMovementMethod.getInstance());
            notifyDataSetChanged();
        } catch (Throwable ignored) {
        }
        holder.btn_download.setVisibility(View.GONE);
        holder.btn_mod.setVisibility(View.GONE);
        holder.btn_mp4.setVisibility(View.GONE);
        holder.btn_share.setVisibility(View.GONE);
        holder.btn_comms.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    // swipe to remove favorites
    @SuppressLint("NotifyDataSetChanged")
    public void removeFav(int position) {
        final Feed Feed = jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyDataSetChanged();
        ButtonsActions.add_to_fav_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 2);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewComments, textViewCategory, textViewHits, txt_plus, textViewName;
        public ImageView imageView, rating_logo, status_logo, fav_star;
        public TextView textViewText;
        public String url;
        public Button btn_comms, btn_download, btn_mod, btn_mp4, btn_share;
        public ProgressBar progressBar;
        public LikeButton likeButton, starButton;
        public LinearLayout name;
        public ClipboardManager myClipboard;
        public ClipData myClip;

        //Initializing Views
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
            btn_comms = itemView.findViewById(R.id.btn_comment);
            btn_download = itemView.findViewById(R.id.btn_download);
            btn_mod = itemView.findViewById(R.id.btn_mod);
            btn_share = itemView.findViewById(R.id.btn_share);
            btn_mp4 = itemView.findViewById(R.id.btn_mp4);
            progressBar = itemView.findViewById(R.id.progressBar);
            likeButton = itemView.findViewById(R.id.thumb_button);
            starButton = itemView.findViewById(R.id.star_button);
            txt_plus = itemView.findViewById(R.id.txt_plus);
            name = itemView.findViewById(R.id.name_layout);
        }

    }
}

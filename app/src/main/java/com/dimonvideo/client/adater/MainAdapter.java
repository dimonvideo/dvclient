package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.db.Provider;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.ui.main.Comments;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.OpenUrl;
import com.dimonvideo.client.util.TextViewClickMovement;
import com.dimonvideo.client.util.URLImageParser;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;

import org.xml.sax.XMLReader;

import java.util.Calendar;
import java.util.List;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private Context context;

    //List to store all
    List<Feed> jsonFeed;

    //Constructor of this class
    public MainAdapter(List<Feed> jsonFeed, Context context) {
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
            context = parent.getContext();
            return new ViewHolder(v);
       } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row, parent, false);
            context = parent.getContext();
            return new ViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final Feed Feed = jsonFeed.get(position);

        final boolean is_vuploader_play = AppController.getInstance().isVuploaderPlay();
        final boolean is_muzon_play = AppController.getInstance().isMuzonPlay();
        final boolean is_share_btn = AppController.getInstance().isShareBtn();

        holder.status_logo.setImageResource(R.drawable.ic_status_green);
        try {
            int status;
            Cursor cursor = Provider.getOneData(String.valueOf(Feed.getId()), com.dimonvideo.client.model.Feed.getRazdel());
            if (cursor != null) {
                status = cursor.getInt(2);
                if (status == 1) holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                cursor.close();
            }

        } catch (Throwable ignored) {
        }


        //Loading image from url
        Glide.with(context)
                .load(Feed.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(new RequestOptions().override(300, 300))
                .transform(new CenterCrop(),new RoundedCorners(15))
                .into(holder.imageView);

        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewText.setText(Html.fromHtml(Feed.getText(), null,  null));

        holder.textViewDate.setText(Feed.getDate());
        holder.textViewCategory.setText(Feed.getCategory());
        holder.textViewComments.setText(String.valueOf(Feed.getComments()));
        holder.textViewComments.setVisibility(View.VISIBLE);
        holder.rating_logo.setVisibility(View.VISIBLE);
        holder.textViewName.setText(Feed.getUser());

        // комментарии
        holder.textViewComments.setOnClickListener(view -> {
            String comm_url = Config.COMMENTS_READS_URL + com.dimonvideo.client.model.Feed.getRazdel() + "&lid=" + Feed.getId() + "&min=";
            Intent intent = null;
            intent = new Intent(context, Comments.class);
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

        // избранное
        if (Feed.getFav() > 0) {
            holder.fav_star.setVisibility(View.VISIBLE);
            holder.fav_star.setOnClickListener(v -> removeFav(position));
        }
        holder.textViewHits.setText(String.valueOf(Feed.getHits()));

        int lid = Feed.getId();

        // открытие подробной информации
        holder.itemView.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            openBottomSheet(view, position, lid);
        });

        // открытие подробной информации
        holder.textViewText.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            openBottomSheet(view, position, lid);
        });

        holder.imageView.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            Provider.updateStatus(lid, com.dimonvideo.client.model.Feed.getRazdel(), 1);
            ButtonsActions.loadScreen(context, Feed.getImageUrl());
        });

        try {
            if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL) && is_vuploader_play))
                holder.imageView.setOnClickListener(view -> {
                    Provider.updateStatus(lid, com.dimonvideo.client.model.Feed.getRazdel(), 1);
                    holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                    ButtonsActions.PlayVideo(context, Feed.getLink());
                });
        } catch (Exception ignored) {
        }

        try {
            if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.MUZON_RAZDEL) && is_muzon_play))
                holder.imageView.setOnClickListener(view -> {
                    Provider.updateStatus(lid, com.dimonvideo.client.model.Feed.getRazdel(), 1);
                    holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                    ButtonsActions.PlayVideo(context, Feed.getLink());
                });
        } catch (Exception ignored) {
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

        // скачать
        holder.small_download.setOnClickListener(view -> DownloadFile.download(context, Feed.getLink(), com.dimonvideo.client.model.Feed.getRazdel()));
        // если нет размера файла
        if ((Feed.getSize() == null) || (Feed.getSize().startsWith("0"))) {
            holder.small_download.setVisibility(View.GONE);
        } else {
            holder.small_download.setVisibility(View.VISIBLE);
        }
        // поделится
        try {
            if (!is_share_btn) holder.small_share.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }

        holder.small_share.setOnClickListener(view -> {

            String url = Config.BASE_URL + "/" + com.dimonvideo.client.model.Feed.getRazdel() + "/" + Feed.getId();
            if (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
                url = Config.BASE_URL + "/" + Feed.getId() + "-news.html";

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, Feed.getTitle());

            try {
                context.startActivity(shareIntent);
            } catch (Throwable ignored) {
            }

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

    // диалог по долгому нажатию
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
    private void openBottomSheet(View v, int position, int lid) {

        final Feed Feed = jsonFeed.get(position);

        String razdel = com.dimonvideo.client.model.Feed.getRazdel();
        Provider.updateStatus(lid, razdel, 1);

        Context context=v.getContext();
        final BottomSheetDialog dialog;
        View views = LayoutInflater.from(context).inflate(R.layout.bottom_detail, null);

        final boolean is_open_link = AppController.getInstance().isOpenLinks();
        final boolean is_vuploader_play_listtext = AppController.getInstance().isVuploaderPlayListtext();
        final boolean is_share_btn = AppController.getInstance().isShareBtn();

        TextView textViewTitle = views.findViewById(R.id.title);
        textViewTitle.setText(Feed.getTitle());
        TextView textViewDate = views.findViewById(R.id.date);
        textViewDate.setText(Feed.getDate());
        TextView textViewCategory = views.findViewById(R.id.category);
        textViewCategory.setText(Feed.getCategory());
        TextView txt_plus = views.findViewById(R.id.txt_plus);
        txt_plus.setText(String.valueOf(Feed.getPlus()));
        TextView textViewAuthor = views.findViewById(R.id.by_name);
        textViewAuthor.setText(String.valueOf(Feed.getUser()));

        Button dismiss2 = views.findViewById(R.id.dismiss2);
        Button btn_comms, btn_download, btn_mod, btn_mp4, btn_share;

        LikeButton likeButton, starButton;
        likeButton = views.findViewById(R.id.thumb_button);

        starButton = views.findViewById(R.id.star_button);

        btn_comms = views.findViewById(R.id.btn_comment);
        btn_download = views.findViewById(R.id.btn_download);
        btn_mod = views.findViewById(R.id.btn_mod);
        btn_share = views.findViewById(R.id.btn_share);
        btn_mp4 = views.findViewById(R.id.btn_mp4);

        // html textview
        TextView textViewText = views.findViewById(R.id.text);
        try {
            URLImageParser parser = new URLImageParser(textViewText);
            Spanned spanned = Html.fromHtml(Feed.getFull_text(), parser, new TagHandler());
            textViewText.setText(spanned);
            textViewText.setMovementMethod(new TextViewClickMovement() {
                @Override
                public void onLinkClick(String url) {
                    OpenUrl.open_url(url, is_open_link, is_vuploader_play_listtext, context);
                }
            });
        } catch (Throwable ignored) {
        }

        ImageView imageView = views.findViewById(R.id.logo);
        ImageView imageDismiss = views.findViewById(R.id.dismiss);

        Glide.with(context).load(Feed.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new FitCenter(),new RoundedCorners(15))
                .into(imageView);

        imageView.setOnClickListener(view -> ButtonsActions.loadScreen(context, Feed.getImageUrl()));

        dialog = new BottomSheetDialog(context);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(views);
        dialog.show();

        imageDismiss.setOnClickListener(view -> {
            dialog.dismiss();
        });
        dismiss2.setOnClickListener(view -> {
            dialog.dismiss();
        });


        btn_comms.setOnClickListener(view -> {
            String comm_url = Config.COMMENTS_READS_URL + com.dimonvideo.client.model.Feed.getRazdel() + "&lid=" + Feed.getId() + "&min=";
            Intent intent = new Intent(context, Comments.class);
            intent.putExtra(Config.TAG_TITLE, Feed.getTitle());
            intent.putExtra(Config.TAG_LINK, comm_url);
            intent.putExtra(Config.TAG_ID, String.valueOf(Feed.getId()));
            intent.putExtra(Config.TAG_RAZDEL, com.dimonvideo.client.model.Feed.getRazdel());
            context.startActivity(intent);

        });
        // комментарии
        if (Feed.getComments() > 0) {
            String comText = context.getResources().getString(R.string.Comments) + " " + Feed.getComments();
            btn_comms.setText(comText);
        } else {
            String comText = context.getResources().getString(R.string.Comments) + " " + 0;
            btn_comms.setText(comText);
        }
        // смотреть онлайн
        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.VUPLOADER_RAZDEL))) {
            btn_mp4.setVisibility(View.VISIBLE);
            btn_mp4.setOnClickListener(view -> ButtonsActions.PlayVideo(context, Feed.getLink()));
        }
        // слушать онлайн
        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.MUZON_RAZDEL))) {
            btn_mp4.setVisibility(View.VISIBLE);
            btn_mp4.setText(R.string.listen_online);
            btn_mp4.setOnClickListener(view -> ButtonsActions.PlayVideo(context, Feed.getLink()));
        }
        // если нет размера файла
        if ((Feed.getSize() == null) || (Feed.getSize().startsWith("0"))) {
            btn_download.setVisibility(View.GONE);
            btn_mod.setVisibility(View.GONE);
        } else {
            btn_download.setText(context.getString(R.string.download) + " " + Feed.getSize());
            btn_download.setVisibility(View.VISIBLE);
        }
        // если нет mod
        if ((Feed.getMod() != null) && (!Feed.getMod().startsWith("null"))) {
            btn_mod.setVisibility(View.VISIBLE);
            btn_mod.setOnClickListener(view -> DownloadFile.download(context, Feed.getMod(), com.dimonvideo.client.model.Feed.getRazdel()));
        }

        // поделится
        try {
            if (!is_share_btn) btn_share.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }

        btn_share.setOnClickListener(view -> {

            String url = Config.BASE_URL + "/" + com.dimonvideo.client.model.Feed.getRazdel() + "/" + Feed.getId();
            if (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
                url = Config.BASE_URL + "/" + Feed.getId() + "-news.html";

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, Feed.getTitle());

            try {
                context.startActivity(shareIntent);
            } catch (Throwable ignored) {
            }

        });

        // скачать
        btn_download.setOnClickListener(view -> DownloadFile.download(context, Feed.getLink(), com.dimonvideo.client.model.Feed.getRazdel()));

        // лайк и избранное
        starButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton starButton) {
                Snackbar.make(views, context.getString(R.string.favorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1); // в избранное
            }

            @Override
            public void unLiked(LikeButton starButton) {
                Snackbar.make(views, context.getString(R.string.unfavorites_btn), Snackbar.LENGTH_LONG).show();
                ButtonsActions.add_to_fav_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 2); // из избранного
            }
        });

        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                Snackbar.make(views, context.getString(R.string.like), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 1);
                txt_plus.setText(String.valueOf(Feed.getPlus() + 1));

            }

            @Override
            public void unLiked(LikeButton likeButton) {
                Snackbar.make(views, context.getString(R.string.unlike), Snackbar.LENGTH_LONG).show();
                ButtonsActions.like_file(context, com.dimonvideo.client.model.Feed.getRazdel(), Feed.getId(), 2);
                txt_plus.setText(String.valueOf(Feed.getPlus() - 1));
            }
        });

        // скрытие лишнего
        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.TRACKER_RAZDEL))) btn_comms.setVisibility(View.GONE); else btn_comms.setVisibility(View.VISIBLE);
        if ((com.dimonvideo.client.model.Feed.getRazdel() != null) && (com.dimonvideo.client.model.Feed.getRazdel().equals(Config.TRACKER_RAZDEL))) btn_share.setVisibility(View.GONE); else btn_share.setVisibility(View.VISIBLE);

        if (is_share_btn) btn_share.setVisibility(View.GONE);

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
        public TextView textViewTitle, textViewDate, textViewComments, textViewCategory, textViewHits, textViewName;
        public ImageView imageView, rating_logo, status_logo, fav_star, small_share, small_download;
        public TextView textViewText;
        public String url;
        public ProgressBar progressBar;
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
            progressBar = itemView.findViewById(R.id.progressBar);
            name = itemView.findViewById(R.id.name_layout);
            small_share = itemView.findViewById(R.id.small_share);
            small_download = itemView.findViewById(R.id.small_download);
        }

    }
}

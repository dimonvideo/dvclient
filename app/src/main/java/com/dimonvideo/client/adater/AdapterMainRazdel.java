package com.dimonvideo.client.adater;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.db.Provider;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.ui.main.MainFragmentCommentsFile;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.OpenBottomSheet;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.xml.sax.XMLReader;

import java.util.List;

public class AdapterMainRazdel extends RecyclerView.Adapter<AdapterMainRazdel.ViewHolder> {

    private Context context;
    @SuppressLint("StaticFieldLeak")
    public static BottomSheetDialog dialog;

    private String razdel;

    //List to store all
    private final List<Feed> jsonFeed;
    //Constructor of this class
    public AdapterMainRazdel(List<Feed> jsonFeed, Context context) {
        super();
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    public static void onDestroy() {
        try{
            if (dialog != null)
                dialog.dismiss();
        } catch(Exception ignored){

        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == 1) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_gallery, parent, false);
            context = parent.getContext();
            return new ViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row, parent, false);
        context = parent.getContext();
        return new ViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {

        Feed feed = jsonFeed.get(position);

        if ((feed.getRazdel() != null) && ((feed.getRazdel().equals(Config.GALLERY_RAZDEL)) || (feed.getRazdel().equals(Config.VUPLOADER_RAZDEL)))) {
            return 1;
        } else {
            return 0;
        }

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        Feed feed = jsonFeed.get(position);

        final boolean is_vuploader_play = AppController.getInstance().isVuploaderPlay();
        final boolean is_muzon_play = AppController.getInstance().isMuzonPlay();
        final boolean is_share_btn = AppController.getInstance().isShareBtn();

        razdel = feed.getRazdel();
        int lid = feed.getId();

        holder.status_logo.setImageResource(R.drawable.ic_status_green);
        try {
            int status;
            Cursor cursor = Provider.getOneData(String.valueOf(lid), razdel);
            if (cursor != null) {
                status = cursor.getInt(2);
                if (status == 1) holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                cursor.close();
            }

        } catch (Throwable ignored) {
        }


        //Loading image from url
        Glide.with(context)
                .load(feed.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(new RequestOptions().override(300, 300))
                .transform(new CenterCrop(),new RoundedCorners(15))
                .into(holder.imageView);

        holder.textViewTitle.setText(feed.getTitle());
        holder.textViewText.setText(Html.fromHtml(feed.getText(), null,  null));

        holder.textViewDate.setText(feed.getDate());
        holder.textViewCategory.setText(feed.getCategory());
        holder.textViewComments.setText(String.valueOf(feed.getComments()));
        holder.textViewComments.setVisibility(View.VISIBLE);
        holder.rating_logo.setVisibility(View.VISIBLE);
        holder.textViewName.setText(feed.getUser());

        // комментарии
        holder.textViewComments.setOnClickListener(view -> {
            openComments(feed, lid);
        });
        holder.rating_logo.setOnClickListener(view -> {
            openComments(feed, lid);
        });

        if (feed.getComments() == 0) {
            holder.textViewComments.setVisibility(View.INVISIBLE);
            holder.rating_logo.setVisibility(View.INVISIBLE);
        }

        // избранное
        if (feed.getFav() > 0) {
            holder.fav_star.setVisibility(View.VISIBLE);
            holder.fav_star.setOnClickListener(v -> removeFav(position));
        }
        holder.textViewHits.setText(String.valueOf(feed.getHits()));


        // открытие подробной информации
        holder.itemView.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            OpenBottomSheet.openFile(razdel, view, lid, feed.getComments(), feed.getTitle(),
                    feed.getUser(), feed.getPlus(), feed.getLink(), feed.getMod(), feed.getSize(), feed.getImageUrl(), feed.getFull_text(), feed.getDate(), feed.getCategory());
        });
        holder.textViewText.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            OpenBottomSheet.openFile(razdel, view, lid, feed.getComments(), feed.getTitle(),
                    feed.getUser(), feed.getPlus(), feed.getLink(), feed.getMod(), feed.getSize(), feed.getImageUrl(), feed.getFull_text(), feed.getDate(), feed.getCategory());
        });

        // показ скриншота
        holder.imageView.setOnClickListener(view -> {
            holder.status_logo.setImageResource(R.drawable.ic_status_gray);
            Provider.updateStatus(lid, razdel, 1);
            Log.e("---", "screen: "+feed.getImageUrl());
            ButtonsActions.loadScreen(context, feed.getImageUrl());
        });

        try {
            if ((razdel != null) && (razdel.equals(Config.VUPLOADER_RAZDEL) && is_vuploader_play))
                holder.imageView.setOnClickListener(view -> {
                    Provider.updateStatus(lid, razdel, 1);
                    holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                    ButtonsActions.PlayVideo(context, feed.getLink());
                });
        } catch (Exception ignored) {
        }

        try {
            if ((razdel != null) && (razdel.equals(Config.MUZON_RAZDEL) && is_muzon_play))
                holder.imageView.setOnClickListener(view -> {
                    Provider.updateStatus(lid, razdel, 1);
                    holder.status_logo.setImageResource(R.drawable.ic_status_gray);
                    ButtonsActions.PlayVideo(context, feed.getLink());
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
        holder.small_download.setOnClickListener(view -> DownloadFile.download(context, feed.getLink(), razdel));
        // если нет размера файла
        if ((feed.getSize() == null) || (feed.getSize().startsWith("0"))) {
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

            String url = Config.BASE_URL + "/" + razdel + "/" + lid;
            if (razdel.equals(Config.COMMENTS_RAZDEL))
                url = Config.BASE_URL + "/" + lid + "-news.html";

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, feed.getTitle());

            try {
                context.startActivity(shareIntent);
            } catch (Throwable ignored) {
            }

        });


    }

    private void openComments(Feed feed, int lid) {
        String comm_url = Config.COMMENTS_READS_URL + razdel + "&lid=" + lid + "&min=";
        Fragment fragment = new MainFragmentCommentsFile();
        Bundle bundle = new Bundle();
        bundle.putString(Config.TAG_TITLE, feed.getTitle());
        bundle.putString(Config.TAG_ID, String.valueOf(lid));
        bundle.putString(Config.TAG_LINK, comm_url);
        bundle.putString(Config.TAG_RAZDEL, razdel);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = ((AppCompatActivity)context).getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.addToBackStack(null);
        ft.replace(R.id.container_frag, fragment);
        ft.commit();
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
        final Feed feed = jsonFeed.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.BASE_URL + "/" + feed.getRazdel() + "/" + feed.getId();
        if (feed.getRazdel().equals(Config.COMMENTS_RAZDEL))
            holder.url = Config.BASE_URL + "/" + feed.getId() + "-news.html";

        holder.myClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(feed.getTitle());
        builder.setItems(items, (dialog, item) -> {
            if (item == 0) { // share
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, holder.url);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, feed.getTitle());
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
                ButtonsActions.add_to_fav_file(context, feed.getRazdel(), feed.getId(), 1);
            }
            if (item == 3) { // like
                ButtonsActions.like_file(context, feed.getRazdel(), feed.getId(), 1);
            }
            if (item == 4) { // screen
                ButtonsActions.loadScreen(context, feed.getImageUrl());
            }
            if (item == 5) { // download
                DownloadFile.download(context, feed.getLink(), feed.getRazdel());
            }
            if (item == 6) { // copy text
                try { holder.myClip = ClipData.newPlainText("text", Html.fromHtml(feed.getFull_text()).toString());
                holder.myClipboard.setPrimaryClip(holder.myClip);
                } catch (Throwable ignored) {
                }
                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    // swipe to remove favorites
    @SuppressLint("NotifyDataSetChanged")
    public void removeFav(int position) {
        final Feed feed = jsonFeed.get(position);
        jsonFeed.remove(position);
        notifyDataSetChanged();
        ButtonsActions.add_to_fav_file(context, feed.getRazdel(), feed.getId(), 2);
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

package com.dimonvideo.client.adater;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.NetworkUtils;

import java.util.Calendar;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private Context context;

    //List to store all
    List<FeedPm> jsonFeed;

    //Constructor of this class
    public FriendsAdapter(List<FeedPm> jsonFeed, Context context){
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_pm, parent, false);
        context = parent.getContext();
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final FeedPm Feed =  jsonFeed.get(position);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        Glide.with(context).load(Feed.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.circleCropTransform()).into(holder.imageView);

        holder.textViewTitle.setText(Feed.getTitle());
        holder.textViewDate.setText(Feed.getLast_poster_name());
        holder.textViewDate.append(" " + Feed.getDate());

        holder.textViewNames.setText(Feed.getFullText());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        try {
            holder.textViewText.setText(Feed.getText());
        } catch (Throwable ignored) {
        }

        try {
            if (Feed.getTime() > cal.getTimeInMillis() / 1000L) {
                holder.status_logo.setImageResource(R.drawable.ic_status_green);
            }
        } catch (Exception ignored) {
        }

        holder.itemView.setOnClickListener(v -> {

            if (holder.btns.getVisibility()==View.VISIBLE) holder.btns.setVisibility(View.GONE); else holder.btns.setVisibility(View.VISIBLE);

        });

        holder.textViewText.setOnClickListener(v -> {

            if (holder.btns.getVisibility()==View.VISIBLE) holder.btns.setVisibility(View.GONE); else holder.btns.setVisibility(View.VISIBLE);

        });

        holder.send.setOnClickListener(v -> {

            holder.textViewText.setText(Html.fromHtml(Feed.getFullText(), null,  new MainAdapter.TagHandler()));
            holder.textViewText.setMovementMethod(LinkMovementMethod.getInstance());
            holder.btns.setVisibility(View.GONE);
            NetworkUtils.sendPm(context, 0, holder.textInput.getText().toString(), 0, null, Feed.getId());

        });

        // меню по долгому нажатию
        holder.itemView.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });
        holder.textViewText.setOnLongClickListener(view -> {
            show_dialog(holder, position, context);
            return true;
        });

    }

    private void show_dialog(FriendsAdapter.ViewHolder holder, final int position, Context context){
        final CharSequence[] items = {context.getString(R.string.action_open),
                context.getString(R.string.action_like_member), context.getString(R.string.copy_name), context.getString(R.string.add_friend), context.getString(R.string.add_ignor), context.getString(R.string.remove_all)};

        FeedPm Feed = jsonFeed.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        holder.url = Config.BASE_URL + "/0/name/" + Feed.getTitle();

        holder.myClipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);

        builder.setTitle(Feed.getTitle());
        builder.setItems(items, (dialog, item) -> {

            if (item == 0) { // browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(holder.url));
                try {
                    context.startActivity(browserIntent);
                } catch (Throwable ignored) {
                }
            }

            if (item == 1) { // like
                ButtonsActions.like_member(context, Feed.getId(), Feed.getTitle(), 5);
            }

            if (item == 2) { // copy text
                try { holder.myClip = ClipData.newPlainText("text", Html.fromHtml(Feed.getTitle()).toString());
                    holder.myClipboard.setPrimaryClip(holder.myClip);
                } catch (Throwable ignored) {
                }
                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show();
            }

            if (item == 3) { // add to friends
                ButtonsActions.add_to_fav_user(context, Feed.getId(), 3);
                jsonFeed.remove(position);
                notifyItemRemoved(position);
            }

            if (item == 4) { // add to ignor
                ButtonsActions.add_to_fav_user(context, Feed.getId(), 4);
                jsonFeed.remove(position);
                notifyItemRemoved(position);
            }

            if (item == 5) { // очистка
                ButtonsActions.add_to_fav_user(context, Feed.getId(), 5);
                jsonFeed.remove(position);
                notifyItemRemoved(position);
            }

        });
        builder.show();
    }
    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    public List<FeedPm> getData() {
        return jsonFeed;
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewNames;
        public ImageView status_logo, imageView;
        public TextView textViewText;
        public LinearLayout btns;
        public Button send;
        public EditText textInput;
        public String url;
        public ClipboardManager myClipboard;
        public ClipData myClip;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
            status_logo = itemView.findViewById(R.id.status);
            textViewTitle = itemView.findViewById(R.id.title);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewNames = itemView.findViewById(R.id.name);
            btns = itemView.findViewById(R.id.linearLayout1);
            send = itemView.findViewById(R.id.btnSend);
            textInput = itemView.findViewById(R.id.textInput);

        }

    }
}

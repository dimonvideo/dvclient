package com.dimonvideo.client.adater;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.DownloadFile;
import com.dimonvideo.client.util.NetworkUtils;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.Calendar;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private Context context;

    //List to store all
    List<FeedForum> jsonFeed;

    //Constructor of this class
    public CommentsAdapter(List<FeedForum> jsonFeed, Context context) {
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_posts, parent, false);


        return new ViewHolder(v);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final FeedForum Feed = jsonFeed.get(position);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        try {
            if (Feed.getTime() > cal.getTimeInMillis() / 1000L)
                holder.status_logo.setImageResource(R.drawable.ic_status_green);
        } catch (Throwable ignored) {

        }
        Glide.with(context).load(Feed.getImageUrl()).apply(RequestOptions.circleCropTransform()).into(holder.imageView);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = sharedPrefs.getString("dvc_password", "null");
        final boolean is_open_link = sharedPrefs.getBoolean("dvc_open_link", false);
        holder.textViewCategory.setText(Feed.getCategory());
        holder.textViewNames.setVisibility(View.GONE);

        if ((Feed.getNewtopic() == 1) && (!password.equals("null")))
            holder.post_layout.setVisibility(View.GONE);

        // отправка ответа
        holder.btnSend.setOnClickListener(v -> {
            NetworkUtils.sendPm(context, Feed.getId(), holder.textInput.getText().toString(), 20, Feed.getState());
            holder.post_layout.setVisibility(View.GONE);
            notifyDataSetChanged();
        });

        holder.textViewText.setHtml(Feed.getText(), new HtmlHttpImageGetter(holder.textViewText));
        holder.textViewTitle.setText("#"+String.valueOf(Feed.getMin()+position+1)+" ");
        holder.textViewTitle.append(Feed.getUser());
        holder.textViewDate.setText(Feed.getDate());
        holder.textViewComments.setVisibility(View.GONE);
        holder.rating_logo.setVisibility(View.GONE);
        holder.textViewHits.setText(String.valueOf(Feed.getHits()));

        // цитирование
        holder.textViewText.setOnClickListener(view -> {
            holder.post_layout.setVisibility(View.VISIBLE);
            holder.textInput.setText("[b]"+ Feed.getUser() +"[/b], ");
            holder.textInput.setSelection(holder.textInput.getText().length());;
            holder.textInput.setFocusableInTouchMode(true);
            holder.textInput.requestFocus();


        });
        holder.itemView.setOnClickListener(view -> {
            holder.post_layout.setVisibility(View.VISIBLE);
            holder.textInput.setText("[b]"+ Feed.getUser() +"[/b], ");
            holder.textInput.setSelection(holder.textInput.getText().length());;
            holder.textInput.requestFocus();
            holder.textInput.setFocusableInTouchMode(true);


        });


        if (Feed.getMin()>0) {
            holder.post_layout.setVisibility(View.GONE);
        }

        // open links from listtext
        if (!is_open_link) {
            holder.textViewText.setOnClickATagListener((widget, href) -> {
                String url = href;
                try {
                    assert href != null;
                    url = href.replace("https://m.dimonvideo.ru/go/?", "");
                    url = href.replace("https://m.dimonvideo.ru/go?", "");
                    url = href.replace("https://dimonvideo.ru/go/?", "");
                    url = href.replace("https://dimonvideo.ru/go?", "");
                } catch (Throwable ignored) {
                }
                assert url != null;
                String extension = url.substring(url.lastIndexOf(".") + 1);
                if ((extension.equals("png")) || (extension.equals("jpg")) || (extension.equals("jpeg")))
                    ButtonsActions.loadScreen(context, url);
                else if ((extension.equals("apk")) || (extension.equals("zip")) || (extension.equals("avi"))
                        || (extension.equals("mp3"))
                        || (extension.equals("m4a"))
                        || (extension.equals("rar"))
                        || (extension.equals("mp4"))) DownloadFile.download(context, url);
                else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        context.startActivity(browserIntent);
                    } catch (Throwable ignored) {
                    }
                }
            });
        }
    }


    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewComments, textViewHits, textViewNames, textViewCategory;
        public ImageView rating_logo, status_logo, imageView;
        public HtmlTextView textViewText;
        public LinearLayout post_layout;
        public Button btnSend;
        public EditText textInput;
        public String url;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
            rating_logo = itemView.findViewById(R.id.rating_logo);
            status_logo = itemView.findViewById(R.id.status);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewHits = itemView.findViewById(R.id.views_count);
            textViewTitle = itemView.findViewById(R.id.title);
            post_layout = itemView.findViewById(R.id.post);
            btnSend = itemView.findViewById(R.id.btnSend);
            textInput = itemView.findViewById(R.id.textInput);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewNames = itemView.findViewById(R.id.names);
        }

    }
}

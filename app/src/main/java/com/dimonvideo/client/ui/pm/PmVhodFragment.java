package com.dimonvideo.client.ui.pm;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.PmAdapter;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.MessageEvent;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PmVhodFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private List<FeedPm> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;
    LinearLayout emptyLayout;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    static int razdel = 13;
    TabLayout tabs;

    public PmVhodFragment() {
        // Required empty public constructor
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        razdel = event.razdel;
    }

    @SuppressLint({"DetachAndAttachSameFragment", "NotifyDataSetChanged"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        requestCount = 1;
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        if (this.getArguments() != null) {
            razdel = getArguments().getInt(Config.TAG_CATEGORY);
            EventBus.getDefault().postSticky(new MessageEvent(razdel, null));
        }
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        EventBus.getDefault().post(new MessageEvent(razdel, ""));

        TextView emptyView = root.findViewById(R.id.empty_view);
        emptyLayout = root.findViewById(R.id.linearEmpty);

        emptyView.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.VISIBLE);

        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());

        progressBar = root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = root.findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new PmAdapter(listFeed, getContext());

        // обновление
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            listFeed.clear();
            getData();
            swipLayout.setRefreshing(false);
        });

        recyclerView = root.findViewById(R.id.recycler_view);

        // разделитель позиций
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(requireContext(), R.drawable.divider);
        assert horizontalDivider != null;
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (isLastItemDisplaying(recyclerView)) {
                    getData();
                }

            }
        });
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setItemViewCacheSize(2);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        // swipe to delete
        ItemTouchHelper.SimpleCallback itemTouchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
            private final ColorDrawable background = new ColorDrawable(Color.RED);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                swipLayout.setEnabled(actionState != ItemTouchHelper.ACTION_STATE_SWIPE);
            }

            @Override
            public void onChildDraw(Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX / 4, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;

                assert deleteIcon != null;
                int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX > 0) {
                    int iconLeft = itemView.getLeft() + iconMargin + deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getLeft() + iconMargin;

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX), itemView.getBottom());
                } else if (dX < 0) {
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    background.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                } else {
                    background.setBounds(0, 0, 0, 0);
                }

                background.draw(c);
                deleteIcon.draw(c);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAbsoluteAdapterPosition();
                ((PmAdapter) adapter).removeItem(position);
                Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.msg_removed), Snackbar.LENGTH_LONG);
                snackbar.setAction(getString(R.string.tab_trash), view -> {
                    assert getParentFragment() != null;
                    ViewPager2 viewPager = getParentFragment().requireView().findViewById(R.id.view_pager);
                    viewPager.setCurrentItem(5);
                });

                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();
            }
        };

        new ItemTouchHelper(itemTouchHelper).attachToRecyclerView(recyclerView);

        Log.e("tagPM--->>>>", ""+adapter.getItemCount());

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.getItemCount() == 0) {
                    emptyLayout.setVisibility(View.GONE);
                    emptyView.setVisibility(View.GONE);
                }
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        @SuppressLint("StaticFieldLeak")
        class AsyncCountPm extends AsyncTask<String, String, String> {

            @Override
            protected String doInBackground(String... params) {
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                TextView fab_badge = requireActivity().findViewById(R.id.fab_badge);
                fab_badge.setVisibility(View.GONE);

            }
        }
        new AsyncCountPm().execute();

    }

    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext());
        String login = sharedPrefs.getString("dvc_login", "null");
        String pass = sharedPrefs.getString("dvc_password", "null");
        try {
            pass = URLEncoder.encode(pass, "utf-8");
            login = URLEncoder.encode(login, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String finalPass = pass;
        String finalLogin = login;
        String url = Config.PM_URL + requestCount + "&login_name=" + finalLogin + "&login_password=" + finalPass;
        return new JsonArrayRequest(url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                    for (int i = 0; i < response.length(); i++) {
                        FeedPm jsonFeed = new FeedPm();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setImageUrl(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setIs_new(json.getInt(Config.TAG_HITS));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setLast_poster_name(json.getString(Config.TAG_LAST_POSTER_NAME));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setFullText(json.getString(Config.TAG_FULL_TEXT));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        adapter.notifyDataSetChanged();
                        listFeed.add(jsonFeed);
                    }

                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                });

    }

    // получение данных и увеличение номера страницы
    private void getData() {
        ProgressBarBottom.setVisibility(View.VISIBLE);
        requestQueue.add(getDataFromServer(requestCount));
        requestCount++;
    }

    // опредление последнего элемента
    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        if (Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() != 0) {
            int lastVisibleItemPosition = ((LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).findLastCompletelyVisibleItemPosition();
            return lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1;
        }
        return false;
    }

    // обновление
    @Override
    public void onRefresh() {
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

}
package com.dimonvideo.client.ui.pm;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.PmAdapter;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PmArhivFragment extends Fragment  {

    public RecyclerView recyclerView;
    SwipeRefreshLayout swipLayout;
    LinearLayout emptyLayout;
    TextView emptyView;
    private ProgressBar progressBar, progressBarBottom;
    String url = Config.PM_URL;
    private FragmentHomeBinding binding;
    public PmAdapter adapter;
    private List<FeedPm> listFeed;
    private int requestCount = 1;


    public PmArhivFragment() {
        // Required empty public constructor
    }

    @SuppressLint({"DetachAndAttachSameFragment", "NotifyDataSetChanged"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        recyclerView = binding.recyclerView;
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        listFeed = new ArrayList<>();
        emptyView = binding.emptyView;
        emptyLayout = binding.linearEmpty;

        emptyView.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.VISIBLE);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (isLastItemDisplaying(recyclerView)) {
                    getData();
                }

            }
        });

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        progressBarBottom = binding.ProgressBarBottom;
        progressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new PmAdapter(listFeed);

        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setHasFixedSize(true);

        recyclerView.setAdapter(adapter);
        // обновление
        swipLayout = binding.swipeLayout;
        swipLayout.setOnRefreshListener(() -> {
            requestCount = 1;
            listFeed.clear();
            getData();
            swipLayout.setRefreshing(false);
        });

        // swipe to delete
        ItemTouchHelper.SimpleCallback itemTouchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
            private final Drawable archiveIcon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_inventory_2_white_20);
            private final ColorDrawable backgroundDelete = new ColorDrawable(Color.RED);
            private final ColorDrawable backgroundArchive = new ColorDrawable(Color.GREEN);

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
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX / 4, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;

                assert deleteIcon != null;
                int iconDeleteMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconDeleteTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconDeleteBottom = iconDeleteTop + deleteIcon.getIntrinsicHeight();

                assert archiveIcon != null;
                int leftIconMargin = (itemView.getHeight() - archiveIcon.getIntrinsicHeight()) / 2;
                int leftIconTop = itemView.getTop() + (itemView.getHeight() - archiveIcon.getIntrinsicHeight()) / 2;
                int leftIconBottom = leftIconTop + archiveIcon.getIntrinsicHeight();

                if (dX > 0) {
                    int leftIconLeft = itemView.getLeft() + leftIconMargin;
                    int leftIconRight = itemView.getLeft() + leftIconMargin + archiveIcon.getIntrinsicWidth();
                    archiveIcon.setBounds(leftIconLeft, leftIconTop, leftIconRight, leftIconBottom);
                    backgroundArchive.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX), itemView.getBottom());
                    backgroundArchive.draw(c);
                    archiveIcon.draw(c);
                } else if (dX < 0) {
                    int iconLeft = itemView.getRight() - iconDeleteMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconDeleteMargin;
                    deleteIcon.setBounds(iconLeft, iconDeleteTop, iconRight, iconDeleteBottom);
                    backgroundDelete.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    backgroundDelete.draw(c);
                    deleteIcon.draw(c);
                } else {
                    backgroundDelete.setBounds(0, 0, 0, 0);
                    backgroundArchive.setBounds(0, 0, 0, 0);
                    backgroundArchive.draw(c);
                    backgroundDelete.draw(c);
                }


            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAbsoluteAdapterPosition();

                if (direction == ItemTouchHelper.LEFT) {
                    adapter.removeItem(position);
                    Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.msg_removed), Snackbar.LENGTH_LONG);
                    snackbar.setAction(getString(R.string.tab_trash), view -> {
                        assert getParentFragment() != null;
                        ViewPager2 viewPager = getParentFragment().requireView().findViewById(R.id.view_pager);
                        viewPager.setCurrentItem(4, true);
                    });

                    TextView fab_badge = MainActivity.binding.appBarMain.fabBadge;
                    fab_badge.setVisibility(View.GONE);
                    snackbar.setActionTextColor(Color.YELLOW);
                    snackbar.show();
                    NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                }

                if (direction == ItemTouchHelper.RIGHT) {
                    adapter.restoreFromArchiveItem(position);
                    TabsAdapter adapt = new TabsAdapter(getChildFragmentManager(), getLifecycle());
                    Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.msg_restored), Snackbar.LENGTH_LONG);
                    snackbar.setAction(getString(R.string.tab_inbox), view -> {
                        assert getParentFragment() != null;
                        ViewPager2 viewPager = getParentFragment().requireView().findViewById(R.id.view_pager);
                        viewPager.setCurrentItem(0, true);
                    });
                    TextView fab_badge = MainActivity.binding.appBarMain.fabBadge;
                    fab_badge.setVisibility(View.GONE);
                    snackbar.setActionTextColor(Color.GREEN);
                    snackbar.show();
                    NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                }
            }
        };

        new ItemTouchHelper(itemTouchHelper).attachToRecyclerView(recyclerView);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.getItemCount() == 0) {
                    emptyLayout.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    emptyLayout.setVisibility(View.GONE);
                    emptyView.setVisibility(View.GONE);
                }
            }
        });
    }
    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext());
        String login = sharedPrefs.getString("dvc_login","null");
        String pass = sharedPrefs.getString("dvc_password","null");
        try {
            pass = URLEncoder.encode(pass, "utf-8");
            login = URLEncoder.encode(login, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String finalPass = pass;
        String finalLogin = login;

        return new JsonArrayRequest(url + requestCount + "&pm=2&login_name=" + finalLogin + "&login_password=" + finalPass,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    progressBarBottom.setVisibility(View.GONE);
                    if (requestCount == 1) {
                        listFeed.clear();
                        adapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(0);
                    }
                    for (int i = 0; i < response.length(); i++) {
                        FeedPm jsonFeed = new FeedPm();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setImageUrl(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setIs_new(json.getInt(Config.TAG_HITS));
                            jsonFeed.setLast_poster_name(json.getString(Config.TAG_LAST_POSTER_NAME));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setFullText(json.getString(Config.TAG_FULL_TEXT));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listFeed.add(jsonFeed);
                    }
                    adapter.notifyDataSetChanged();
                    if (adapter.getItemCount() == 0) {
                        emptyLayout.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        emptyLayout.setVisibility(View.GONE);
                        emptyView.setVisibility(View.GONE);
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    progressBarBottom.setVisibility(View.GONE);
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        RequestQueue queue = AppController.getInstance().getRequestQueue();
        progressBarBottom.setVisibility(View.VISIBLE);
        queue.add(getDataFromServer(requestCount));
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

}
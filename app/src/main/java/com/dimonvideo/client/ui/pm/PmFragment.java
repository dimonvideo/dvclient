package com.dimonvideo.client.ui.pm;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.toolbox.JsonArrayRequest;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterPm;
import com.dimonvideo.client.databinding.FragmentHomeBinding;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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

public class PmFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipLayout;
    private LinearLayout emptyLayout;
    private TextView emptyView;
    private ProgressBar progressBar, progressBarBottom;
    private FragmentHomeBinding binding;
    private AdapterPm adapter;
    private List<FeedPm> listFeed;
    private int requestCount = 1;
    private String tab_title;
    private AppController controller;


    public PmFragment() {
        // Required empty public constructor
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        String pm = event.action;
        if ((pm != null) && (pm.equals("restored"))) update();
        Log.e("---", "PmFragment event: "+pm );
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (this.getArguments() != null) {
            tab_title = getArguments().getString("tab");
        }

        controller = AppController.getInstance();

        listFeed = new ArrayList<>();

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        emptyView = binding.emptyView;
        emptyLayout = binding.linearEmpty;

        emptyView.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.VISIBLE);

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        progressBarBottom = binding.ProgressBarBottom;
        progressBarBottom.setVisibility(View.GONE);
        recyclerView = binding.recyclerView;

        // получение данных
        getData();
        adapter = new AdapterPm(listFeed, getContext());

        // разделитель позиций
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(requireContext(), R.drawable.divider);
        assert horizontalDivider != null;
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        ((SimpleItemAnimator) Objects.requireNonNull(recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
        recyclerView.setAdapter(adapter);

        // показ кнопки наверх
        FloatingActionButton fab = binding.fabTop;
        boolean is_top = controller.isOnTop();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) { // down
                    new Handler().postDelayed(() -> fab.setVisibility(View.GONE), 6000);
                } else if (dy < 0) { // up
                    fab.setVisibility(View.VISIBLE);
                    if (!is_top) fab.setVisibility(View.GONE);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // подгрузка ленты
                if (isLastItemDisplaying(recyclerView)) {
                    getData();
                }
            }
        });
        fab.setOnClickListener(views -> {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(0));
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

                    if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_trash)))) {
                        if (position >= 0) adapter.restoreItem(position);
                        Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.msg_restored), Snackbar.LENGTH_LONG);
                        snackbar.setAction(getString(R.string.tab_inbox), view -> {
                            assert getParentFragment() != null;
                            ViewPager2 viewPager = getParentFragment().requireView().findViewById(R.id.view_pager);
                            viewPager.setCurrentItem(0, true);
                            EventBus.getDefault().postSticky(new MessageEvent("13", null, null, null, "restored", null));
                        });
                        snackbar.setActionTextColor(Color.GREEN).show();

                    } else {
                        if (position >= 0) adapter.removeItem(position);
                        Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.msg_removed), Snackbar.LENGTH_LONG);
                        snackbar.setAction(getString(R.string.tab_trash), view -> {
                            EventBus.getDefault().postSticky(new MessageEvent("13", null, null, null, "deleted", null));
                            assert getParentFragment() != null;
                            ViewPager2 viewPager = getParentFragment().requireView().findViewById(R.id.view_pager);
                            viewPager.setCurrentItem(4, true);
                        });
                        snackbar.setActionTextColor(Color.YELLOW).show();
                    }
                    NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                    TextView fab_badge = MainActivity.binding.appBarMain.fabBadge;
                    fab_badge.setVisibility(View.GONE);
                }

                if (direction == ItemTouchHelper.RIGHT) {

                    if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_arhiv)))) {
                        if (position >= 0) adapter.restoreFromArchiveItem(position);

                        Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.msg_restored), Snackbar.LENGTH_LONG);
                        snackbar.setAction(getString(R.string.tab_inbox), view -> {
                            assert getParentFragment() != null;
                            ViewPager2 viewPager = getParentFragment().requireView().findViewById(R.id.view_pager);
                            viewPager.setCurrentItem(0, true);
                        });
                        EventBus.getDefault().postSticky(new MessageEvent("13", null, null, null, "restored", null));
                        snackbar.setActionTextColor(Color.GREEN).show();
                    } else {
                        if (position >= 0) adapter.archiveItem(position);
                        Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.msg_archived), Snackbar.LENGTH_LONG);
                        snackbar.show();
                        EventBus.getDefault().postSticky(new MessageEvent("13", null, null, null, "archived", null));

                    }
                    NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                    TextView fab_badge = MainActivity.binding.appBarMain.fabBadge;
                    fab_badge.setVisibility(View.GONE);
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

        // обновление
        swipLayout = binding.swipeLayout;
        swipLayout.setOnRefreshListener(this::update);
    }

    private void update() {
        requestCount = 1;
        getData();
        TextView fab_badge = MainActivity.binding.appBarMain.fabBadge;
        fab_badge.setVisibility(View.GONE);
        swipLayout.setRefreshing(false);
    }

    // запрос к серверу апи
    private JsonArrayRequest getDataFromServer(int requestCount) {
        String login_name = controller.userName(getString(R.string.nav_header_title));
        String pass = controller.userPassword();
        try {
            pass = URLEncoder.encode(pass, "utf-8");
            login_name = URLEncoder.encode(login_name, "utf-8");
        } catch (UnsupportedEncodingException ignored) {

        }
        String finalPass = pass;
        String finalLogin = login_name;

        String url = Config.PM_URL;
        String final_url = url + requestCount + "&pm=0&login_name=" + finalLogin + "&login_password=" + finalPass;

        if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_trash)))) {
            final_url = url + requestCount + "&pm=5&login_name=" + finalLogin + "&login_password=" + finalPass;
        }
        if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_outbox)))) {
            final_url = url + requestCount + "&pm=1&login_name=" + finalLogin + "&login_password=" + finalPass;
        }
        if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_arhiv)))) {
            final_url = url + requestCount + "&pm=2&login_name=" + finalLogin + "&login_password=" + finalPass;
        }
        if ((tab_title != null) && (tab_title.equalsIgnoreCase(requireContext().getString(R.string.tab_ish)))) {
            final_url = url + requestCount + "&pm=3&login_name=" + finalLogin + "&login_password=" + finalPass;
        }

        return new JsonArrayRequest(final_url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    progressBarBottom.setVisibility(View.GONE);

                    if (requestCount == 1) {
                        listFeed.clear();
                        recyclerView.post(() -> recyclerView.scrollToPosition(0));
                    }

                    if (response.length() == 0) {
                        swipLayout.setRefreshing(false);
                        return;
                    } else emptyView.setVisibility(View.GONE);


                    List<FeedPm> newItems = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        FeedPm jsonFeed = new FeedPm();
                        try {
                            JSONObject json = response.getJSONObject(i);
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setImageUrl(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setIs_new(json.getInt(Config.TAG_HITS));
                            jsonFeed.setLast_poster_name(json.getString(Config.TAG_LAST_POSTER_NAME));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setFullText(json.getString(Config.TAG_FULL_TEXT));
                            newItems.add(jsonFeed);
                        } catch (JSONException e) {
                            Log.e("PmFragment", "JSON parsing error", e);
                        }
                    }
                    listFeed.addAll(newItems);
                    recyclerView.post(() -> adapter.updateData(listFeed));
                    swipLayout.setRefreshing(false);
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    progressBarBottom.setVisibility(View.GONE);
                    swipLayout.setRefreshing(false);
                    Log.e("PmFragment", "Volley error", error);
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        progressBarBottom.setVisibility(View.VISIBLE);
        controller.addToRequestQueue(getDataFromServer(requestCount));
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
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


}
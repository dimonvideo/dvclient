package com.dimonvideo.client.ui.pm;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

public class PmTrashFragment extends Fragment {

    private List<FeedPm> listFeed;
    public RecyclerView recyclerView;
    public AdapterPm adapter;
    SwipeRefreshLayout swipLayout;
    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    String url = Config.PM_URL, pm;
    private FragmentHomeBinding binding;
    LinearLayout emptyLayout;
    TextView emptyView;
    protected EventBus eventBus;

    public PmTrashFragment() {
        // Required empty public constructor
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        String pm = event.action;
        if ((pm != null) && (pm.equals("deleted"))) update();
        Log.e("---", "PmTrashFragment event: "+pm );
    }

    @SuppressLint({"NotifyDataSetChanged"})
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

        requestCount = 1;
        emptyView = binding.emptyView;
        emptyLayout = binding.linearEmpty;

        emptyView.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.VISIBLE);
        recyclerView = binding.recyclerView;
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());

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
        listFeed = new ArrayList<>();

        progressBar = binding.progressbar;
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = binding.ProgressBarBottom;
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new AdapterPm(listFeed);

        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);

        // swipe to restore
        ItemTouchHelper.SimpleCallback itemTouchHelper = new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT) {
            private final Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_email);
            private final ColorDrawable background = new ColorDrawable(Color.GREEN);
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
                super.onChildDraw(c, recyclerView, viewHolder, dX/4, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;

                int iconMargin = (itemView.getHeight() - Objects.requireNonNull(deleteIcon).getIntrinsicHeight()) / 2;
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
                if (position >= 0) ((AdapterPm) adapter).restoreItem(position);
                Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.msg_restored), Snackbar.LENGTH_LONG);
                snackbar.setAction(getString(R.string.tab_inbox), view -> {
                    assert getParentFragment() != null;
                    ViewPager2 viewPager = getParentFragment().requireView().findViewById(R.id.view_pager);
                    viewPager.setCurrentItem(0, true);
                    EventBus.getDefault().postSticky(new MessageEvent("13", null, null, null, "restored", null));

                });

                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();
            }
        };

        new ItemTouchHelper(itemTouchHelper).attachToRecyclerView(recyclerView);

        recyclerView.setAdapter(adapter);

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

    // запрос к серверу апи
    @SuppressLint("NotifyDataSetChanged")
    private JsonArrayRequest getDataFromServer(int requestCount) {
        String login_name = AppController.getInstance().userName(getString(R.string.nav_header_title));
        String pass = AppController.getInstance().userPassword();
        try {
            pass = URLEncoder.encode(pass, "utf-8");
            login_name = URLEncoder.encode(login_name, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String finalPass = pass;
        String finalLogin = login_name;

        return new JsonArrayRequest(url + requestCount + "&pm=5&login_name=" + finalLogin + "&login_password=" + finalPass,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
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
                    ProgressBarBottom.setVisibility(View.GONE);
                });
    }

    // получение данных и увеличение номера страницы
    private void getData() {
        ProgressBarBottom.setVisibility(View.VISIBLE);
        AppController.getInstance().addToRequestQueue(getDataFromServer(requestCount));
        requestCount++;
    }

    private void update() {
        requestCount = 1;
        listFeed = new ArrayList<>();
        getData();
        TextView fab_badge = MainActivity.binding.appBarMain.fabBadge;
        fab_badge.setVisibility(View.GONE);
        swipLayout.setRefreshing(false);
        Log.e(Config.TAG, "PmFragmentTrash update ");
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
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
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

}
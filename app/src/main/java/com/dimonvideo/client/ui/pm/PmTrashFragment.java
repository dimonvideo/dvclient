package com.dimonvideo.client.ui.pm;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.PmAdapter;
import com.dimonvideo.client.model.FeedPm;
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

@RequiresApi(api = Build.VERSION_CODES.M)
public class PmTrashFragment extends Fragment implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener  {

    private List<FeedPm> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    static int razdel = 13;
    String url = Config.PM_URL;

    public PmTrashFragment() {
        // Required empty public constructor
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        requestCount = 1;
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        recyclerView = root.findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnScrollChangeListener(this);
        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireActivity());

        progressBar = root.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = root.findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new PmAdapter(listFeed, getContext());

        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);

        // swipe to restore
        ItemTouchHelper.SimpleCallback itemTouchHelper = new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT) {
            private Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_email);
            private final ColorDrawable background = new ColorDrawable(Color.GREEN);
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    swipLayout.setEnabled(false);
                } else {
                    swipLayout.setEnabled(true);
                }
            }

            @Override
            public void onChildDraw(Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX/4, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;

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
                int position = viewHolder.getAdapterPosition();
                ((PmAdapter) adapter).restoreItem(position);
                Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.msg_restored), Snackbar.LENGTH_LONG);
                snackbar.setAction(getString(R.string.tab_pm), view -> {

                    FragmentManager fragmentManager = getParentFragmentManager();

                    Fragment homeFrag = new PmFragment();

                    fragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, homeFrag)
                            .addToBackStack(null)
                            .commit();
                });

                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();
            }
        };

        new ItemTouchHelper(itemTouchHelper).attachToRecyclerView(recyclerView);

        recyclerView.setAdapter(adapter);
        // pull to refresh
        swipLayout = root.findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(this);
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.tab_trash));
        setHasOptionsMenu(true);

        return root;
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_pm_inbox, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentManager fragmentManager = getParentFragmentManager();
        if (item.getItemId() == R.id.action_inbox) {
            Fragment homeFrag = new PmFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFrag)
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        if (item.getItemId() == R.id.action_outbox) {
            Fragment homeFrag = new PmOutboxFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFrag)
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        if (item.getItemId() == R.id.action_ish) {
            Fragment homeFrag = new PmIshFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFrag)
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        if (item.getItemId() == R.id.action_arh) {
            Fragment homeFrag = new PmArhivFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFrag)
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        if (item.getItemId() == R.id.action_trash) {
            Fragment homeFrag = new PmTrashFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFrag)
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
    }

    // запрос к серверу апи
    private JsonArrayRequest getDataFromServer(int requestCount) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
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

        return new JsonArrayRequest(url + requestCount + "&pm=5&login_name=" + finalLogin + "&login_password=" + finalPass,
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

    // получение следующей страницы при скролле
    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (isLastItemDisplaying(recyclerView)) {
            getData();
        }
    }

    // обновление
    @Override
    public void onRefresh() {
        requestCount = 1;
        getParentFragmentManager()
                .beginTransaction()
                .detach(PmTrashFragment.this)
                .attach(PmTrashFragment.this)
                .commit();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

}
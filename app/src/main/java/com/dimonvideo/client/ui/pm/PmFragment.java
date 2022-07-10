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
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
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
import androidx.annotation.RequiresApi;
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
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.PmAdapter;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.model.FeedPm;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.SwipeController;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

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

    private List<FeedPm> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    SwipeRefreshLayout swipLayout;
    LinearLayout emptyLayout;

    private RequestQueue requestQueue;

    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    static int razdel = 13;
    ViewPager2 viewPager;
    TabsAdapter adapt;
    TabLayoutMediator tabLayoutMediator;
    TabLayout tabs;
    private ArrayList<String> tabTiles = new ArrayList<>();

    public PmFragment() {
        // Required empty public constructor
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        razdel = event.razdel;
    }

    @SuppressLint({"DetachAndAttachSameFragment", "NotifyDataSetChanged"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tabs, container, false);
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

        tabs = root.findViewById(R.id.tabLayout);
        viewPager = root.findViewById(R.id.view_pager);
        adapt = new TabsAdapter(getChildFragmentManager(), getLifecycle());

        tabTiles.add(getString(R.string.tab_inbox));
        tabTiles.add(getString(R.string.tab_outbox));
        tabTiles.add(getString(R.string.tab_arhiv));
        tabTiles.add(getString(R.string.tab_friends));
        tabTiles.add(getString(R.string.tab_ish));
        tabTiles.add(getString(R.string.tab_trash));

        adapt.clearList();
        adapt.addFragment(new PmVhodFragment());
        adapt.addFragment(new PmOutboxFragment());
        adapt.addFragment(new PmArhivFragment());
        adapt.addFragment(new PmFriendsFragment());
        adapt.addFragment(new PmIshFragment());
        adapt.addFragment(new PmTrashFragment());

        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0,false);
        viewPager.setOffscreenPageLimit(2);
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.tab_pm);
        tabLayoutMediator = new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            //position of the current tab and the tab
            tab.setText(tabTiles.get(position));
        });

        tabLayoutMediator.attach();
        // override back pressed
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {

            if( keyCode == KeyEvent.KEYCODE_BACK  && event.getAction() == KeyEvent.ACTION_DOWN )
            {
                if (viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1,false);
                } else {
                    requireActivity().onBackPressed();
                }
                return true;
            } else {
                return false;
            }
        });




        return root;
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
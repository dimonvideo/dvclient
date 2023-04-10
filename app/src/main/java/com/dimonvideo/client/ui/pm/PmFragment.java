package com.dimonvideo.client.ui.pm;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.PmAdapter;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.databinding.FragmentTabsBinding;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.UpdatePm;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Objects;

public class PmFragment extends Fragment {

    public RecyclerView recyclerView;
    public PmAdapter adapter;
    ViewPager2 viewPager;
    TabsAdapter adapt;
    TabLayoutMediator tabLayoutMediator;
    TabLayout tabs;
    private final ArrayList<String> tabTiles = new ArrayList<>();
    private FragmentTabsBinding binding;
    static int razdel = 13;

    public PmFragment() {
        // Required empty public constructor
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentTabsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        final boolean is_outbox = sharedPrefs.getBoolean("dvc_pm_outbox", true);
        final boolean is_arc = sharedPrefs.getBoolean("dvc_pm_arc", false);

        tabs = binding.tabLayout;
        viewPager = binding.viewPager;
        adapt = new TabsAdapter(getChildFragmentManager(), getLifecycle());

        tabTiles.add(getString(R.string.tab_inbox));
        tabTiles.add(getString(R.string.tab_members));
        tabTiles.add(getString(R.string.tab_friends));
        tabTiles.add(getString(R.string.tab_ignore));
        tabTiles.add(getString(R.string.tab_trash));
        if (!is_outbox) tabTiles.add(getString(R.string.tab_outbox));
        if (!is_outbox) tabTiles.add(getString(R.string.tab_ish));
        if (!is_arc) tabTiles.add(getString(R.string.tab_arhiv));

        adapt.clearList();
        adapt.addFragment(new PmVhodFragment());
        adapt.addFragment(new PmMembersFragment());
        adapt.addFragment(new PmFriendsFragment());
        adapt.addFragment(new PmIgnorFragment());
        adapt.addFragment(new PmTrashFragment());
        if (!is_outbox) adapt.addFragment(new PmOutboxFragment());
        if (!is_outbox) adapt.addFragment(new PmIshFragment());
        if (!is_arc) adapt.addFragment(new PmArhivFragment());

        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0,false);
        viewPager.setOffscreenPageLimit(4);
        viewPager.setUserInputEnabled(false);
        Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
        toolbar.setTitle(R.string.tab_pm);

        SearchView searchView = toolbar.findViewById(R.id.action_search);
        if (searchView != null) searchView.setVisibility(View.INVISIBLE);

        // прячем поиск и кнопку где не используется
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();

                if (pos == 1) {
                    if (searchView != null) searchView.setVisibility(View.VISIBLE);

                } else {
                    if (searchView != null) searchView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });



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
                    viewPager.setCurrentItem(0,false);
                } else {
                    requireActivity().onBackPressed();
                }
                return true;
            } else {
                return false;
            }
        });

        View view = MainActivity.binding.getRoot();
        UpdatePm.update(requireContext(), view);



        return root;
    }

    @Override
    public void onDestroy() {
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
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
    }

}
package com.dimonvideo.client.ui.pm;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.PmAdapter;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.databinding.FragmentTabsBinding;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.UpdatePm;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class PmFragment extends Fragment {

    public RecyclerView recyclerView;
    public PmAdapter adapter;
    ViewPager2 viewPager;
    TabsAdapter adapt;
    TabLayoutMediator tabLayoutMediator;
    TabLayout tabs;
    private final ArrayList<String> tabTiles = new ArrayList<>();
    private final ArrayList<Integer> tabIcons = new ArrayList<>();
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
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        final boolean is_outbox = AppController.getInstance().isPmOutbox();
        final boolean is_arc = AppController.getInstance().isPmArchive();
        final boolean dvc_tab_icons = AppController.getInstance().isTabIcons();

        tabs = binding.tabLayout;
        viewPager = binding.viewPager;
        adapt = new TabsAdapter(getChildFragmentManager(), getLifecycle());

        tabTiles.add(getString(R.string.tab_inbox));
        tabIcons.add(R.drawable.outline_inbox_24);
        tabTiles.add(getString(R.string.tab_members));
        tabIcons.add(R.drawable.outline_people_24);
        tabTiles.add(getString(R.string.tab_friends));
        tabIcons.add(R.drawable.outline_group_add_24);
        tabTiles.add(getString(R.string.tab_ignore));
        tabIcons.add(R.drawable.outline_group_remove_24);
        tabTiles.add(getString(R.string.tab_trash));
        tabIcons.add(R.drawable.outline_delete_24);
        if (!is_outbox) {
            tabTiles.add(getString(R.string.tab_outbox));
            tabIcons.add(R.drawable.outline_outbox_24);
        }
        if (!is_outbox) {
            tabTiles.add(getString(R.string.tab_ish));
            tabIcons.add(R.drawable.outline_call_missed_outgoing_24);
        }
        if (!is_arc) {
            tabTiles.add(getString(R.string.tab_arhiv));
            tabIcons.add(R.drawable.outline_archive_24);
        }

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
                toolbar.setSubtitle(tabTiles.get(pos));

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


            if (dvc_tab_icons) {
                tab.setIcon(tabIcons.get(position));
            } else {
                tab.setText(tabTiles.get(position));
            }
        });

        tabLayoutMediator.attach();

        UpdatePm.update(requireContext());
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
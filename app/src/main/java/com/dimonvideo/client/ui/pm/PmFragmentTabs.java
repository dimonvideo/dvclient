package com.dimonvideo.client.ui.pm;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterTabs;
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

public class PmFragmentTabs extends Fragment {

    private final ArrayList<String> tabTiles = new ArrayList<>();
    private final ArrayList<Integer> tabIcons = new ArrayList<>();
    private FragmentTabsBinding binding;
    private String razdel = "13";

    public PmFragmentTabs() {
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

        TabLayout tabs = binding.tabLayout;
        ViewPager2 viewPager = binding.viewPager;
        AdapterTabs adapt = new AdapterTabs(getChildFragmentManager(), getLifecycle());

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
        Bundle bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_inbox));
        PmFragment fragment_inbox = new PmFragment();
        fragment_inbox.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_trash));
        PmFragment fragment_trash = new PmFragment();
        fragment_trash.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_outbox));
        PmFragment fragment_outbox = new PmFragment();
        fragment_outbox.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab",getString(R.string.pm_send));
        PmFragment fragment_send = new PmFragment();
        fragment_send.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_arhiv));
        PmFragment fragment_arc = new PmFragment();
        fragment_arc.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_members));
        PmFragmentMembers fragment_members = new PmFragmentMembers();
        fragment_members.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_friends));
        PmFragmentMembers fragment_friends = new PmFragmentMembers();
        fragment_friends.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_ignore));
        PmFragmentMembers fragment_ignore = new PmFragmentMembers();
        fragment_ignore.setArguments(bundle);

        adapt.addFragment(fragment_inbox);
        adapt.addFragment(fragment_members);
        adapt.addFragment(fragment_friends);
        adapt.addFragment(fragment_ignore);
        adapt.addFragment(fragment_trash);
        if (!is_outbox) adapt.addFragment(fragment_outbox);
        if (!is_outbox) adapt.addFragment(fragment_send);
        if (!is_arc) adapt.addFragment(fragment_arc);

        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0,false);
        viewPager.setOffscreenPageLimit(2);
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


        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabs, viewPager, (tab, position) -> {


            if (dvc_tab_icons) {
                tab.setIcon(tabIcons.get(position));
            } else {
                tab.setText(tabTiles.get(position));
            }
        });

        tabLayoutMediator.attach();

        UpdatePm.update(requireContext(), razdel, MainActivity.binding.getRoot());


        // перехват кнопки назад.
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                Log.e("---", "handleOnBackPressed: "+ razdel);
                if (viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1,false);
                } else {
                    requireActivity().onBackPressed();
                }

            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
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
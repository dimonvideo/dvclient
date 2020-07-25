package com.dimonvideo.client.ui.forum;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.util.MessageEvent;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;

public class ForumFragment extends Fragment  {

    int razdel = 8; // forum fragment

    public ForumFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tabs, container, false);

        TabLayout tabLayout = root.findViewById(R.id.tabLayout);
        ViewPager viewPager = root.findViewById(R.id.view_pager);

        TabsAdapter adapt = new TabsAdapter(getChildFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, getContext());
        adapt.addfrg(new ForumFragmentTopics(),getString(R.string.tab_topics));
        adapt.addfrg(new ForumFragmentForums(),getString(R.string.tab_forums));
        adapt.addfrg(new ForumFragmentTopicsNoPosts(),getString(R.string.tab_topics_no_posts));

        viewPager.setAdapter(adapt);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setOffscreenPageLimit(2);

        tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        // set default title
        toolbar.setTitle(getString(R.string.tab_topics));
        // add here
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) toolbar.setTitle(getString(R.string.tab_topics));
                if (pos == 1) toolbar.setTitle(getString(R.string.tab_forums));
                if (pos == 2) toolbar.setTitle(getString(R.string.tab_topics_no_posts));
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if( keyCode == KeyEvent.KEYCODE_BACK )
            {
                if (viewPager.getCurrentItem() == 0) toolbar.setTitle(getString(R.string.tab_topics));
                if (viewPager.getCurrentItem() == 1) toolbar.setTitle(getString(R.string.tab_forums));
                if (viewPager.getCurrentItem() == 2) toolbar.setTitle(getString(R.string.tab_topics_no_posts));

                return false;
            } else {
                return false;
            }
        });
        EventBus.getDefault().post(new MessageEvent(razdel, null));
        Log.e("tag", String.valueOf(razdel));
        return root;
    }


}
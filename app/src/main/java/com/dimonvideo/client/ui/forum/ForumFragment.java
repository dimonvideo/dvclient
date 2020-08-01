package com.dimonvideo.client.ui.forum;

import android.content.SharedPreferences;
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
import androidx.preference.PreferenceManager;
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
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String login = sharedPrefs.getString("dvc_password", "");
        final boolean dvc_tab_inline = sharedPrefs.getBoolean("dvc_tab_inline", false);
        final boolean tab_topics_no_posts = sharedPrefs.getBoolean("dvc_tab_topics_no_posts", true);
        TabsAdapter adapt = new TabsAdapter(getChildFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, getContext());
        adapt.addfrg(new ForumFragmentTopics(),getString(R.string.tab_topics));
        adapt.addfrg(new ForumFragmentForums(),getString(R.string.tab_forums));
        if (tab_topics_no_posts) adapt.addfrg(new ForumFragmentTopicsNoPosts(),getString(R.string.tab_topics_no_posts));
        if ((login != null) && (login.length() > 2)) adapt.addfrg(new ForumFragmentTopicsFav(),getString(R.string.tab_favorites));

        viewPager.setAdapter(adapt);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setOffscreenPageLimit(4);
        if (dvc_tab_inline) tabLayout.setTabMode(TabLayout.MODE_FIXED);

        tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        // set default title
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.tab_topics));
        // add here
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) toolbar.setTitle(getString(R.string.tab_topics));
                if (pos == 1) toolbar.setTitle(getString(R.string.tab_forums));
                if (pos == 2) toolbar.setTitle(getString(R.string.tab_topics_no_posts));
                if (pos == 3) toolbar.setTitle(getString(R.string.tab_favorites));
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
                if (viewPager.getCurrentItem() == 3) toolbar.setTitle(getString(R.string.tab_favorites));
             //   if (viewPager.getCurrentItem() > 0) viewPager.setCurrentItem(viewPager.getCurrentItem()-1);

                return false;
            } else {
                return false;
            }
        });
        EventBus.getDefault().post(new MessageEvent(razdel, null));
        return root;
    }


}
package com.dimonvideo.client.ui.forum;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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
        toolbar.setTitle(getString(R.string.menu_forum));
        EventBus.getDefault().post(new MessageEvent(razdel, null));

        // override back pressed
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {

            if( keyCode == KeyEvent.KEYCODE_BACK  && event.getAction() == KeyEvent.ACTION_DOWN )
            {
                if (viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1,false);
                } else if (viewPager.getCurrentItem() == 0) {
                    requireActivity().onBackPressed();
                }
                return true;
            } else {
                return false;
            }
        });
        return root;
    }

}
package com.dimonvideo.client.ui.forum;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.ForumTabsAdapter;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.util.FragmentToActivity;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

public class ForumFragment extends Fragment  {

    int razdel = 8; // forum fragment
    private FragmentToActivity mCallback;

    public ForumFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_forum, container, false);

        TabLayout tabLayout = root.findViewById(R.id.tabLayout);
        ViewPager viewPager = root.findViewById(R.id.view_pager);

        ForumTabsAdapter adapt = new ForumTabsAdapter(getParentFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, getContext());
        adapt.addfrg(new ForumFragmentTopics(),getString(R.string.tab_topics));
        adapt.addfrg(new ForumFragmentForums(),getString(R.string.tab_forums));
        adapt.addfrg(new ForumFragmentTopicsNoPosts(),getString(R.string.tab_topics_no_posts));

        viewPager.setAdapter(adapt);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setOffscreenPageLimit(2);
        sendData(String.valueOf(razdel));


        root.setFocusableInTouchMode(true);
        root.requestFocus();

        root.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    //If first tab is open, then quit
                    if (viewPager.getCurrentItem() == 0) {

                        requireActivity().onBackPressed();


                    }else {
                        viewPager.post(() -> viewPager.setCurrentItem(0));
                        FragmentManager fragmentManager = getParentFragmentManager();

                        Fragment homeFrag = new ForumFragment(); // forum

                        Bundle bundle = new Bundle();
                        bundle.putInt(Config.TAG_CATEGORY, 8);
                        homeFrag.setArguments(bundle);

                        fragmentManager.beginTransaction()
                                .replace(R.id.nav_host_fragment, homeFrag)
                                .addToBackStack(String.valueOf(R.string.menu_forum))
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .commit();
                    }
                    return true;
                }
            }
            return false;
        });

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
                if (pos == 2) toolbar.setTitle(getString(R.string.tab_topics_no_posts));            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        return root;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mCallback = (FragmentToActivity) context;
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
    }

    private void sendData(String comm)
    {
        try{ mCallback.communicate(comm);
        } catch (Throwable ignored) {
        }
    }


}
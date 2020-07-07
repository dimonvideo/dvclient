package com.dimonvideo.client.ui.forum;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.ForumTabsAdapter;
import com.google.android.material.tabs.TabLayout;

public class ForumFragment extends Fragment  {


    public ForumFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_forum, container, false);

        final TabLayout tabLayout = (TabLayout) root.findViewById(R.id.tabLayout);
        final ViewPager viewPager = (ViewPager) root.findViewById(R.id.view_pager);
        ForumTabsAdapter adapt = new ForumTabsAdapter(getChildFragmentManager());
        adapt.addfrg(new ForumFragmentTopics(),getString(R.string.tab_topics));
        adapt.addfrg(new ForumFragmentForums(),getString(R.string.tab_forums));

        viewPager.setAdapter(adapt);
        tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));

        return root;
    }



}
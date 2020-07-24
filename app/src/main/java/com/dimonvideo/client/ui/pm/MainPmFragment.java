package com.dimonvideo.client.ui.pm;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.ui.main.MainFragmentCats;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.ui.main.MainFragmentHorizontal;
import com.dimonvideo.client.util.MessageEvent;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;

public class MainPmFragment extends Fragment  {

    int razdel = 10;
    String story = null;

    public MainPmFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tabs, container, false);
        if (this.getArguments() != null) {
            razdel = getArguments().getInt(Config.TAG_CATEGORY);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            EventBus.getDefault().post(new MessageEvent(razdel, story));
        }

        TabLayout tabLayout = root.findViewById(R.id.tabLayout);
        ViewPager viewPager = root.findViewById(R.id.view_pager);

        TabsAdapter adapt = new TabsAdapter(getChildFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, getContext());
        adapt.addfrg(new PmFragment(),getString(R.string.tab_inbox));
        adapt.addfrg(new MainFragmentHorizontal(),getString(R.string.tab_details));
        adapt.addfrg(new MainFragmentCats(),getString(R.string.tab_categories));

        viewPager.setAdapter(adapt);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setOffscreenPageLimit(2);

        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.tab_pm));



        tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));

        return root;
    }
}
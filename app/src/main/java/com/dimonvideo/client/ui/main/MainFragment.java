package com.dimonvideo.client.ui.main;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.ui.forum.ForumFragmentTopicsNoPosts;
import com.dimonvideo.client.util.MessageEvent;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;

public class MainFragment extends Fragment  {

    int razdel = 10;
    String story = null;

    public MainFragment() {
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
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        final boolean is_more = sharedPrefs.getBoolean("dvc_more", false);
        String login = sharedPrefs.getString("dvc_password", "");
        TabsAdapter adapt = new TabsAdapter(getChildFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, getContext());
        adapt.addfrg(new MainFragmentContent(),getString(R.string.tab_last));
        if (is_more) adapt.addfrg(new MainFragmentHorizontal(),getString(R.string.tab_details));
        adapt.addfrg(new MainFragmentCats(),getString(R.string.tab_categories));
        if ((login != null) && (login.length() > 2)) adapt.addfrg(new MainFragmentFav(),getString(R.string.tab_favorites));

        viewPager.setAdapter(adapt);

        tabLayout.setupWithViewPager(viewPager);
        viewPager.setOffscreenPageLimit(4);
        EventBus.getDefault().post(new MessageEvent(razdel, null));


        Log.e("tag", String.valueOf(razdel));


        tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));



        return root;
    }
}
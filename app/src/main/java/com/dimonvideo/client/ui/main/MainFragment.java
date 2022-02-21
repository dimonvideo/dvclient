package com.dimonvideo.client.ui.main;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
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
import androidx.viewpager2.widget.ViewPager2;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.util.MessageEvent;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;

public class MainFragment extends Fragment  {

    int razdel = 10;
    String story = null;

    public MainFragment() {

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

                View root = inflater.inflate(R.layout.fragment_tabs, container, false);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        if (this.getArguments() != null) {
            razdel = getArguments().getInt(Config.TAG_CATEGORY);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            EventBus.getDefault().postSticky(new MessageEvent(razdel, story));
        }

        final boolean is_more = sharedPrefs.getBoolean("dvc_more", false);
        final boolean dvc_tab_inline = sharedPrefs.getBoolean("dvc_tab_inline", false);
        String login = sharedPrefs.getString("dvc_password", "");


        TabLayout tabLayout = root.findViewById(R.id.tabLayout);
        ViewPager viewPager = root.findViewById(R.id.view_pager);
        TabsAdapter adapt = new TabsAdapter(
                getChildFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, getContext());

        adapt.addfrg(new MainFragmentContent(),getString(R.string.tab_last));
        if (!is_more) adapt.addfrg(new MainFragmentHorizontal(),getString(R.string.tab_details));
        adapt.addfrg(new MainFragmentCats(),getString(R.string.tab_categories));
        if ((login != null) && (login.length() > 2)) adapt.addfrg(new MainFragmentFav(),getString(R.string.tab_favorites));

        if (dvc_tab_inline) tabLayout.setTabMode(TabLayout.MODE_FIXED);

        viewPager.setAdapter(adapt);

        tabLayout.setupWithViewPager(viewPager);
        viewPager.setOffscreenPageLimit(4);

        EventBus.getDefault().post(new MessageEvent(razdel, null));

        tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));

        // override back pressed
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {

            if( keyCode == KeyEvent.KEYCODE_BACK  && event.getAction() == KeyEvent.ACTION_DOWN )
            {
                if (viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1,false);
                } else {
                    Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
                    toolbar.setTitle(getString(R.string.menu_home));
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
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
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.ui.main.MainFragmentCats;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.ui.main.MainFragmentFav;
import com.dimonvideo.client.ui.main.MainFragmentHorizontal;
import com.dimonvideo.client.util.MessageEvent;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

public class ForumFragment extends Fragment  {

    int razdel = 8; // forum fragment
    String story = null;
    ViewPager2 viewPager;
    TabsAdapter adapt;
    TabLayoutMediator tabLayoutMediator;
    TabLayout tabs;
    private ArrayList<String> tabTiles = new ArrayList<>();

    public ForumFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tabs, container, false);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String login = sharedPrefs.getString("dvc_password", "");
        final boolean dvc_tab_inline = sharedPrefs.getBoolean("dvc_tab_inline", false);
        final boolean tab_topics_no_posts = sharedPrefs.getBoolean("dvc_tab_topics_no_posts", true);
        final boolean is_favor = sharedPrefs.getBoolean("dvc_favor", false);

        tabs = root.findViewById(R.id.tabLayout);
        if (dvc_tab_inline) tabs.setTabMode(TabLayout.MODE_FIXED);
        viewPager = root.findViewById(R.id.view_pager);
        adapt = new TabsAdapter(getChildFragmentManager(), getLifecycle());

        // вкладки
        tabTiles.add(getString(R.string.tab_topics));
        tabTiles.add(getString(R.string.tab_forums));
        if (tab_topics_no_posts) tabTiles.add(getString(R.string.tab_topics_no_posts));
        if ((login != null) && (login.length() > 2) && (!is_favor)) tabTiles.add(getString(R.string.tab_favorites));
        adapt.clearList();
        adapt.addFragment(new ForumFragmentTopics());
        adapt.addFragment(new ForumFragmentForums());
        if (tab_topics_no_posts) adapt.addFragment(new ForumFragmentTopicsNoPosts());
        if ((login != null) && (login.length() > 2) && (!is_favor))adapt.addFragment(new ForumFragmentTopicsFav());


        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0,true);
        viewPager.setOffscreenPageLimit(4);

        tabLayoutMediator = new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            tab.setText(tabTiles.get(position));
        });

        tabLayoutMediator.attach();

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
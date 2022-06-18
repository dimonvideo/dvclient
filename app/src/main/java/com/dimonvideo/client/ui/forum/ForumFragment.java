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
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.ui.main.MainFragmentCats;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.ui.main.MainFragmentFav;
import com.dimonvideo.client.ui.main.MainFragmentHorizontal;
import com.dimonvideo.client.util.MessageEvent;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Objects;

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

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
        story = event.story;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tabs, container, false);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        razdel = 8;
        EventBus.getDefault().postSticky(new MessageEvent(razdel, story));



        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String login = sharedPrefs.getString("dvc_password", "");
        final boolean dvc_tab_inline = sharedPrefs.getBoolean("dvc_tab_inline", false);
        final boolean tab_topics_no_posts = sharedPrefs.getBoolean("dvc_tab_topics_no_posts", false);
        final boolean is_favor = sharedPrefs.getBoolean("dvc_favor", false);

        tabs = root.findViewById(R.id.tabLayout);
        if (dvc_tab_inline) tabs.setTabMode(TabLayout.MODE_FIXED);
        viewPager = root.findViewById(R.id.view_pager);
        adapt = new TabsAdapter(getChildFragmentManager(), getLifecycle());

        // вкладки
        tabTiles.add(getString(R.string.tab_topics));
        tabTiles.add(getString(R.string.tab_forums));
        if (tab_topics_no_posts) tabTiles.add(getString(R.string.tab_topics_no_posts));
        if ((login != null) && (login.length() > 2) && (is_favor)) tabTiles.add(getString(R.string.tab_favorites));
        adapt.clearList();
        adapt.addFragment(new ForumFragmentTopics());
        adapt.addFragment(new ForumFragmentForums());
        if (tab_topics_no_posts) adapt.addFragment(new ForumFragmentTopicsNoPosts());
        if ((login != null) && (login.length() > 2) && (is_favor))adapt.addFragment(new ForumFragmentTopicsFav());


        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0,true);
        viewPager.setOffscreenPageLimit(1);

        tabLayoutMediator = new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            tab.setText(tabTiles.get(position));
        });

        tabLayoutMediator.attach();

        // set default title
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.menu_forum));
        toolbar.setSubtitle(null);
        EventBus.getDefault().post(new MessageEvent(razdel, null));

        // override back pressed
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {

            if( keyCode == KeyEvent.KEYCODE_BACK  && event.getAction() == KeyEvent.ACTION_DOWN )
            {
                if (viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1,false);
                } else {


                    toolbar.setSubtitle(null);
                    FragmentManager fm = getParentFragmentManager();
                    if (fm.getBackStackEntryCount() == 1) {
                        Fragment fragment = new ForumFragment();
                        FragmentTransaction ft = ((FragmentActivity) requireContext()).getSupportFragmentManager().beginTransaction();
                        ft.add(R.id.nav_host_fragment, fragment);
                        ft.addToBackStack("ForumFragment");
                        ft.commit();
                    } else if (fm.getBackStackEntryCount() == 0){
                        toolbar.setTitle(R.string.menu_home);
                        Fragment fragment = new MainFragment();
                        FragmentTransaction ft = ((FragmentActivity) requireContext()).getSupportFragmentManager().beginTransaction();
                        ft.add(R.id.nav_host_fragment, fragment);
                        ft.addToBackStack(null);
                        ft.commit();
                    } else requireActivity().finish();

                }
                return true;
            } else {
                return false;
            }
        });
        return root;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
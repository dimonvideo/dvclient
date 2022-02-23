package com.dimonvideo.client.ui.main;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class MainFragment extends Fragment  {

    static int razdel = 10;
    String story = null;
    ViewPager2 viewPager;
    TabsAdapter adapt;
    TabLayoutMediator tabLayoutMediator;
    TabLayout tabs;
    private ArrayList<String> tabTiles = new ArrayList<>();

    public MainFragment() {

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

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        if (this.getArguments() != null) {
            razdel = getArguments().getInt(Config.TAG_CATEGORY);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            EventBus.getDefault().postSticky(new MessageEvent(razdel, story));
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        if (this.getArguments() != null) {
            razdel = getArguments().getInt(Config.TAG_CATEGORY);
            EventBus.getDefault().postSticky(new MessageEvent(razdel, null));
        }
        final boolean is_more = sharedPrefs.getBoolean("dvc_more", false);
        final boolean dvc_tab_inline = sharedPrefs.getBoolean("dvc_tab_inline", false);
        final boolean is_favor = sharedPrefs.getBoolean("dvc_favor", false);
        final boolean is_comment = sharedPrefs.getBoolean("dvc_comment", false);
        String login = sharedPrefs.getString("dvc_password", "");


        tabs = root.findViewById(R.id.tabLayout);
        if (dvc_tab_inline) tabs.setTabMode(TabLayout.MODE_FIXED);
        viewPager = root.findViewById(R.id.view_pager);
        adapt = new TabsAdapter(getChildFragmentManager(), getLifecycle());

        // вкладки
        tabTiles.add(getString(R.string.tab_last));
        if (!is_more)  tabTiles.add(getString(R.string.tab_details));
        tabTiles.add(getString(R.string.tab_categories));
        if ((login != null) && (login.length() > 2) && (!is_favor)) tabTiles.add(getString(R.string.tab_favorites));
        if (!is_comment)  tabTiles.add(getString(R.string.Comments));
        adapt.clearList();
        adapt.addFragment(new MainFragmentContent());
        if (!is_more) adapt.addFragment(new MainFragmentHorizontal());
        adapt.addFragment(new MainFragmentCats());
        if ((login != null) && (login.length() > 2) && (!is_favor))adapt.addFragment(new MainFragmentFav());
        if (!is_comment) adapt.addFragment(new MainFragmentComments());


        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0,true);
        viewPager.setOffscreenPageLimit(4);

        tabLayoutMediator = new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            //position of the current tab and the tab
            tab.setText(tabTiles.get(position));
        });

        tabLayoutMediator.attach();
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
                    Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
                    toolbar.setTitle(getString(R.string.menu_home));
                    requireActivity().finish();

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
}
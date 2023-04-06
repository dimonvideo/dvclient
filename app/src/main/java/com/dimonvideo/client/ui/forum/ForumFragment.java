package com.dimonvideo.client.ui.forum;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.databinding.FragmentTabsBinding;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.UpdatePm;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class ForumFragment extends Fragment  {

    int razdel = 8; // forum fragment
    String story = null;
    ViewPager2 viewPager;
    TabsAdapter adapt;
    TabLayoutMediator tabLayoutMediator;
    TabLayout tabs;
    private final ArrayList<String> tabTiles = new ArrayList<>();
    FloatingActionButton fab;
    private FragmentTabsBinding binding;

    public ForumFragment() {
        // Required empty public constructor
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        razdel = event.razdel;
        story = event.story;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentTabsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        razdel = 8;
        EventBus.getDefault().postSticky(new MessageEvent(razdel, story, null));



        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String login = sharedPrefs.getString("dvc_password", "");
        final boolean dvc_tab_inline = sharedPrefs.getBoolean("dvc_tab_inline", false);
        final boolean tab_topics_no_posts = sharedPrefs.getBoolean("dvc_tab_topics_no_posts", false);
        final boolean is_favor = sharedPrefs.getBoolean("dvc_favor", false);

        tabs = binding.tabLayout;
        if (dvc_tab_inline) tabs.setTabMode(TabLayout.MODE_FIXED);
        viewPager = binding.viewPager;
        adapt = new TabsAdapter(getChildFragmentManager(), getLifecycle());

        // вкладки
        tabTiles.add(getString(R.string.tab_topics));
        tabTiles.add(getString(R.string.tab_forums));
        if (tab_topics_no_posts) tabTiles.add(getString(R.string.tab_topics_no_posts));
        if (login.length() > 2 && is_favor) tabTiles.add(getString(R.string.tab_favorites));
        adapt.clearList();
        adapt.addFragment(new ForumFragmentTopics());
        adapt.addFragment(new ForumFragmentForums());
        if (tab_topics_no_posts) adapt.addFragment(new ForumFragmentTopicsNoPosts());
        if (login.length() > 2 && is_favor)adapt.addFragment(new ForumFragmentTopicsFav());


        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0,true);
        viewPager.setOffscreenPageLimit(1);

        tabLayoutMediator = new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            tab.setText(tabTiles.get(position));
        });

        tabLayoutMediator.attach();

        // set default title
        Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
        toolbar.setTitle(getString(R.string.menu_forum));
        toolbar.setSubtitle(null);
        EventBus.getDefault().post(new MessageEvent(razdel, null, null));

        // override back pressed
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {

            if( keyCode == KeyEvent.KEYCODE_BACK  && event.getAction() == KeyEvent.ACTION_DOWN )
            {
                if (viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1,false);
                } else {
                    requireActivity().onBackPressed();
                }
                return true;
            } else {
                return false;
            }
        });

        // написание личных сообщений
        fab = MainActivity.binding.appBarMain.fab;
        fab.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.baseline_mail_24));
        fab.setOnClickListener(view -> {
            ((MainActivity) requireContext()).fabClick();
        });
        View view = MainActivity.binding.getRoot();
        UpdatePm.update(requireActivity(), view);
        return root;
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
        binding = null;

    }

    @Override
    public void onStart() {

        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
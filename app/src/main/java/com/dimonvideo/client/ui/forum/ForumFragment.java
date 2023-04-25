package com.dimonvideo.client.ui.forum;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.databinding.FragmentTabsBinding;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.dimonvideo.client.util.UpdatePm;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class ForumFragment extends Fragment  {

    int razdel = 8; // forum fragment
    String story = null;
    public static ViewPager2 viewPager;
    TabsAdapter adapt;
    TabLayoutMediator tabLayoutMediator;
    TabLayout tabs;
    private final ArrayList<String> tabTiles = new ArrayList<>();
    private final ArrayList<Integer> tabIcons = new ArrayList<>();
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
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        razdel = 8;
        EventBus.getDefault().postSticky(new MessageEvent(razdel, story, null));

        String login = AppController.getInstance().userName("");
        final boolean dvc_tab_inline = AppController.getInstance().isTabsInline();
        final boolean tab_topics_no_posts = AppController.getInstance().isTopicsNoPosts();
        final boolean is_favor = AppController.getInstance().isTabFavor();
        final boolean dvc_tab_icons = AppController.getInstance().isTabIcons();

        tabs = binding.tabLayout;
        if (dvc_tab_inline) tabs.setTabMode(TabLayout.MODE_FIXED);
        viewPager = binding.viewPager;
        adapt = new TabsAdapter(getChildFragmentManager(), getLifecycle());

        // вкладки
        tabTiles.add(getString(R.string.tab_topics));
        tabIcons.add(R.drawable.baseline_home_24);
        tabTiles.add(getString(R.string.tab_forums));
        tabIcons.add(R.drawable.baseline_forum_24);
        if (tab_topics_no_posts) {
            tabTiles.add(getString(R.string.tab_topics_no_posts));
            tabIcons.add(R.drawable.outline_category_24);
        }
        if (login.length() > 2 && is_favor) {
            tabTiles.add(getString(R.string.tab_favorites));
            tabIcons.add(R.drawable.outline_star_border_24);
        }

        adapt.clearList();
        adapt.addFragment(new ForumFragmentTopics());
        adapt.addFragment(new ForumFragmentForums());
        if (tab_topics_no_posts) adapt.addFragment(new ForumFragmentTopicsNoPosts());
        if (login.length() > 2 && is_favor)adapt.addFragment(new ForumFragmentTopicsFav());

        Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
        toolbar.setTitle(getString(R.string.menu_forum));

        if (this.getArguments() != null) {
            viewPager.setCurrentItem(0,true);
            String f_name = String.valueOf(getArguments().getInt(Config.TAG_CATEGORY));
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            toolbar.setSubtitle(f_name);
        }

        // прячем поиск и кнопку где не используется
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                toolbar.setSubtitle(tabTiles.get(pos));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) {
                    toolbar.setSubtitle(tabTiles.get(pos));
                    Fragment fragment = new ForumFragment();
                    Bundle bundle = new Bundle();
                    fragment.setArguments(bundle);
                    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.addToBackStack(fragment.toString());
                    ft.replace(R.id.nav_host_fragment, fragment);
                    ft.commit();
                }

            }
        });


        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0,false);
        viewPager.setOffscreenPageLimit(3);

        tabLayoutMediator = new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            if (dvc_tab_icons) {
                tab.setIcon(tabIcons.get(position));
            } else {
                tab.setText(tabTiles.get(position));
            }
        });

        tabLayoutMediator.attach();

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
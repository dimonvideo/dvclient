package com.dimonvideo.client.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.AdapterTabs;
import com.dimonvideo.client.databinding.FragmentTabsBinding;
import com.dimonvideo.client.util.AppController;
import com.dimonvideo.client.util.MessageEvent;
import com.dimonvideo.client.util.NetworkUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Objects;

public class MainFragment extends Fragment  {

    private String razdel;
    private String story = null;
    public static ViewPager2 viewPager;
    private final ArrayList<String> tabTiles = new ArrayList<>();
    private final ArrayList<Integer> tabIcons = new ArrayList<>();
    private FragmentTabsBinding binding;
    private TextView opros;

    public MainFragment() {

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

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (this.getArguments() != null) {
            razdel = getArguments().getString(Config.TAG_CATEGORY);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            String f_name = getArguments().getString(Config.TAG_RAZDEL);
        }
        final boolean is_opros = AppController.getInstance().isOpros();

        opros = binding.oprosText;
        if (!Objects.equals(razdel, "13")) opros.setVisibility(View.VISIBLE);
        if (!is_opros) opros.setVisibility(View.GONE);

        final boolean is_more = AppController.getInstance().isMore();
        final boolean is_favor = AppController.getInstance().isTabFavor();
        final boolean dvc_tab_icons = AppController.getInstance().isTabIcons();
        final boolean is_comment = AppController.getInstance().isCommentTab();
        String login = AppController.getInstance().userName("");
        final boolean dvc_tab_inline = AppController.getInstance().isTabsInline();

        TabLayout tabs = binding.tabLayout;
        if (dvc_tab_inline) tabs.setTabMode(TabLayout.MODE_FIXED);
        viewPager = binding.viewPager;
        AdapterTabs adapt = new AdapterTabs(getChildFragmentManager(), getLifecycle());

        // вкладки
        tabTiles.add(getString(R.string.tab_last));
        tabIcons.add(R.drawable.baseline_home_24);
        if (is_more)  {
            tabTiles.add(getString(R.string.tab_details));
            tabIcons.add(R.drawable.outline_info_24);
        }
        tabTiles.add(getString(R.string.tab_categories));
        tabIcons.add(R.drawable.outline_category_24);
        if (login.length() > 2 && is_favor) {
            tabTiles.add(getString(R.string.tab_favorites));
            tabIcons.add(R.drawable.outline_star_border_24);
        }
        if (is_comment)  {
            tabTiles.add(getString(R.string.Comments));
            tabIcons.add(R.drawable.baseline_chat_24);
        }
        adapt.clearList();
        adapt.addFragment(new MainFragmentContent());
        if (is_more) adapt.addFragment(new MainFragmentHorizontal());
        adapt.addFragment(new MainFragmentCats());
        if (login.length() > 2 && is_favor) adapt.addFragment(new MainFragmentFav());
        if (is_comment) adapt.addFragment(new MainFragmentComments());


        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0,true);
        viewPager.setOffscreenPageLimit(2);

        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            if (dvc_tab_icons) {
                tab.setIcon(tabIcons.get(position));
            } else {
                tab.setText(tabTiles.get(position));
            }
        });

        tabLayoutMediator.attach();

        if (this.getArguments() != null) {
            viewPager.setCurrentItem(0,true);
        }

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) {
                    if (is_opros) opros.setVisibility(View.VISIBLE);
                } else {
                    opros.setVisibility(View.GONE);

                }
                Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
                toolbar.setSubtitle(tabTiles.get(pos));
                SearchView searchView = toolbar.findViewById(R.id.action_search);
                if ((searchView != null) && (pos != 0)) {
                    searchView.setVisibility(View.INVISIBLE);
                }
                if ((searchView != null) && (pos == 0)) {
                    searchView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) {
                    Fragment fragment = new MainFragmentContent();
                    Bundle bundle = new Bundle();
                    fragment.setArguments(bundle);
                    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.addToBackStack(fragment.toString());
                    ft.replace(R.id.container_frag, fragment);
                    ft.commit();
                    Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
                    toolbar.setSubtitle("");
                }
            }
        });

        // перехват кнопки назад.
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                if (viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1,false);
                } else {
                    requireActivity().onBackPressed();

                }

            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        EventBus.getDefault().postSticky(new MessageEvent(razdel, null, null, null, null, null));

        NetworkUtils.getOprosTitle(opros, requireContext());

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
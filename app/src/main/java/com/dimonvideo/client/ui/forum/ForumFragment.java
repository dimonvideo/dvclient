/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.ui.forum;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
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

public class ForumFragment extends Fragment  {

    private String razdel = "8"; // forum fragment
    private String story = null;
    public static ViewPager2 viewPager;
    private final ArrayList<String> tabTiles = new ArrayList<>();
    private final ArrayList<Integer> tabIcons = new ArrayList<>();
    private FragmentTabsBinding binding;
    private final Toolbar toolbar = MainActivity.binding.appBarMain.toolbar;
    private TextView opros;
    private boolean doubleBackToExitPressedOnce = false;
    private NavController navController;
    private static final long DOUBLE_BACK_PRESS_DELAY = 2000; // Задержка в мс

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

        AppController controller = AppController.getInstance();
        razdel = "8";
        EventBus.getDefault().postSticky(new MessageEvent(razdel, story, null, null, null, null));
        opros = binding.oprosText;
        if (!Objects.equals(razdel, "13")) opros.setVisibility(View.VISIBLE);
        final boolean is_opros = controller.isOpros();
        if (!is_opros) opros.setVisibility(View.GONE);
        String login = controller.userName("");
        final boolean dvc_tab_inline = controller.isTabsInline();
        final boolean tab_topics_no_posts = controller.isTopicsNoPosts();
        final boolean is_favor = controller.isTabFavor();
        final boolean dvc_tab_icons = controller.isTabIcons();

        TabLayout tabs = binding.tabLayout;
        if (dvc_tab_inline) tabs.setTabMode(TabLayout.MODE_FIXED);
        viewPager = binding.viewPager;
        AdapterTabs adapt = new AdapterTabs(getChildFragmentManager(), getLifecycle());

        // Инициализация navController
        try {
            navController = Navigation.findNavController(view);
        } catch (IllegalStateException e) {
            Log.e("MainFragment", "Failed to find NavController", e);
        }

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
        Bundle bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_last));
        ForumFragmentTopics fragment_main = new ForumFragmentTopics();
        fragment_main.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_details));
        ForumFragmentTopics fragment_no_post = new ForumFragmentTopics();
        fragment_no_post.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab",getString(R.string.tab_favorites));
        ForumFragmentTopics fragment_fav = new ForumFragmentTopics();
        fragment_fav.setArguments(bundle);

        adapt.addFragment(fragment_main);
        adapt.addFragment(new ForumFragmentForums());
        if (tab_topics_no_posts) adapt.addFragment(fragment_no_post);
        if (login.length() > 2 && is_favor) adapt.addFragment(fragment_fav);

        toolbar.setTitle(getString(R.string.menu_forum));

        if (this.getArguments() != null) {
            viewPager.setCurrentItem(0,true);
            String f_name = getArguments().getString(Config.TAG_CATEGORY);
            toolbar.setSubtitle(f_name);
        }

        // прячем поиск и кнопку где не используется
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                toolbar.setSubtitle(tabTiles.get(pos));
                if (pos == 0) {
                    if (is_opros) opros.setVisibility(View.VISIBLE);
                } else opros.setVisibility(View.GONE);
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

        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            if (dvc_tab_icons) {
                tab.setIcon(tabIcons.get(position));
            } else {
                tab.setText(tabTiles.get(position));
            }
        });

        tabLayoutMediator.attach();

        // Регистрация OnBackPressedCallback
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewPager.getCurrentItem() > 0) {
                    // Переключение на предыдущую вкладку
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, false);
                } else {
                    // Проверка, находимся ли мы на главном экране
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() != R.id.nav_home) {
                        navController.navigate(R.id.nav_home);
                    } else {
                        // Логика двойного нажатия для выхода
                        if (doubleBackToExitPressedOnce) {
                            requireActivity().finishAffinity();
                            return;
                        }
                        doubleBackToExitPressedOnce = true;
                        Toast.makeText(requireContext(), getString(R.string.press_twice), Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                                doubleBackToExitPressedOnce = false, DOUBLE_BACK_PRESS_DELAY);
                    }
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
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
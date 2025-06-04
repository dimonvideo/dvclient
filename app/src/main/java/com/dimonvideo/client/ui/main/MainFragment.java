/*
 * Copyright (c) 2025. Разработчик: Дмитрий Вороной.
 * Разработано для сайта dimonvideo.ru
 * При использовании кода ссылка на проект обязательна.
 */

package com.dimonvideo.client.ui.main;

import android.content.Intent;
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

public class MainFragment extends Fragment {

    private String razdel;
    private String story = null;
    public static ViewPager2 viewPager;
    private final ArrayList<String> tabTiles = new ArrayList<>();
    private final ArrayList<Integer> tabIcons = new ArrayList<>();
    private FragmentTabsBinding binding;
    private TextView opros;
    private boolean doubleBackToExitPressedOnce = false;
    private static final long DOUBLE_BACK_PRESS_DELAY = 2000; // Задержка в мс
    private NavController navController;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public MainFragment() {
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
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

        // Инициализация NavController
        try {
            navController = Navigation.findNavController(view);
        } catch (IllegalStateException e) {
            Log.e("MainFragment", "Failed to find NavController", e);
        }

        if (this.getArguments() != null) {
            razdel = getArguments().getString(Config.TAG_CATEGORY);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            String f_name = getArguments().getString(Config.TAG_RAZDEL);
        }

        AppController controller = AppController.getInstance();

        final boolean is_opros = controller.isOpros();

        opros = binding.oprosText;
        if (!Objects.equals(razdel, "13")) opros.setVisibility(View.VISIBLE);
        if (!is_opros) opros.setVisibility(View.GONE);

        boolean is_more = controller.isMore();
        boolean is_favor = controller.isTabFavor();
        final boolean dvc_tab_icons = controller.isTabIcons();
        final boolean is_comment = controller.isCommentTab();
        String login = controller.userName("");
        final boolean dvc_tab_inline = controller.isTabsInline();
        final boolean is_more_odob = controller.isMoreOdob();

        if (razdel != null && razdel.equals("18")) {
            is_more = false;
            is_favor = false;
        }

        TabLayout tabs = binding.tabLayout;
        if (dvc_tab_inline) tabs.setTabMode(TabLayout.MODE_FIXED);
        viewPager = binding.viewPager;
        AdapterTabs adapt = new AdapterTabs(getChildFragmentManager(), getLifecycle());

        // Вкладки
        tabTiles.add(getString(R.string.tab_last));
        tabIcons.add(R.drawable.baseline_home_24);
        if (is_more) {
            if (!is_more_odob) tabTiles.add(getString(R.string.tab_details));
            else tabTiles.add(getString(R.string.tab_waiting));
            tabIcons.add(R.drawable.outline_info_24);
        }
        if (razdel == null || !razdel.equals("18")) {
            tabTiles.add(getString(R.string.tab_categories));
            tabIcons.add(R.drawable.outline_category_24);
        }
        if (login.length() > 2 && is_favor) {
            tabTiles.add(getString(R.string.tab_favorites));
            tabIcons.add(R.drawable.outline_star_border_24);
        }
        if (is_comment) {
            tabTiles.add(getString(R.string.Comments));
            tabIcons.add(R.drawable.baseline_chat_24);
        }
        adapt.clearList();

        Bundle bundle = new Bundle();
        bundle.putString("tab", getString(R.string.tab_last));
        MainFragmentContent fragment_main = new MainFragmentContent();
        fragment_main.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab", getString(R.string.tab_details));
        MainFragmentContent fragment_info = new MainFragmentContent();
        fragment_info.setArguments(bundle);

        bundle = new Bundle();
        bundle.putString("tab", getString(R.string.tab_favorites));
        MainFragmentContent fragment_fav = new MainFragmentContent();
        fragment_fav.setArguments(bundle);

        adapt.addFragment(fragment_main);
        if (is_more) adapt.addFragment(fragment_info);
        if (razdel == null || !razdel.equals("18")) adapt.addFragment(new MainFragmentCategories());
        if (login.length() > 2 && is_favor) adapt.addFragment(fragment_fav);
        if (is_comment) adapt.addFragment(new MainFragmentCommentsTab());

        viewPager.setAdapter(adapt);
        viewPager.setCurrentItem(0, false);
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
            viewPager.post(() -> viewPager.setCurrentItem(0));
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
                if (searchView != null) {
                    searchView.setVisibility(pos == 0 ? View.VISIBLE : View.INVISIBLE);
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

        // Регистрация OnBackPressedCallback
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewPager.getCurrentItem() > 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, false);
                } else {
                    if (navController != null && navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() != R.id.nav_home) {
                        navController.navigate(R.id.nav_home);
                    } else {
                        if (doubleBackToExitPressedOnce) {
                            requireActivity().finishAffinity();
                            return;
                        }
                        doubleBackToExitPressedOnce = true;
                        Toast.makeText(requireContext(), getString(R.string.press_twice), Toast.LENGTH_SHORT).show();
                        handler.postDelayed(() ->
                                doubleBackToExitPressedOnce = false, DOUBLE_BACK_PRESS_DELAY);
                    }
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        EventBus.getDefault().postSticky(new MessageEvent(razdel, null, null, null, null, null));

        NetworkUtils.getOprosTitle(opros, requireContext());

        // Открываем раздел из dvadmin
        Intent intent_admin = requireActivity().getIntent();
        if (intent_admin != null) {
            String action_admin = intent_admin.getStringExtra("action_admin");
            Log.d("MainFragment", "DVAdmin intent: " + action_admin);
            if (action_admin != null && is_more) {
                viewPager.post(() -> viewPager.setCurrentItem(1));
            }
        }
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
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
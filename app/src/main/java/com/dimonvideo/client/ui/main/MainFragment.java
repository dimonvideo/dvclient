package com.dimonvideo.client.ui.main;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.TabsAdapter;
import com.dimonvideo.client.ui.forum.ForumFragmentTopicsNoPosts;
import com.dimonvideo.client.util.FragmentToActivity;
import com.dimonvideo.client.util.MessageEvent;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;

public class MainFragment extends Fragment  {

    static int razdel = 0;
    String url = Config.COMMENTS_URL;
    String search_url = Config.COMMENTS_SEARCH_URL;
    String story = null;
    String s_url = "";
    String key = "comments";

    private FragmentToActivity mCallback;

    public MainFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tabs, container, false);

        TabLayout tabLayout = root.findViewById(R.id.tabLayout);
        ViewPager viewPager = root.findViewById(R.id.view_pager);

        TabsAdapter adapt = new TabsAdapter(getParentFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, getContext());
        adapt.addfrg(new MainFragmentContent(),getString(R.string.tab_last));
        adapt.addfrg(new MainFragmentHorizontal(),getString(R.string.tab_details));
        adapt.addfrg(new ForumFragmentTopicsNoPosts(),getString(R.string.tab_categories));
        adapt.addfrg(new ForumFragmentTopicsNoPosts(),getString(R.string.tab_favorites));

        viewPager.setAdapter(adapt);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setOffscreenPageLimit(2);

        if (this.getArguments() != null) {
            razdel = getArguments().getInt(Config.TAG_CATEGORY);
            story = (String) getArguments().getSerializable(Config.TAG_STORY);
            if (razdel == 1) {
                url = Config.GALLERY_URL;
                search_url = Config.GALLERY_SEARCH_URL;
                key = Config.GALLERY_RAZDEL;
            }
            if (razdel == 2) {
                url = Config.UPLOADER_URL;
                search_url = Config.UPLOADER_SEARCH_URL;
                key = Config.UPLOADER_RAZDEL;

            }
            if (razdel == 3) {
                url = Config.VUPLOADER_URL;
                search_url = Config.VUPLOADER_SEARCH_URL;
                key = Config.VUPLOADER_RAZDEL;

            }
            if (razdel == 4) {
                url = Config.NEWS_URL;
                search_url = Config.NEWS_SEARCH_URL;
                key = Config.NEWS_RAZDEL;

            }
            if (razdel == 5) {
                url = Config.MUZON_URL;
                search_url = Config.MUZON_SEARCH_URL;
                key = Config.MUZON_RAZDEL;

            }
            if (razdel == 6) {
                url = Config.BOOKS_URL;
                search_url = Config.BOOKS_SEARCH_URL;
                key = Config.BOOKS_RAZDEL;

            }
            if (razdel == 7) {
                url = Config.ARTICLES_URL;
                search_url = Config.ARTICLES_SEARCH_URL;
                key = Config.ARTICLES_RAZDEL;

            }

            if (!TextUtils.isEmpty(story)) {
                url = search_url;
            }

            EventBus.getDefault().post(new MessageEvent(razdel));
            Log.d("tag4", String.valueOf(razdel));

        }

        sendData(String.valueOf(razdel));

        tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        // set default title
        // add here
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if( keyCode == KeyEvent.KEYCODE_BACK )
            {
                if (viewPager.getCurrentItem() == 0) toolbar.setTitle(getString(R.string.tab_topics));
                if (viewPager.getCurrentItem() == 1) toolbar.setTitle(getString(R.string.tab_forums));
                if (viewPager.getCurrentItem() == 2) toolbar.setTitle(getString(R.string.tab_topics_no_posts));

                return false;
            } else {
                return false;
            }
        });
        return root;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mCallback = (FragmentToActivity) context;
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
    }

    private void sendData(String comm)
    {
        try{ mCallback.communicate(comm);
        } catch (Throwable ignored) {
        }
    }
}
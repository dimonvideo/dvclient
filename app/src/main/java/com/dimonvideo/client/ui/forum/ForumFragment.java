package com.dimonvideo.client.ui.forum;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.ForumTabsAdapter;
import com.dimonvideo.client.util.FragmentToActivity;
import com.google.android.material.tabs.TabLayout;

public class ForumFragment extends Fragment  {

    int razdel = 8; // forum fragment
    private FragmentToActivity mCallback;

    public ForumFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_forum, container, false);

        final TabLayout tabLayout = (TabLayout) root.findViewById(R.id.tabLayout);
        final ViewPager viewPager = (ViewPager) root.findViewById(R.id.view_pager);

        ForumTabsAdapter adapt = new ForumTabsAdapter(getChildFragmentManager());
        adapt.addfrg(new ForumFragmentTopics(),getString(R.string.tab_topics));
        adapt.addfrg(new ForumFragmentForums(),getString(R.string.tab_forums));
        adapt.addfrg(new ForumFragmentTopicsNoPosts(),getString(R.string.tab_topics_no_posts));

        viewPager.setAdapter(adapt);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setOffscreenPageLimit(2);
        sendData(String.valueOf(razdel));

        tabLayout.post(() -> tabLayout.setupWithViewPager(viewPager));
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        //Back pressed Logic for fragment
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        requireActivity().finish();
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        startActivity(intent);

                        return true;
                    }
                }
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
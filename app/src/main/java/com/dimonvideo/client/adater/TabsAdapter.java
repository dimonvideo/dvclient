package com.dimonvideo.client.adater;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.ui.main.MainFragmentCats;
import com.dimonvideo.client.ui.main.MainFragmentContent;
import com.dimonvideo.client.ui.main.MainFragmentFav;
import com.dimonvideo.client.ui.main.MainFragmentHorizontal;

import java.util.ArrayList;
import java.util.List;

public class TabsAdapter extends FragmentStateAdapter {
    private final List<Fragment> fragmentList = new ArrayList<>();

    public TabsAdapter(@NonNull FragmentManager fm, @NonNull Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }

    public void clearList(){
        for (Fragment fr: fragmentList
        ) {
            fragmentList.remove(fr);
            notifyItemRemoved(fragmentList.indexOf(fr));
        }
    }

    public void addFragment(Fragment fragment) {
        fragmentList.add(fragment);
    }

}

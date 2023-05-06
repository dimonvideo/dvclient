package com.dimonvideo.client.adater;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class AdapterTabs extends FragmentStateAdapter {
    List<Fragment> fragmentList = new ArrayList<>();

    public AdapterTabs(@NonNull FragmentManager fm, @NonNull Lifecycle lifecycle) {
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

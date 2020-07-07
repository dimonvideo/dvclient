package com.dimonvideo.client.adater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ForumTabsAdapter extends FragmentStatePagerAdapter {
    private final List<Fragment> frgList = new ArrayList<>();
    private final List<String> titleList = new ArrayList<>();

    public ForumTabsAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }
    public void addfrg(Fragment frg,String title){
        frgList.add(frg);
        titleList.add(title);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titleList.get(position);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return frgList.get(position);
    }

    @Override
    public int getCount() {
        return titleList.size();
    }
}

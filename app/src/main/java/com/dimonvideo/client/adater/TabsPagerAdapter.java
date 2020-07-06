package com.dimonvideo.client.adater;

import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.dimonvideo.client.ui.forum.ForumFragment;
import com.dimonvideo.client.ui.main.MainFragment;
import com.dimonvideo.client.R;

public class TabsPagerAdapter extends FragmentPagerAdapter {
    @StringRes
    private static final int[] TAB_TITLES =
            new int[] { R.string.tab_text_1, R.string.tab_text_2, R.string.tab_text_3 };
    private final Context mContext;
    public TabsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return MainFragment.newInstance();
            case 1:
                return ForumFragment.newInstance();
            case 2:
              //  return ContactsFragment.newInstance();
            default:
                return null;
        }
    }
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }
    @Override
    public int getCount() {
        // Show 3 total pages.
        return 2;
    }
}
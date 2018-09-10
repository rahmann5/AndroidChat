package com.example.naziur.androidchat.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.naziur.androidchat.fragment.GroupSessionFragment;
import com.example.naziur.androidchat.fragment.SingleSessionFragment;

/**
 * Created by Hamidur on 28/08/2018.
 */

public class SessionFragmentPagerAdapter extends FragmentPagerAdapter {

    private String tabTitles [];

    public SessionFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
        tabTitles = new String[]{"Single Chats", "Group chats"};
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0 :
                return new SingleSessionFragment();
            case 1 :
                return new GroupSessionFragment();
            default:
                return new SingleSessionFragment();
        }
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }
}

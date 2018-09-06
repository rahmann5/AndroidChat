package com.example.naziur.androidchat.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.naziur.androidchat.fragment.SessionFragment;

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
        return new SessionFragment();
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

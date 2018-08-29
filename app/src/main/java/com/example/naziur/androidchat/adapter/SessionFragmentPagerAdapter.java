package com.example.naziur.androidchat.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.naziur.androidchat.fragment.ContactsFragment;

/**
 * Created by Hamidur on 28/08/2018.
 */

public class SessionFragmentPagerAdapter extends FragmentPagerAdapter {

    public SessionFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return new ContactsFragment();
    }

    @Override
    public int getCount() {
        return 1;
    }
}

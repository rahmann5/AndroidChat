package com.example.naziur.androidchat.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.example.naziur.androidchat.fragment.GroupSessionFragment;
import com.example.naziur.androidchat.fragment.SingleSessionFragment;

/**
 * Created by Hamidur on 28/08/2018.
 */

public class SessionFragmentPagerAdapter extends FragmentPagerAdapter {

    private String tabTitles [];
    SparseArray<Fragment> registeredFragments = new SparseArray<>();

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
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
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

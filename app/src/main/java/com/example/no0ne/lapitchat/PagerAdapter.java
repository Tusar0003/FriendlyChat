package com.example.no0ne.lapitchat;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.no0ne.lapitchat.Fragments.ChatsFragment;
import com.example.no0ne.lapitchat.Fragments.FriendsFragment;
import com.example.no0ne.lapitchat.Fragments.RequestsFragment;

/**
 * Created by no0ne on 9/18/17.
 */

public class PagerAdapter extends FragmentPagerAdapter {

    public PagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new RequestsFragment();
            case 1:
                return new ChatsFragment();
            case 2:
                return new FriendsFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    public String getPageTitle(int position) {
        switch (position) {
            case 0:
                return "REQUESTS";
            case 1:
                return "MESSAGES";
            case 2:
                return "FRIENDS";
            default:
                return null;
        }
    }
}

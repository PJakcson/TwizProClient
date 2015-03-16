package com.aceft.ui_fragments.channel_fragments.channel_pager;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aceft.R;

import java.util.ArrayList;
import java.util.List;


public class ChannelPagerFragment extends Fragment{

    public ChannelPagerFragment newInstance(String name, String displayName, int page) {
        ChannelPagerFragment fragment = new ChannelPagerFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", name);
        args.putString("channel_display_name", displayName);
        args.putInt("current_page", page);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_channel_pager, container, false);
        String mChannelName = getArguments().getString("channel_name");
        String mChannelDisplayName = getArguments().getString("channel_display_name");
        int currentPage = getArguments().getInt("current_page");

        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(mChannelDisplayName);

        ViewPager mPager = (ViewPager) rootView.findViewById(R.id.pager);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new ChannelVideoOverviewFragment().newInstance(mChannelName));
        fragments.add(new ChannelVodCategoryFragment().newInstance(mChannelName, 0));
        fragments.add(new ChannelVodCategoryFragment().newInstance(mChannelName, 1));

        FragmentPagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getChildFragmentManager(), fragments);
        mPagerAdapter.notifyDataSetChanged();
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(currentPage);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    private class ScreenSlidePagerAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragments;

        public ScreenSlidePagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return "Overview";
                case 1: return "Highlights";
                case 2: return "Past Broadcasts";
            }
            return "" + position;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

}
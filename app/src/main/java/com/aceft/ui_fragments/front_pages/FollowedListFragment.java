package com.aceft.ui_fragments.front_pages;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.ChannelListAdapter;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Stream;


public class FollowedListFragment extends Fragment {
    private int mLoadedItems, INT_LIST_UPDATE_VALUE, INT_LIST_UPDATE_THRESHOLD;
    private ChannelListAdapter mChannelListAdapter;
    private onChannelSelectedListener mCallback;
    private ProgressBar mProgressBar;
    private String mUrl;
    private String mTitle;
    private boolean adIsOnTop = false;

    private ArrayList<Channel> mChannels;
    private GridView mGridView;

    public FollowedListFragment newInstance(String url) {
        FollowedListFragment fragment = new FollowedListFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        args.putString("bar_title", "Favorites");
        fragment.setArguments(args);
        return fragment;
    }

    public interface onChannelSelectedListener {
        public void onChannelSelected(Channel c);
    }

    public interface OnLiveStreamSelectedListener {
        public void onLiveStreamSelected(Channel c);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_channel_list, container, false);
        mGridView = (GridView) rootView.findViewById(R.id.channelTopList);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.channels_list_progress);

        if (savedInstanceState != null) {
            mUrl = savedInstanceState.getString("url");
            mTitle = savedInstanceState.getString("bar_title");
        } else {
            mUrl = getArguments().getString("url");
            mTitle = getArguments().getString("bar_title");
        }

        mTitle = getActivity().getString(R.string.title_section4);

        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(mTitle);

        mLoadedItems = getResources().getInteger(R.integer.channel_list_start_items);

        mChannelListAdapter = new ChannelListAdapter(getActivity());
        mGridView.setAdapter(mChannelListAdapter);

        mLoadedItems = getResources().getInteger(R.integer.channel_list_start_items);
        INT_LIST_UPDATE_VALUE = getResources().getInteger(R.integer.channel_list_update_items);
        INT_LIST_UPDATE_THRESHOLD = getResources().getInteger(R.integer.channel_list_update_threshold);


        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                mCallback.onChannelSelected(mChannelListAdapter.getItem(position));
            }
        });

        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisibleItem = firstVisibleItem + visibleItemCount;
                if (totalItemCount > 0 && totalItemCount >= visibleItemCount) {
                    if (lastVisibleItem >= totalItemCount-1 && !adIsOnTop) {
                        ((MainActivity)getActivity()).pushDownAd();
                        adIsOnTop = true;
                    }
                    if (lastVisibleItem < totalItemCount-1 && adIsOnTop) {
                        ((MainActivity)getActivity()).pushUpAd();
                        adIsOnTop = false;
                    }
                }
                if (lastVisibleItem >= mLoadedItems - INT_LIST_UPDATE_THRESHOLD) {
                    downloadChannelData(INT_LIST_UPDATE_VALUE, mLoadedItems);
                    mLoadedItems += INT_LIST_UPDATE_VALUE;
                }
            }
        });

        return rootView;
    }

    public void downloadChannelData(int limit, int offset) {
        String request = mUrl;
        request += "limit=" + limit + "&offset=" + offset;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void dataReceived(String s) {
        dataParsed(TwitchJSONParser.followedChannelsToArrayList(s));
    }

    private void dataParsed(ArrayList<Channel> l) {
        if (l == null) return;
        if (mChannels == null) {
            mChannels = l;
        }
        else
            mChannels.addAll(l);

        String request = getActivity().getResources().getString(R.string.channel_stream_url) + "?channel=";
        for (int i = 0; i < mChannels.size(); i++) {
            request += mChannels.get(i).getName() + ",";
        }
        request += "&limit=" + mChannels.size();
        new checkIfOnlineTask().execute(request);
    }

    private void allDataComplete() {
        Collections.sort(mChannels, new CustomComparator());
        mProgressBar.setVisibility(View.INVISIBLE);
        updateLayout();
    }

    public void updateLayout() {
        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mGridView, "alpha",  0f, 1f);
        fadeInStream.setDuration(500);
        fadeInStream.start();
        mChannelListAdapter.clearAllData();
        mChannelListAdapter.update(mChannels);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChannels != null) {
            if (mChannels.size() > getResources().getInteger(R.integer.game_grid_start_items)) {
                mLoadedItems = mChannels.size();
            }
            mChannelListAdapter.resetDimensions();
            mChannelListAdapter.update(mChannels);
            mProgressBar.setVisibility(View.INVISIBLE);
//            downloadChannelData(mLoadedItems, 0);
        } else {
            mLoadedItems = getResources().getInteger(R.integer.game_grid_start_items);
            mChannelListAdapter.resetDimensions();
            downloadChannelData(mLoadedItems, 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity)getActivity()).resetAdPosition();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("url", mUrl);
        outState.putString("bar_title", mTitle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (onChannelSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnChannelSelectedListener");
        }
    }

    private class checkIfOnlineTask extends AsyncTask<String, Void, ArrayList<Channel>> {
        protected ArrayList<Channel> doInBackground(String... urls) {
            String s = TwitchNetworkTasks.downloadStringData(urls[0]);
            ArrayList<Stream> streams = TwitchJSONParser.streamJSONtoArrayList(s);
            int index;
            for (Stream str: streams) {
                index = mChannels.indexOf(str.getChannel());
                if (index >= 0) {
                    mChannels.get(index).setbIsOnline(true);
                }
            }
            return mChannels;
        }

        protected void onPostExecute(ArrayList<Channel> c) {
            mChannels = c;
            allDataComplete();
        }
    }

    private class CustomComparator implements Comparator<Channel> {
        @Override
        public int compare(Channel lhs, Channel rhs) {
            if (lhs.isbIsOnline() && !rhs.isbIsOnline()) return -1;
            if (!lhs.isbIsOnline() && rhs.isbIsOnline()) return 1;
            return lhs.getName().compareTo(rhs.getName());
        }
    }
}
package com.aceft.ui_fragments.front_pages;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
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

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.StreamListAdapter;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.primitives.Stream;


/**
 * Created by marc on 27.01.2015. Gridview of available games
 */
public class StreamListFragment extends Fragment{
    private int mLoadedItems, INT_LIST_UPDATE_VALUE, INT_LIST_UPDATE_THRESHOLD;
    private StreamListAdapter mAdapter;
    private onStreamSelectedListener mCallback;
    private ProgressBar mProgressBar;
    private String mUrl, mTitle;

    private ArrayList<Stream> mStreams;
    private GridView mGridView;

    private boolean adIsOnTop = false;

    public Fragment newInstance(String url, String mTitle) {
        StreamListFragment fragment = new StreamListFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        if (mTitle == null)
            mTitle = "Popular Streams";
        args.putString("bar_title", mTitle);
        fragment.setArguments(args);
        return fragment;
    }

    public interface onStreamSelectedListener {
        public void onStreamSelected(Stream c);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_top_streams, container, false);
        mGridView = (GridView) rootView.findViewById(R.id.grid_top_streams);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.channels_list_progress);

        mLoadedItems = getResources().getInteger(R.integer.channel_list_start_items);
        INT_LIST_UPDATE_VALUE = getResources().getInteger(R.integer.channel_list_update_items);
        INT_LIST_UPDATE_THRESHOLD = getResources().getInteger(R.integer.channel_list_update_threshold);

        if (savedInstanceState != null) {
            mLoadedItems = mStreams.size();
            mUrl = savedInstanceState.getString("url");
            mTitle = savedInstanceState.getString("bar_title");
        } else {
            mUrl = getArguments().getString("url");
            mTitle = getArguments().getString("bar_title");
        }

        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(mTitle);

        mAdapter = new StreamListAdapter(this);
        mGridView.setAdapter(mAdapter);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                mCallback.onStreamSelected(mAdapter.getItem(position));
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
                    downloadStreamData(INT_LIST_UPDATE_VALUE, mLoadedItems);
                    mLoadedItems += INT_LIST_UPDATE_VALUE;
                }
            }
        });

        return rootView;
    }

    public void downloadStreamData(int limit, int offset) {
        String request = mUrl;
        request += "limit=" + limit + "&offset=" + offset;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this);
        t.downloadJSONInBackground(request, Thread.MAX_PRIORITY);
    }

    public void dataReceived(String s) {
//        TwitchJSONParserThread t = new TwitchJSONParserThread(this);
//        t.parseJSONInBackground(s, Thread.MAX_PRIORITY);
        dataParsed(TwitchJSONParser.streamJSONtoArrayList(s));
    }

    public void dataParsed(ArrayList<Stream> l) {
        if (mStreams == null) {
            mStreams = l;
            mProgressBar.setVisibility(View.INVISIBLE);
            ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mGridView, "alpha",  0f, 1f);
            fadeInStream.setDuration(500);
            fadeInStream.start();
        }
        else
            mStreams.addAll(l);
        mAdapter.update(l);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("url", mUrl);
        outState.putString("bar_title", mTitle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (onStreamSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStreams != null) {
            mAdapter.clearData();
            mLoadedItems = mStreams.size();
            mAdapter.update(mStreams);
            mProgressBar.setVisibility(View.INVISIBLE);
        } else {
            downloadStreamData(mLoadedItems, 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity)getActivity()).resetAdPosition();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }
}
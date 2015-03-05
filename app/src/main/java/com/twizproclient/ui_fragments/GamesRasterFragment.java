package com.twizproclient.ui_fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import java.util.ArrayList;

import com.twizproclient.MainActivity;
import com.twizproclient.R;
import com.twizproclient.adapter.GamesAdapter;
import com.twizproclient.data.TwitchJSONParser;
import com.twizproclient.data.async_tasks.TwitchJSONDataThread;
import com.twizproclient.data.primitives.Game;

public class GamesRasterFragment extends Fragment
{
    private Game mSelectedItem;
    OnGameSelectedListener  mCallback;
    private String mBaseUrl;
    private int mLoadedItems, INT_GRID_UPDATE_VALUE, INT_GRID_UPDATE_THRESHOLD;

    private long start;

    private ArrayList<Game> mGames;
    private ProgressBar mProgressBar;
    private GamesAdapter mGamesAdapter;
    private GridView mGridView;
    private AbsListView.OnScrollListener mOnScrollListener;


    public GamesRasterFragment newInstance(String url) {
        GamesRasterFragment fragment = new GamesRasterFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnGameSelectedListener {
        public void onGameSelected(Game g);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_games, container, false);
        mGridView = (GridView) rootView.findViewById(R.id.gridView);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.games_grid_progress);
        mBaseUrl = getArguments().getString("url");
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(getActivity().getString(R.string.title_section1));

        mLoadedItems = getResources().getInteger(R.integer.game_grid_start_items);
        INT_GRID_UPDATE_VALUE = getResources().getInteger(R.integer.game_grid_update_items);
        INT_GRID_UPDATE_THRESHOLD = getResources().getInteger(R.integer.game_grid_update_threshold);

        mGamesAdapter = new GamesAdapter(this);
        mGridView.setAdapter(mGamesAdapter);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
                mSelectedItem = mGamesAdapter.getItem(position);
                actionBar.setTitle(mSelectedItem.mTitle);
                mCallback.onGameSelected(mSelectedItem);
            }
        });

        mOnScrollListener = new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisibleItem = firstVisibleItem + visibleItemCount;
                if (lastVisibleItem >= mLoadedItems - INT_GRID_UPDATE_THRESHOLD) {
                    loadGameData(INT_GRID_UPDATE_VALUE, mLoadedItems);
                    mLoadedItems += INT_GRID_UPDATE_VALUE;
                }
            }
        };

        return rootView;
    }

    public void loadGameData(int limit, int offset) {
        String request = mBaseUrl;
        request += "limit=" + limit + "&offset=" + offset;

        start = System.currentTimeMillis();
        TwitchJSONDataThread t = new TwitchJSONDataThread(this);
        t.downloadJSONInBackground(request, Thread.MAX_PRIORITY);
    }

    public void dataReceived(String s) {
        Log.d("Downloaded content time", "" + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        dataParsed(TwitchJSONParser.topGamesJSONtoArrayList(s));
    }

    public void dataParsed(ArrayList<Game> l) {
        Log.d("Parsing Time", "" + (System.currentTimeMillis() - start) + " ms");
        if (mGames == null) {
            mGames = l;
            mProgressBar.setVisibility(View.INVISIBLE);
        } else {
            mGames.addAll(l);
        }
        mGamesAdapter.update(l);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mLoadedItems", mLoadedItems);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGames != null) {
            mLoadedItems = mGames.size();
            mGamesAdapter.cleanData();
            mGamesAdapter.update(mGames);
            mGridView.setOnScrollListener(mOnScrollListener);
            mProgressBar.setVisibility(View.INVISIBLE);
        } else {
            mLoadedItems = getResources().getInteger(R.integer.game_grid_start_items);
            loadGameData(mLoadedItems, 0);
            mGridView.setOnScrollListener(mOnScrollListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnGameSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }
}
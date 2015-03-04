package com.twizproclient.data.async_tasks;

import java.util.ArrayList;

import com.twizproclient.data.TwitchJSONParser;
import com.twizproclient.data.primitives.Channel;
import com.twizproclient.data.primitives.Game;
import com.twizproclient.data.primitives.Stream;
import com.twizproclient.ui_fragments.ChannelDetailFragment;
import com.twizproclient.ui_fragments.ChannelListFragment;
import com.twizproclient.ui_fragments.GamesRasterFragment;
import com.twizproclient.ui_fragments.StreamListFragment;

public class TwitchJSONParserThread {
    private StreamListFragment mStreamListFragment;
    private GamesRasterFragment mGamesRasterFragment;
    private ChannelListFragment mChannelListFragment;
    private ChannelDetailFragment mChannelDetailFragment;
    private Thread mThread;
    private boolean mAbort = false;
    private int mDetailRequestType;

    public TwitchJSONParserThread(ChannelDetailFragment c, int request_type) {
        mChannelDetailFragment = c;
        mDetailRequestType = request_type;
    }

    public TwitchJSONParserThread(ChannelListFragment c) {
        mChannelListFragment = c;
    }

    public TwitchJSONParserThread(GamesRasterFragment g) {
        mGamesRasterFragment = g;
    }

    public TwitchJSONParserThread(StreamListFragment s) {
        mStreamListFragment = s;
    }

    public void parseJSONInBackground(String s, int priority) {
        final String fJSON = s;
        mThread = new Thread(new Runnable() {
            public void run() {
                if (mAbort) return;
                parseAndPush(fJSON);
            }
        });
        if (priority > 0) mThread.setPriority(priority);
        mThread.start();
    }

    private void parseAndPush(String j) {
        if (mChannelListFragment != null) parseAndPushChannel(j);
        if (mGamesRasterFragment != null) parseAndPushGame(j);
        if (mStreamListFragment != null) parseAndPushStream(j);
        if (mChannelDetailFragment != null && mDetailRequestType == 0) parseAndPushOneChannel(j);
    }

    private void parseAndPushOneChannel(String j) {
        final Channel channel = TwitchJSONParser.channelJSONtoChannel(j);
        if (mChannelDetailFragment.getActivity() == null) return;
        mChannelDetailFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mChannelDetailFragment.channelDataParsed(channel);
            }
        });
    }

    private void parseAndPushChannel(String j) {
        final ArrayList<Channel> channels = TwitchJSONParser.channelsJSONtoArrayList(j);
        if (mChannelListFragment.getActivity() == null) return;
        mChannelListFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mChannelListFragment != null)
                    mChannelListFragment.dataParsed(channels);
            }
        });
    }

    private void parseAndPushGame(String j) {
        final ArrayList<Game> games = TwitchJSONParser.topGamesJSONtoArrayList(j);
        if (mGamesRasterFragment.getActivity() == null) return;
        mGamesRasterFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGamesRasterFragment.dataParsed(games);
            }
        });
    }

    private void parseAndPushStream(String j) {
        final ArrayList<Stream> streams = TwitchJSONParser.streamJSONtoArrayList(j);
        if (mStreamListFragment.getActivity() == null) return;
        mStreamListFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStreamListFragment.dataParsed(streams);
            }
        });
    }

    public void stopThread() {
        mAbort = true;
    }
}

package com.twizproclient.data.async_tasks;

import org.json.JSONObject;

import java.util.HashMap;

import com.twizproclient.data.TwitchJSONParser;
import com.twizproclient.data.TwitchNetworkTasks;
import com.twizproclient.data.primitives.TwitchVideo;
import com.twizproclient.data.primitives.TwitchVod;
import com.twizproclient.ui_fragments.ChannelDetailFragment;
import com.twizproclient.ui_fragments.ChannelListFragment;
import com.twizproclient.ui_fragments.GamesRasterFragment;
import com.twizproclient.ui_fragments.StreamListFragment;

public class TwitchOldBroadcastThread {
    private StreamListFragment mStreamListFragment;
    private GamesRasterFragment mGamesRasterFragment;
    private ChannelListFragment mChannelListFragment;
    private ChannelDetailFragment mChannelDetailFragment;
    private TwitchVideo mVideo;
    private Thread mThread;
    private HashMap<String, String> mStreamUrls;
    private boolean mIsAuthenticated;
    private String mUserToken;
    private TwitchVod mVod;

    public TwitchOldBroadcastThread(ChannelDetailFragment c) {
        mChannelDetailFragment = c;
    }

    public void downloadJSONInBackground(final String url, int priority) {
        mThread = new Thread(new Runnable() {
            public void run() {
                mVod = oldVods(url);
                if (mChannelDetailFragment.getActivity() == null) return;
                    mChannelDetailFragment.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pushResult(mVod);
                    }
                });
            }
        });
        if (priority > 0) mThread.setPriority(priority);
        mThread.start();
    }

    private TwitchVod oldVods(String s) {
        JSONObject jData = TwitchNetworkTasks.downloadJSONData(s);
        TwitchVod vod = TwitchJSONParser.oldVideoDataToPlaylist(jData);
        return vod;
    }

    private void pushResult(TwitchVod result) {
         if (mChannelDetailFragment != null) mChannelDetailFragment.oldVideoPlaylistReceived(result);
    }

}

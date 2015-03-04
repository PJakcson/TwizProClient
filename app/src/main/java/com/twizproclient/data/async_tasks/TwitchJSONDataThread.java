package com.twizproclient.data.async_tasks;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import com.twizproclient.ui_fragments.ChannelDetailFragment;
import com.twizproclient.ui_fragments.ChannelListFragment;
import com.twizproclient.ui_fragments.GamesRasterFragment;
import com.twizproclient.ui_fragments.SearchFragment;
import com.twizproclient.ui_fragments.StreamListFragment;

public class TwitchJSONDataThread {
    private SearchFragment mSearchFragment;
    private StreamListFragment mStreamListFragment;
    private GamesRasterFragment mGamesRasterFragment;
    private ChannelListFragment mChannelListFragment;
    private ChannelDetailFragment mChannelDetailFragment;
    private Thread mThread;
    private int mDetailRequestType, mSearchRequestType;

    public TwitchJSONDataThread(ChannelDetailFragment c, int request_type) {
        mChannelDetailFragment = c;
        mDetailRequestType = request_type;
    }

    public TwitchJSONDataThread(ChannelListFragment c) {
        mChannelListFragment = c;
    }

    public TwitchJSONDataThread(GamesRasterFragment g) {
        mGamesRasterFragment = g;
    }

    public TwitchJSONDataThread(StreamListFragment s) {
        mStreamListFragment = s;
    }

    public TwitchJSONDataThread(SearchFragment s, int request_type) {
        mSearchFragment = s;
        mSearchRequestType = request_type;
    }

    public void downloadJSONInBackground(String s, int priority) {
        final String fUrl = s;
        mThread = new Thread(new Runnable() {
            public void run() {
                final String is = downloadJSONData(fUrl);
                if (getThreadActivity() == null) return;
                getThreadActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pushResult(is);
                    }
                });
            }
        });
        if (priority > 0) mThread.setPriority(priority);
        mThread.start();
    }

    private Activity getThreadActivity() {
        if (mChannelDetailFragment != null) return mChannelDetailFragment.getActivity();
        if (mChannelListFragment != null) return mChannelListFragment.getActivity();
        if (mGamesRasterFragment != null) return mGamesRasterFragment.getActivity();
        if (mStreamListFragment != null) return mStreamListFragment.getActivity();
        if (mSearchFragment != null) return mSearchFragment.getActivity();
        return null;
    }

    private void pushResult(String s) {
        if (mChannelListFragment != null) mChannelListFragment.dataReceived(s);
        if (mGamesRasterFragment != null) mGamesRasterFragment.dataReceived(s);
        if (mStreamListFragment != null) mStreamListFragment.dataReceived(s);

        if (mChannelDetailFragment != null && mDetailRequestType == 0) mChannelDetailFragment.channelDataReceived(s);
        if (mChannelDetailFragment != null && mDetailRequestType == 1) mChannelDetailFragment.streamDataReceived(s);
        if (mChannelDetailFragment != null && mDetailRequestType == 2) mChannelDetailFragment.highlightDataReceived(s);
        if (mChannelDetailFragment != null && mDetailRequestType == 3) mChannelDetailFragment.broadcastDataReceived(s);
        if (mChannelDetailFragment != null && mDetailRequestType == 4) mChannelDetailFragment.userDataReceived(s);

        if (mSearchFragment != null && mSearchRequestType == 0) mSearchFragment.channelDataReceived(s);
        if (mSearchFragment != null && mSearchRequestType == 1) mSearchFragment.streamDataReceived(s);
        if (mSearchFragment != null && mSearchRequestType == 2) mSearchFragment.gameDataReceived(s);
    }

    private String downloadJSONData(String myurl) {
        InputStream is = null;
        String result;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append("\n");
            }
            result = sb.toString();
            return result;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}

package com.aceft.ui_fragments.channel_fragments.channel_pager;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.VideoAdapter;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.async_tasks.TwitchBroadcastThread;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.async_tasks.TwitchOldBroadcastThread;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;

import java.util.HashMap;
import java.util.LinkedHashMap;


/**
 * Created by marc on 27.01.2015. Gridview of available games
 */
public class ChannelVodCategoryFragment extends Fragment {

    private SharedPreferences mPreferences;
    onOldVideoSelectedListener mCallback;

    private GridView mVideoGrid;
    private int mQualitySelected;
    private VideoAdapter mVideoGridAdapter;
    private ProgressBar mProgressBar;
    private TwitchVideo mPlayingVideo;
    private int mLoadedItems, INT_GRID_UPDATE_VALUE, INT_GRID_UPDATE_THRESHOLD;

    private boolean adIsOnTop = false;

    public interface onOldVideoSelectedListener {
        public void onOldVideoSelected(TwitchVod t1, TwitchVideo t2);
    }

    public ChannelVodCategoryFragment newInstance(String name, int type) {
        ChannelVodCategoryFragment fragment = new ChannelVodCategoryFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", name);
        args.putInt("video_type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video_category, container, false);
        mVideoGrid = (GridView) rootView.findViewById(R.id.videoGrid);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mVideoGridAdapter = new VideoAdapter(getActivity());

        final int videoType = getArguments().getInt("video_type");

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        mVideoGrid.setAdapter(mVideoGridAdapter);

        mLoadedItems = getResources().getInteger(R.integer.game_grid_start_items);
        INT_GRID_UPDATE_VALUE = getResources().getInteger(R.integer.game_grid_update_items);
        INT_GRID_UPDATE_THRESHOLD = getResources().getInteger(R.integer.game_grid_update_threshold);

        if (videoType == 0) {
            downloadHighlightData(mLoadedItems,0);
        }
        if (videoType == 1) {
            downloadBroadcastData(mLoadedItems, 0);
        }

        mVideoGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playSelectedVideo(mVideoGridAdapter.getItem(position));
            }
        });

        mVideoGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
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
                if (lastVisibleItem >= mLoadedItems - INT_GRID_UPDATE_THRESHOLD) {
                    if (videoType == 0) {
                        downloadHighlightData(INT_GRID_UPDATE_VALUE,mLoadedItems);
                    }
                    if (videoType == 1) {
                        downloadBroadcastData(INT_GRID_UPDATE_VALUE, mLoadedItems);
                    }
                    mLoadedItems += INT_GRID_UPDATE_VALUE;
                }
            }
        });

        return rootView;
    }

    //------------------ Highlight Stuff -------------------------///////////////////////////////////////////

    public void downloadHighlightData(int limit, int offset) {
        String request = getString(R.string.channel_videos_url);
        request += getArguments().getString("channel_name") + "/videos?";
        request += "limit=" + limit + "&offset=" + offset;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 0);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void highlightDataReceived(String s) {
        if (mVideoGridAdapter != null) {
            mVideoGridAdapter.updateVideos(TwitchJSONParser.dataToVideoList(s));
        }
        mVideoGrid.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    //------------------ PastBroadcast Stuff -------------------------/////////////////////////////////////////////

    public void downloadBroadcastData(int limit, int offset) {
        String request = getString(R.string.channel_videos_url);
        request += getArguments().getString("channel_name") + "/videos?";
        request += getString(R.string.channel_broadcasts_url_appendix);
        request += "limit=" + limit + "&offset=" + offset;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 1);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void broadcastDataReceived(String s) {
        if (mVideoGridAdapter.getCount() == 0) {
            ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mVideoGrid, "alpha",  0f, 1f);
            fadeInStream.setDuration(500);
            fadeInStream.start();
        }
        if (mVideoGridAdapter != null) {
            mVideoGridAdapter.updateVideos(TwitchJSONParser.dataToVideoList(s));
        }
        mVideoGrid.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    //-------------------VideoPlayback------------------------------//////////////////////////////////////

    public void playSelectedVideo(TwitchVideo v) {
        mPlayingVideo = v;
        if (v == null) {
            Toast.makeText(getActivity(), "Could not load Video", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isAuthenticated = mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false);

        String userToken = "";
//                userScope = "";
        if (isAuthenticated) {
            userToken = mPreferences.getString(Preferences.USER_AUTH_TOKEN, "");
//            userScope = mPreferences.getString(Preferences.SCOPES_OF_USER, "");
        }

        mProgressBar.setVisibility(View.VISIBLE);
        String prefix = v.mId.substring(0,1);
        String suffix = v.mId.substring(1, v.mId.length());
        String request;
        TwitchOldBroadcastThread to;
        switch (prefix) {
            case "v":
                TwitchBroadcastThread t = new TwitchBroadcastThread(this);
                if (isAuthenticated)
                    t = new TwitchBroadcastThread(this, userToken);
                request = getString(R.string.twitch_video_token_url) + suffix + "/access_token";
                t.downloadJSONInBackground(request, suffix, 0, Thread.NORM_PRIORITY);
                break;
            case "a":
                to = new TwitchOldBroadcastThread(this);
                request = "https://api.twitch.tv/api/videos/" + v.mId + "?as3=t";
                if (isAuthenticated)
                    request += "&oauth_token=" + userToken;
                to.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
                break;
            case "c":
                to = new TwitchOldBroadcastThread(this);
                request = "https://api.twitch.tv/api/videos/" + v.mId + "?as3=t";
                if (isAuthenticated)
                    request += "&oauth_token=" + userToken;
                to.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
                break;
        }
    }

    public void videoPlaylistReceived(LinkedHashMap<String, String> result) {
        mProgressBar.setVisibility(View.INVISIBLE);
        playVideo(result);
    }

    public void oldVideoPlaylistReceived(TwitchVod t) {
        mProgressBar.setVisibility(View.INVISIBLE);
        if (t.bestPossibleUrl() >= 0) {
            showOldVodFragment(t);
        } else {
            Toast.makeText(getActivity(), "Could not load Video, You may need to subscribe to the channel.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showOldVodFragment(TwitchVod t) {
        mCallback.onOldVideoSelected(t, mPlayingVideo);
    }

    public void playStream(String s) {
        Intent stream = new Intent(Intent.ACTION_VIEW);
        stream.setDataAndType(Uri.parse(s), "video/*");
        startActivity(stream);
    }

    // ------------------------VideoPlayer-----------------/////////////////////////////////
    private void playVideo(LinkedHashMap<String, String> q) {
        if (bestPossibleQuality2(q) >= 0) {
            switch (mPreferences.getString("settings_stream_quality_type", "")) {
                case "always ask": showPlayDialog(q, preferredQualityOrBest(q)); break;
                case "auto select best": playStream(q.get(bestPossibleQuality(q))); break;
                case "set maximum":
                    if(preferredQualityOrWorse(q) == null) {
                        showPlayDialog(q, preferredQualityOrBest(q));
                        Toast.makeText(getActivity(), "Sorry. No video below the maximum quality.", Toast.LENGTH_SHORT).show();
                    } else {
                        playStream(q.get(preferredQualityOrWorse(q)));
                    }
                    break;
            }
        } else {
            Toast.makeText(getActivity(), "Could not load Video, You may need to subscribe to the channel.", Toast.LENGTH_SHORT).show();
        }
    }

    public String bestPossibleQuality(HashMap<String, String> qualities) {
        if (qualities.containsKey("source")) return "source";
        if (qualities.containsKey("high")) return "high";
        if (qualities.containsKey("medium")) return "medium";
        if (qualities.containsKey("low")) return "low";
        if (qualities.containsKey("mobile")) return "mobile";
        if (qualities.containsKey("audio_only")) return "audio_only";
        return null;
    }

    public int bestPossibleQuality2(HashMap<String, String> q) {
        final String qa[] = q.keySet().toArray(new String[q.size()]);
        int bestQ = -1;
        int bestI = -1;

        for (int i = 0; i < qa.length; i++) {
            if (qualityValue(qa[i]) > bestQ) {
                bestQ = qualityValue(qa[i]);
                bestI = i;
            }
        }
        return bestI;
    }

    public int preferredQualityOrBest(HashMap<String, String> q) {
        final String qa[] = q.keySet().toArray(new String[q.size()]);
        String pref = mPreferences.getString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY,"");
        int iPref = qualityValue(pref);

        for (int i = 0; i < qa.length; i++) {
            if (qualityValue(qa[i]) == iPref) {
                return i;
            }
        }
        return bestPossibleQuality2(q);
    }

    public String preferredQualityOrWorse(HashMap<String, String> q) {
        final String qa[] = q.keySet().toArray(new String[q.size()]);
        String pref = mPreferences.getString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY,"");
        int iPref = qualityValue(pref);

        int bestQ = -1;
        int bestI = -1;

        for (int i = 0; i < qa.length; i++) {
            if (qualityValue(qa[i]) <= iPref && qualityValue(qa[i]) > bestQ) {
                bestQ = qualityValue(qa[i]);
                bestI = i;
            }
        }
        if (bestI < 0) return null;
        return qa[bestI];
    }


    private void showPlayDialog(final LinkedHashMap<String, String> q, int best) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String qualities[] = q.keySet().toArray(new String[q.size()]);
        String cleanQualities[] = getCleanQualities(qualities);

        builder.setTitle("Select Quality")
                .setSingleChoiceItems(cleanQualities, best, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mQualitySelected = which;
                    }
                })
                .setPositiveButton("Play", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        playStream(q.get(qualities[mQualitySelected]));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private int qualityValue(String s) {
        if (s.contains("audio_only")) return 0;
        if (s.contains("240")) return 1;
        if (s.contains("mobile")) return 1;
        if (s.contains("360")) return 2;
        if (s.contains("low")) return 2;
        if (s.contains("480")) return 3;
        if (s.contains("medium")) return 3;
        if (s.contains("720")) return 4;
        if (s.contains("high")) return 4;
        if (s.contains("live")) return 5;
        if (s.contains("source")) return 5;
        if (s.contains("chunked")) return 5;
        return -1;
    }

    private String[] getCleanQualities(String[] s) {
        String q[] = new String[s.length];

        for (int i = 0; i < s.length; i++) {
            if (s[i].contains("live")) {
                q[i] = "source";
                continue;
            }
            if (s[i].contains("chunked")){
                q[i] = "source";
                continue;
            }
            if (s[i].contains("audio_only")) {
                q[i] = "audio only";
                continue;
            }
            q[i] = s[i];
        }
        return q;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity)getActivity()).resetAdPosition();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (onOldVideoSelectedListener) activity;
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


    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}
package com.aceft.ui_fragments.channel_fragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.aceft.R;
import com.aceft.adapter.PastBroadcastsListAdapter;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.async_tasks.TwitchBroadcastThread;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.async_tasks.TwitchLiveStreamThread;
import com.aceft.data.async_tasks.TwitchOldBroadcastThread;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Stream;
import com.aceft.data.primitives.TwitchUser;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;
import com.aceft.ui_fragments.channel_fragments.channel_pager.ChannelPagerFragment;


public class ChannelDetailFragment extends Fragment {
    private final static int IS_HEADER = 0;
    private final static int IS_HIGHLIGHT_HEADER = 1;
    private final static int IS_HIGHLIGHT = 2;
    private final static int IS_BROADCAST_HEADER = 3;
    private final static int IS_BROADCAST = 4;
    private final static int IS_CHAT = 5;

    LinkedHashMap<String, String> mAvailableQualities;
    HashMap<String, String> mData;
    private int mLoadedItems;
    private ImageView mPlayOverlay;
    private ProgressBar mProgressBar;
    private View.OnTouchListener mTouchListener;
    private AdapterView.OnItemClickListener mVideoClicked;

    private int mQualitySelected;

    private RelativeLayout mStreamView;
    private Channel mChannel;
    private Stream mStream;
    private TwitchUser mUser;

    private String mUserToken;
    private boolean mIsAuthenticated;
    private int mOrientation = 0;

    private ImageView mThumbnail, mChannelBanner;
    private ListView mVideoList;
    private PastBroadcastsListAdapter mVideoListAdapter2;
    private View mStreamHeader;
    private View mChannelHeader;
    private TwitchVideo mPlayingVideo;
    private SharedPreferences mPreferences;
    private View mChatHeader;

    private boolean adIsOnTop = false;
    private View.OnClickListener mStreamClicked;

    public ChannelDetailFragment newInstance(HashMap<String,String> h) {
        ChannelDetailFragment fragment = new ChannelDetailFragment();
        Bundle args = new Bundle();
        args.putString("title", h.get("display_name"));
        args.putSerializable("data", h);
        fragment.setArguments(args);
        return fragment;
    }

    public ChannelDetailFragment newInstance(String c) {
        ChannelDetailFragment fragment = new ChannelDetailFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", c);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_channel_detail, container, false);

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mIsAuthenticated = sp.getBoolean(Preferences.USER_IS_AUTHENTICATED, false);

        if (mIsAuthenticated) {
            mUserToken = sp.getString(Preferences.USER_AUTH_TOKEN, "");
        }

        //mSpinner = (Spinner) rootView.findViewById(R.id.quality_spinner);
        mStreamView = (RelativeLayout) rootView.findViewById(R.id.stream_layout_top);
        mThumbnail = (ImageView) rootView.findViewById(R.id.videoFeed);

        mPlayOverlay = (ImageView) rootView.findViewById(R.id.imageOverlay);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.channel_detail_progress);

        mVideoList = (ListView) rootView.findViewById(R.id.videoList);
        mChannelHeader = getActivity().getLayoutInflater().inflate(R.layout.channel_video_header, null);
        mStreamHeader = getActivity().getLayoutInflater().inflate(R.layout.stream_video_header, null);
        mChatHeader = getActivity().getLayoutInflater().inflate(R.layout.stream_chat_header, null);

        mLoadedItems = 8;


        if (mVideoListAdapter2 == null) {
            mVideoListAdapter2 = new PastBroadcastsListAdapter(getActivity());
        }

        mVideoClicked = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int group = mVideoListAdapter2.getGroup(position);
                int childPos = mVideoListAdapter2.getChildPosition(position, group)-1;
                FragmentTransaction transaction;
                switch (group) {
                    case IS_HEADER:
                        transaction = getFragmentManager().beginTransaction();
                        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        transaction.replace(R.id.container,
                                new ChannelPagerFragment().newInstance(mChannel.getName(), mChannel.getDisplayName(), 0));
                        transaction.addToBackStack("pager");
                        transaction.commit();
                        break;
                    case IS_CHAT:
                        if (!mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false)) {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.user_not_authenticated), Toast.LENGTH_LONG).show();
                            break;
                        }
                        transaction = getFragmentManager().beginTransaction();
                        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        transaction.replace(R.id.container,
                                new ChatFragment().newInstance(mChannel.getName(), mChannel.getDisplayName()));
                        transaction.addToBackStack("chat");
                        transaction.commit();
                        break;
                    case IS_HIGHLIGHT_HEADER:
                        transaction = getFragmentManager().beginTransaction();
                        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        transaction.replace(R.id.container,
                                new ChannelPagerFragment().newInstance(mChannel.getName(), mChannel.getDisplayName(), 1));
                        transaction.addToBackStack("pager");
                        transaction.commit();
                        break;
                    case IS_HIGHLIGHT:
                        playSelectedVideo(mVideoListAdapter2.getHighlight(childPos));
                        break;
                    case IS_BROADCAST_HEADER:
                        transaction = getFragmentManager().beginTransaction();
                        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        transaction.replace(R.id.container,
                                new ChannelPagerFragment().newInstance(mChannel.getName(), mChannel.getDisplayName(), 2));
                        transaction.addToBackStack("pager");
                        transaction.commit();
                        break;
                    case IS_BROADCAST:
                        playSelectedVideo(mVideoListAdapter2.getBroadcast(childPos));
                        break;
                }
            }
        };


        mTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                playVideo(mAvailableQualities);
                return false;
            }
        };

        mStreamClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVideo(mAvailableQualities);
            }
        };

        mVideoList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisibleItem = firstVisibleItem + visibleItemCount;
                if (totalItemCount > 2 && totalItemCount >= visibleItemCount) {
                    if (lastVisibleItem >= totalItemCount-1 && !adIsOnTop) {
                        ((MainActivity)getActivity()).pushDownAd();
                        adIsOnTop = true;
                    }
                    if (lastVisibleItem < totalItemCount-1 && adIsOnTop) {
                        ((MainActivity)getActivity()).pushUpAd();
                        adIsOnTop = false;
                    }
                }
            }
        });

        return rootView;
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


    //------------------ Channel Stuff -------------------------///////////////////
    private void downloadChannelData() {
        String request = getActivity().getResources().getString(R.string.channel_url);
        request += getArguments().getString("channel_name");
        if (mIsAuthenticated) request += "?oauth_token=" + mUserToken;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 0);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void channelDataReceived(String s) {
        mChannel = TwitchJSONParser.channelStringToChannel(s);
        if (mChannel == null) {
            errorScreen();
        }
        else {
            updateChannelLayout();
            downloadHighlightData(mLoadedItems, 0);
        }
    }

    private void updateChannelLayout() {
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(mChannel.getDisplayName());
        mProgressBar.setVisibility(View.INVISIBLE);
        mStreamView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 0));

        if (mVideoList != null) {
            mVideoList.setOnItemClickListener(mVideoClicked);
            if (mVideoList.getHeaderViewsCount() == 0) {
                mVideoList.addHeaderView(mChatHeader);
                mVideoList.addHeaderView(mChannelHeader);
                mVideoList.setAdapter(mVideoListAdapter2);
            }

            mChannelBanner = (ImageView) mChannelHeader.findViewById(R.id.channelBanner);
            Picasso.with(getActivity())
                    .load(mChannel.getLogoLink())
                    .placeholder(R.drawable.ic_placeholder)
                    .config(Bitmap.Config.RGB_565)
                    .into(mChannelBanner);

            ((TextView) mChannelHeader.findViewById(R.id.textTitleView)).setText(mChannel.getDisplayName());
            ((TextView) mChannelHeader.findViewById(R.id.textBioView)).setText(mUser.getBio());
            ((TextView) mChannelHeader.findViewById(R.id.textViewsView)).setText(mChannel.getFollowers() + " Followers");
            mVideoList.setVisibility(View.VISIBLE);
        }
    }

    //------------------ Stream Stuff -------------------------///////////////////
    private void downloadStreamData(String name) {
//        String request = getActivity().getResources().getString(R.string.channel_stream_url) + "?channel=";
        String request = getActivity().getResources().getString(R.string.channel_stream_url);
        request += name;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 1);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void streamDataReceived(String s) {
        Stream st = TwitchJSONParser.streamStringToStream(s);
        if (st == null) {
            downloadChannelData();
//        } else if (st.isEmpty()) {
//            downloadChannelData();
        }
        else {
//            mStream = st.get(0);
            mStream = st;
            mChannel = mStream.getChannel();
            updateLiveStreamLayout();
            downloadHighlightData(mLoadedItems, 0);
        }
    }

    private void updateLiveStreamLayout() {
        mProgressBar.setVisibility(View.GONE);
        mStreamView.setVisibility(View.VISIBLE);
        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mStreamView, "alpha",  0f, 1f);
        fadeInStream.setDuration(500);
        fadeInStream.start();

        Picasso.with(getActivity())
                .load(mStream.getPreviewLink())
                .placeholder(R.drawable.ic_placeholder)
                .config(Bitmap.Config.RGB_565)
                .into(mThumbnail);

        TextView sTitle = (TextView) mStreamView.findViewById(R.id.channelTitel);
        sTitle.setText(mStream.getChannel().getDisplayName());
        TextView sGame = (TextView) mStreamView.findViewById(R.id.channelGame);
        sGame.setText("playing " + mStream.getChannel().getGame());
        TextView sViewers = (TextView) mStreamView.findViewById(R.id.channelViewers);
        sViewers.setText(String.valueOf(mStream.getViewers()));
        ImageView viewers_icon = (ImageView)mStreamView.findViewById(R.id.viewers_icon);
        viewers_icon.setVisibility(View.VISIBLE);

        if (mVideoList != null) {
            mVideoList.setOnItemClickListener(mVideoClicked);
            if (mVideoList.getHeaderViewsCount() == 0) {
                mVideoList.addHeaderView(mChatHeader);
                mVideoList.addHeaderView(mStreamHeader);
                mVideoList.setAdapter(mVideoListAdapter2);
            }

            mChannelBanner = (ImageView) mStreamHeader.findViewById(R.id.channelBanner);
            Picasso.with(getActivity())
                    .load(mChannel.getLogoLink())
                    .placeholder(R.drawable.ic_placeholder)
                    .config(Bitmap.Config.RGB_565)
                    .into(mChannelBanner);

            if (mPreferences.getBoolean(Preferences.PREF_USER_LEARNED_DETAILS, true)) {
                ((TextView) mStreamHeader.findViewById(R.id.textTitleView)).setText(mChannel.getStatus());
            } else {
                ((TextView) mStreamHeader.findViewById(R.id.textTitleView)).setText(mChannel.getStatus()
                            + getActivity().getResources().getString(R.string.get_details_message));
                mPreferences.edit().putBoolean(Preferences.PREF_USER_LEARNED_DETAILS, true).apply();
            }

            ((TextView) mStreamHeader.findViewById(R.id.textViewsView)).setText(mStream.getViewers() + " Viewers");
            mVideoList.setVisibility(View.VISIBLE);
            ObjectAnimator fadeInList = ObjectAnimator.ofFloat(mVideoList, "alpha",  0f, 1f);
            fadeInList.setDuration(500);
            fadeInList.start();
        }
        ((ActionBarActivity) getActivity()).getSupportActionBar().hide();
    }

    //------------------ Livestream Stuff -------------------------/////////////////////////////////////////////
    private void fetchStreamToken(String s) {
        String tokenUrl = getString(R.string.stream_token_url) + s + "/access_token";
        TwitchLiveStreamThread t;
        if (mIsAuthenticated) {
            t = new TwitchLiveStreamThread(this, mUserToken);
        } else {
            t = new TwitchLiveStreamThread(this);
        }
        t.downloadJSONInBackground(tokenUrl, getArguments().getString("channel_name"), 0, Thread.NORM_PRIORITY);
    }

    public void liveLinksReceived(LinkedHashMap<String, String> result) {
        mAvailableQualities = result;
        mPlayOverlay.setImageResource(R.drawable.ic_play_overlay);
        mThumbnail.setOnClickListener(mStreamClicked);
//        mThumbnail.setOnTouchListener(mTouchListener);
    }

    //------------------ User Stuff -------------------------///////////////////
    private void downloadUserData(String name) {
        String request = getActivity().getResources().getString(R.string.twitch_user_url);
        request += name;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 4);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void userDataReceived(String s) {
        mUser = TwitchJSONParser.userDataToUser(s);
        if (mUser != null) {
            downloadStreamData(getArguments().getString("channel_name"));
        }
    }

    //------------------ Video Stuff -------------------------///////////////////////////////////////////

    public void playSelectedVideo(TwitchVideo v) {
        mPlayingVideo = v;
        if (v == null) {
            Toast.makeText(getActivity(), "Could not load Video", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        String prefix = v.mId.substring(0,1);
        String suffix = v.mId.substring(1, v.mId.length());
        String request;
        TwitchBroadcastThread t = new TwitchBroadcastThread(this);
        TwitchOldBroadcastThread to = new TwitchOldBroadcastThread(this);
        if (mIsAuthenticated)
            t = new TwitchBroadcastThread(this, mUserToken);
        switch (prefix) {
            case "v":
                request = getString(R.string.twitch_video_token_url) + suffix + "/access_token";
                t.downloadJSONInBackground(request, suffix, 0, Thread.NORM_PRIORITY);
                break;
            case "a":
                request = "https://api.twitch.tv/api/videos/" + v.mId + "?as3=t";
                if (mIsAuthenticated)
                    request += "&oauth_token=" + mUserToken;
                to.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
                break;
            case "c":
                request = "https://api.twitch.tv/api/videos/" + v.mId + "?as3=t";
                if (mIsAuthenticated)
                    request += "&oauth_token=" + mUserToken;
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
        VideoFragment videoFragment = new VideoFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, videoFragment.newInstance(t, mPlayingVideo));
        transaction.addToBackStack("oldvideo");
        transaction.commit();
    }

    //------------------ Highlight Stuff -------------------------///////////////////////////////////////////

    public void downloadHighlightData(int limit, int offset) {
        String request = getString(R.string.channel_videos_url);
        request += getArguments().getString("channel_name") + "/videos?";
        request += "limit=" + limit + "&offset=" + offset;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 2);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void highlightDataReceived(String s) {
        downloadBroadcastData(mLoadedItems, 0);
        mChannel.mHighlights = TwitchJSONParser.dataToVideoList(s);
        if (mVideoListAdapter2 != null) {
            mVideoListAdapter2.updateHighlights(mChannel.mHighlights);
            mLoadedItems = mVideoListAdapter2.getHighlights().size();
        }
    }

    //------------------ PastBroadcast Stuff -------------------------/////////////////////////////////////////////

    public void downloadBroadcastData(int limit, int offset) {
        String request = getString(R.string.channel_videos_url);
        request += getArguments().getString("channel_name") + "/videos?";
        request += getString(R.string.channel_broadcasts_url_appendix);
        request += "limit=" + limit + "&offset=" + offset;
        TwitchJSONDataThread t = new TwitchJSONDataThread(this, 3);
        t.downloadJSONInBackground(request, Thread.NORM_PRIORITY);
    }

    public void broadcastDataReceived(String s) {
        mChannel.mBroadcasts = TwitchJSONParser.dataToVideoList(s);
        if (mVideoListAdapter2 != null) {
            mVideoListAdapter2.updateBroadcasts(mChannel.mBroadcasts);
            mLoadedItems = mVideoListAdapter2.getBroadcasts().size();
        }
    }

    //------------------------------- Stuff ---------------------------//////////////////////////////

    public void playStream(String s) {
        Intent stream = new Intent(Intent.ACTION_VIEW);
        stream.setDataAndType(Uri.parse(s), "video/*");
        startActivity(stream);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(getArguments().getString("channel_name"));

        if(isInLandscape()) {
            setParams(getActivity());
            if (mOrientation == 1) {
                mOrientation = 2;
                resetAdapter();
            }
        }
        if(!isInLandscape()) {
            float scale = 1.0f * mPlayOverlay.getDrawable().getIntrinsicHeight() / mPlayOverlay.getDrawable().getIntrinsicWidth();
            mStreamView.getLayoutParams().height = (int) (getWindowWidth()*scale);
            mThumbnail.getLayoutParams().height = (int) (getWindowWidth()*scale);
            if (mOrientation == 2) {
                mOrientation = 1;
                resetAdapter();
            }
        }
        if (mOrientation == 0) {
            mOrientation = !isInLandscape() ? 1 : 2;
        }
        if (mStream != null && mUser != null) {
            updateLiveStreamLayout();
            if (mAvailableQualities != null) {
                liveLinksReceived(mAvailableQualities);
            }
        } else if (mChannel != null && mUser != null) {
            updateChannelLayout();

        } else {
            if (mVideoListAdapter2 != null) mVideoListAdapter2.clearAllData();
            downloadUserData(getArguments().getString("channel_name"));
            fetchStreamToken(getArguments().getString("channel_name"));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity)getActivity()).resetAdPosition();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("data", mData);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStop() {
        super.onStop();
        ((ActionBarActivity) getActivity()).getSupportActionBar().show();
//        if(isInLandscape()) showUi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public String bestPossibleQuality(LinkedHashMap<String, String> qualities) {
        if (qualities.containsKey("source")) return "source";
        if (qualities.containsKey("high")) return "high";
        if (qualities.containsKey("medium")) return "medium";
        if (qualities.containsKey("low")) return "low";
        if (qualities.containsKey("mobile")) return "mobile";
        if (qualities.containsKey("audio_only")) return "audio_only";
        return null;
    }

    public int bestPossibleQuality2(LinkedHashMap<String, String> q) {
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

    public int preferredQualityOrBest(LinkedHashMap<String, String> q) {
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

    public String preferredQualityOrWorse(LinkedHashMap<String, String> q) {
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

    private boolean isInLandscape() {
        return getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
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

    private void errorScreen() {

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

    private int getWindowWidth() {
        int width;
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;

        return width;
    }

    private void setParams(Context context) {
        int screenLayout = context.getResources().getConfiguration().screenLayout;
        screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;

        switch (screenLayout) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                mStreamView.getLayoutParams().width = getWindowWidth(); break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                mStreamView.getLayoutParams().width = getWindowWidth(); break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                mStreamView.getLayoutParams().width = (int) (0.6f * getWindowWidth()); break;
            case 4: // Configuration.SCREENLAYOUT_SIZE_XLARGE is API >= 9
                mStreamView.getLayoutParams().width = (int) (0.6f * getWindowWidth()); break;
            default:
                mStreamView.getLayoutParams().width = getWindowWidth(); break;
        }
    }

    private void resetAdapter() {
        if (mVideoListAdapter2 != null) {
            ArrayList<TwitchVideo> mSavedHighlights = (ArrayList<TwitchVideo>) mVideoListAdapter2.getHighlights().clone();
            ArrayList<TwitchVideo> mSavedBroadcasts = (ArrayList<TwitchVideo>) mVideoListAdapter2.getBroadcasts().clone();
            mVideoListAdapter2 = new PastBroadcastsListAdapter(getActivity());
            mVideoListAdapter2.updateHighlights(mSavedHighlights);
            mVideoListAdapter2.updateBroadcasts(mSavedBroadcasts);
        }
    }
}
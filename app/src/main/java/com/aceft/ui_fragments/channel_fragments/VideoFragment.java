package com.aceft.ui_fragments.channel_fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.OldVideoListAdapter;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;

public class VideoFragment extends Fragment {

    private ArrayList<String> qualities;
    private LinkedHashMap <String,String> mData;
    private SharedPreferences mPreferences;

    private int mQualitySelected;
    private int mStartIndex;

    private boolean adIsOnTop = false;

    public VideoFragment newInstance(TwitchVod h, TwitchVideo twitchVideo) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putInt("start_index", h.getStartOffsetIndex());
        args.putStringArrayList("lengths", h.getLengths());
        args.putStringArrayList("qualities", h.getAvailableQualities());
        args.putSerializable("data", h.toHashmap());
        args.putString("title", twitchVideo.mTitle);
        args.putString("description", twitchVideo.mDesc);
        args.putString("views", twitchVideo.mViews);
        args.putString("previewLink", twitchVideo.mPreviewLink);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video, container, false);
        RelativeLayout header = (RelativeLayout) rootView.findViewById(R.id.channelData);
        ListView videos = (ListView) rootView.findViewById(R.id.videoGrid);

        ArrayList<String> lengths = getArguments().getStringArrayList("lengths");
        mStartIndex = getArguments().getInt("start_index");
        qualities = getArguments().getStringArrayList("qualities");

        String title = getArguments().getString("title");
        String desc = getArguments().getString("description");
        String views = getArguments().getString("views");
        String pLink = getArguments().getString("previewLink");

        try {
            if(getArguments().getSerializable("data") instanceof LinkedHashMap) {
                mData = (LinkedHashMap<String, String>) getArguments().getSerializable("data");
            } else {
                Toast.makeText(getActivity(), "Ups, something went wrong.", Toast.LENGTH_SHORT).show();
            }
        } catch (ClassCastException e) {
            Toast.makeText(getActivity(), "Ups, something went wrong.", Toast.LENGTH_SHORT).show();
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        OldVideoListAdapter adapter = new OldVideoListAdapter(this, lengths);

        ImageView thumb = (ImageView) rootView.findViewById(R.id.videoThumb);
        loadLogo(pLink, thumb);

        setHeaderHeight(thumb);

        ((TextView)header.findViewById(R.id.videoTitle)).setText(title);
        ((TextView)header.findViewById(R.id.viewsAndRecorded)).setText(desc);
        ((TextView)header.findViewById(R.id.videoViews)).setText(views);

        videos.setAdapter(adapter);

        videos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playVideo(getHash(position + mStartIndex));
            }
        });

        videos.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisibleItem = firstVisibleItem + visibleItemCount;
                if (totalItemCount > 1 && totalItemCount >= visibleItemCount) {
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
        if (mData == null) return;
        if (bestPossibleQualityIndex(q) >= 0 && !mData.isEmpty()) {
            switch (mPreferences.getString("settings_stream_quality_type", "")) {
                case "always ask": showPlayDialog(q, preferredQualityOrBest(q)); break;
                case "auto select best": playStream(q.get(bestPossibleQualityKey(q))); break;
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

    private void setHeaderHeight(ImageView header) {
        int width;
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;

        if (isInLandscape())
            header.getLayoutParams().width = width/4;
        else
            header.getLayoutParams().width = width/3;
    }

    private LinkedHashMap<String, String> getHash(int p) {
        if (mData == null) return null;
        LinkedHashMap<String, String> qurls = new LinkedHashMap<>();
        for (String q: qualities) {
            qurls.put(q, mData.get(q+p));
        }
        return qurls;
    }

    private void showPlayDialog(final LinkedHashMap<String, String> q, int best) {
        if (q == null) return;
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

    public void playStream(String s) {
        if (s == null) {
            Toast.makeText(getActivity(), "Could not load Video, You may need to subscribe to the channel.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent stream = new Intent(Intent.ACTION_VIEW);
        stream.setDataAndType(Uri.parse(s), "video/*");
        startActivity(stream);
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

    private void loadLogo(final String url, final ImageView imageView) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                final Bitmap bitmap = TwitchNetworkTasks.downloadBitmap(url);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (imageView != null)
                            imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public String bestPossibleQualityKey(LinkedHashMap<String, String> qualities) {
        final String qa[] = qualities.keySet().toArray(new String[qualities.size()]);
        return qa[bestPossibleQualityIndex(qualities)];
    }

    public int bestPossibleQualityIndex(LinkedHashMap<String, String> q) {
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
        return bestPossibleQualityIndex(q);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
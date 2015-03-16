package com.aceft.ui_fragments.channel_fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.IRCAdapter;
import com.aceft.data.MyIRCClient;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.async_tasks.TwitchJSONDataThread;
import com.aceft.data.primitives.Emoticon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;


/**
 * Created by marc on 27.01.2015. Gridview of available games
 */
public class ChatFragment extends Fragment {
    private int mNumberOfAttempts = 0;
    private View mRootView;
    MyIRCClient mClient;
    private EditText mChatBox;
    private ListView mMessageList;
    private IRCAdapter mIRCAdapter;
    private String mChatRoom;
    private String mUsername;
    private String mChannelName;
    private ArrayList<Emoticon> mEmotis;
    private String mToken, mServer;
    private int mPort;
    ArrayList<String> mServers;

    public ChatFragment newInstance(String c, String d) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("channel_name", c);
        args.putString("channel_display_name", d);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_chat, container, false);
        mChatBox = (EditText) mRootView.findViewById(R.id.chatBox);
        mMessageList = (ListView) mRootView.findViewById(R.id.messageList);

        mChannelName = getArguments().getString("channel_name");
        String channelDisplayName = getArguments().getString("channel_display_name");
        mChatRoom = "#"+ mChannelName;

        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle("#"+channelDisplayName);

        if (mIRCAdapter == null) {
            mIRCAdapter = new IRCAdapter(getActivity());
        }
        mMessageList.setAdapter(mIRCAdapter);

        mMessageList.setOnScrollListener(new AbsListView.OnScrollListener() {
            int state = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                        state = AbsListView.OnScrollListener.SCROLL_STATE_FLING;
                        mMessageList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                        state = AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
                        mMessageList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                        state = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisibleItem = firstVisibleItem + visibleItemCount;
                if (lastVisibleItem >= totalItemCount - 5 && state == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    mMessageList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                }
            }
        });

        return mRootView;
    }

    private void startFreshIRCClient() {
        Toast.makeText(getActivity(), getActivity().getString(R.string.connecting_to_chat), Toast.LENGTH_SHORT).show();
        if (mServers == null) {
            getChatProperties();
            return;
        }
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        mUsername = sp.getString(Preferences.TWITCH_DISPLAY_USERNAME, "");
        mToken = sp.getString(Preferences.USER_AUTH_TOKEN, "");
        String nick = sp.getString(Preferences.TWITCH_USERNAME, "");
        mClient = new MyIRCClient(this, nick, mServers);
        mClient.loadChat2(mChatRoom, mToken, 0);
    }

    public void newMessage(String sender, String message) {
        mIRCAdapter.update(sender, message);
    }

    public void onJoined() {
        Toast.makeText(getActivity(), getActivity().getString(R.string.joined_chatroom), Toast.LENGTH_SHORT).show();
        mRootView.findViewById(R.id.chatProgress).setVisibility(View.GONE);
        mChatBox.setVisibility(View.VISIBLE);
        mChatBox.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String message = String.valueOf(mChatBox.getText());
                    mClient.sendMessage(mChatRoom, message);
                    mIRCAdapter.update(mUsername, message);
                    mChatBox.setText("");
                    return true;
                }
                return false;
            }
        });
    }

    private void getEmoticons() {
//        Toast.makeText(getActivity(), getActivity().getString(R.string.fetching_emoticons), Toast.LENGTH_SHORT).show();
        String req = getActivity().getString(R.string.twich_chat_emoticons) + mChannelName + "/emoticons";
        TwitchJSONDataThread t = new TwitchJSONDataThread(this);
        t.downloadJSONInBackground(req, Thread.NORM_PRIORITY);
    }

    public void emotisDataReceived(String s) {
        mEmotis = TwitchJSONParser.chatEmotisJSONtoArrayList(s);
        String urls[] = new String[mEmotis.size()];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = mEmotis.get(i).getUrl();
        }
        new DownloadEmotisTask().execute(urls);
    }

    public void emotisBitmapsReceived(Bitmap[] e) {
        for (int i = 0; i < mEmotis.size(); i++) {
            mEmotis.get(i).setEmoti(e[i]);
        }
        if (getActivity() == null) return;
        Toast.makeText(getActivity(), "Emoticons activated", Toast.LENGTH_SHORT).show();
        mIRCAdapter.updateEmotis(mEmotis);
//        startFreshIRCClient();
    }

    private class DownloadEmotisTask extends AsyncTask<String, Void, Bitmap[]> {

        protected Bitmap[] doInBackground(String... urls) {
            Bitmap emotis[] = new Bitmap[urls.length];
            for (int i = 0; i < urls.length; i++) {
                emotis[i] = TwitchNetworkTasks.downloadBitmap(urls[i]);
            }
            return emotis;
        }

        protected void onPostExecute(Bitmap[] result) {
            emotisBitmapsReceived(result);
        }
    }

    private void getChatProperties() {
        final String req = getActivity().getString(R.string.twich_chat_properties) + mChannelName + "/chat_properties";
        mServers = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                JSONObject j = TwitchNetworkTasks.downloadJSONData(req);
                try {
                    JSONArray jsonArray = j.getJSONArray("chat_servers");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        mServers.add(jsonArray.getString(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (mServers.isEmpty()) {
                            Toast.makeText(getActivity(), "Could not find any servers online", Toast.LENGTH_LONG).show();
                            return;
                        }
                        startFreshIRCClient();
                    }
                });
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
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
    public void onResume() {
        super.onResume();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ((MainActivity)getActivity()).setAdPosition(RelativeLayout.ALIGN_PARENT_TOP);
        if (mEmotis == null) {
            getEmoticons();
            startFreshIRCClient();
        } else {
            if (mClient == null) {
                startFreshIRCClient();
            } else {
                mClient.reconnectToChannel(mChatRoom);
            }
        }
    }

    @Override
    public void onPause() {
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ((MainActivity)getActivity()).setAdPosition(RelativeLayout.ALIGN_PARENT_BOTTOM);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mClient.partChannel(mChatRoom);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
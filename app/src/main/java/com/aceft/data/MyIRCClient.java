package com.aceft.data;

import android.util.Log;
import android.widget.Toast;

import com.aceft.ui_fragments.channel_fragments.ChatFragment;

import org.jibble.pircbot.PircBot;

import java.util.ArrayList;

public class MyIRCClient extends PircBot {
    private ChatFragment mChatFragment;
    private ArrayList<String> mServers;

    public MyIRCClient(ChatFragment cF, String nick, ArrayList<String> servers) {
        mChatFragment = cF;
        this.setName(nick);
        this.setLogin(nick);
        mServers = servers;
    }

    public void loadChat2(final String c, final String t, final int retry) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    setVerbose(true);
                    connect(getServerAddress(mServers.get(0)), getServerPort(mServers.get(0)), "oauth:" + t);
                    joinChannel(c);
                    if (mChatFragment.getActivity() == null) return;
                    mChatFragment.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mChatFragment.onJoined();
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (retry < mServers.size()-1) {
                        loadChat2(c, t, retry+1);
                        return;
                    }
                    Toast.makeText(mChatFragment.getActivity(), "Could not connect to Chat", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }

    public void reconnectToChannel(final String c) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    joinChannel(c);
                    if (mChatFragment.getActivity() == null) return;
                    mChatFragment.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mChatFragment.onJoined();
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onMessage(String channel, String sender,
                          String login, String hostname, String message) {
        //Log.d("IRC", sender + ": " + message);
        final String fSender = sender;
        final String fMessage = message;
        if (mChatFragment.getActivity() == null) return;
        mChatFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChatFragment.newMessage(fSender, fMessage);
            }
        });
    }

    private String getServerAddress(String s) {
        return s.substring(0, s.indexOf(":"));
    }

    private int getServerPort(String s) {
        String p = s.substring(s.indexOf(":")+1);
        return Integer.parseInt(p);
    }
}

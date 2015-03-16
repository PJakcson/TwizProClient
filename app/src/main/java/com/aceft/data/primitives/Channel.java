package com.aceft.data.primitives;

import android.graphics.Bitmap;

import com.aceft.data.TwitchJSONParser;

import java.util.ArrayList;
import java.util.HashMap;

public class Channel {
    public HashMap<String, String> mData;
    public Bitmap mLogoBitmap, mBannerBitmap;
    public ArrayList<TwitchVideo> mHighlights, mBroadcasts;
    private int isOnline = 0;
    private boolean bIsOnline = false;

    public Channel(String name) {
        mData = new HashMap<>();
        mData.put("name", name);
    }

    public Channel(HashMap<String, String> h) {
        mData = h;
    }

    public String getLogoLink() {
        return mData.get("logo");
    }

    public String getBannerLink() {
        return mData.get("video_banner");
    }

    public String getStatus() {
        return mData.get("status");
    }

    public String getDisplayName() {
        return mData.get("display_name");
    }

    public String getId() {
        return mData.get("_id");
    }

    public String getViews() {
        return mData.get("views");
    }

    public String getFollowers() {
        return mData.get("followers");
    }

    public String getGame() {
        return mData.get("game");
    }

    public String getName() {
        return mData.get("name");
    }

    public String getUrl() {
        return mData.get("url");
    }

    public String getMature() {
        return mData.get("mature");
    }

    public String getCreated() {
        return TwitchJSONParser.createdToDate(mData.get("created_at"));
    }

    public int getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(int isOnline) {
        this.isOnline = isOnline;
    }

    public boolean isbIsOnline() {
        return bIsOnline;
    }

    public void setbIsOnline(boolean bIsOnline) {
        this.bIsOnline = bIsOnline;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Channel))
            return false;
        if (obj == this)
            return true;

        Channel rhs = (Channel) obj;
        return this.getName().equals(rhs.getName());
    }
}
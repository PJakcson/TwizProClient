package com.aceft.data.primitives;

import android.graphics.Bitmap;

public class Emoticon {
    private boolean mSubOnly;
    private String mRegex, mUrl, mState;
    private Bitmap emoti;

    public Emoticon(String regex, String url, String state, boolean subscriber_only) {
        mRegex = regex;
        mUrl = url;
        mState = state;
        mSubOnly = subscriber_only;
    }

    public boolean getSubOnly() {
        return mSubOnly;
    }

    public String getState() {
        return mState;
    }

    public String getRegex() {
        return mRegex;
    }

    public String getUrl() {
        return mUrl;
    }


    public Bitmap getEmoti() {
        return emoti;
    }

    public void setEmoti(Bitmap emoti) {
        this.emoti = emoti;
    }
}
package com.aceft.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.aceft.R;
import com.aceft.data.primitives.Emoticon;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class IRCAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private ArrayList<String> mSenders;
    private ArrayList<String> mMessages;
    private ArrayList<Integer> mSenderColor;
    private ArrayList<Emoticon> mEmotis;
    private ArrayList<CharSequence> mFormatedMessage;
    private Context mContext;
    private int mTextLineHeight = 0;
    private ViewGroup.LayoutParams mParams;


    public IRCAdapter(Activity c) {
        mContext = c;
        if (mMessages == null) mMessages = new ArrayList<>();
        if (mSenders == null) mSenders = new ArrayList<>();
        if (mSenderColor == null) mSenderColor = new ArrayList<>();
        if (mEmotis == null) mEmotis = new ArrayList<>();
        if (mFormatedMessage == null) mFormatedMessage = new ArrayList<>();
        mInflater = LayoutInflater.from(c);
    }

    public void update(String sender, String message) {
        if (mMessages.size() > 2000) {
            mMessages = new ArrayList<>(mMessages.subList(1000, 2000));
            mSenders = new ArrayList<>(mSenders.subList(1000, 2000));
            mSenderColor = new ArrayList<>(mSenderColor.subList(1000, 2000));
        }
        if (mSenders.size() == 0)
            mSenderColor.add(randChatColor());
        else if (mSenders.indexOf(sender) > -1)
            mSenderColor.add(mSenderColor.get(mSenders.indexOf(sender)));
        else
            mSenderColor.add(randChatColor());

        mSenders.add(sender);
        mMessages.add(message);
        mFormatedMessage.add(null);
        notifyDataSetChanged();
    }


    public void updateEmotis(ArrayList<Emoticon> emotis) {
        mEmotis = emotis;
        notifyDataSetChanged();
    }

    public void clearData() {
        mMessages.clear();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.item_layout_chat, parent, false);
            holder = new ViewHolder();
            holder.message = (TextView)convertView.findViewById(R.id.textChatMessage);

            if (holder.message.getLineHeight() > 0 && mTextLineHeight == 0) {
                mTextLineHeight = holder.message.getLineHeight() + 5;
            }

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mFormatedMessage.get(position) == null) {
            mFormatedMessage.set(position, getSpan(position));
            holder.message.setText(mFormatedMessage.get(position));
        } else
            holder.message.setText(mFormatedMessage.get(position));

        return convertView;
    }

    public int getCount() {
        return mSenders.size();
    }

    private int randChatColor() {

        int rand = (int) (Math.random()*14);
        String s = mContext.getResources().getStringArray(R.array.chat_colors)[rand];
        return Color.parseColor(s);
    }

    private CharSequence getSpan(int i) {
        String sender = mSenders.get(i);
        int sColor = mSenderColor.get(i);
        String message = mMessages.get(i);

        SpannableString spanS = new SpannableString(sender);
        spanS.setSpan(new StyleSpan(Typeface.BOLD),0 ,sender.length(), 0);
        spanS.setSpan(new ForegroundColorSpan(sColor), 0 ,sender.length(), 0);

        SpannableString spanM = getStringWithEmotis(message);
        return TextUtils.concat(spanS, ": ", spanM);
    }

    private SpannableString getStringWithEmotis(String m) {
        SpannableString spanM = new SpannableString(m);
        ImageSpan span;

        Pattern pattern;
        Matcher matcher;
        Bitmap b;
        float scale;
        for (int i = 0; i < mEmotis.size(); i++) {
            pattern = Pattern.compile(mEmotis.get(i).getRegex());
            if (mEmotis.get(i).getRegex().equals("\\&lt\\;3"))
                pattern = Pattern.compile("<3");
            matcher = pattern.matcher(m);
            while (matcher.find()) {
                b = mEmotis.get(i).getEmoti();
                scale = 1.0f * b.getWidth() / b.getHeight();
                b = Bitmap.createScaledBitmap(b,(int) (mTextLineHeight * scale) , mTextLineHeight, true);
                span = new ImageSpan(mContext, b, ImageSpan.ALIGN_BOTTOM);
                spanM.setSpan(span, matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

        }
        return spanM;
    }

    @Override
    public String getItem(int position) {
        return mMessages.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    public class ViewHolder {
        TextView message;
    }
}
package com.aceft.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.aceft.R;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.primitives.Channel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class ChannelListAdapter extends BaseAdapter {
    private Activity mActivity;
    private LayoutInflater mInflater;
    private ArrayList<Channel> mChannels;
    private RelativeLayout.LayoutParams mRelativeLayout;
    private int mWidth = 0;

    public ChannelListAdapter(Activity c) {
        mActivity = c;
        mChannels = new ArrayList<>();
        mInflater = LayoutInflater.from(c);
    }

    public void update(ArrayList <Channel> c) {
        if (c == null) return;
        mChannels.addAll(c);
        notifyDataSetChanged();
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if(convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.item_layout_channel, parent, false);
            holder = new ViewHolder();
            holder.firstLine = (TextView) convertView.findViewById(R.id.firstLine);
            holder.secondLine = (TextView) convertView.findViewById(R.id.secondLine);
            holder.secondLineViewers = (TextView) convertView.findViewById(R.id.secondLineViewers);
            holder.imageView = (ImageView) convertView.findViewById(R.id.icon);
            holder.textLive = (TextView) convertView.findViewById(R.id.textLive);

            if (convertView.getMeasuredWidth() != mWidth && convertView.getMeasuredWidth() != 0) {
                mWidth = Math.round(convertView.getMeasuredWidth() * 0.4f);
                mRelativeLayout = new RelativeLayout.LayoutParams(mWidth, mWidth);
            }

            if (mRelativeLayout != null)
                holder.imageView.setLayoutParams(mRelativeLayout);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        Picasso.with(mActivity)
                .load(mChannels.get(position).getLogoLink())
                .placeholder(R.drawable.ic_placeholder)
                .config(Bitmap.Config.RGB_565)
                .into(holder.imageView);

        holder.firstLine.setText(mChannels.get(position).getDisplayName());
        holder.secondLine.setText(mChannels.get(position).getGame());
        String followers;
        if (mChannels.get(position).getFollowers().equals("1")) {
            followers = mChannels.get(position).getFollowers() + " Follower";
        } else {
            followers = mChannels.get(position).getFollowers() + " Followers";
        }
        holder.secondLineViewers.setText(followers);

        if (!mChannels.get(position).isbIsOnline()) {
            holder.textLive.setVisibility(View.INVISIBLE);
        } else {
            holder.textLive.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    public int getCount() {
        return mChannels.size();
    }

    public Channel getItem(int position) {
        return mChannels.get(position);
    }

    public long getItemId(int position) {
        return Long.valueOf(mChannels.get(position).getId());
    }

    public void resetDimensions() {
        mRelativeLayout = null;
        mWidth = 0;
    }

    public void clearAllData() {
        mChannels.clear();
    }

    public class ViewHolder {
        public ImageView imageView;
        public TextView secondLine;
        public TextView firstLine;
        public TextView secondLineViewers;
        public TextView textLive;
    }

    private void checkIfOnline(final int pos, final TextView overlay) {
        final String request = mActivity.getResources().getString(R.string.channel_stream_url)
                + mChannels.get(pos).getName();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                final JSONObject j = TwitchNetworkTasks.downloadJSONData(request);
                if (j == null || mActivity == null) return;
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            if (j.getString("stream").equals("null")) {
                                mChannels.get(pos).setIsOnline(2);
                            }
                            else {
                                overlay.setVisibility(View.VISIBLE);
                                mChannels.get(pos).setIsOnline(1);
                            }
                        } catch (JSONException e) {
                            mChannels.get(pos).setIsOnline(2);
                        }
                    }
                });
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

}
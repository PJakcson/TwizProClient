package com.twizproclient.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.twizproclient.R;
import com.twizproclient.data.TwitchNetworkTasks;
import com.twizproclient.data.primitives.Game;
import com.twizproclient.ui_fragments.GamesRasterFragment;

import java.util.ArrayList;


public class GamesAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private ArrayList<Game> mGames;
    private double aspectRatio;
    private RelativeLayout.LayoutParams mRelativeLayout;
    private Context mContext;
    private int mWidth;
    private ViewGroup.LayoutParams mParams;


    public GamesAdapter(GamesRasterFragment c) {
        mContext = c.getActivity();
        if (mGames == null) mGames = new ArrayList<>();
        mInflater = LayoutInflater.from(c.getActivity());
        mGames = new ArrayList<>();
    }

    public void update(ArrayList<Game> g) {
        mGames.addAll(g);
        notifyDataSetChanged();
    }

    public void cleanData() {
        mGames.clear();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.item_layout_game, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.game_desc);
            holder.viewers = (TextView) convertView.findViewById(R.id.game_viewers);
            holder.thumbImage = (ImageView) convertView.findViewById(R.id.game_thumbnail);

            if (mWidth == 0 && ((GridView)parent).getColumnWidth() > 0) {
                float scale = 380.f / 272.f;
                mWidth = ((GridView)parent).getColumnWidth();
                mParams = holder.thumbImage.getLayoutParams();
                mParams.width = mWidth;
                mParams.height = (int) (mWidth * scale);
            }

            if (mParams.height > 0) {
                holder.thumbImage.setLayoutParams(mParams);
            }

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        Picasso.with(mContext)
                .load(mGames.get(position).mThumbnail)
                .placeholder(R.drawable.ic_placeholder)
                .config(Bitmap.Config.RGB_565)
                .into(holder.thumbImage);

        holder.title.setText(mGames.get(position).mTitle);
        holder.viewers.setText(Integer.toString(mGames.get(position).mViewers));

        return convertView;
    }

    public int getCount() {
        return mGames.size();
    }

    public Game getItem(int position) {
        return mGames.get(position);
    }

    public long getItemId(int position) {
        return Long.valueOf(mGames.get(position).mId);
    }

    public class ViewHolder {
        ImageView thumbImage;
        TextView title;
        TextView viewers;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;
        private final int pos;

        public DownloadImageTask(ImageView imageView, int pos) {
            this.imageView = imageView;
            this.pos = pos;
        }

         protected Bitmap doInBackground(String... urls) {
            return TwitchNetworkTasks.downloadBitmap(urls[0]);
        }

        protected void onPostExecute(Bitmap result) {
            if (imageView.getTag().equals(mGames.get(pos).mThumbnail)) imageView.setImageBitmap(result);
            mGames.get(pos).mBitmapThumb = result;
        }
    }

    private void loadImage2(int pos, final ImageView imageView) {
        final int fPos = pos;
        new Thread(new Runnable() {
            public void run() {
                final Bitmap bitmap = TwitchNetworkTasks.downloadBitmap(mGames.get(fPos).mThumbnail);
                mGames.get(fPos).mBitmapThumb = bitmap;
                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (imageView.getTag().equals(mGames.get(fPos).mThumbnail)) imageView.setImageBitmap(bitmap);
                        mGames.get(fPos).mBitmapThumb = bitmap;
                    }
                });
            }
        }).start();
    }
}
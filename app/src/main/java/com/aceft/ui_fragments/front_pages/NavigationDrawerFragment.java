package com.aceft.ui_fragments.front_pages;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.aceft.MainActivity;
import com.aceft.R;
import com.aceft.adapter.DrawerAdapter;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.TwitchNetworkTasks;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class NavigationDrawerFragment extends Fragment {

    private NavigationDrawerCallbacks mCallbacks;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;

    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer, mUserHasCompletedSetup;

    private ArrayList<Channel> mChannels;
    private DrawerAdapter mDrawerAdapter;

    private SharedPreferences mPreferences;
    private boolean mOnlineLoading;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = mPreferences.getBoolean(Preferences.PREF_USER_LEARNED_DRAWER, false);
        mUserHasCompletedSetup = mPreferences.getBoolean(Preferences.PREF_USER_COMPLETED_SETUP, false);
        mCurrentSelectedPosition = mPreferences.getInt(Preferences.APP_DEFAULT_HOME, 0);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(Preferences.STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }

        if (!mFromSavedInstanceState) {
            selectItem(mCurrentSelectedPosition);
        }


    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        String sections[] = new String[]{
                getString(R.string.title_section1),
                getString(R.string.title_section2),
                getString(R.string.title_section3),
                getString(R.string.title_section4)
        };

        int drawables[] = new int[] {
                R.drawable.drawer_games,
                R.drawable.drawer_channel,
                R.drawable.drawer_search,
                R.drawable.drawer_favorites
        };

        String footer[] = new String[]{"Settings", "Feedback", "Support"};

        mDrawerAdapter = new DrawerAdapter(getActivity(),sections , drawables, footer);
        mDrawerListView.setAdapter(mDrawerAdapter);

        if (!mOnlineLoading)
            checkFollowedChannels();

        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return mDrawerListView;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),
                mDrawerLayout,
                null,
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(Preferences.PREF_USER_LEARNED_DRAWER, true).apply();
                }

                //getActivity().invalidateOptionsMenu();
            }
        };

        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            //mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public void selectItem(int position) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserHasCompletedSetup = sp.getBoolean(Preferences.PREF_USER_COMPLETED_SETUP, false);

        if (position == 4) return;
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            if (!mUserHasCompletedSetup) {
                mCallbacks.onNavigationDrawerItemSelected(-1);
                mCurrentSelectedPosition = 0;
                return;
            }
            if (position < 8)
                mCallbacks.onNavigationDrawerItemSelected(position);
            if (position == 9) {
                Toast.makeText(getActivity(), "Refreshing Drawer", Toast.LENGTH_SHORT).show();
                checkFollowedChannels();
                deselectList();
            }
            if (position > 9)
                channelClicked(position);
        }
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(mFragmentContainerView);
    }

    public void selectListItem(int position) {
        mDrawerListView.setItemChecked(position, true);
    }

    public void deselectList(){
        mDrawerListView.setItemChecked(mDrawerListView.getCheckedItemPosition(), false);
    }

    public void disableDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void enableDrawer() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Preferences.STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return item.getItemId() == R.id.action_example || super.onOptionsItemSelected(item);

    }

    private void channelClicked(int p) {
        ((MainActivity) getActivity()).onChannelSelected(mDrawerAdapter.getChannel(p));
    }

    public static interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(int position);
    }

    private class CustomComparator implements Comparator<Channel> {
        @Override
        public int compare(Channel lhs, Channel rhs) {
            if (lhs.isbIsOnline() && !rhs.isbIsOnline()) return -1;
            if (!lhs.isbIsOnline() && rhs.isbIsOnline()) return 1;
            return lhs.getName().compareTo(rhs.getName());
        }
    }

    public void checkFollowedChannels() {
        if (!mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false) || mOnlineLoading) {
            return;
        }
        mOnlineLoading = true;
        mDrawerAdapter.setIsLoading();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                mChannels = new ArrayList<>();
                if (getActivity() == null) {
                    mOnlineLoading = false;
                    return;
                }
                // get Channels
                String req = mPreferences.getString(Preferences.TWITCH_USERNAME, "");
                req = getString(R.string.twitch_user_url) + req + getString(R.string.twitch_user_following_suffix);
                String d = TwitchNetworkTasks.downloadStringData(req);
                mChannels = TwitchJSONParser.followedChannelsToArrayList(d);
                if (getActivity() == null) {
                    mOnlineLoading = false;
                    return;
                }

                // get Streams
                if (getActivity() == null) return;
                String request = getString(R.string.channel_stream_url);
                request += "?channel=";

                for (Channel c : mChannels) {
                    request += c.getName()+",";
                }
                request += "&limit=" + mChannels.size();
                String j = TwitchNetworkTasks.downloadStringData(request);
                ArrayList<Stream> streams = TwitchJSONParser.streamJSONtoArrayList(j);
                if (streams == null) {
                    streams = new ArrayList<>();
                }

                // get online Channels
                final ArrayList<Channel> online = new ArrayList<>();
                for (Stream s : streams) {
                    online.add(s.getChannel());
                }

                Collections.sort(online, new CustomComparator());

                if (getActivity() == null) {
                    mOnlineLoading = false;
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mOnlineLoading = false;
                        mDrawerAdapter.updateFollowed(online);
                    }
                });
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void clearFollowed() {
        mDrawerAdapter.clearFollowed();
    }
}

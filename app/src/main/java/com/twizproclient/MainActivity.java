package com.twizproclient;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.twizproclient.data.Preferences;
import com.twizproclient.data.TwitchJSONParser;
import com.twizproclient.data.primitives.Channel;
import com.twizproclient.data.primitives.Game;
import com.twizproclient.data.primitives.Stream;
import com.twizproclient.ui_fragments.ChannelDetailFragment;
import com.twizproclient.ui_fragments.ChannelListFragment;
import com.twizproclient.ui_fragments.GamesRasterFragment;
import com.twizproclient.ui_fragments.NavigationDrawerFragment;
import com.twizproclient.ui_fragments.SearchFragment;
import com.twizproclient.ui_fragments.SettingsFragment;
import com.twizproclient.ui_fragments.SetupFragment;
import com.twizproclient.ui_fragments.StreamListFragment;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GamesRasterFragment.OnGameSelectedListener, StreamListFragment.onStreamSelectedListener,
        ChannelListFragment.onChannelSelectedListener {

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private static final String ARG_ACTIONBAR_TITLE = "action_bar";
    private String mUrls[];
    private AdView mAdView;
    private boolean mIsInSetup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUrls = getResources().getStringArray(R.array.drawer_urls);
        setBitmapQuality();
        setContentView(R.layout.activity_main);

        //Picasso.with(this).setIndicatorsEnabled(true);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        //mAdView.loadAd(adRequest);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentTransaction transaction;
        switch (position){
            case 0:
                GamesRasterFragment mGamesRasterFragment = new GamesRasterFragment();
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, mGamesRasterFragment.newInstance(mUrls[position]));
                transaction.addToBackStack("0");
                transaction.commit();
                break;
            case 1:
                StreamListFragment mStreamListFragment = new StreamListFragment();
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, mStreamListFragment.newInstance(mUrls[position], null));
                transaction.addToBackStack("1");
                transaction.commit();
                break;
            case 2:
                SearchFragment searchFragment = new SearchFragment();
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, searchFragment);
                transaction.addToBackStack("2");
                transaction.commit();
                break;
            case 3:
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                String req = sp.getString(Preferences.TWITCH_USERNAME, "");
                if (sp.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false) && !req.isEmpty()) {
                    req = getString(R.string.twitch_user_url) + req + getString(R.string.twitch_user_following_suffix);
                    ChannelListFragment favoritesFragment = new ChannelListFragment();
                    transaction = getFragmentManager().beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.container, favoritesFragment.newInstance(req));
                    transaction.addToBackStack("3");
                    transaction.commit();
                }
                break;
            case 4:
                //divider
                break;
            case 5:
                //gopro
                break;
            case 6:
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, new SettingsFragment());
                transaction.addToBackStack("6");
                transaction.commit();
                break;
            case 100:
                setDefaultSettings();
                mIsInSetup = true;
                SetupFragment s = new SetupFragment();
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.container, s);
                transaction.commit();
                break;
        }
    }

    public void setBitmapQuality() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        String qArray[] = getResources().getStringArray(R.array.settings_bitmap_qualities);
        String q = sp.getString(Preferences.TWITCH_BITMAP_QUALITY, "");

        if (q.contains(qArray[0])) TwitchJSONParser.setHighQuality();
        if (q.contains(qArray[1])) TwitchJSONParser.setMediumQuality();
        if (q.contains(qArray[2])) TwitchJSONParser.setSmallQuality();
    }

    private void setDefaultSettings() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putString(Preferences.TWITCH_STREAM_QUALITY_TYPE, getString(R.string.default_stream_quality_type)).apply();
        sp.edit().putString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY, getString(R.string.default_preferred_video_quality)).apply();
        String defaultBitmap = getResources().getStringArray(R.array.settings_bitmap_qualities)[0];
        sp.edit().putString(Preferences.TWITCH_BITMAP_QUALITY, defaultBitmap).apply();
    }

    @Override
    public void onPause() {
        mAdView.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdView.resume();
    }

    @Override
    public void onDestroy() {
        mAdView.destroy();
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        int itemCount = fm.getBackStackEntryCount();

        if (itemCount > 1) {
            synchronizeDrawer(fm.getBackStackEntryAt(itemCount-2).getName());
            fm.popBackStack();

        } else if (mIsInSetup) {
            mIsInSetup = false;
            startApp();
        } else {
            super.onBackPressed();
        }
    }

    private void synchronizeDrawer(String i) {
        if(i == null) return;
        try {
            int drawer = Integer.valueOf(i);
            switch (drawer) {
                case 0: mNavigationDrawerFragment.selectListItem(0); break;
                case 1: mNavigationDrawerFragment.selectListItem(1); break;
                case 2: mNavigationDrawerFragment.selectListItem(2); break;
                case 3: mNavigationDrawerFragment.selectListItem(3); break;
                case 6: mNavigationDrawerFragment.selectListItem(6); break;
            }
        } catch (NumberFormatException e) {
            Log.d("synchrDrawer", e.toString());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        savedInstanceState.putString(ARG_ACTIONBAR_TITLE, (String) actionBar.getTitle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onGameSelected(Game g) {
        mNavigationDrawerFragment.deselectList();
        String url = getString(R.string.game_streams_url);
        url += g.toURL() + "&";
        StreamListFragment mStreamListFragment = new StreamListFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, mStreamListFragment.newInstance(url, g.mTitle));
        transaction.addToBackStack(g.mId);
        transaction.commit();
    }

    @Override
    public void onStreamSelected(Stream g) {
        mNavigationDrawerFragment.deselectList();
        ChannelDetailFragment mChannelDetailFragment = new ChannelDetailFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, mChannelDetailFragment.newInstance(g.mName));
        transaction.addToBackStack(String.valueOf(g.mId));
        transaction.commit();
    }

    @Override
    public void onChannelSelected(Channel c) {
        mNavigationDrawerFragment.deselectList();
        ChannelDetailFragment mChannelDetailFragment = new ChannelDetailFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, mChannelDetailFragment.newInstance(c.getName()));
        transaction.addToBackStack(c.getId());
        transaction.commit();
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.v("ASDFasdf", intent.toString());
    }

    public void startApp() {
        mIsInSetup = false;
        GamesRasterFragment mGamesRasterFragment = new GamesRasterFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, mGamesRasterFragment.newInstance(mUrls[0]));
        transaction.addToBackStack("0");
        transaction.commit();
    }
}

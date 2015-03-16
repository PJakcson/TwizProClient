package com.aceft;

import android.animation.ObjectAnimator;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.aceft.ui_fragments.channel_fragments.channel_pager.ChannelVodCategoryFragment;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.aceft.data.Preferences;
import com.aceft.data.TwitchJSONParser;
import com.aceft.data.primitives.Channel;
import com.aceft.data.primitives.Game;
import com.aceft.data.primitives.Stream;
import com.aceft.data.primitives.TwitchVideo;
import com.aceft.data.primitives.TwitchVod;
import com.aceft.ui_fragments.channel_fragments.ChannelDetailFragment;
import com.aceft.ui_fragments.front_pages.FollowedListFragment;
import com.aceft.ui_fragments.front_pages.GamesRasterFragment;
import com.aceft.ui_fragments.front_pages.NavigationDrawerFragment;
import com.aceft.ui_fragments.front_pages.SearchFragment;
import com.aceft.ui_fragments.front_pages.SettingsFragment;
import com.aceft.ui_fragments.front_pages.StreamListFragment;
import com.aceft.ui_fragments.channel_fragments.VideoFragment;
import com.aceft.ui_fragments.setup.SetupPagerFragment;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GamesRasterFragment.OnGameSelectedListener, StreamListFragment.onStreamSelectedListener,
        FollowedListFragment.onChannelSelectedListener,
        ChannelVodCategoryFragment.onOldVideoSelectedListener, SearchFragment.OnGameSelectedListener,
        SearchFragment.onChannelSelectedListener, FragmentManager.OnBackStackChangedListener {

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private static final String ARG_ACTIONBAR_TITLE = "action_bar";
    private String mUrls[];
    private AdView mAdView;
    private String mUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUrls = getResources().getStringArray(R.array.drawer_urls);
        setBitmapQuality();
        setContentView(R.layout.activity_main);
        mUsername = "öaoibsnwotzböslfhösudasodvasopdfoasngdüas";

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
//        if (!mIsInSetup) {
            mAdView.loadAd(adRequest);
//        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentTransaction transaction;
        switch (position){
            case 0:
                GamesRasterFragment mGamesRasterFragment = new GamesRasterFragment();
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, mGamesRasterFragment.newInstance(mUrls[position]), "0");
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
                if (!sp.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
                    gotoHome();
                    Toast.makeText(this, "Please set up your account under settings.", Toast.LENGTH_SHORT).show();
                } else {
                    String req = sp.getString(Preferences.TWITCH_USERNAME, "");
                    req = getString(R.string.twitch_user_url) + req + getString(R.string.twitch_user_following_suffix);
                    FollowedListFragment favoritesFragment = new FollowedListFragment();
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
//            case 5:
//                gopro
//                break;
            case 5:
                transaction = getFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.replace(R.id.container, new SettingsFragment(), "settings");
                transaction.addToBackStack("6");
                transaction.commit();
                break;
            case 6:
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:acefortwitch@gmail.com?subject=Feedback " + Build.MANUFACTURER + " " + Build.MODEL)));
                break;
            case 7:
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:acefortwitch@gmail.com?subject=Support " + Build.MANUFACTURER + " " + Build.MODEL)));
                break;
            case -1:
                setDefaultSettings();
                transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.container, new SetupPagerFragment(), "setup");
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
        sp.edit().putInt(Preferences.APP_DEFAULT_HOME, 0);
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

        if (mNavigationDrawerFragment.isDrawerOpen()) {
            mNavigationDrawerFragment.closeDrawer();
            return;
        }

        if (fm.findFragmentByTag("setup") != null) {
            fm.beginTransaction().remove(fm.findFragmentByTag("setup")).commit();
            mNavigationDrawerFragment.enableDrawer();
            gotoHome();
        }else if (itemCount > 1) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    public void exitSetup(SetupPagerFragment s) {
        mNavigationDrawerFragment.enableDrawer();
        getFragmentManager().beginTransaction().remove(s).commit();
        gotoHome();
    }

    private void synchronizeDrawer(String i) {
        if(i == null) return;
        switch (i) {
            case "0": mNavigationDrawerFragment.selectListItem(0); return;
            case "1": mNavigationDrawerFragment.selectListItem(1); return;
            case "2": mNavigationDrawerFragment.selectListItem(2); return;
            case "3": mNavigationDrawerFragment.selectListItem(3); return;
            case "6": mNavigationDrawerFragment.selectListItem(5); return;
            case "7": mNavigationDrawerFragment.selectListItem(6); return;
            case "8": mNavigationDrawerFragment.selectListItem(7); return;
        }
        mNavigationDrawerFragment.deselectList();
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
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
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
        transaction.replace(R.id.container, mChannelDetailFragment.newInstance(g.getName()));
        transaction.addToBackStack(String.valueOf(g.getId()));
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
    public void onOldVideoSelected(TwitchVod t1, TwitchVideo t2) {
        VideoFragment videoFragment = new VideoFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, videoFragment.newInstance(t1, t2));
        transaction.addToBackStack("video");
        transaction.commit();
    }

    public void gotoHome() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        int home = sp.getInt(Preferences.APP_DEFAULT_HOME, 0);
        mNavigationDrawerFragment.selectItem(home);
    }

    public void pauseAd() {
        if (mAdView == null) return;
        mAdView.setVisibility(View.GONE);
    }

    public void resumeAd() {
        if (mAdView == null) return;
        mAdView.setVisibility(View.VISIBLE);
    }

    public void resetAdPosition() {
        if (mAdView == null) return;
        if (mAdView.getMeasuredHeight() < 0) return;
        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mAdView, "translationY", mAdView.getMeasuredHeight(), 0f);
        fadeInStream.setDuration(0);
        fadeInStream.start();
    }

    public void pushUpAd() {
        if (mAdView == null) return;
        if (mAdView.getMeasuredHeight() < 0) return;
        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mAdView, "translationY", mAdView.getMeasuredHeight(), 0f);
        fadeInStream.setDuration(300);
        fadeInStream.start();
    }

    public void pushDownAd() {
        if (mAdView == null) return;
        if (mAdView.getMeasuredHeight() < 0) return;
        ObjectAnimator fadeInStream = ObjectAnimator.ofFloat(mAdView, "translationY", 0f, mAdView.getMeasuredHeight());
        fadeInStream.setDuration(300);
        fadeInStream.start();
    }

    public void setAdPosition(int pos) {
        if (mAdView == null) return;
        if (mAdView.getLayoutParams() == null) return;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mAdView.getLayoutParams();
        if (pos == RelativeLayout.ALIGN_PARENT_TOP) {
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        }
        if (pos == RelativeLayout.ALIGN_PARENT_BOTTOM) {
            params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        mAdView.setLayoutParams(params);
    }

    public void refreshNavDrawer() {
        mNavigationDrawerFragment.checkFollowedChannels();
    }

    public void disableDrawer() {
        mNavigationDrawerFragment.disableDrawer();
    }

    public void enableDrawer() {
        mNavigationDrawerFragment.enableDrawer();
    }

    @Override
    public void onBackStackChanged() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() <= 0) return;
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        String home = String.valueOf(sp.getInt(Preferences.APP_DEFAULT_HOME, 0));

        if (!sp.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
            mNavigationDrawerFragment.clearFollowed();
            mUsername = "";
        } else if (!sp.getString(Preferences.TWITCH_USERNAME, "").equals(mUsername)) {
            mUsername = sp.getString(Preferences.TWITCH_USERNAME, "");
            mNavigationDrawerFragment.checkFollowedChannels();
        }

        if (fm.getBackStackEntryAt(fm.getBackStackEntryCount()-1).getName() != null) {
            if (fm.getBackStackEntryAt(fm.getBackStackEntryCount()-1).getName().equals(home)) {
                fm.popBackStack(1, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }
        synchronizeDrawer(fm.getBackStackEntryAt(fm.getBackStackEntryCount()-1).getName());
    }
}

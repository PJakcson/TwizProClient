package com.twizproclient.ui_fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.twizproclient.MainActivity;
import com.twizproclient.R;
import com.twizproclient.data.Preferences;
import com.twizproclient.data.TwitchNetworkTasks;


/**
 * Created by marc on 27.01.2015. Gridview of available games
 */
public class SettingsFragment extends Fragment {
    private LinearLayout mQualityLayout, mPreferredQualityLayout, mUsernameLayout, mLoginStatusLayout, mTwitchLoginLayout, mRefreshTokenLayout, mThumbnailQualityLayout;
    private TextView mQualityText, mPreferredQualityText, mLoginStatusText, mUsernameText, mThumbnailQualityText;
    private EditText mUsernameEditText;
    private SharedPreferences mPreferences;
    private int mItemSelected, mQualityTypeSelected, mPreferredQualitySelected, mBitmapQualitySelected;
    private View mUsernameDialogView;

    public SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        // Video Settings
        mQualityLayout = (LinearLayout) rootView.findViewById(R.id.qualitySetting);
        mQualityText = (TextView) mQualityLayout.findViewById(R.id.textQualitySetting);
        mPreferredQualityLayout = (LinearLayout) rootView.findViewById(R.id.preferredQualitySetting);
        mPreferredQualityText = (TextView) mPreferredQualityLayout.findViewById(R.id.textPreferredQualitySetting);

        // Twitch Settings
        mLoginStatusLayout = (LinearLayout) rootView.findViewById(R.id.settingsLoginStatus);
        mLoginStatusText = (TextView) mLoginStatusLayout.findViewById(R.id.textLoginStatus);
        mUsernameLayout = (LinearLayout) rootView.findViewById(R.id.usernameSetting);
        mUsernameText = (TextView) mUsernameLayout.findViewById(R.id.textUsernameSetting);
        mTwitchLoginLayout = (LinearLayout) rootView.findViewById(R.id.twitchLoginSetting);
        mRefreshTokenLayout = (LinearLayout) rootView.findViewById(R.id.refreshTokenSetting);

        // UI Settings
        mThumbnailQualityLayout = (LinearLayout) rootView.findViewById(R.id.thumbnailSetting);
        mThumbnailQualityText = (TextView) mThumbnailQualityLayout.findViewById(R.id.textThumbnailQuality);

        // Set initial values
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mQualityTypeSelected = getTypeIndex(mPreferences.getString(Preferences.TWITCH_STREAM_QUALITY_TYPE, ""));
        mPreferredQualitySelected = getPrefQualityIndex(mPreferences.getString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY, ""));
        mBitmapQualitySelected = getBitmapQualityIndex(mPreferences.getString(Preferences.TWITCH_BITMAP_QUALITY, ""));

        updateTextFields();

        // Set Listeners
        mQualityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQualityDialog();
            }
        });

        mPreferredQualityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPreferredQualityDialog();
            }
        });

        if (mQualityTypeSelected == 1) disablePrefView();
        else enablePrefView();

        mUsernameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUsernameDialog();
            }
        });

        mTwitchLoginLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
                    newLoginDialog();
                }
                else {
                    SetupFragment s = new SetupFragment();
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.container, s);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            }
        });

        mRefreshTokenLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthFragment a = new AuthFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.container, a);
                transaction.addToBackStack(null);
                transaction.commit();
                Toast.makeText(getActivity(), "refresh token", Toast.LENGTH_SHORT).show();
            }
        });

        mTwitchLoginLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
                    newLoginDialog();
                }
                else {
                    SetupFragment s = new SetupFragment();
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.container, s);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            }
        });

        mThumbnailQualityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBitmapDialog();
            }
        });

        mLoginStatusLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false)) {
                    newLoginDialog();
                }
            }
        });

        return rootView;
    }



    private void showQualityDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String types[] = getActivity().getResources().getStringArray(R.array.settings_stream_quality_type);


        builder.setTitle("Select Play Behaviour")
                .setSingleChoiceItems(types, mQualityTypeSelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mQualityTypeSelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mQualityText.setText(types[mQualityTypeSelected]);
                        mPreferences.edit().putString(Preferences.TWITCH_STREAM_QUALITY_TYPE, types[mQualityTypeSelected]).apply();
                        if (types[mQualityTypeSelected].equals("auto select best")) {
                            disablePrefView();
                        }
                        else {
                            enablePrefView();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();

        mThumbnailQualityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBitmapDialog();
            }
        });
    }

    private void showPreferredQualityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String qualities[] = getActivity().getResources().getStringArray(R.array.livestream_qualities);

        builder.setTitle("Select Preferred Quality")
                .setSingleChoiceItems(qualities, mPreferredQualitySelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPreferredQualitySelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mPreferredQualityText.setText(qualities[mPreferredQualitySelected]);
                        mPreferences.edit().putString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY, qualities[mPreferredQualitySelected]).apply();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void showUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        mUsernameDialogView = inflater.inflate(R.layout.setting_username_dialog, null);
        mUsernameEditText = (EditText) mUsernameDialogView.findViewById(R.id.usernameEditText);

        builder.setTitle("Please Enter Username")
                .setView(mUsernameDialogView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String request = getActivity().getResources().getString(R.string.twitch_user_url)
                                + mUsernameEditText.getText().toString();
                        mUsernameText.setText(mUsernameEditText.getText());
                        new DownloadJSONTask().execute(request);
                        mUsernameLayout.findViewById(R.id.usernameUpdateProgress).setVisibility(View.VISIBLE);
                        mUsernameLayout.findViewById(R.id.usernameStatusIcon).setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getActivity(),""+ mQualityTypeSelected,Toast.LENGTH_SHORT).show();
                    }
                });
        builder.create();
        builder.show();
    }

    private void newLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("New Twitch Account")
                .setMessage("Do your really want to log in with a new Account?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CookieManager cookieManager = CookieManager.getInstance();
                        cookieManager.removeAllCookie();

                        SetupFragment s = new SetupFragment();
                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                        transaction.replace(R.id.container, s);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void newUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Recognized new Twitch Account")
                .setMessage("Do you want to log into Twitch for restricted streams?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AuthFragment a = new AuthFragment();
                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                        transaction.replace(R.id.container, a);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    private void usernameDataReceived(JSONObject userData) {
        mUsernameLayout.findViewById(R.id.usernameUpdateProgress).setVisibility(View.GONE);
        String username = "", userDisplayName = "";
        try {
            username = userData.getString("name");
            userDisplayName = userData.getString("display_name");
            usernameConfirmed(username, userDisplayName);
        } catch (JSONException e) {
            mUsernameLayout.findViewById(R.id.usernameStatusIcon).setVisibility(View.VISIBLE);
            ((ImageView)mUsernameLayout.findViewById(R.id.usernameStatusIcon)).setImageResource(R.drawable.ic_username_fail);
            mPreferences.edit().putBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false).apply();
            mPreferences.edit().putBoolean(Preferences.USER_IS_AUTHENTICATED, false).apply();
            updateTextFields();
            Log.d("SetupFragment:username", "no valid username" + username);
        } catch (NullPointerException e) {
            mUsernameLayout.findViewById(R.id.usernameStatusIcon).setVisibility(View.VISIBLE);
            ((ImageView)mUsernameLayout.findViewById(R.id.usernameStatusIcon)).setImageResource(R.drawable.ic_username_fail);
            mPreferences.edit().putBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false).apply();
            mPreferences.edit().putBoolean(Preferences.USER_IS_AUTHENTICATED, false).apply();
            updateTextFields();
            Log.d("SetupFragment:username", "no valid username" + username + " Nullpointer");
        }
    }

    private void usernameConfirmed(String username, String userDisplayName) {
        mUsernameLayout.findViewById(R.id.usernameStatusIcon).setVisibility(View.VISIBLE);
        ((ImageView)mUsernameLayout.findViewById(R.id.usernameStatusIcon)).setImageResource(R.drawable.ic_username_ok);

        mPreferences.edit().putBoolean(Preferences.USER_HAS_TWITCH_USERNAME, true).apply();
        mPreferences.edit().putString(Preferences.TWITCH_USERNAME, username).apply();
        mPreferences.edit().putString(Preferences.TWITCH_DISPLAY_USERNAME, userDisplayName).apply();

        if (username.equals(mPreferences.getString(Preferences.TWITCH_USERNAME, ""))) {
            mPreferences.edit().putBoolean(Preferences.USER_IS_AUTHENTICATED, false).apply();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            newUserDialog();
        }

        updateTextFields();
    }

    private void showBitmapDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String sizes[] = getActivity().getResources().getStringArray(R.array.settings_bitmap_qualities);

        builder.setTitle("Select Preferred Quality")
                .setSingleChoiceItems(sizes, mBitmapQualitySelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBitmapQualitySelected = which;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mThumbnailQualityText.setText(sizes[mBitmapQualitySelected]);
                        mPreferences.edit().putString(Preferences.TWITCH_BITMAP_QUALITY, sizes[mBitmapQualitySelected]).apply();
                        ((MainActivity)getActivity()).setBitmapQuality();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create();
        builder.show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    private void updateTextFields() {
        mQualityText.setText(mPreferences.getString(Preferences.TWITCH_STREAM_QUALITY_TYPE, ""));
        mPreferredQualityText.setText(mPreferences.getString(Preferences.TWITCH_PREFERRED_VIDEO_QUALITY, ""));

        mThumbnailQualityText.setText(mPreferences.getString(Preferences.TWITCH_BITMAP_QUALITY, ""));

        if (mPreferences.getBoolean(Preferences.USER_IS_AUTHENTICATED, false)) {
            mLoginStatusText.setText("Logged In and Authorized");
            mUsernameText.setText(mPreferences.getString(Preferences.TWITCH_DISPLAY_USERNAME, ""));
            ((ImageView)mLoginStatusLayout.findViewById(R.id.loginStatusIcon)).setImageResource(R.drawable.ic_login_auth);
        }
        else if (mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false)) {
            mLoginStatusText.setText("Logged In with Username, not yet Authorized");
            mUsernameText.setText(mPreferences.getString(Preferences.TWITCH_DISPLAY_USERNAME, ""));
            ((ImageView)mLoginStatusLayout.findViewById(R.id.loginStatusIcon)).setImageResource(R.drawable.ic_login_user);
        }
        else {
            mUsernameText.setText("No Username set");
            mLoginStatusText.setText("Not logged in");
        }

        if (mPreferences.getBoolean(Preferences.USER_HAS_TWITCH_USERNAME, false))
            ((ImageView)mUsernameLayout.findViewById(R.id.usernameStatusIcon)).setImageResource(R.drawable.ic_login_auth);
        else
            ((ImageView)mUsernameLayout.findViewById(R.id.usernameStatusIcon)).setImageResource(R.drawable.ic_username_fail);
    }

    private int getTypeIndex(String s) {
        String types[] = getActivity().getResources().getStringArray(R.array.settings_stream_quality_type);
        for (int i = 0; i < types.length; i++) {
            if (s.equals(types[i])) return i;
        }
        return 0;
    }

    private int getPrefQualityIndex(String s) {
        String types[] = getActivity().getResources().getStringArray(R.array.livestream_qualities);
        for (int i = 0; i < types.length; i++) {
            if (s.equals(types[i])) return i;
        }
        return 0;
    }

    private int getBitmapQualityIndex(String s) {
        String sizes[] = getActivity().getResources().getStringArray(R.array.settings_bitmap_qualities);
        for (int i = 0; i < sizes.length; i++) {
            if (s.equals(sizes[i])) return i;
        }
        return 0;
    }

    private class DownloadJSONTask extends AsyncTask<String, Void, JSONObject> {

        public DownloadJSONTask(){}

        protected JSONObject doInBackground(String... urls) {
            return TwitchNetworkTasks.downloadJSONData(urls[0]);
        }

        protected void onPostExecute(JSONObject result) {
            usernameDataReceived(result);
        }
    }

    private void disablePrefView () {
        final int disabled = getActivity().getResources().getColor(R.color.primary_text_disabled_material_light);
        mPreferredQualityText.setTextColor(disabled);
        mPreferredQualityLayout.setClickable(false);
    }

    private void enablePrefView () {
        final int enabled = getActivity().getResources().getColor(R.color.secondary_text_default_material_light);
        mPreferredQualityText.setTextColor(enabled);
        mPreferredQualityLayout.setClickable(true);
    }
}
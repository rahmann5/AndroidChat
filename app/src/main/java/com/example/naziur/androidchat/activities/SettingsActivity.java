package com.example.naziur.androidchat.activities;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.LogoutLoader;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {
    public static final int UNBLOCK_REQUEST_CODE = 1;
    private static final int LOGOUT_LOADER_ID = 0;
    protected DatabaseReference database;
    protected FirebaseAuth mAuth;
    private static boolean controlOffline = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedpreferences.getBoolean(getResources().getString(R.string.key_online_presence), true)) {
            database= FirebaseHelper.setOnlineStatusListener(mAuth.getCurrentUser().getUid(), false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (controlOffline && database != null)
            database.child("online").setValue(false);

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserAuthenticated ();
        controlOffline = true;
    }


    private void checkUserAuthenticated () {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        } else {
            if (database != null)
                database.child("online").setValue(true);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            // For all other preferences, set the summary to the value's
            // simple string representation.
            preference.setSummary(stringValue);
            return true;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_my_headers, target);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }


    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || SettingsActivity.AccountPreferenceFragment.class.getName().equals(fragmentName)
                || SettingsActivity.HelpPreferenceFragment.class.getName().equals(fragmentName)
                || SettingsActivity.AboutPreferenceFragment.class.getName().equals(fragmentName);
    }


    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AccountPreferenceFragment extends PreferenceFragment implements FirebaseHelper.FirebaseHelperListener, LoaderManager.LoaderCallbacks<Void>{
        FirebaseHelper firebaseHelper;
        FirebaseUserModel firebaseUserModel;
        FirebaseAuth mAuth;
        User user = User.getInstance();
        private static final String TAG = "AccountPreference";
        private ProgressDialog progressDialog;
        private List<String> tempKeys = new ArrayList<>();
        private List<String> singleChatKeys;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            firebaseHelper = FirebaseHelper.getInstance();
            mAuth = FirebaseAuth.getInstance();
            firebaseHelper.setFirebaseHelperListener(this);
            addPreferencesFromResource(R.xml.pref_account);
            progressDialog = new ProgressDialog(getActivity(), R.layout.progress_dialog, false);
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_email)));

            Preference blockListPref = findPreference(getString(R.string.key_block_list));
            blockListPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), MemberSelectorActivity.class);
                    intent.putExtra("block_list", "true");
                    startActivityForResult(intent, UNBLOCK_REQUEST_CODE);
                    return true;
                }
            });

            Preference deleteAccPref = findPreference(getString(R.string.key_delete_acc));
            deleteAccPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    getDialog().show();
                    return true;
                }
            });

            Preference logoutPref = findPreference(getString(R.string.key_logout));
            logoutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    LoaderManager loaderManager = getLoaderManager();
                    loaderManager.initLoader(LOGOUT_LOADER_ID, null, AccountPreferenceFragment.this);
                    return true;
                }
            });
        }


        private Dialog getDialog(){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.delete_dialog_msg)
                    .setPositiveButton(R.string.delete_dialog_op_1, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            toggleProgressDialog(true);
                            progressDialog.setInfo(getResources().getString(R.string.progress_info_1));
                            firebaseHelper.toggleListenerFor("users", "username", user.name,
                            firebaseHelper.getValueEventListener("", FirebaseHelper.NON_CONDITION, FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_2, FirebaseUserModel.class),
                                    true, true);
                        }
                    })
                    .setNegativeButton(R.string.delete_dialog_op_2, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });

            return builder.create();
        }
        /*
        * Legend: (D) = delete, (R) = remove, (SC) = single chat, (GC) = group chat
        *
        * Group Chat steps -> if step 2 doesn't occur all subsequent steps are aborted
        * (R) from groups -> (D) groups -> Collect (GC) message images -> (D) (GC) message images + group pic ->
        * (D) all (GC) messages
        *
        * Single Chat steps
        * find all chats to leave -> Collect (SC) message images -> (D) (SC) message images ->
        * (D) all (SC) messages
        *
        * General
        * (D) profile pic -> (D) user object -> (D) auth account
        *
        * */
        @Override
        public void onCompleteTask(String tag, int condition, Container container) {
            if(tag.equals("getValueEventListener")){
                if(condition == FirebaseHelper.CONDITION_1){
                    Toast.makeText(getActivity(), "User doesn't exist", Toast.LENGTH_SHORT).show();
                } else if (condition == FirebaseHelper.CONDITION_2){
                    firebaseUserModel = (FirebaseUserModel) container.getObject();
                    if(!firebaseUserModel.getGroupKeys().isEmpty()) {
                        progressDialog.setInfo(getResources().getString(R.string.progress_info_2));
                        firebaseHelper.exitGroup(null, firebaseUserModel.getUsername(), false, Arrays.asList(firebaseUserModel.getGroupKeys().split(",")));
                    }else if(!firebaseUserModel.getChatKeys().isEmpty()){
                        progressDialog.setInfo(getResources().getString(R.string.progress_info_3));
                        firebaseHelper.accumulateAllChatsForDeletion(getAllUsersInChatWith(firebaseUserModel.getChatKeys().split(",")), user.name);
                    } else {
                        progressDialog.setInfo(getResources().getString(R.string.progress_info_4));
                        checkIfProfileHasPicToDelete();
                    }
                }
            } else if (tag.equals("exitGroup")){
                if(condition == FirebaseHelper.CONDITION_1){
                    progressDialog.setInfo(getResources().getString(R.string.progress_info_5));
                    firebaseUserModel.setGroupKeys("");
                    Log.i(TAG, "Found " +container.getGroups().size() + " that need deleting");
                    firebaseHelper.deleteGroups(container.getGroups());
                } else if(condition == FirebaseHelper.CONDITION_2) {
                    toggleProgressDialog(false);
                    Toast.makeText(getActivity(), "Failed to exit groups please try again", Toast.LENGTH_SHORT).show();
                }
            } else if (tag.equals("deleteGroups")){
                if(condition == FirebaseHelper.CONDITION_1){
                    progressDialog.setInfo(getResources().getString(R.string.progress_info_6));
                    List<String> test = Arrays.asList(getChatKeysFor(container.getGroups()));
                    List<String> test2 = new ArrayList<>();
                    for(String s : test){
                        test2.add(s);
                    };
                    firebaseHelper.collectImagesForDeletion("group", test2, picUrlsFor(container.getGroups()));
                } else if(condition == FirebaseHelper.CONDITION_2) {
                    progressDialog.setInfo(getResources().getString(R.string.progress_info_7));
                    Log.i(TAG, "No. groups to delete deleting chats with keys: " + firebaseUserModel.getChatKeys());
                    firebaseHelper.accumulateAllChatsForDeletion(getAllUsersInChatWith(firebaseUserModel.getChatKeys().split(",")), user.name);
                }
            } else if (tag.equals("cleanDeleteAllMessages")){
                if(condition == FirebaseHelper.CONDITION_1){
                    if(container.getBoolean()) {
                        Log.i(TAG, "current chat keys are " + firebaseUserModel.getChatKeys() + " after deletion of messages");
                        if (!firebaseUserModel.getChatKeys().isEmpty()) {
                            progressDialog.setInfo(getResources().getString(R.string.progress_info_7));
                            firebaseHelper.accumulateAllChatsForDeletion(getAllUsersInChatWith(firebaseUserModel.getChatKeys().split(",")), user.name);
                        } else {
                            checkIfProfileHasPicToDelete();
                        }
                    }
                }else if(condition == FirebaseHelper.CONDITION_2) {
                    toggleProgressDialog(false);
                    Toast.makeText(getActivity(), "Failed to deleted all images and messages, please try again.", Toast.LENGTH_SHORT).show();
                }
            } else if (tag.equals("accumulateAllChatsForDeletion")){
                if(condition == FirebaseHelper.CONDITION_1) {
                    singleChatKeys = getAllChatsWithUsers(container.getStringList());
                    firebaseHelper.collectImagesForDeletion("single", singleChatKeys, new ArrayList<String>());
                }else if(condition == FirebaseHelper.CONDITION_2){
                    progressDialog.setInfo(getResources().getString(R.string.progress_info_4));
                    checkIfProfileHasPicToDelete();
                }
            } else if(tag.equals("collectImagesForDeletion")){
                if(condition == FirebaseHelper.CONDITION_1) {
                    Log.i(TAG, "found "+container.getStringList()+" uris and "+tempKeys.size() +" keys, now deleting images");
                    Network.deleteUploadImages(firebaseHelper, container.getStringList(), getArrayForList(tempKeys), container.getString());
                }
            }else if(tag.equals("deleteUserFromDatabase")){
                progressDialog.setInfo(getResources().getString(R.string.progress_info_8));
                toggleProgressDialog(false);
                if(condition == FirebaseHelper.CONDITION_1) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if(firebaseUser != null)
                        firebaseUser.delete();
                    user.logout();
                    startActivity(new Intent(getActivity(), RegisterActivity.class));
                    getActivity().finish();
                }
            }
        }

        private void toggleProgressDialog(boolean show){
            progressDialog.toggleDialog(show);
            progressDialog.toggleInfoDisplay(show);
        }

        private String[] getArrayForList(List<String> list){
            String[] stockArr = new String[list.size()];
            stockArr = list.toArray(stockArr);
            return stockArr;
        }

        private void checkIfProfileHasPicToDelete(){
            toggleProgressDialog(false);
            if(!firebaseUserModel.getProfilePic().isEmpty()) {
                List<String> list = new ArrayList<String>();
                list.add(firebaseUserModel.getProfilePic());
               Network.deleteUploadImages(firebaseHelper, list, new String[]{firebaseUserModel.getUsername()}, "profile");
            } else {
                firebaseHelper.deleteUserFromDatabase(firebaseUserModel.getUsername());
            }
        }

        private String[] getChatKeysFor(List<FirebaseGroupModel> groups){
            String [] keys = new String[groups.size()];
            for(int i = 0; i < groups.size(); i++)
                keys[i] = groups.get(i).getGroupKey();
            return keys;
        }

        private List<String> picUrlsFor(List<FirebaseGroupModel> groups){
            List<String> picUrls = new ArrayList<>();
            for(FirebaseGroupModel fbg: groups){
                if(!fbg.getPic().isEmpty()){
                    picUrls.add(fbg.getPic());
                }
            }
            Log.i(TAG, "Getting "+picUrls.size()+" group profile images");
            return picUrls;
        }

        private List<String> getAllUsersInChatWith(String [] chatKeys){
            List<String> users = new ArrayList<>();
            for(String key: chatKeys){
                String p1 = key.split("-")[0];
                String p2 = key.split("-")[1];
                if(!user.name.equals(p1)) {
                    users.add(p1);
                } else if(!user.name.equals(p2)) {
                    users.add(p2);
                }
                Log.i(TAG, "Adding user: " +users.get(users.size()-1));
            }
            return users;
        }

        private List<String> getAllChatsWithUsers(List<String> users){
            String [] chatKeys = firebaseUserModel.getChatKeys().split(",");
            Log.i(TAG, "now matching users to keys: " + firebaseUserModel.getChatKeys());
            List<String> keys = new ArrayList<>();
            for(String key: chatKeys){
                String p1 = key.split("-")[0];
                String p2 = key.split("-")[1];
                if(users.contains(p1)){
                    keys.add(key);
                } else if(users.contains(p2)){
                    keys.add(key);
                }
                Log.i(TAG, "Adding key: " +keys.get(keys.size()-1));
            }
            firebaseUserModel.setChatKeys("");
            return keys;
        }

        @Override
        public void onFailureTask(String tag, DatabaseError databaseError) {
            toggleProgressDialog(false);
            Toast.makeText(getActivity(), "An error occurred, delete was aborted.", Toast.LENGTH_SHORT).show();
            Log.i(TAG, tag+" "+databaseError.getMessage());
        }

        @Override
        public void onChange(String tag, int condition, Container container) {
            if(tag.equals("collectImagesForDeletion")){
                if(condition == FirebaseHelper.CONDITION_1){
                    tempKeys.add(container.getString());
                    firebaseHelper.collectImagesForDeletion(container.getContainer().getString(), container.getContainer().getStringList(), container.getStringList());
                }
            }
        }

        @Override
        public Loader<Void> onCreateLoader(int i, Bundle bundle) {
            progressDialog.toggleDialog(true);
            return new LogoutLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<Void> loader, Void Obj) {
            progressDialog.toggleDialog(false);
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseHelper.setOnlineStatusListener(mAuth.getCurrentUser().getUid(), true);
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }

        @Override
        public void onLoaderReset(Loader<Void> loader) {

        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_about);
            Preference aboutUs = findPreference(getString(R.string.key_about_us));
            aboutUs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), AboutActivity.class);
                    intent.putExtra("key", R.string.key_about_us);
                    startActivity(intent);
                    return true;
                }
            });

            Preference appInfo = findPreference(getString(R.string.key_app_info));
            appInfo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), AboutActivity.class);
                    intent.putExtra("key", R.string.key_app_info);
                    startActivity(intent);
                    return true;
                }
            });

            Preference reportIssue = findPreference(getString(R.string.key_report_issue));
            reportIssue.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    controlOffline = false;
                    User user = User.getInstance();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri data = Uri.parse("mailto:?subject=" + "Error Reporting by "+ user.profileName +" ("+user.name+")" + "&to=" +  "johnB1994@hotmail.co.uk");
                    intent.setData(data);
                    startActivity(Intent.createChooser(intent, ""));
                    return true;
                }
            });
        }

    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class HelpPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_help);

            Preference ratePref = findPreference(getString(R.string.pref_rate_key));
            ratePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" +  getActivity().getPackageName())));
                    return true;
                }
            });
            Preference sharePref = findPreference(getString(R.string.pref_share_key));
            sharePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controlOffline = false;
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    String shareBodyText = "Check it out. Android Chat (Place URL Here)";
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,"Android Chat");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBodyText);
                    startActivity(Intent.createChooser(sharingIntent, "Shearing Option"));
                    return true;
                }
            });

            Preference faqPref = findPreference(getString(R.string.pref_faq_key));
            faqPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controlOffline = false;
                    Uri newsUri = Uri.parse("https://tutoriallibrary.000webhostapp.com/faqs");
                    Intent websiteIntent = new Intent(Intent.ACTION_VIEW, newsUri);
                    startActivity(websiteIntent);
                    return true;
                }
            });
            Preference tAndCPref = findPreference(getString(R.string.pref_tandc_key));
            tAndCPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controlOffline = false;
                    Uri newsUri = Uri.parse("https://tutoriallibrary.000webhostapp.com/legal/1");
                    Intent websiteIntent = new Intent(Intent.ACTION_VIEW, newsUri);
                    startActivity(websiteIntent);
                    return true;
                }
            });
            Preference privacyPref = findPreference(getString(R.string.pref_privacy_key));
            privacyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    controlOffline = false;
                    Uri newsUri = Uri.parse("https://tutoriallibrary.000webhostapp.com/legal/0");
                    Intent websiteIntent = new Intent(Intent.ACTION_VIEW, newsUri);
                    startActivity(websiteIntent);
                    return true;
                }
            });

        }

    }
}

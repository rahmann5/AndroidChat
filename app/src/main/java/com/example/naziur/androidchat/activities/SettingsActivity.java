package com.example.naziur.androidchat.activities;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {
    public static final int UNBLOCK_REQUEST_CODE = 1;
    protected DatabaseReference database;
    protected FirebaseAuth mAuth;
    private static boolean controlOffline = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        database= FirebaseHelper.setOnlineStatusListener(mAuth.getCurrentUser().getUid(), false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (controlOffline)
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
    public static class AccountPreferenceFragment extends PreferenceFragment implements FirebaseHelper.FirebaseHelperListener{
        FirebaseHelper firebaseHelper;
        FirebaseUserModel firebaseUserModel;
        FirebaseAuth mAuth;
        User user = User.getInstance();
        private static final String TAG = "AccountPreference";
        private ProgressDialog progressDialog;
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
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    FirebaseHelper.setOnlineStatusListener(mAuth.getCurrentUser().getUid(), true);
                    mAuth.signOut();
                    startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
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
                            progressDialog.setInfo("Retrieving user information");
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
        * S1: (R) from groups -> (D) groups -> Collect (GC) message images -> (D) (GC) message images + group pic ->
        * (D) all (GC) messages -> find all chats to leave -> Collect (SC) message images -> (D) (SC) message images ->
        * (D) all (SC) messages -> (D) profile pic -> (D) user object -> (D) auth account
        *
        * S2: (R) from groups -> (D) groups -> (D) group pic -> D) all (GC) messages ->
        * find all chats to leave -> Collect (SC) message images -> (D) (SC) message images ->
        * (D) all (SC) messages -> (D) profile pic -> (D) user object -> (D) auth account
        * */
        @Override
        public void onCompleteTask(String tag, int condition, Container container) {
            if(tag.equals("getValueEventListener")){ //STEP 1
                if(condition == FirebaseHelper.CONDITION_1){
                    Toast.makeText(getActivity(), "User doesn't exist", Toast.LENGTH_SHORT).show();
                } else if (condition == FirebaseHelper.CONDITION_2){
                    firebaseUserModel = (FirebaseUserModel) container.getObject();
                    if(!firebaseUserModel.getGroupKeys().isEmpty()) { //Start at step 2
                        progressDialog.setInfo("Exiting from associated groups");
                        firebaseHelper.exitGroup(null, firebaseUserModel.getUsername(), false, Arrays.asList(firebaseUserModel.getGroupKeys().split(",")));
                    }else if(!firebaseUserModel.getChatKeys().isEmpty()){ //Start at step 6
                        progressDialog.setInfo("Exiting from associated one-to-one chats");
                        firebaseHelper.accumulateAllChatsForDeletion(getAllUsersInChatWith(firebaseUserModel.getChatKeys().split(",")), user.name);
                    } else {
                        progressDialog.setInfo("Proceeding to delete user account");
                        checkIfProfileHasPicToDelete();
                    }
                }
            } else if (tag.equals("exitGroup")){ //STEP 2
                if(condition == FirebaseHelper.CONDITION_1){
                    progressDialog.setInfo("Deleting any redundant groups");
                    firebaseUserModel.setGroupKeys("");
                    firebaseHelper.deleteGroups(container.getGroups());
                } else if(condition == FirebaseHelper.CONDITION_2) {
                    toggleProgressDialog(false);
                    Toast.makeText(getActivity(), "Failed to exit groups please try again", Toast.LENGTH_SHORT).show();
                }
            } else if (tag.equals("deleteGroups")){ //STEP 3
                if(condition == FirebaseHelper.CONDITION_1){
                    progressDialog.setInfo("Collecting any related images from groups for deletion");
                    firebaseHelper.collectAllImagesForDeletionThenDeleteAllRelatedMessages("group", Arrays.asList(getChatKeysFor(container.getGroups())), picUrlsFor(container.getGroups()));
                } else if(condition == FirebaseHelper.CONDITION_2) {
                    progressDialog.setInfo("Deleting any associated one-to-one chats");
                    firebaseHelper.accumulateAllChatsForDeletion(getAllUsersInChatWith(firebaseUserModel.getChatKeys().split(",")), user.name);
                }
            } else if(tag.equals("collectAllImagesForDeletionThenDeleteAllRelatedMessages")){ //STEP 4
                String[] stockArr = new String[container.getContainer().getStringList().size()];
                stockArr = container.getContainer().getStringList().toArray(stockArr);
                if(condition == FirebaseHelper.CONDITION_1) {
                    progressDialog.setInfo("Deleting all collected images");
                    Network.deleteUploadImages(firebaseHelper, container.getStringList(), stockArr, container.getString());
                }else if(condition == FirebaseHelper.CONDITION_2) { //GO TO STEP 5
                    progressDialog.setInfo("Deleting all messages");
                    firebaseHelper.cleanDeleteAllMessages(container.getString(), stockArr);
                }
            } else if (tag.equals("cleanDeleteAllMessages")){ //STEP 5
                if(condition == FirebaseHelper.CONDITION_1){
                    if(!firebaseUserModel.getChatKeys().isEmpty()){
                        progressDialog.setInfo("Deleting any associated one-to-one chats");
                        firebaseHelper.accumulateAllChatsForDeletion(getAllUsersInChatWith(firebaseUserModel.getChatKeys().split(",")), user.name);
                    } else {
                        checkIfProfileHasPicToDelete();
                    }
                }else if(condition == FirebaseHelper.CONDITION_2) {
                    toggleProgressDialog(false);
                    Toast.makeText(getActivity(), "Failed to deleted all images and messages, please try again.", Toast.LENGTH_SHORT).show();
                }
            } else if (tag.equals("accumulateAllChatsForDeletion")){//STEP 6
                if(condition == FirebaseHelper.CONDITION_1) {
                    List<String> list = getAllChatsWithUsers(container.getStringList());
                    firebaseUserModel.setChatKeys("");
                    firebaseHelper.collectAllImagesForDeletionThenDeleteAllRelatedMessages("single", list, null);
                }else if(condition == FirebaseHelper.CONDITION_2){
                    progressDialog.setInfo("Proceeding to delete user account");
                    checkIfProfileHasPicToDelete();
                }
            } else if(tag.equals("deleteUserFromDatabase")){
                progressDialog.setInfo("User account deleted");
                toggleProgressDialog(false);
                if(condition == FirebaseHelper.CONDITION_1) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if(firebaseUser != null)
                        firebaseUser.delete();
                    Toast.makeText(getActivity(), "Your account has been deleted", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getActivity(), RegisterActivity.class));
                    getActivity().finish();
                }
            }
        }

        private void toggleProgressDialog(boolean show){
            progressDialog.toggleDialog(show);
            progressDialog.toggleInfoDisplay(show);
        }

        private void checkIfProfileHasPicToDelete(){
            toggleProgressDialog(false);
            if(!firebaseUserModel.getProfilePic().isEmpty()) {
                List<String> list = new ArrayList<String>();
                list.add(firebaseUserModel.getProfilePic());
                Network.deleteUploadImages(firebaseHelper, list, new String[]{firebaseUserModel.getUsername()}, "profile");
            } else {
                //firebaseHelper.deleteUserFromDatabase(firebaseUserModel.getUsername());
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
            return picUrls;
        }

        private List<String> getAllUsersInChatWith(String [] chatKeys){
            List<String> users = new ArrayList<>();
            for(String key: chatKeys){
                String p1 = key.split("-")[0];
                String p2 = key.split("-")[1];
                if(!user.name.equals(p1)) {
                    System.out.println("Adding name: "+p1);
                    users.add(p1);
                } else if(!user.name.equals(p2)) {
                    System.out.println("Adding name: "+p2);
                    users.add(p2);
                }
            }
            return users;
        }

        private List<String> getAllChatsWithUsers(List<String> users){
            String [] chatKeys = firebaseUserModel.getChatKeys().split(",");
            List<String> keys = new ArrayList<>();
            for(String key: chatKeys){
                String p1 = key.split("-")[0];
                String p2 = key.split("-")[1];
                if(users.contains(p1)){
                    System.out.println("Adding key: "+key);
                    keys.add(key);
                } else if(users.contains(p2)){
                    System.out.println("Adding key: "+key);
                    keys.add(key);
                }
            }
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

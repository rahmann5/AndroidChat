package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.fragment.AddContactDialogFragment;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MemberSelectorActivity extends AuthenticatedActivity implements FirebaseHelper.FirebaseHelperListener, MyContactsAdapter.OnItemClickListener,  AddContactDialogFragment.ContactDialogListener{
    private static final String TAG = "MemberSelectorActivity";
    private User user = User.getInstance();
    private FirebaseHelper firebaseHelper;
    private LinearLayout emptyState;
    private MyContactsAdapter myContactsAdapter;
    private ArrayList<String> selectedContacts;
    private String [] currentMembers;
    private boolean blockListMode = false;
    private RecyclerView myContactsRecycler;
    private ContactDBHelper db;
    private ProgressDialog progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_contacts);
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            if (extra.getString("current_members") != null){
                if (extra.getString("current_members").equals("")) {
                    currentMembers = extra.getString("current_members").split(",");
                }
            } else if (extra.getString("block_list") != null ) {
                blockListMode = true;
            } else {
                finish();
            }
        }
        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        selectedContacts = new ArrayList<>();
        emptyState = (LinearLayout) findViewById(R.id.empty);
        FloatingActionButton floatingActionButton  = (FloatingActionButton) findViewById(R.id.add_contact);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showContctsDialog();
            }
        });
        myContactsRecycler = (RecyclerView) findViewById(R.id.contacts_recycler);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        myContactsRecycler.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                mLayoutManager.getOrientation());
        myContactsRecycler.addItemDecoration(dividerItemDecoration);
        if (blockListMode) {
            progressBar.toggleDialog(true);
            floatingActionButton.setVisibility(View.GONE);
            if (!Network.isInternetAvailable(this, true)) {
                return;
            }
            createSingleEvent(user.name, FirebaseHelper.CONDITION_2); // load block listed users
        } else {
            progressBar.toggleDialog(true);
            db = new ContactDBHelper(getApplicationContext());
            firebaseHelper.updateAllLocalContactsFromFirebase(this, db);
        }

        createActionBar ();
    }

    public void showContctsDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new AddContactDialogFragment();
        dialog.show(getSupportFragmentManager(), "AddContactDialogFragment");
    }


    private void toggleEmptyState () {
        if (myContactsAdapter.getItemCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }

    }

    private FirebaseUserModel createFirebaseUserModel(String username, String profileName, String profilePic) {
        FirebaseUserModel fbModel = new FirebaseUserModel();
        fbModel.setUsername(username);
        fbModel.setProfileName(profileName);
        fbModel.setProfilePic(profilePic);
        return fbModel;
    }

    private void createActionBar () {
        ActionBar actionBar = getSupportActionBar();
        if (blockListMode) {
            actionBar.setTitle(getResources().getString(R.string.title_block_list));
        } else {
            actionBar.setTitle(getResources().getString(R.string.title_select_member));
        }
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP| ActionBar.DISPLAY_SHOW_TITLE| ActionBar.DISPLAY_SHOW_CUSTOM);
        ImageView imageView = new ImageView(actionBar.getThemedContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setImageResource(R.drawable.ic_thumb_up);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.END
                | Gravity.CENTER_VERTICAL);
        imageView.setLayoutParams(layoutParams);
        actionBar.setCustomView(imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Network.isInternetAvailable(MemberSelectorActivity.this, true)) {
                    if (!blockListMode) {
                        Intent data = new Intent();
                        data.putStringArrayListExtra("members", selectedContacts);
                        setResult(RESULT_OK,data);
                        finish();
                    } else {
                        progressBar.toggleDialog(true);
                        firebaseHelper.updateBlockList(selectedContacts.toArray(new String[selectedContacts.size()]), false);
                    }
                }


            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(Contact contact, int pos, View itemView) {
        if (!blockListMode && !contact.isActive()) {
            return;
        }

        if (!selectedContacts.contains(contact.getContact().getUsername())) {
            selectedContacts.add(contact.getContact().getUsername());
            itemView.setBackgroundResource(R.color.green);
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
            selectedContacts.remove(contact.getContact().getUsername());
        }

    }

    private boolean isAlreadyMember (String username) {
        if (currentMembers != null) {
            for (String member : currentMembers) {
                if (member.equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, final String username) {

        if (!Network.isInternetAvailable(this, true)) {
            dialog.dismiss();
            return;
        }

        if(!db.isUserAlreadyInContacts(username) && !username.equals(user.name) && !isAlreadyMember(username)){
            createSingleEvent(username, FirebaseHelper.CONDITION_1);
        } else {
            Toast.makeText(MemberSelectorActivity.this, "User cannot be added as they may already exist or it is your username", Toast.LENGTH_LONG).show();
        }
    }

    private void createSingleEvent (String target, int condition) {
        ValueEventListener valueEventListener = firebaseHelper.getValueEventListener(target,
                FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION, condition, FirebaseUserModel.class);
        firebaseHelper.toggleListenerFor("users", "username", target, valueEventListener, true, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null)
            db.close();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
    }

    private List<Contact> getAllNonMemberContacts (List<Contact> allContacts) {
        List<Contact> nonMemberContacts = new ArrayList<>();
        List<String> allMembers = Arrays.asList(currentMembers);
        for (Contact c : allContacts) {
            if (!allMembers.contains(c.getContact().getUsername())) {
                nonMemberContacts.add(c);
            }
        }
        return nonMemberContacts;
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("updateAllLocalContactsFromFirebase")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    myContactsAdapter = new MyContactsAdapter(this, getAllNonMemberContacts(container.getContacts()), this);
                    myContactsRecycler.setAdapter(myContactsAdapter);
                    toggleEmptyState();
                    progressBar.toggleDialog(false);
                    break;
            }
        } else if (tag.equals("getValueEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    FirebaseUserModel fbModel = (FirebaseUserModel) container.getObject();
                    myContactsAdapter.addNewItem(fbModel);
                    toggleEmptyState();
                    break;

                case FirebaseHelper.CONDITION_2 :
                    ContactDBHelper db = new ContactDBHelper(getApplicationContext());
                    FirebaseUserModel userModel = (FirebaseUserModel) container.getObject();
                    List<Contact> allContacts = new ArrayList<>();
                    try {
                        JSONArray blockedUsers = new JSONArray(userModel.getBlockedUsers());
                        for (int i = 0 ; i < blockedUsers.length(); i++) {
                            String username = blockedUsers.getString(i);
                            if (db.isUserAlreadyInContacts(username)) {
                                String [] userInfo = db.getProfileNameAndPic(username);
                                allContacts.add(new Contact(createFirebaseUserModel(username, userInfo[0], userInfo[1])));
                            } else {
                                createSingleEvent(username, FirebaseHelper.CONDITION_3);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    myContactsAdapter = new MyContactsAdapter(this, allContacts, this);
                    myContactsRecycler.setAdapter(myContactsAdapter);
                    toggleEmptyState();
                    db.close();
                    progressBar.toggleDialog(false);
                    break;

                case FirebaseHelper.CONDITION_3 :
                    FirebaseUserModel uModel = (FirebaseUserModel) container.getObject();
                    myContactsAdapter.addNewItem(uModel);
                    toggleEmptyState();
                    break;
            }
        } else if (tag.equals("updateBlockList")) {
            switch (condition){
                case FirebaseHelper.CONDITION_1 :
                    progressBar.toggleDialog(false);
                    Toast.makeText(this, "Successfully updated block list", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
            }
        }

    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        progressBar.toggleDialog(false);
        Log.i(TAG, tag + ": " + databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

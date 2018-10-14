package com.example.naziur.androidchat.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.models.Contact;

import com.example.naziur.androidchat.fragment.AddContactDialogFragment;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MyContactsActivity extends AuthenticatedActivity implements AddContactDialogFragment.ContactDialogListener, FirebaseHelper.FirebaseHelperListener{
    private static final String TAG = "MyContactsActivity";
    private ContactDBHelper db;
    private RecyclerView myContactsRecycler;
    private MyContactsAdapter myContactsAdapter;
    private LinearLayout emptyState;
    private User user = User.getInstance();
    private ProgressDialog progressBar;
    FirebaseHelper firebaseHelper;
    private Contact selectedContact;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_contacts);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        db = new ContactDBHelper(getApplicationContext());
        myContactsRecycler = (RecyclerView) findViewById(R.id.contacts_recycler);
        progressBar = new ProgressDialog(MyContactsActivity.this, R.layout.progress_dialog, true);
        emptyState = (LinearLayout) findViewById(R.id.empty);
        FloatingActionButton floatingActionButton  = (FloatingActionButton) findViewById(R.id.add_contact);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showContctsDialog();
            }
        });

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setUpList ();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setUpList () {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        myContactsRecycler.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                mLayoutManager.getOrientation());
        myContactsRecycler.addItemDecoration(dividerItemDecoration);

        Cursor c = db.getAllMyContacts(null);

        if (c != null && c.getCount() > 0) {
            updateExistingContacts(c);
        } else {
            myContactsAdapter = new MyContactsAdapter(this, setUpListener());
            myContactsRecycler.setAdapter(myContactsAdapter);
            toggleContactsEmptyState();
        }


    }

    private void updateExistingContacts (final Cursor c) {
        progressBar.toggleDialog(true);
        myContactsAdapter = new MyContactsAdapter(this, setUpListener());
        boolean hasInternet = Network.isInternetAvailable(this, true);
        try{
            while (c.moveToNext()) {

                final FirebaseUserModel fbModel = new FirebaseUserModel();
                fbModel.setUsername(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME)));
                fbModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));

                if (hasInternet) {
                    firebaseHelper.updateLocalContactsFromFirebase("users", fbModel, db);
                } else {
                    fbModel.setProfilePic(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC)));
                    myContactsAdapter.addNewItem(fbModel);
                }

            }
        } finally {
            if (!hasInternet){
                Toast.makeText(this, "Data maybe outdated", Toast.LENGTH_LONG).show();
                myContactsRecycler.setAdapter(myContactsAdapter);
                progressBar.toggleDialog(false);
                toggleContactsEmptyState();
            }
            c.close();
        }
    }

    private MyContactsAdapter.OnItemClickListener setUpListener () {
        return new MyContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Contact contact, int position, View itemView) {
                createDialog(contact, position).show();
            }
        };

    }

    private AlertDialog createDialog (final Contact contact, final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MyContactsActivity.this);
        builder.setTitle(R.string.dialog_friend_select_action)
                .setItems(R.array.contact_dialog_actions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                       // String[] actions = getResources().getStringArray(R.array.contact_dialog_actions);
                        onActionSelected(which, contact, position);
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }


    private void toggleContactsEmptyState() {
        if (myContactsAdapter.getItemCount() == 0)
            emptyState.setVisibility(View.VISIBLE);
        else
            emptyState.setVisibility(View.GONE);

    }


    public void showContctsDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new AddContactDialogFragment();
        dialog.show(getSupportFragmentManager(), "AddContactDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, final String username) {
        progressBar.toggleDialog(true);
        if(!db.isUserAlreadyInContacts(username) && !username.equals(user.name)){
            firebaseHelper.addUserToContacts(username, db, 0);
        } else {
            progressBar.toggleDialog(false);
            Toast.makeText(MyContactsActivity.this, "User cannot be added as they may already exist or it is your username", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
    }

    private void onActionSelected (int pos, Contact c, int itemLoc) {
        switch (pos) {
            case 0 : // see profile info
                Intent chatDetailActivity = new Intent(this, ChatDetailActivity.class);
                chatDetailActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                chatDetailActivity.putExtra("username", c.getContact().getUsername());
                startActivity(chatDetailActivity);
                break;

            case 1 : // chat with contact
                if (Network.isInternetAvailable(MyContactsActivity.this, true) && c.isActive()) {
                    progressBar.toggleDialog(true);
                    // checking where exists if it exists at all
                    ValueEventListener contactSingleEven = firebaseHelper.getValueEventListener(c.getContact().getUsername(), FirebaseHelper.NON_CONDITION, FirebaseHelper.CONDITION_3, FirebaseHelper.CONDITION_1, FirebaseUserModel.class);
                    firebaseHelper.toggleListenerFor("users", "username", c.getContact().getUsername(), contactSingleEven, true, true);
                    //firebaseHelper.setUpSingleChat("users", c.getContact().getUsername(), user.name);
                }
                break;
            case 2 : // delete contact
                if (db.removeContact(c.getContact().getUsername()) > 0) {
                    myContactsAdapter.updateState(itemLoc);
                    toggleContactsEmptyState();
                } else {
                    Log.i(TAG, "Failed to delete the user");
                }
                break;
        }
    }

    private void startChatActivity (String chatKey) {
        Intent chatIntent = new Intent(MyContactsActivity.this, ChatActivity.class);
        chatIntent.putExtra("chatKey", chatKey);
        chatIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(chatIntent);
        finish();
    }

    private String findChatKey (FirebaseUserModel userModel, FirebaseUserModel withUser) {
        String lChatKey = "";
        if (!userModel.getChatKeys().equals("")) {
            String[] allKeys  = userModel.getChatKeys().split(",");
            for(String key : allKeys) {
                String username1 = key.split("-")[0];
                String username2 = key.split("-")[1];
                if (username1.equals(withUser.getUsername()) || username2.equals(withUser.getUsername())) {
                    lChatKey = key;
                    break;
                }
            }
        }
        return lChatKey ;
    }


    private void createKeyAndSendInvitation (final Contact contact) {
        String newChatKey = user.name + "-" + contact.getContact().getUsername();
        firebaseHelper.updateChatKeyFromContact(contact, newChatKey , true, false);

    }

    private StringEntity generateEntity (Contact c) {
        JSONObject params = new JSONObject();
        //params.put("registration_ids", registration_ids);
        StringEntity entity = null;

        try {
            params.put("to", c.getContact().getDeviceToken());
            JSONObject payload = new JSONObject();
            payload.put("notification", user.name); // used for extra intent in main activity
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("click_action", ".MainActivity");
            notificationObject.put("body", getResources().getString(R.string.invitation_message));
            notificationObject.put("title", user.profileName);
            notificationObject.put("tag", user.deviceId);
            params.put("data", payload);
            params.put("notification", notificationObject);

            entity = new StringEntity(params.toString());
        }catch (Exception e) {
            e.printStackTrace();
        }

        return entity;
    }

    private void createBackgroundNotification (Contact c, final String chatKey) {
        StringEntity entity = generateEntity(c);
        if (entity == null){
            progressBar.toggleDialog(false);
            Toast.makeText(MyContactsActivity.this, "Failed to make background notification", Toast.LENGTH_SHORT).show();
            startChatActivity(chatKey);
            return;
        }

        Network.createAsyncClient().post(getApplicationContext(), Constants.NOTIFICATION_URL, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                progressBar.toggleDialog(false);
                Toast.makeText(MyContactsActivity.this, "Error sending background notification", Toast.LENGTH_SHORT).show();
                Log.i(TAG, responseString);
                startChatActivity(chatKey);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                progressBar.toggleDialog(false);
                Log.i(TAG, responseString);
                startChatActivity(chatKey);
            }
        });
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("updateLocalContactsFromFirebase")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    progressBar.toggleDialog(false);
                    myContactsAdapter.addNewItem(container.getContact().getContact());
                    break;
                case FirebaseHelper.CONDITION_2 :
                    progressBar.toggleDialog(false);
                    myContactsRecycler.setAdapter(myContactsAdapter);
                    toggleContactsEmptyState();
                    break;
            }
        } else if (tag.equals("addUserToContacts")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    myContactsAdapter.addNewItem(container.getUserModel());
                    toggleContactsEmptyState();
                    break;

                case FirebaseHelper.CONDITION_2 :
                    Toast.makeText(MyContactsActivity.this, "That contact does not exist", Toast.LENGTH_LONG).show();
                    break;
            }
            progressBar.toggleDialog(false);
        } else if (tag.equals("getValueEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    FirebaseUserModel friendModel  = (FirebaseUserModel) container.getObject();
                    selectedContact = new Contact(friendModel);
                    ValueEventListener contactSingleEven = firebaseHelper.getValueEventListener(user.name, FirebaseHelper.NON_CONDITION, FirebaseHelper.CONDITION_3, FirebaseHelper.CONDITION_2, FirebaseUserModel.class);
                    firebaseHelper.toggleListenerFor("users", "username", user.name, contactSingleEven, true, true);
                    break;

                case FirebaseHelper.CONDITION_2 :
                    Contact contact = selectedContact;
                    FirebaseUserModel currentUser = (FirebaseUserModel) container.getObject();
                    String currentUserChatKey = findChatKey(currentUser, contact.getContact()); // check user contains chat key with chosen contact
                    String contactChatKey = findChatKey(contact.getContact(), currentUser); // check chosen contact contains chat key with user
                    boolean isBlocked = Network.isBlockListed(user.name, contact.getContact().getBlockedUsers());
                    // both have same key
                    if (!contactChatKey.equals("") && !currentUserChatKey.equals("") && currentUserChatKey.equals(contactChatKey)) { // both have chat keys
                        progressBar.toggleDialog(false);
                        startChatActivity(contactChatKey);
                    } else if (!currentUserChatKey.equals("") && contactChatKey.equals("")) { // only user has key
                        progressBar.toggleDialog(false);
                        startChatActivity(currentUserChatKey);
                    } else if (currentUserChatKey.equals("") && !contactChatKey.equals("") && !isBlocked) { // only contact has key
                        firebaseHelper.updateChatKeyFromContact(contact, contactChatKey , false, false);
                    } else { // neither has keys or maybe opposite of each others key
                        if (!isBlocked) {
                            if (contactChatKey.equals("") && currentUserChatKey.equals("")) {
                                createKeyAndSendInvitation(contact);
                            } else {
                                firebaseHelper.notificationNodeExists(contact.getContact().getUsername(), currentUserChatKey, null);
                            }
                        } else {
                            Toast.makeText(this, getResources().getString(R.string.block_list_msg_blocked_by_them), Toast.LENGTH_SHORT).show();
                            progressBar.toggleDialog(false);
                        }
                    }
                    break;

                case FirebaseHelper.CONDITION_3:
                    Toast.makeText(MyContactsActivity.this, "Error finding user information", Toast.LENGTH_LONG).show();
                    progressBar.toggleDialog(false);
                    break;
            }
        }  else if (tag.equals("updateChatKeyFromContact")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    firebaseHelper.updateNotificationNode("chatKey", container.getContact().getContact(), container.getString());
                    break;

                case FirebaseHelper.CONDITION_2 :
                    firebaseHelper.removeNotificationNode(container.getContact().getContact().getUsername(), container.getString(), true);
                    break;
            }
        } else if (tag.equals("updateNotificationNode")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    createBackgroundNotification(container.getContact(), container.getString());
                    break;

                case FirebaseHelper.CONDITION_2:
                    progressBar.toggleDialog(false);
                    Toast.makeText(MyContactsActivity.this, "Failed to make a notification", Toast.LENGTH_SHORT).show();
                    startChatActivity(container.getString());
                    break;
            }
        } else if (tag.equals("removeNotificationNode")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    startChatActivity(container.getString());
                    break;
            }
        } else if (tag.equals("notificationNodeExists")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    startActivity(new Intent(MyContactsActivity.this, NotificationActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    break;

                case FirebaseHelper.CONDITION_2 :
                    startChatActivity(container.getString());
                    break;
            }
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {

        switch (tag) {
            case "updateLocalContactsFromFirebase" :
                toggleContactsEmptyState();
                break;

            case "getValueEventListener" :
                Toast.makeText(MyContactsActivity.this, "Error has occurred creating chat", Toast.LENGTH_LONG).show();
                break;

            case "updateChatKeyFromContact" :
                Toast.makeText(MyContactsActivity.this, "Error has occurred creating connection", Toast.LENGTH_LONG).show();
                break;

        }
        progressBar.toggleDialog(false);
        Log.i(TAG, tag + " "+ databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
       if (tag.equals("updateLocalContactsFromFirebase")) {
           switch (condition) {
               case FirebaseHelper.CONDITION_1 :
                   myContactsAdapter.addNewItem(container.getContact().getContact());
                   break;
           }
       }
    }
}

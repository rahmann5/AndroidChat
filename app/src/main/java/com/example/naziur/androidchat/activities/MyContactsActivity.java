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
import android.widget.TextView;
import android.widget.Toast;

import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.Contact;

import com.example.naziur.androidchat.fragment.AddContactDialogFragment;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.Notification;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

public class MyContactsActivity extends AppCompatActivity implements AddContactDialogFragment.ContactDialogListener{
    private static final String TAG = "MyContactsActivity";
    private ContactDBHelper db;
    private RecyclerView myContactsRecycler;
    private MyContactsAdapter myContactsAdapter;
    private TextView emptyState;
    private User user = User.getInstance();
    private DatabaseReference userRef;
    private ProgressDialog progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_contacts);

        setTitle("My Contacts");
        db = new ContactDBHelper(getApplicationContext());
        myContactsRecycler = (RecyclerView) findViewById(R.id.contacts_recycler);
        progressBar = new ProgressDialog(MyContactsActivity.this, R.layout.progress_dialog, true);
        emptyState = (TextView) findViewById(R.id.empty_contacts);
        userRef = FirebaseDatabase.getInstance().getReference("users");
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

        Cursor c = db.getAllMyContacts(null);
        if (c != null && c.getCount() > 0) {
            myContactsAdapter = new MyContactsAdapter(this, updateExistingContacts(c), setUpListener ());
            emptyState.setVisibility(View.GONE);
        } else {
            Log.i(TAG, "Found no items");
            emptyState.setVisibility(View.VISIBLE);
            myContactsAdapter = new MyContactsAdapter(this, setUpListener());
        }
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        myContactsRecycler.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                mLayoutManager.getOrientation());
        myContactsRecycler.addItemDecoration(dividerItemDecoration);
        myContactsRecycler.setAdapter(myContactsAdapter);
    }

    private List<Contact> updateExistingContacts (Cursor c) {
        progressBar.toggleDialog(true);
        final List<Contact> contacts = new ArrayList<>();
        boolean hasInternet = Network.isInternetAvailable(this, true);
        try{
            while (c.moveToNext()) {

                final FirebaseUserModel fbModel = new FirebaseUserModel();
                fbModel.setUsername(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME)));
                fbModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));

                if (hasInternet) {
                    // need one for profile picture
                    Query query = userRef.orderByChild("username").equalTo(fbModel.getUsername());
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                    FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                                    if(firebaseUserModel.getUsername().equals(fbModel.getUsername())) {
                                        db.updateProfile(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic());
                                        contacts.add(new Contact(firebaseUserModel));
                                        break;
                                    }
                                }
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            progressBar.toggleDialog(false);
                            Log.i(TAG, databaseError.getMessage());
                        }
                    });
                } else {
                    fbModel.setProfilePic(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC)));
                    contacts.add(new Contact(fbModel));
                }

            }
        } finally {
            if (!hasInternet) Toast.makeText(this, "Data maybe outdated", Toast.LENGTH_LONG).show();
            progressBar.toggleDialog(false);
            c.close();
        }
        return contacts;
    }

    private MyContactsAdapter.OnItemClickListener setUpListener () {
        return new MyContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Contact contact, int position) {
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

    public void showContctsDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new AddContactDialogFragment();
        dialog.show(getSupportFragmentManager(), "AddContactDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, final String username) {
        progressBar.toggleDialog(true);
        if(!db.isUserAlreadyInContacts(username) && !username.equals(user.name)){
            Query query = userRef.orderByChild("username").equalTo(username);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            //Getting the data from snapshot
                            FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);

                            if (firebaseUserModel.getUsername().equals(username)) {
                                Log.i(TAG, "Adding to contacts: " + firebaseUserModel.getUsername());
                                db.insertContact(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic(), firebaseUserModel.getDeviceToken());
                                myContactsAdapter.addNewItem(firebaseUserModel);
                                emptyState.setVisibility(View.GONE);
                                break;
                            }
                        }

                    } else {
                        Toast.makeText(MyContactsActivity.this, "That contact does not exist", Toast.LENGTH_LONG).show();
                    }
                    progressBar.toggleDialog(false);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    progressBar.toggleDialog(false);
                    Log.i(TAG, "Failed to add contact " + databaseError.getMessage());
                }
            });
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
                chatDetailActivity.putExtra("username", c.getContact().getUsername());
                startActivity(chatDetailActivity);
                break;

            case 1 : // chat with contact
                checkKeyExists(c);
                break;
            case 2 : // delete contact
                if (db.removeContact(c.getContact().getUsername()) > 0) {
                    myContactsAdapter.updateState(itemLoc);
                } else {
                    System.out.println("Failed to delete the user");
                }
                break;
        }
    }

    private void checkKeyExists (final Contact contact) {
        progressBar.toggleDialog(true);
        // get users latest chat keys
        userRef.orderByChild("username").equalTo(user.name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapShot : dataSnapshot.getChildren()) {
                    FirebaseUserModel currentUser = userSnapShot.getValue(FirebaseUserModel.class);
                    if (currentUser.getUsername().equals(user.name)) { // if user matched
                        String currentUserChatKey = findChatKey(currentUser, contact.getContact()); // check user contains chat key with chosen contact
                        String contactChatKey = findChatKey(contact.getContact(), currentUser); // check chosen contact contains chat key with user
                        // both have same key
                        if (!contactChatKey.equals("") && !currentUserChatKey.equals("") && currentUserChatKey.equals(contactChatKey)) { // both have chat keys
                            progressBar.toggleDialog(false);
                            startChatActivity(contactChatKey);
                        } else if (!currentUserChatKey.equals("") && contactChatKey.equals("")) { // only user has key
                            progressBar.toggleDialog(false);
                            startChatActivity(currentUserChatKey);
                        } else if (currentUserChatKey.equals("") && !contactChatKey.equals("")) { // only contact has key
                            updateChatKeyFromContact(contact, contactChatKey, false);
                        } else { // neither has keys
                            createKeyAndSendInvitation (contact);
                        }
                        break;
                    } else {
                        Toast.makeText(MyContactsActivity.this, "Could not match to user", Toast.LENGTH_LONG).show();
                        progressBar.toggleDialog(false);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.toggleDialog(false);
                Toast.makeText(MyContactsActivity.this, "Error has occurred creating chat", Toast.LENGTH_LONG).show();
                Log.i(TAG, databaseError.getMessage());
            }
        });
    }

    private void startChatActivity (String chatKey) {
        Intent chatIntent = new Intent(MyContactsActivity.this, ChatActivity.class);
        chatIntent.putExtra("chatKey", chatKey);
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

    private void updateChatKeyFromContact (final Contact c, final String chatKey, final boolean invite) {
        userRef.orderByChild("username").equalTo(user.name).getRef().runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
               for (MutableData data : mutableData.getChildren()) {
                   FirebaseUserModel currentUser = data.getValue(FirebaseUserModel.class);
                   if (currentUser == null) return Transaction.success(mutableData);
                   if (currentUser.getUsername().equals(user.name)) {
                       String currentKeys = currentUser.getChatKeys();
                       if (currentKeys.equals("")) {
                           currentKeys = chatKey;
                       } else {
                           currentKeys = currentKeys + "," + chatKey;
                       }

                       currentUser.setChatKeys(currentKeys);
                       data.setValue(currentUser);
                       break;
                   }
               }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    if (invite) {
                        createNotification(c, chatKey);
                    } else {
                        removeMyPendingNotifications(c, chatKey);
                    }
                } else {
                    progressBar.toggleDialog(false);
                    Toast.makeText(MyContactsActivity.this, "Error has occurred creating connection", Toast.LENGTH_LONG).show();
                    Log.i(TAG, databaseError.getMessage());
                }
            }
        });
    }

    private void removeMyPendingNotifications(final Contact contact, final String chatKey) {
        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(user.name);
        notificationRef.orderByChild("sender").equalTo(contact.getContact().getUsername()).getRef().runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    Notification notification = data.getValue(Notification.class);
                    if (notification == null) return Transaction.success(mutableData);

                    if (contact.getContact().getUsername().equals(notification.getSender())) {
                        notification = null;
                    }

                    data.setValue(notification);
                    break;
                }

                 return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                progressBar.toggleDialog(false);
                if (databaseError == null) {
                    startChatActivity(chatKey);
                } else {
                    Toast.makeText(MyContactsActivity.this, "Failed to remove pending invite notification", Toast.LENGTH_LONG).show();
                    Log.i(TAG, databaseError.getMessage());
                }
            }
        });
    }

    private void createKeyAndSendInvitation (final Contact contact) {
        // check for any same pending notification
        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(contact.getContact().getUsername());
        notificationRef.orderByChild("sender").equalTo(user.name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean invite = true;
                if (dataSnapshot.exists()) {
                    for (DataSnapshot notiSnapshot : dataSnapshot.getChildren()) {
                        Notification notification = notiSnapshot.getValue(Notification.class);
                        if (notification.getSender().equals(user.name)) {
                            invite = false;
                            break;
                        }
                    }
                }

                String newChatKey = user.name + "-" + contact.getContact().getUsername();
                updateChatKeyFromContact(contact, newChatKey , invite);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.toggleDialog(false);
                Toast.makeText(MyContactsActivity.this, "Error creating chat between users.", Toast.LENGTH_LONG).show();
                Log.i(TAG, databaseError.getMessage() );
            }
        });

    }

    private void createNotification (final Contact c, final String chatKey) {
        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(c.getContact().getUsername());
        Notification newNotification = new Notification();
        newNotification.setSender(user.name);
        newNotification.setChatKey(chatKey);
        notificationRef.push().setValue(newNotification).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
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

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.toggleDialog(false);
                Toast.makeText(MyContactsActivity.this, "Failed to make a notification", Toast.LENGTH_SHORT).show();
                startChatActivity(chatKey);
            }
        });
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
}

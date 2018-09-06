package com.example.naziur.androidchat.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.models.Contact;

import com.example.naziur.androidchat.fragment.AddContactDialogFragment;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.List;

public class MyContactsActivity extends AppCompatActivity implements AddContactDialogFragment.ContactDialogListener{
    private static final String TAG = "MyContactsActivity";
    private ContactDBHelper db;
    private RecyclerView myContactsRecycler;
    private MyContactsAdapter myContactsAdapter;
    private TextView emptyState;
    private User user = User.getInstance();
    private DatabaseReference userRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_contacts);

        setTitle("My Contacts");
        db = new ContactDBHelper(getApplicationContext());
        myContactsRecycler = (RecyclerView) findViewById(R.id.contacts_recycler);
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
        myContactsRecycler.setAdapter(myContactsAdapter);
    }

    private List<Contact> updateExistingContacts (Cursor c) {
        final List<Contact> contacts = new ArrayList<>();
        try{
            while (c.moveToNext()) {
                final FirebaseUserModel fbModel = new FirebaseUserModel();
                fbModel.setUsername(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME)));
                fbModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));
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

                    }
                });

            }
        } finally {
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
                        Log.i(TAG, "Contact doesn't exist");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.i(TAG, "Failed to add contact");
                }
            });
        } else {
            Log.i(TAG, "User cannot be added as they may already exist or it is your username");
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
                Intent chat = new Intent(MyContactsActivity.this, ChatActivity.class);
                chat.putExtra("username", c.getContact().getUsername());
                startActivity(chat);
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
}

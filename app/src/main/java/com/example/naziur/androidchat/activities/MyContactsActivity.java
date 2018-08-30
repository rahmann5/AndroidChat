package com.example.naziur.androidchat.activities;

import android.database.Cursor;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
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

        setUpList ();
    }



    private void setUpList () {
        Cursor c = db.getAllMyContacts(null);
        if (c != null && c.getCount() > 0) {
            Log.i(TAG, "Found "+ c.getCount() + " items");
            myContactsAdapter = new MyContactsAdapter(this,c);

        } else {
            Log.i(TAG, "Found no items");
            emptyState.setVisibility(View.VISIBLE);
            myContactsAdapter = new MyContactsAdapter(this);
        }
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        myContactsRecycler.setLayoutManager(mLayoutManager);
        myContactsRecycler.setAdapter(myContactsAdapter);
        emptyState.setVisibility(View.GONE);
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
}

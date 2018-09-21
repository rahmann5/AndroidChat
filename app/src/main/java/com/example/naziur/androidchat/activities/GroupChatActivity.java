package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChatActivity extends AppCompatActivity {
    private static final String TAG = "GroupChatActivity";
    User user = User.getInstance();

    EditText textComment;
    CircleImageView btnSend, btnMedia;
    FloatingActionButton sendBottom;
    FirebaseDatabase database;
    DatabaseReference messagesRef;

    private ActionBar actionBar;
    private ProgressDialog progressBar;
    private String groupKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Bundle extra = getIntent().getExtras();
        if (extra == null) {
            Toast.makeText(this, "Error occurred", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        database = FirebaseDatabase.getInstance();
        groupKey = extra.getString("group_uid");
        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        messagesRef = database.getReference("messages")
                .child("group")
                .child(groupKey);


        final ValueEventListener messageEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    // TO DO
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TO DO
            }
        };



        createCustomActionBar ();
        // assuming that user is guaranteed member of group
        if (Network.isInternetAvailable(this, true)) {
            database.getReference("groups").orderByChild("groupKey").equalTo(groupKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        FirebaseGroupModel groupModel = postSnapshot.getValue(FirebaseGroupModel.class);
                        if (groupModel.getGroupKey().equals(groupKey)) {
                            ((TextView) actionBar.getCustomView().findViewById(R.id.profile_name)).setText(groupModel.getTitle());
                            Glide.with(getApplicationContext())
                                    .load(groupModel.getPic())
                                    .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                                    .into(((CircleImageView) actionBar.getCustomView().findViewById(R.id.profile_icon)));
                            break;
                        }
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // TO DO
                }
            });
        } else {
            // TO DO
        }

    }

    private void createCustomActionBar () {
        actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.toolbar);
        actionBar.getCustomView().findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(GroupChatActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.view_details :
                break;
            case R.id.leave_group :
                leaveGroup();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void leaveGroup () {
        DatabaseReference groupRef = database.getReference("groups").orderByChild("groupKey").equalTo(groupKey).getRef();
        groupRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {

                for (MutableData data : mutableData.getChildren()) {
                    FirebaseGroupModel groupData = data.getValue(FirebaseGroupModel.class);

                    if (groupData == null) return Transaction.success(mutableData);

                    if (groupData.getGroupKey().equals(groupKey)) {
                        String [] membersNames = groupData.getMembers().split(",");
                        String newMembersList = "";
                        for (String username : membersNames) {
                            if (!username.equals(user.name))
                                newMembersList += (newMembersList.equals("")) ? username :  "," + username ;
                        }

                        groupData.setMembers(newMembersList);
                        data.setValue(groupData);
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    leaveGroupFromUser();
                } else {
                    Toast.makeText(GroupChatActivity.this, "Error leaving group", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, databaseError.getMessage());
                }
            }
        });
    }

    private void leaveGroupFromUser(){
        DatabaseReference userRef = database.getReference("users").orderByChild("username").equalTo(user.name).getRef();
        userRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {

                for(MutableData data : mutableData.getChildren()) {
                    FirebaseUserModel firebaseUser = data.getValue(FirebaseUserModel.class);

                    if (firebaseUser == null) return Transaction.success(mutableData);

                    if (firebaseUser.getUsername().equals(user.name)) {
                        String [] groupsKeys = firebaseUser.getGroupKeys().split(",");
                        String newKeys = "";
                        for (String gKey : groupsKeys) {
                            if (!gKey.equals(groupKey)) {
                                newKeys += (newKeys.equals(""))? gKey : ","  + gKey;
                            }
                        }

                        firebaseUser.setGroupKeys(newKeys);

                        data.setValue(firebaseUser);
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null){
                    Toast.makeText(GroupChatActivity.this, "Successfully left group", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(TAG, databaseError.getMessage());
                }
            }
        });
    }
}

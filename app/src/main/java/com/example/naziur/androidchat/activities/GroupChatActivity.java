package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
            database.getReference("groups").orderByChild(groupKey).equalTo(groupKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getKey().equals(groupKey)) {
                            FirebaseGroupModel groupModel = dataSnapshot.getValue(FirebaseGroupModel.class);
                            ((TextView) actionBar.getCustomView().findViewById(R.id.profile_name)).setText(groupModel.getTitle());
                            Glide.with(getApplicationContext())
                                    .load(groupModel.getPic())
                                    .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                                    .into(((CircleImageView) actionBar.getCustomView().findViewById(R.id.profile_icon)));

                        } else {
                            System.out.println("Key found is " + dataSnapshot.getKey());
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
                startActivity(new Intent(GroupChatActivity.this, SessionActivity.class));
                finish();
            }
        });
    }
}

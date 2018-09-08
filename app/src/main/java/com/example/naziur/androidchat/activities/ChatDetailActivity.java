package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.database.Cursor;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChatDetailActivity extends AppCompatActivity {

    private FirebaseUserModel user;
    private FirebaseDatabase database;
    private DatabaseReference userRef;
    private ContactDBHelper db;
    private boolean isInContacts;
    private FloatingActionButton fab;
    private Toolbar mToolbar;
    private Menu menu;
    private ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);
        db = new ContactDBHelper(this);
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");
        progressBar = new ProgressDialog(ChatDetailActivity.this, R.layout.progress_dialog, true);
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            user = new FirebaseUserModel();
            user.setUsername(extra.getString("username"));
        } else {
            Toast.makeText(this, "Error occurred", Toast.LENGTH_LONG).show();
            finish();
        }
        isInContacts = db.isUserAlreadyInContacts(user.getUsername());

        mToolbar = (Toolbar) findViewById(R.id.toolbar);


        if (Network.isInternetAvailable(this, true))
            getUsersInformationOnline();
        else if (!Network.isInternetAvailable(this, true) && isInContacts) {
            getUsersInformationOffline();
        } else {
            finish();
        }

        fab = (FloatingActionButton) findViewById(R.id.fab);
        if(!isInContacts) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addUserAsContact();
                    fab.setVisibility(View.GONE);
                }
            });
        }

        AppBarLayout mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if(!isInContacts) {
                    if (scrollRange == -1) {
                        scrollRange = appBarLayout.getTotalScrollRange();
                    }
                    if (scrollRange + verticalOffset == 0) {
                        isShow = true;
                        showOption(R.id.action_info);
                    } else if (isShow) {
                        isShow = false;
                        hideOption(R.id.action_info);
                    }
                }
            }
        });
    }

    private void addUserAsContact(){
        db.insertContact(user.getUsername(), user.getProfileName(), user.getProfilePic(), user.getDeviceToken());
        isInContacts = true;
        finish();
        startActivity(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.chat_detail_menu, menu);
        hideOption(R.id.action_info);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id){
            case R.id.action_profile:
                startActivity(new Intent(ChatDetailActivity.this, ProfileActivity.class));
                break;
            case R.id.action_contacts:
                startActivity(new Intent(ChatDetailActivity.this, MyContactsActivity.class));
                break;
            case R.id.action_info:
                addUserAsContact();
                fab.setVisibility(View.GONE);
                break;
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void hideOption(int id) {
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    private void showOption(int id) {
        if(menu != null) {
            MenuItem item = menu.findItem(id);
            item.setVisible(true);
        }
    }

    private void getUsersInformationOffline () {
        String[] profileAndPic= db.getProfileNameAndPic(user.getUsername());
        user.setProfileName(profileAndPic[0]);
        user.setProfilePic(profileAndPic[1]);
        user.setStatus(getResources().getString(R.string.status_available));
        putUserData();
    }

    private void getUsersInformationOnline() {
        progressBar.toggleDialog(true);
        userRef.orderByChild("username").equalTo(user.getUsername()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                    if (user.getUsername().equals(user.getUsername())){
                        user = firebaseUserModel;
                        putUserData();

                    }
                }
                progressBar.toggleDialog(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatDetailActivity.this, "Failed to retrieve latest information", Toast.LENGTH_SHORT).show();
                progressBar.toggleDialog(false);
            }
        });
    }

    private void putUserData() {
        TextView usernameTv = (TextView) findViewById(R.id.username_tv);
        TextView profileTv = (TextView) findViewById(R.id.profile_tv);
        TextView statusTv = (TextView) findViewById(R.id.status_tv);
        ImageView profilePicIv = (ImageView) findViewById(R.id.expandedImage);
        usernameTv.setText("Username: " + user.getUsername());
        profileTv.setText("Profile Name: " + user.getProfileName());
        statusTv.setText("Status: " + user.getStatus());
        mToolbar.setTitle(user.getProfileName());
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        invalidateOptionsMenu();
        Glide.with(ChatDetailActivity.this).load(user.getProfilePic()).apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown)).into(profilePicIv);
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}

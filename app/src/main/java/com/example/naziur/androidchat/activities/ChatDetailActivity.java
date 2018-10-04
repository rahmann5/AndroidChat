package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.activities.ProfileActivity;
import com.example.naziur.androidchat.adapter.AllGroupsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class ChatDetailActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = ChatDetailActivity.class.getSimpleName();
    private User user = User.getInstance();
    private FirebaseUserModel userBeingViewed;
    private ContactDBHelper db;
    private boolean isInContacts;
    private FloatingActionButton fab;
    private Toolbar mToolbar;
    private Menu menu;
    private ProgressDialog progressBar;
    FirebaseHelper firebaseHelper;
    private AllGroupsAdapter groupsAdapter;
    private List<String> groupKeys;
    private TextView emptyGroupsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);
        db = new ContactDBHelper(this);
        progressBar = new ProgressDialog(ChatDetailActivity.this, R.layout.progress_dialog, true);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            userBeingViewed = new FirebaseUserModel();
            userBeingViewed.setUsername(extra.getString("username"));
        } else {
            Toast.makeText(this, "Error occurred", Toast.LENGTH_LONG).show();
            finish();
        }
        isInContacts = db.isUserAlreadyInContacts(userBeingViewed.getUsername());

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        if (Network.isInternetAvailable(this, true))
            getUsersInformationOnline();
        else if (!Network.isInternetAvailable(this, true) && isInContacts) {
            getUsersInformationOffline(false);
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
        findRelatedGroups ();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.chat_detail_menu, menu);
        return true;
    }

    private void findRelatedGroups () {
        groupKeys = new ArrayList<>();
        groupsAdapter = new AllGroupsAdapter(this);
        emptyGroupsList = (TextView) findViewById(R.id.no_groups);
        RecyclerView myGroups = (RecyclerView) findViewById(R.id.chat_groups_list);
        LinearLayoutManager l = new LinearLayoutManager(ChatDetailActivity.this);
        myGroups.setLayoutManager(l);
        myGroups.setAdapter(groupsAdapter);
        ValueEventListener userListener = firebaseHelper.getValueEventListener(user.name, FirebaseHelper.CONDITION_1, FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION,FirebaseUserModel.class);
        firebaseHelper.toggleListenerFor("users", "username" , user.name, userListener, true, true); //  single event
    }

    private void getGroupInfo (String key) {
        ValueEventListener userListener = firebaseHelper.getValueEventListener(key, FirebaseHelper.CONDITION_3 , FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION, FirebaseGroupModel.class);
        firebaseHelper.toggleListenerFor("groups", "groupKey" , key, userListener, true, true); //  single event
    }

    private void isInSameGroup (FirebaseGroupModel groupModel) {
        String [] members  = groupModel.getMembers().split(",");
        for (String member : members) {
            if (member.equals(userBeingViewed.getUsername())){
                groupsAdapter.addGroupItem(groupModel);
                break;
            }else if (groupModel.getAdmin().equals(userBeingViewed.getUsername())) {
                groupsAdapter.addGroupItem(groupModel);
                break;
            }
        }
        if (groupsAdapter.getItemCount() == 0)
            emptyGroupsList.setVisibility(View.VISIBLE);
        else
            emptyGroupsList.setVisibility(View.GONE);

    }

    private void addUserAsContact(){
        db.insertContact(userBeingViewed.getUsername(), userBeingViewed.getProfileName(), userBeingViewed.getProfilePic(), userBeingViewed.getDeviceToken());
        isInContacts = true;
        finish();
        startActivity(getIntent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id){
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

    private void getUsersInformationOffline (boolean notExistsInServer) {
        if(isInContacts) {
            String[] profileAndPic = db.getProfileNameAndPic(userBeingViewed.getUsername());
            userBeingViewed.setProfileName(profileAndPic[0]);
            userBeingViewed.setProfilePic(profileAndPic[1]);
            userBeingViewed.setStatus(getResources().getString(R.string.status_available));
            putUserData();
        } else if(notExistsInServer){
            Toast.makeText(this, "This userBeingViewed may not exist anymore", Toast.LENGTH_SHORT);
            finish();
        }
    }

    private void getUsersInformationOnline() {
        progressBar.toggleDialog(true);
        firebaseHelper.getOnlineInfoForUser(userBeingViewed.getUsername());
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    private void putUserData() {
        TextView usernameTv = (TextView) findViewById(R.id.username_tv);
        TextView profileTv = (TextView) findViewById(R.id.profile_tv);
        TextView statusTv = (TextView) findViewById(R.id.status_tv);
        ImageView profilePicIv = (ImageView) findViewById(R.id.expandedImage);
        usernameTv.setText("Username: " + userBeingViewed.getUsername());
        profileTv.setText("Profile Name: " + userBeingViewed.getProfileName());
        statusTv.setText("Status: " + userBeingViewed.getStatus());
        mToolbar.setTitle(userBeingViewed.getProfileName());
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        invalidateOptionsMenu();
        Glide.with(ChatDetailActivity.this).load(userBeingViewed.getProfilePic()).apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown)).into(profilePicIv);
    }

    private void hideOption(int id) {
        if(menu != null) {
            MenuItem item = menu.findItem(id);
            item.setVisible(false);
        }
    }

    private void showOption(int id) {
        if(menu != null) {
            MenuItem item = menu.findItem(id);
            item.setVisible(true);
        }
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch(tag){
            case "getOnlineInfoForUser":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        userBeingViewed = container.getUserModel();
                        putUserData();
                        break;
                    case FirebaseHelper.CONDITION_2:
                        getUsersInformationOffline(true);
                        break;
                }
                progressBar.toggleDialog(false);
                break;
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag){
            case "getOnlineInfoForUser":
                Toast.makeText(ChatDetailActivity.this, "Failed to retrieve latest information", Toast.LENGTH_SHORT).show();
                Log.i(TAG, tag+": "+databaseError.getMessage());
                progressBar.toggleDialog(false);
                break;
        }
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        switch (tag) {
            case "getValueEventListener" :
                switch (condition) {
                    case FirebaseHelper.CONDITION_1 :
                        FirebaseUserModel currentUser = (FirebaseUserModel) container.getObject();
                        String[] allKeys = currentUser.getGroupKeys().split(",");
                        for(String key: allKeys){
                            if(!key.equals(""))
                                groupKeys.add(key);
                        }
                        if (!groupKeys.isEmpty())
                            getGroupInfo(groupKeys.get(0)); // first item
                        else
                            emptyGroupsList.setVisibility(View.VISIBLE);

                        break;

                    case FirebaseHelper.CONDITION_3 :
                        FirebaseGroupModel groupModel = (FirebaseGroupModel) container.getObject();
                        isInSameGroup(groupModel);
                        int currentIndex = groupKeys.indexOf(groupModel.getGroupKey());
                        if ((currentIndex + 1) < groupKeys.size())
                            getGroupInfo(groupKeys.get(currentIndex + 1)); // subsequent item
                        break;
                }
        }

    }
}
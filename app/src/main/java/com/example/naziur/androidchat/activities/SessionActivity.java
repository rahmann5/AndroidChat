package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.SessionFragmentPagerAdapter;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.fragment.GroupSessionFragment;
import com.example.naziur.androidchat.fragment.SingleSessionFragment;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.NetworkChangeReceiver;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import static android.R.attr.fragment;

public class SessionActivity extends AuthenticatedActivity implements NetworkChangeReceiver.OnNetworkStateChangeListener, FirebaseHelper.FirebaseHelperListener{
    private static final String TAG = "SessionActivity";
    private NetworkChangeReceiver networkChangeReceiver;
    ViewPager viewPager;
    private Menu menu;
    private ValueEventListener notificationListener;
    private FloatingActionButton startChat;

    SessionFragmentPagerAdapter sessionFragmentPagerAdapter;
    private FirebaseHelper firebaseHelper;
    private int pos = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        sessionFragmentPagerAdapter = new SessionFragmentPagerAdapter(getSupportFragmentManager());
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        startChat = (FloatingActionButton) findViewById(R.id.start_chat);

        startChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SessionActivity.this, MyContactsActivity.class));
            }
        });

        viewPager.setAdapter(sessionFragmentPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        notificationListener = firebaseHelper.getNotificationChecker(null, null);


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Fragment fragment =(Fragment)sessionFragmentPagerAdapter.getRegisteredFragment(position);
                pos = position;
                if(menu != null) {
                    if (fragment instanceof GroupSessionFragment){
                        menu.findItem(R.id.action_group).setVisible(true);
                        startChat.setVisibility(View.GONE);
                    } else {
                        menu.findItem(R.id.action_group).setVisible(false);
                        startChat.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        networkChangeReceiver = new NetworkChangeReceiver();
        networkChangeReceiver.setOnNetworkChangedListener(this);

    }


    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(networkChangeReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sessions_menu, menu);
        Fragment fragment = sessionFragmentPagerAdapter.getRegisteredFragment(pos);
        if (fragment instanceof GroupSessionFragment){
            startChat.setVisibility(View.GONE);
        } else {
            MenuItem groupItem = menu.findItem(R.id.action_group);
            groupItem.setVisible(false);
            startChat.setVisibility(View.VISIBLE);
        }
           return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.my_contacts:
                startActivity(new Intent(SessionActivity.this, MyContactsActivity.class));
                return true;
            case R.id.my_profile:
                startActivity(new Intent(SessionActivity.this, ProfileActivity.class));
                return true;
            case R.id.settings:
                startActivity(new Intent(SessionActivity.this, SettingsActivity.class));
                return true;
            case R.id.action_notification:
                startActivity(new Intent(SessionActivity.this, NotificationActivity.class));
                return true;
            case R.id.action_group:
                startActivity(new Intent(SessionActivity.this, GroupCreatorActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseHelper.notificationNodeExists(null, null, notificationListener);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        FirebaseHelper.removeNotificationListener(notificationListener);
        super.onStop();

    }

    @Override
    public void onNetworkStateChanged(boolean connected) {
        if(connected) {
            sessionFragmentPagerAdapter.getItemPosition(sessionFragmentPagerAdapter.getCurrentFragment());
            sessionFragmentPagerAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("notificationNodeExists")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                case FirebaseHelper.CONDITION_2 :
                    if (menu != null) {
                        final MenuItem notificationItem = menu.findItem(R.id.action_notification);
                        if (container.getBoolean()) {
                            notificationItem.setIcon(R.drawable.ic_action_alert_notification);
                        } else {
                            notificationItem.setIcon(R.drawable.ic_action_notification);
                        }
                    }

                    break;
            }
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag) {
            case "notificationNodeExists" :
                Log.i(TAG, databaseError.getMessage());
                break;
        }
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

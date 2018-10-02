package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
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

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.SessionFragmentPagerAdapter;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.fragment.GroupSessionFragment;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.NetworkChangeReceiver;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class SessionActivity extends AppCompatActivity implements NetworkChangeReceiver.OnNetworkStateChangeListener, FirebaseHelper.FirebaseHelperListener{
    private static final String TAG = "SessionActivity";
    private User user = User.getInstance();
    private NetworkChangeReceiver networkChangeReceiver;
    ViewPager viewPager;
    private Menu menu;
    private ValueEventListener notificationListener;

    SessionFragmentPagerAdapter sessionFragmentPagerAdapter;
    private FirebaseHelper firebaseHelper;
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
                if(menu != null) {
                    if (fragment instanceof GroupSessionFragment)
                        menu.findItem(R.id.action_group).setVisible(true);
                    else
                        menu.findItem(R.id.action_group).setVisible(false);
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
        MenuItem groupItem = menu.findItem(R.id.action_group);
        groupItem.setVisible(false);


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
            case R.id.settings2:
            startActivity(new Intent(SessionActivity.this, SettingsActivity2.class));
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

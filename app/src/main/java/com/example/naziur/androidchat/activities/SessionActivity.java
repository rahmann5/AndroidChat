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
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.SessionFragmentPagerAdapter;
import com.example.naziur.androidchat.fragment.GroupSessionFragment;
import com.example.naziur.androidchat.fragment.SingleSessionFragment;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.NetworkChangeReceiver;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SessionActivity extends AppCompatActivity implements NetworkChangeReceiver.OnNetworkStateChangeListener {
    private static final String TAG = "SessionActivity";
    private User user = User.getInstance();
    private NetworkChangeReceiver networkChangeReceiver;
    ViewPager viewPager;
    private Menu menu;
    private ValueEventListener notificationListener;
    private DatabaseReference notificationRef;

    SessionFragmentPagerAdapter sessionFragmentPagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

       viewPager = (ViewPager) findViewById(R.id.viewpager);
        sessionFragmentPagerAdapter = new SessionFragmentPagerAdapter(getSupportFragmentManager());

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("All Chats");
        setSupportActionBar(mToolbar);

        viewPager.setAdapter(sessionFragmentPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

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
        final MenuItem notificationItem = menu.findItem(R.id.action_notification);
        groupItem.setVisible(false);

        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    notificationItem.setIcon(R.drawable.ic_action_alert_notification);
                } else {
                    notificationItem.setIcon(R.drawable.ic_action_notification);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i(TAG, databaseError.getMessage());
            }
        };


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
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
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

        notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(user.name);
        // could be improved with non-single event value listener in the case of receiving first notification
        notificationRef.addValueEventListener(notificationListener);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationRef != null)
            notificationRef.removeEventListener(notificationListener);

    }

    @Override
    public void onNetworkStateChanged(boolean connected) {
        if(connected) {
            sessionFragmentPagerAdapter.getItemPosition(new SingleSessionFragment());
            sessionFragmentPagerAdapter.notifyDataSetChanged();
        }
    }


}

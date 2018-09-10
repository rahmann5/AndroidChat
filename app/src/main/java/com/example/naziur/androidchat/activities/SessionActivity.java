package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.SessionFragmentPagerAdapter;
import com.example.naziur.androidchat.fragment.SingleSessionFragment;
import com.example.naziur.androidchat.utils.NetworkChangeReceiver;

public class SessionActivity extends AppCompatActivity implements NetworkChangeReceiver.OnNetworkStateChangeListener {
    private NetworkChangeReceiver networkChangeReceiver;
    ViewPager viewPager;
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sessions_menu, menu);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    public void onNetworkStateChanged(boolean connected) {
        if(connected) {
            sessionFragmentPagerAdapter.getItemPosition(new SingleSessionFragment());
            sessionFragmentPagerAdapter.notifyDataSetChanged();
        }
    }


}

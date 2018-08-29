package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.SessionFragmentPagerAdapter;

public class SessionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        SessionFragmentPagerAdapter sessionFragmentPagerAdapter = new SessionFragmentPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(sessionFragmentPagerAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.chat:
                startActivity(new Intent(SessionActivity.this, MyContactsActivity.class));
                return true;
            case R.id.my_contacts:
                startActivity(new Intent(SessionActivity.this, ChatActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

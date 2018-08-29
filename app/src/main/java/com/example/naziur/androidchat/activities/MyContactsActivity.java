package com.example.naziur.androidchat.activities;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.naziur.androidchat.Database.ContactDBHelper;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;

public class MyContactsActivity extends AppCompatActivity {

    private ContactDBHelper db;
    private RecyclerView myContactsRecycler;
    private MyContactsAdapter myContactsAdapter;
    private TextView emptyState;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_contacts);

        setTitle("My Contacts");
        db = new ContactDBHelper(getApplicationContext());
        myContactsRecycler = (RecyclerView) findViewById(R.id.contacts_recycler);
        myContactsRecycler = (RecyclerView) findViewById(R.id.contacts_recycler);
        emptyState = (TextView) findViewById(R.id.empty_contacts);
        setUpList ();
    }

    private void setUpList () {
        Cursor c = db.getAllMyContacts(null);
        if (c != null && c.getCount() > 0) {
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
            myContactsAdapter = new MyContactsAdapter(this,c);
            myContactsRecycler.setLayoutManager(mLayoutManager);
            myContactsRecycler.setAdapter(myContactsAdapter);
        } else {
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }
}

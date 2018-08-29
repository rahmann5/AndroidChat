package com.example.naziur.androidchat.activities;

import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.models.Contact;

import java.util.Arrays;
import java.util.List;

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
            myContactsAdapter = new MyContactsAdapter(this, c, new MyContactsAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(Contact contact) {
                    createDialog(contact).show();
                }
            });
            myContactsRecycler.setLayoutManager(mLayoutManager);
            myContactsRecycler.setAdapter(myContactsAdapter);

            emptyState.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    private AlertDialog createDialog (final Contact contact) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MyContactsActivity.this);
        builder.setTitle(R.string.dialog_friend_select_action)
                .setItems(R.array.contact_dialog_actions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String[] actions = getResources().getStringArray(R.array.contact_dialog_actions);

                        dialog.dismiss();
                    }
                });
        return builder.create();
    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }
}

package com.example.naziur.androidchat.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
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

import com.example.naziur.androidchat.fragment.AddContactDialogFragment;

public class MyContactsActivity extends AppCompatActivity implements AddContactDialogFragment.ContactDialogListener{

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
        FloatingActionButton floatingActionButton  = (FloatingActionButton) findViewById(R.id.add_contact);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        //db.insertContact("bob", "bob", "", "daaeb0_jNOg:APA91bFsiK-Oh7DjJOrybGluNlzST9eBy9Ag639MYXdkeub4DHzGzQ1ISzpxL4U82EKOIr4NIsvrUbbJ0wZx4LxV4puJK5yHW02EEshfl4KZJhmYFkZyIZu5Jks4Pyb1Zw8CzhxWtpC1");
        setUpList ();
    }



    private void setUpList () {
        Cursor c = db.getAllMyContacts(null);
        if (c != null && c.getCount() > 0) {
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
            myContactsAdapter = new MyContactsAdapter(this, c, new MyContactsAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(Contact contact, int position) {
                    createDialog(contact, position).show();
                }
            });
            myContactsRecycler.setLayoutManager(mLayoutManager);
            myContactsRecycler.setAdapter(myContactsAdapter);

            emptyState.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    private AlertDialog createDialog (final Contact contact, final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MyContactsActivity.this);
        builder.setTitle(R.string.dialog_friend_select_action)
                .setItems(R.array.contact_dialog_actions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                       // String[] actions = getResources().getStringArray(R.array.contact_dialog_actions);
                        onActionSelected(which, contact, position);
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

    public void showNoticeDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new AddContactDialogFragment();
        dialog.show(getSupportFragmentManager(), "AddContactDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
    }

    private void onActionSelected (int pos, Contact c, int itemLoc) {
        switch (pos) {
            case 0 : // see profile info
                break;

            case 1 : // chat with contact
                Intent chat = new Intent(MyContactsActivity.this, ChatActivity.class);
                chat.putExtra("username", c.getContact().getUsername());
                startActivity(chat);
                break;
            case 2 : // delete contact
                if (db.removeContact(c.getContact().getUsername()) > 0) {
                    myContactsAdapter.updateState(itemLoc);
                } else {
                    System.out.println("Failed to delete the user");
                }
                break;
        }
    }
}

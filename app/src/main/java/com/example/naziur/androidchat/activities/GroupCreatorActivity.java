package com.example.naziur.androidchat.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.models.Contact;

import java.util.List;

public class GroupCreatorActivity extends AppCompatActivity {

    private List<Contact> myContacts;
    private List<String> newUsersNotInContacts;
    private RecyclerView recyclerView;
    private MyContactsAdapter myContactsAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_creator);

        EditText groupNameEt = (EditText) findViewById(R.id.group_name);
        EditText searchedUsernameEt = (EditText) findViewById(R.id.user_not_in_contacts);
        ImageButton searchAddUserBtn = (ImageButton) findViewById(R.id.search_add_contact);
        Button createGroupBtn = (Button) findViewById(R.id.make_group_btn);
        ContactDBHelper contactDbHelper = new ContactDBHelper(this);

        recyclerView = (RecyclerView) findViewById(R.id.chat_groups_list);
        //myContactsAdapter = new MyContactsAdapter(this, )

    }
}

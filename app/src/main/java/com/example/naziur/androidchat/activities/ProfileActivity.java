package com.example.naziur.androidchat.activities;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.utils.FadingActionBarHelper;
import com.example.naziur.androidchat.models.User;

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    User user = User.getInstance();
    private ListView myGroups, myContacts;
    private TextView emptyGroupsList, emptyContactsList;
    private AppCompatButton saveButton;
    private ImageView editToggle, prodilePic;
    private ImageButton updatePic;
    private AppCompatEditText profileName;
    private Spinner profileStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(user.profileName);
        FadingActionBarHelper helper = new FadingActionBarHelper()
                .actionBarBackground(R.color.colorPrimaryDark)
                .headerLayout(R.layout.header)
                .contentLayout(R.layout.activity_profile)
                .allowHeaderTouchEvents(true);
        setContentView(helper.createView(this));
        helper.initActionBar(this);

        prodilePic = (ImageView) findViewById(R.id.image_header);
        updatePic = (ImageButton) findViewById(R.id.update_profile_pic);
        updatePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ProfileActivity.this, "Updating profile picture", Toast.LENGTH_SHORT).show();
            }
        });

        profileStatus = (Spinner) findViewById(R.id.prof_status);

        profileName = (AppCompatEditText) findViewById(R.id.edit_prof_name);
        profileName.setText(user.profileName);
        editToggle = (ImageView) findViewById(R.id.edit_button_prof_name);

        saveButton = (AppCompatButton) findViewById(R.id.save_profile_btn);
        ArrayAdapter<String> adapterContacts = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        emptyContactsList = (TextView) findViewById(R.id.no_contacts);
        myContacts = (ListView) findViewById(R.id.profile_contacts_list);
        myContacts.setAdapter(adapterContacts);
        myContacts.setEmptyView(emptyContactsList);

        emptyGroupsList = (TextView) findViewById(R.id.no_groups);
        ArrayAdapter<String> adapterGroup = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        myGroups = (ListView) findViewById(R.id.profile_groups_list);
        myGroups.setAdapter(adapterGroup);
        myGroups.setEmptyView(emptyGroupsList);

        editToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!profileName.isEnabled()) {
                    profileName.setEnabled(true);
                } else {
                    profileName.setEnabled(false);
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ProfileActivity.this, "Saving changes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }


}

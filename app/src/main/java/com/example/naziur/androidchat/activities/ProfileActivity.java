package com.example.naziur.androidchat.activities;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.User;

public class ProfileActivity extends AppCompatActivity {

    User user = User.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setTitle(user.profileName);
    }


}

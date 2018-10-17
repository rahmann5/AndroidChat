package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.example.naziur.androidchat.database.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by Hamidur on 07/10/2018.
 */

public abstract class AuthenticatedActivity extends AppCompatActivity {
    protected FirebaseAuth mAuth;
    protected DatabaseReference  database;
    protected boolean controlOffline;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        database= FirebaseHelper.setOnlineStatusListener(mAuth.getCurrentUser().getUid(), false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (controlOffline)
         database.child("online").setValue(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //checkUserAuthenticated ();
        controlOffline = true;
    }


    private void checkUserAuthenticated () {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        } else {
            database.child("online").setValue(true);
        }
    }
}

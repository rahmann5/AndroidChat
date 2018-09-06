package com.example.naziur.androidchat.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;


import com.example.naziur.androidchat.R;

public class MainActivity extends AppCompatActivity {

    String currentDeviceId;

    User user = User.getInstance();

    FirebaseDatabase database;
    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        user.sharedpreferences = getSharedPreferences(user.appPreferences, Context.MODE_PRIVATE);

        currentDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        usersRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                boolean fail = true;
                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    //Getting the data from snapshot
                    FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);

                    if (firebaseUserModel.getDeviceId().equals(currentDeviceId)) {
                        fail = false;
                        firebaseUserModel.setDeviceToken(FirebaseInstanceId.getInstance().getToken());
                        user.login(firebaseUserModel);
                        user.saveFirebaseKey(userSnapshot.getKey());
                        moveToSessionScreen();
                        break;
                    }
                }

                if (fail) {
                    moveToLoginActivity ();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                moveToLoginActivity ();
                System.out.println("The read failed: " + databaseError.getMessage());
            }
        });

    }

    public void moveToSessionScreen() {
        //Intent intent = new Intent(this, ChatActivity.class);
        Intent intent;
        Bundle extra = getIntent().getExtras();
        if (extra != null && extra.getString("sender") != null) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("username", extra.getString("sender"));
        } else {
            intent = new Intent(this, SessionActivity.class);
            System.out.println("No Extra");
        }
        startActivity(intent);
        finish();
    }

    private void moveToLoginActivity () {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }



}

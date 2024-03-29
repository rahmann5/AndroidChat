package com.example.naziur.androidchat.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;

import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;


import com.example.naziur.androidchat.R;

public class MainActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = "MainActivity";
    String currentDeviceId;
    private FirebaseHelper firebaseHelper;
    User user = User.getInstance();
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Launcher);
        super.onCreate(savedInstanceState);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        if (!Network.isInternetAvailable(this, true)) {
            moveToLoginActivity ();
            return;
        }

        mAuth = FirebaseAuth.getInstance();



    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        user.sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Check if user is signed in (non-null) and update UI accordingly.
        if (user.getAutoLogin(this) && !user.getUserAuthentication(this).equals("")) {

            currentDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            if (currentUser != null) {
                firebaseHelper.autoLogin("users", currentDeviceId, user);
            } else {
                mAuth.signInWithEmailAndPassword(user.getUserAuthentication(this), currentDeviceId).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            firebaseHelper.autoLogin("users", currentDeviceId, user);
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to authenticate user.", Toast.LENGTH_SHORT).show();
                            moveToLoginActivity();
                        }

                    }
                });
            }
        }
        else {
            if (currentUser != null) mAuth.signOut();
            moveToLoginActivity();
        }
    }

    public void moveToSessionScreen() {
        //Intent intent = new Intent(this, ChatActivity.class);
        Intent intent = null;
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            if (extra.getString("chatKey") != null) {
                intent = new Intent(this, ChatActivity.class);
                intent.putExtra("chatKey", extra.getString("chatKey"));
            } else if (extra.getString("notification") != null) {
                intent = new Intent(this, NotificationActivity.class);
                intent.putExtra("notification", extra.getString("notification"));
            } else if(extra.getString("group_uid") != null){
                intent = new Intent(this, GroupChatActivity.class);
                intent.putExtra("group_uid", extra.getString("group_uid"));
            }
        }
        if (intent == null) {
            intent = new Intent(this, SessionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        startActivity(intent);
        finish();
    }

    private void moveToLoginActivity () {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("autoLogin")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    moveToSessionScreen();
                    break;

                case FirebaseHelper.CONDITION_2 :
                    moveToLoginActivity ();
                    break;

                case FirebaseHelper.CONDITION_3 :
                    firebaseHelper.updateUserDeviceToken(container.getString());
                    break;
            }
        } else if (tag.equals("updateUserDeviceToken")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    moveToSessionScreen();
                    break;

                case FirebaseHelper.CONDITION_2 :
                    Toast.makeText(this, "Failed to register new device token, cannot receive notification.", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, tag + ": Failed to update new device token " + container.getString());
                    break;
            }
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        if (tag.equals("autoLogin")) {
            moveToLoginActivity ();
        }
        Log.i(TAG, tag + " "+ databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        // not required
    }
}

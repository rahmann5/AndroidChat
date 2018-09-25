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

        user.sharedpreferences = getSharedPreferences(user.appPreferences, Context.MODE_PRIVATE);

        currentDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        firebaseHelper.autoLogin("users", currentDeviceId, user);

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
            }
        }
        if (intent == null) {
            intent = new Intent(this, SessionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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


    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch (condition) {
            case FirebaseHelper.CONDITION_1 :
                moveToSessionScreen();
                break;

            case FirebaseHelper.CONDITION_2 :
                moveToLoginActivity ();
                break;
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        moveToLoginActivity ();
        Log.i(TAG, tag + " "+ databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        // not required
    }
}

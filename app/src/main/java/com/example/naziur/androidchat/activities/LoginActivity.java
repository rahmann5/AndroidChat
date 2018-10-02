package com.example.naziur.androidchat.activities;


import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = "LoginActivity";
    private EditText editTextUsername, editTextProfileName;
    User user = User.getInstance();
    private FirebaseDatabase database;
    private String currentDeviceId;
    private ProgressDialog progressDialog;
    private FirebaseHelper firebaseHelper;
    private CheckBox autoLog;
    private TextView forgotUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        progressDialog = new ProgressDialog(this, R.layout.progress_dialog, true);
        user.sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        currentDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        database = FirebaseDatabase.getInstance();

        editTextUsername = (EditText) findViewById(R.id.editTextUsername);
        editTextProfileName = (EditText) findViewById(R.id.editTextProfileName);

        autoLog = (CheckBox) findViewById(R.id.auto_log);
        autoLog.setChecked(user.getAutoLogin());

        forgotUsername = (TextView) findViewById(R.id.forgot_username);
        forgotUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Network.isInternetAvailable(LoginActivity.this, true)) return;
                progressDialog.toggleDialog(true);
                firebaseHelper.autoLogin("users", currentDeviceId, user);
            }
        });
    }

    public void btnLoginTapped(View view) {

        if (!Network.isInternetAvailable(this, true)) return;

        final String strUsername = editTextUsername.getText().toString().trim();
        final String strProfileName = editTextProfileName.getText().toString().trim();

        if (strUsername.isEmpty() && strUsername.matches("[a-zA-Z0-9]*")) {
            Toast.makeText(this, "The username cannot be empty and made of only numbers and letters", Toast.LENGTH_LONG).show();
        } else {

            if (!Network.isInternetAvailable(this, true)) {
                return;
            }

            progressDialog.toggleDialog(true);
            String currentDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            firebaseHelper.manualLogin(user, strUsername, strProfileName, currentDeviceId);
        }
    }


    private void addUserToDatabase(final String strUsername, String strProfileName){
        final FirebaseUserModel firebaseUserModel = new FirebaseUserModel();
        firebaseUserModel.setUsername(strUsername);
        firebaseUserModel.setProfileName(strProfileName);
        firebaseUserModel.setStatus(getResources().getString(R.string.status_available));
        firebaseUserModel.setDeviceId(currentDeviceId);
        firebaseUserModel.setDeviceToken(FirebaseInstanceId.getInstance().getToken());
        firebaseHelper.registerNewUser(firebaseUserModel);
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch (tag){
            case "manualLogin":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        /*Container: 0=>profile name, 1=> username*/
                        if (!container.getStringList().get(0).isEmpty())
                            addUserToDatabase(container.getStringList().get(1), container.getStringList().get(0));
                        else
                            Toast.makeText(LoginActivity.this, "Please enter a profile name", Toast.LENGTH_LONG).show();
                        break;
                    case FirebaseHelper.CONDITION_2:
                        container.getUserModel().setDeviceToken(FirebaseInstanceId.getInstance().getToken());
                        user.login(container.getUserModel());
                        break;
                    case FirebaseHelper.CONDITION_3:
                        Toast.makeText(LoginActivity.this, "Device Id do not match", Toast.LENGTH_LONG).show();
                        break;
                    case FirebaseHelper.CONDITION_4:
                        Toast.makeText(LoginActivity.this, "Please enter unique username", Toast.LENGTH_LONG).show();
                        break;
                    case FirebaseHelper.CONDITION_5:
                        user.setAutoLogin(autoLog.isChecked());
                        startActivity(new Intent(LoginActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        finish();
                        break;
                }
                break;
            case "registerNewUser":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        if (user.login(container.getUserModel())) {
                            Intent intent = new Intent(LoginActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            user.setAutoLogin(autoLog.isChecked());
                            startActivity(intent);
                            finish();
                        }
                        break;
                }
                break;

            case "autoLogin" :
                switch (condition) {
                    case FirebaseHelper.CONDITION_1 :
                        Intent intent = new Intent(LoginActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        user.setAutoLogin(autoLog.isChecked());
                        startActivity(intent);
                        finish();
                    break;

                    case FirebaseHelper.CONDITION_2 :
                        Toast.makeText(LoginActivity.this, "Error: This device maybe new therefore please register again.", Toast.LENGTH_LONG).show();
                        break;

                }


        }
        progressDialog.toggleDialog(false);
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag) {
            case "manualLogin":
                break;
            case "registerNewUser":
                Toast.makeText(LoginActivity.this, "Failed to register user please try again.", Toast.LENGTH_LONG).show();
                break;
        }
        progressDialog.toggleDialog(false);
        Log.i(TAG, tag+": "+databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

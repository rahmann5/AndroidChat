package com.example.naziur.androidchat.activities;


import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
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

public class LoginActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = "LoginActivity";
    private EditText editTextUsername, editTextEmail;
    User user = User.getInstance();
    private String currentDeviceId;
    private ProgressDialog progressDialog;
    private FirebaseHelper firebaseHelper;
    private CheckBox autoLog;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        progressDialog = new ProgressDialog(this, R.layout.progress_dialog, true);
        user.sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        currentDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        mAuth = FirebaseAuth.getInstance();

        editTextUsername = (EditText) findViewById(R.id.editTextUsername);
        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextEmail.setText(user.getUserAuthentication(this));
        TextView autoLogText = (TextView) findViewById(R.id.auto_log_text);
        autoLogText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (autoLog.isChecked()) {
                    autoLog.setChecked(false);
                } else {
                    autoLog.setChecked(true);
                }
            }
        });

        autoLog = (CheckBox) findViewById(R.id.auto_log);
        autoLog.setChecked(user.getAutoLogin(this));

        TextView register = (TextView) findViewById(R.id.register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();

            }
        });

        TextView forgotUsername = (TextView) findViewById(R.id.forgot_username);
        forgotUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!editTextEmail.getText().toString().trim().equals("")) {
                    if (!Network.isInternetAvailable(LoginActivity.this, true)) return;
                    progressDialog.toggleDialog(true);
                    String email = editTextEmail.getText().toString().trim();
                    mAuth.signInWithEmailAndPassword(email, currentDeviceId)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                firebaseHelper.autoLogin("users", currentDeviceId, user);
                            } else {
                                progressDialog.toggleDialog(false);
                                Toast.makeText(LoginActivity.this, "Email provided does not match.", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                } else {
                    Toast.makeText(LoginActivity.this, "Please enter your email address.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void btnLoginTapped(View view) {

        if (!Network.isInternetAvailable(this, true)) return;

        final String strUsername = editTextUsername.getText().toString().trim();
        final String strEmail = editTextEmail.getText().toString().trim();

        if (!inputVerification(strUsername, strEmail)) {
            Toast.makeText(this, "Username can only contain numbers and letter and both fields cannot be empty", Toast.LENGTH_LONG).show();
        } else {

            if (!Network.isInternetAvailable(this, true)) {
                return;
            }

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                progressDialog.toggleDialog(true);
                firebaseHelper.manualLogin(strUsername, currentDeviceId);
                mAuth.signInWithEmailAndPassword(strEmail, currentDeviceId).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            firebaseHelper.manualLogin(strUsername, currentDeviceId);
                        } else {
                            progressDialog.toggleDialog(false);
                            Toast.makeText(LoginActivity.this, "Failed to authenticate user.", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            } else {
                progressDialog.toggleDialog(true);
                firebaseHelper.manualLogin(strUsername, currentDeviceId);
            }

        }
    }

    private boolean inputVerification(String username, String email) {
        return (!username.isEmpty() && username.matches("[a-zA-Z0-9]*") && !email.isEmpty());
    }

    private void moveToSessionActivity() {
        Intent intent = new Intent(LoginActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        user.setUserAuthentication(this, editTextEmail.getText().toString().trim());
        user.setAutoLogin(this, autoLog.isChecked());
        startActivity(intent);
        finish();
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        progressDialog.toggleDialog(false);
        if (tag.equals("manualLogin")) {
            switch (condition){
                case FirebaseHelper.CONDITION_1:
                    moveToSessionActivity();
                    break;
                case FirebaseHelper.CONDITION_2:
                    Toast.makeText(LoginActivity.this, "Device Id do not match", Toast.LENGTH_LONG).show();
                    break;
                case FirebaseHelper.CONDITION_3:
                    Toast.makeText(LoginActivity.this, "This username does not exist.", Toast.LENGTH_LONG).show();
                    break;
                case FirebaseHelper.CONDITION_4:
                    firebaseHelper.updateUserDeviceToken(container.getString());
                    break;
            }
        } else if (tag.equals("autoLogin")){
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    moveToSessionActivity();
                    break;

                case FirebaseHelper.CONDITION_2 :
                    Toast.makeText(LoginActivity.this, "Error: This device maybe new therefore please register again.", Toast.LENGTH_LONG).show();
                    break;

                case FirebaseHelper.CONDITION_3 :
                    firebaseHelper.updateUserDeviceToken(container.getString());
                    break;

            }
        } else if (tag.equals("updateUserDeviceToken")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    moveToSessionActivity();
                    Log.i(TAG, tag + ": Successfully updated new device token " + container.getString());
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
        progressDialog.toggleDialog(false);
        Log.i(TAG, tag+": "+databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

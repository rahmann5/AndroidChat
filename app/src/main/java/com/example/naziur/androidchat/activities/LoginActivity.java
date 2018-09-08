package com.example.naziur.androidchat.activities;

import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    EditText editTextUsername, editTextProfileName;
    User user = User.getInstance();
    FirebaseDatabase database;
    DatabaseReference usersRef;
    String currentDeviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        user.sharedpreferences = getSharedPreferences(user.appPreferences, Context.MODE_PRIVATE);

        currentDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        editTextUsername = (EditText) findViewById(R.id.editTextUsername);
        editTextProfileName = (EditText) findViewById(R.id.editTextProfileName);
    }

    public void btnLoginTapped(View view) {
        final String strUsername = editTextUsername.getText().toString().trim();
        final String strProfileName = editTextProfileName.getText().toString().trim();
        if (strUsername.isEmpty()) {
            Toast.makeText(this, "Please enter unique username", Toast.LENGTH_LONG).show();
        } else {

            if (!Network.isInternetAvailable(this, true)) {
                return;
            }

            final ProgressDialog progressDialog = new ProgressDialog(this, R.layout.progress_dialog, true);
            progressDialog.toggleDialog(true);
            Query query = usersRef.orderByChild("username").equalTo(strUsername);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        if (!strProfileName.isEmpty())
                            addUserToDatabase(strUsername, strProfileName, progressDialog);
                        else
                            Toast.makeText(LoginActivity.this, "Please enter a profile name", Toast.LENGTH_LONG).show();
                    } else {
                        progressDialog.toggleDialog(false);
                        Toast.makeText(LoginActivity.this, "Please enter unique username", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    progressDialog.toggleDialog(false);
                    Log.i(TAG, databaseError.getMessage());
                }
            });
            //

        }
    }


    private void addUserToDatabase(final String strUsername, String strProfileName, final ProgressDialog progressDialog){
        final FirebaseUserModel firebaseUserModel = new FirebaseUserModel();
        firebaseUserModel.setUsername(strUsername);
        firebaseUserModel.setProfileName(strProfileName);
        firebaseUserModel.setStatus(getResources().getString(R.string.status_available));
        firebaseUserModel.setDeviceId(currentDeviceId);
        firebaseUserModel.setDeviceToken(FirebaseInstanceId.getInstance().getToken());

        final DatabaseReference newRef = usersRef.push();
        newRef.setValue(firebaseUserModel, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                progressDialog.toggleDialog(false);
                if (databaseError == null) {
                    if (user.login(firebaseUserModel)) {
                        Intent intent = new Intent(LoginActivity.this, SessionActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to register user " +  strUsername + " please try again.", Toast.LENGTH_LONG).show();
                    Log.i(TAG, databaseError.getMessage());
                }

            }
        });
    }
}
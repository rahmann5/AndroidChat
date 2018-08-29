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

    private static final String TAG = "MainActivity";

    private Button btnLogin;

    String currentDeviceId;

    User user = User.getInstance();

    EditText editTextUsername, editTextProfileName;

    FirebaseDatabase database;
    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedpreferences = getSharedPreferences(user.appPreferences, Context.MODE_PRIVATE);
        user.sharedpreferences = sharedpreferences;

        currentDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        final ProgressDialog Dialog = new ProgressDialog(this);
        Dialog.setMessage("Please wait..");
        Dialog.setCancelable(false);
        Dialog.show();

        usersRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                Dialog.dismiss();
                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    //Getting the data from snapshot
                    FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);

                    if (firebaseUserModel.getDeviceId().equals(currentDeviceId)) {
                        firebaseUserModel.setDeviceToken(FirebaseInstanceId.getInstance().getToken());
                        user.login(firebaseUserModel);
                        user.saveFirebaseKey(userSnapshot.getKey());
                        moveToSessionScreen();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Dialog.dismiss();
                System.out.println("The read failed: " + databaseError.getMessage());
            }
        });

        editTextUsername = (EditText) findViewById(R.id.editTextUsername);
        editTextProfileName = (EditText) findViewById(R.id.editTextProfileName);

    }

    public void moveToSessionScreen() {
        Intent intent = new Intent(this, ChatActivity.class);
        //Intent intent = new Intent(this, SessionActivity.class);
        startActivity(intent);
        finish();
    }

    public void showMessage(String strTitle, String strMessage) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(strTitle)
                .setMessage(strMessage)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    public void btnLoginTapped(View view) {
        final String strUsername = editTextUsername.getText().toString().trim();
        final String strProfileName = editTextProfileName.getText().toString().trim();
        if (strUsername.isEmpty()) {
            showMessage("Invalid", "Please enter a username");
        } else if (strProfileName.isEmpty()){
            showMessage("Invalid", "Please enter a profile name");
        } else {

            final ProgressDialog Dialog = new ProgressDialog(this);
            Dialog.setMessage("Please wait..");
            Dialog.setCancelable(false);
            Dialog.show();
            Query query = usersRef.orderByChild("username").equalTo(strUsername);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        addUserToDatabase(strUsername, strProfileName, Dialog);
                    } else {
                        Dialog.dismiss();
                        showMessage("Invalid", "Please provide a unique username");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            //



        }
    }

    private void addUserToDatabase(String strUsername, String strProfileName, final ProgressDialog dialog){
        final FirebaseUserModel firebaseUserModel = new FirebaseUserModel();
        firebaseUserModel.setUsername(strUsername);
        firebaseUserModel.setProfileName(strProfileName);
        firebaseUserModel.setDeviceId(currentDeviceId);
        firebaseUserModel.setDeviceToken(FirebaseInstanceId.getInstance().getToken());

        final DatabaseReference newRef = usersRef.push();
        newRef.setValue(firebaseUserModel, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                dialog.dismiss();
                if (user.login(firebaseUserModel)) {
                    moveToSessionScreen();
                }
            }
        });
    }
}

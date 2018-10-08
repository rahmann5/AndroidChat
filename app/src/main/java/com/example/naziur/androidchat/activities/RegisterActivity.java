package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.iid.FirebaseInstanceId;

public class RegisterActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = "RegisterActivity";
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private FirebaseHelper firebaseHelper;
    private User user = User.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        firebaseHelper =FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        progressDialog = new ProgressDialog(this, R.layout.progress_dialog, false);
        final EditText usernameEt = (EditText) findViewById(R.id.username);
        final EditText emailEt = (EditText) findViewById(R.id.email);
        final EditText profileEt = (EditText) findViewById(R.id.profile);
        TextView loginTv = (TextView) findViewById(R.id.login);

        final Button registerBtn = (Button) findViewById(R.id.register_btn);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!inputVerification(usernameEt.getText().toString().trim(), emailEt.getText().toString().trim(), profileEt.getText().toString().trim())){
                    Toast.makeText(RegisterActivity.this, "You must provide a valid username/email/profile", Toast.LENGTH_SHORT).show();
                    return;
                }
                progressDialog.toggleDialog(true);
                registerUser(emailEt.getText().toString().trim(), usernameEt.getText().toString().trim(), profileEt.getText().toString().trim());
            }
        });

        loginTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });

    }

    private void addUserToDatabase(final String strUsername, String strProfileName, final String currentDeviceId, String uid){
        final FirebaseUserModel firebaseUserModel = new FirebaseUserModel();
        user.saveFirebaseKey(uid);
        firebaseUserModel.setUsername(strUsername);
        firebaseUserModel.setProfileName(strProfileName);
        firebaseUserModel.setStatus(getResources().getString(R.string.status_available));
        firebaseUserModel.setDeviceId(currentDeviceId);
        firebaseUserModel.setDeviceToken(FirebaseInstanceId.getInstance().getToken());
        firebaseHelper.registerNewUser(firebaseUserModel, uid);
    }

    private boolean inputVerification(String username, String email, String profile) {
        return (!username.isEmpty() && username.matches("[a-zA-Z0-9]*") && !email.isEmpty() && !profile.isEmpty());
    }

    private void registerUser(String email, final String username, final String profile){
        final String password = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.i(TAG, "createUserWithEmail:success");
                            FirebaseUser authUser = mAuth.getCurrentUser();
                            addUserToDatabase(username, profile, password, authUser.getUid());
                        } else {
                            progressDialog.toggleDialog(false);
                            // If sign in fails, display a message to the user.
                            Log.i(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
       /* if(mAuth.getCurrentUser() != null){
            startActivity(new Intent(RegisterActivity.this, SessionActivity.class));
        }*/
    }


    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch (tag){
            case "registerNewUser":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        if (user.login(container.getUserModel())) {
                            //user.setUserAuthentication(this, mAuth.getCurrentUser().getEmail());
                            Intent intent = new Intent(RegisterActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }
                        progressDialog.toggleDialog(false);
                        break;
                }
                break;
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag) {
            case "registerNewUser":
                Toast.makeText(RegisterActivity.this, "Failed to register user please try again.", Toast.LENGTH_LONG).show();
                break;
        }
        progressDialog.toggleDialog(false);
        Log.i(TAG, tag+": "+databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

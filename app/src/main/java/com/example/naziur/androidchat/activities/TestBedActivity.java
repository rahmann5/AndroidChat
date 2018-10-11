package com.example.naziur.androidchat.activities;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.google.firebase.database.DatabaseError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class TestBedActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{

    private Button login, logout, run;
    private TextView complete, fail, change;
    private FirebaseAuth mAuth;
    String currentDeviceId;
    private FirebaseHelper firebaseHelper;
    private User user = User.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_bed);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);

        login = (Button) findViewById(R.id.login);
        logout = (Button) findViewById(R.id.logout);
        run = (Button) findViewById(R.id.run);
        mAuth = FirebaseAuth.getInstance();
        complete = (TextView)findViewById(R.id.complete);
        fail = (TextView)findViewById(R.id.fail);
        change = (TextView)findViewById(R.id.change);
        currentDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mAuth.signInWithEmailAndPassword("snr96@hotmail.co.uk", currentDeviceId).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            System.out.println("Successful login");
                        } else {
                            System.out.println("failed login");
                        }
                    }
                });
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
            }
        });

        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseHelper.autoLogin("users", currentDeviceId, user);
            }
        });
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("autoLogin")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    complete.setText("Found device with same device token");
                    break;

                case FirebaseHelper.CONDITION_2 :
                    complete.setText("Device does not exists");
                    break;

                case FirebaseHelper.CONDITION_3 :
                    complete.setText("Need to update device token");
                    break;
            }
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        fail.setText("onFailureTask " + tag + ": " + databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

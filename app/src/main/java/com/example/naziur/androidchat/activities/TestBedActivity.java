package com.example.naziur.androidchat.activities;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.naziur.androidchat.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class TestBedActivity extends AppCompatActivity {

    private Button login, logout, run;
    private TextView complete, fail, change;
    private FirebaseAuth mAuth;
    String currentDeviceId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_bed);
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
    }
}

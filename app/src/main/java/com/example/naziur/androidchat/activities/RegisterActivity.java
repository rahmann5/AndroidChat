package com.example.naziur.androidchat.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.naziur.androidchat.R;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
    }
}

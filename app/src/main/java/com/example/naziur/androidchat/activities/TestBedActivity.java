package com.example.naziur.androidchat.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.naziur.androidchat.R;

public class TestBedActivity extends AppCompatActivity {

    private Button login, logout, run;
    private TextView complete, fail, change;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_bed);
        login = (Button) findViewById(R.id.login);
        logout = (Button) findViewById(R.id.logout);
        run = (Button) findViewById(R.id.run);

        complete = (TextView)findViewById(R.id.complete);
        fail = (TextView)findViewById(R.id.fail);
        change = (TextView)findViewById(R.id.change);
    }
}

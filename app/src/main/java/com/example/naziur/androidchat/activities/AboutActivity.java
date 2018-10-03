package com.example.naziur.androidchat.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.naziur.androidchat.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyCorrectLayout();
    }

    private void applyCorrectLayout(){
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            switch (extra.getInt("key")){
                case R.string.key_about_us:
                    setContentView(R.layout.activity_about_us);
                    break;
                case R.string.key_app_info:
                    setContentView(R.layout.activity_app_info);
                    break;
            }
        }
    }
}

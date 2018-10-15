package com.example.naziur.androidchat.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.example.naziur.androidchat.R;

public class AboutActivity extends AuthenticatedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

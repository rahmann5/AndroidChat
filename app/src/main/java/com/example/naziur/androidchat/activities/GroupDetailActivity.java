package com.example.naziur.androidchat.activities;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.utils.Container;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class GroupDetailActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{

    private FirebaseHelper firebaseHelper;
    private FirebaseGroupModel groupModel;
    private ValueEventListener groupListener;
    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            groupListener = firebaseHelper.getGroupInfo(extra.getString("g_uid"));
        } else {
            Toast.makeText(this, "Error occurred", Toast.LENGTH_LONG).show();
            finish();
        }

        /*ImageView groupPic = (ImageView) findViewById(R.id.expandedImage);
        groupPic.setColorFilter(ContextCompat.getColor(this, R.color.group_detail_tint), android.graphics.PorterDuff.Mode.MULTIPLY);
        */
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void populateWithGroupData(){

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_detail_menu, menu);
        return true;
    }

    @Override
    protected void onStop() {
        firebaseHelper.toggleListenerFor("groups", "groupKey", groupModel.getGroupKey(), groupListener, false, false);
        super.onStop();
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch (tag){
            case "getGroupInfo":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        groupModel = container.getGroupModel();
                        break;
                }
                break;
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {

    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

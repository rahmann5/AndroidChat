package com.example.naziur.androidchat.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupDetailActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{

    private FirebaseHelper firebaseHelper;
    private FirebaseGroupModel groupModel;
    private ValueEventListener groupListener;
    private Toolbar toolbar;
    private ProgressDialog progressBar;
    private User user = User.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            progressBar.toggleDialog(true);
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
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void populateWithGroupData(){
        getSupportActionBar().setTitle(groupModel.getTitle());
        TextView titleTv = (TextView) findViewById(R.id.title_tv);
        TextView adminTv = (TextView) findViewById(R.id.admin_tv);
        TextView emptyTv = (TextView) findViewById(R.id.empty_view);
        ImageView groupIv = (ImageView) findViewById(R.id.expandedImage);
        ListView membersListView = (ListView) findViewById(R.id.members_list_view);
        titleTv.setText(groupModel.getTitle());
        if(!groupModel.getAdmin().isEmpty())
            adminTv.setText(groupModel.getAdmin());
        ArrayAdapter membersAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, getEveryOneBesidesYou());
        if(membersAdapter.getCount() == 0)
            emptyTv.setVisibility(View.VISIBLE);
        else
            emptyTv.setVisibility(View.GONE);
        membersListView.setAdapter(membersAdapter);
        Glide.with(GroupDetailActivity.this).load(groupModel.getPic()).apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown)).into(groupIv);
    }

    private List<String> getEveryOneBesidesYou(){
        String [] membersIngroup = groupModel.getMembers().split(",");
        List<String> members = new ArrayList<>();
        if(groupModel.getAdmin().equals(user.name))
            return Arrays.asList(membersIngroup);
        else {
            if(!groupModel.getAdmin().isEmpty())
                members.add(groupModel.getAdmin());
            for (int i = 0; i < membersIngroup.length; i++) {
                if (!membersIngroup[i].equals(user.name)) {
                    members.add(membersIngroup[i]);
                }
            }
            return members;
        }
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
                        populateWithGroupData();
                        progressBar.toggleDialog(false);
                        break;
                }
                break;
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag){
            case "getGroupInfo":
                progressBar.toggleDialog(false);
                break;
        }
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

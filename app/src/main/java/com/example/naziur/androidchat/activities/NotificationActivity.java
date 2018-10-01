package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.NotificationAdapter;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.Notification;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnItemClickListener, FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = "NotificationActivity";
    private User user = User.getInstance();

    private RecyclerView notificationRecycler;
    private NotificationAdapter notificationAdapter;

    ValueEventListener notificationEvent;
    List<Notification> allNotifications;
    private FirebaseHelper firebaseHelper;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        allNotifications = new ArrayList<>();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        emptyText = (TextView) findViewById(R.id.empty_notifications);
        notificationRecycler = (RecyclerView) findViewById(R.id.notification_recycler);
        notificationRecycler.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                mLayoutManager.getOrientation());

        notificationRecycler.addItemDecoration(dividerItemDecoration);

        notificationEvent = firebaseHelper.getNotificationChecker(user.name, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Network.isInternetAvailable(this, true)) {
            firebaseHelper.notificationNodeExists(null, null, notificationEvent);
        } else {
            toggleEmpty(null);
        }
    }

    private void toggleEmpty (List<Notification> allNotifications) {
       if (allNotifications != null) {
           if (allNotifications.isEmpty()) {
               emptyText.setVisibility(View.VISIBLE);
           } else {
               emptyText.setVisibility(View.GONE);
           }
       } else {
           emptyText.setVisibility(View.VISIBLE);
       }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
                break;

            case R.id.action_settings :

                break;

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStop() {
        super.onStop();
        FirebaseHelper.removeNotificationListener(notificationEvent);
    }

    @Override
    public void onButtonClicked(Notification notification, int pos, boolean accept) {
        if (accept) {
            FirebaseUserModel temp = new FirebaseUserModel();
            temp.setUsername(notification.getSender());
            firebaseHelper.updateChatKeyFromContact(new Contact(temp), notification.getChatKey(), true, true);
        } else { // reject
            firebaseHelper.removeNotificationNode(notification.getSender(), notification.getChatKey(), false);
        }
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("notificationNodeExists")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                case FirebaseHelper.CONDITION_2 :
                    allNotifications = container.getNotifications();
                    notificationAdapter = new NotificationAdapter(NotificationActivity.this, NotificationActivity.this, allNotifications);
                    notificationRecycler.setAdapter(notificationAdapter);
                    toggleEmpty(allNotifications);
                    break;
            }
        } else if (tag.equals("removeNotificationNode")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    Intent i = new Intent(NotificationActivity.this, ChatActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra("chatKey", container.getString());
                    startActivity(i);
                    break;
            }
        } else if (tag.equals("updateChatKeyFromContact")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1:
                    firebaseHelper.removeNotificationNode(container.getContact().getContact().getUsername(), container.getString(), true);
            }
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag) {
            case "notificationNodeExists" :
                toggleEmpty(null);
                Toast.makeText(NotificationActivity.this, "Failed to retrieve notifications", Toast.LENGTH_SHORT).show();
                break;

            case "removeNotificationNode" :
                Toast.makeText(NotificationActivity.this, "Failed to remove pending invite notification", Toast.LENGTH_LONG).show();
                break;

            case "updateChatKeyFromContact" :
                Toast.makeText(NotificationActivity.this, "Failed to remove pending invite notification", Toast.LENGTH_LONG).show();
                break;

        }
        Log.i(TAG, databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

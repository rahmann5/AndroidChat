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
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.Notification;
import com.example.naziur.androidchat.models.User;
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

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnItemClickListener{

    private static final String TAG = "NotificationActivity";
    private User user = User.getInstance();

    private RecyclerView notificationRecycler;
    private NotificationAdapter notificationAdapter;
    private DatabaseReference notificationRef;

    ValueEventListener notificationEvent;

    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        setTitle("Notifications");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(user.name);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);

        emptyText = (TextView) findViewById(R.id.empty_notifications);
        notificationRecycler = (RecyclerView) findViewById(R.id.notification_recycler);
        notificationRecycler.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                mLayoutManager.getOrientation());

        notificationRecycler.addItemDecoration(dividerItemDecoration);

        notificationEvent = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Notification> allNotifications = new ArrayList<>();
                for (DataSnapshot notificationSnapshot : dataSnapshot.getChildren()) {
                    Notification singleNotification = notificationSnapshot.getValue(Notification.class);
                    allNotifications.add(singleNotification);
                }

                notificationAdapter = new NotificationAdapter(NotificationActivity.this, NotificationActivity.this, allNotifications);
                notificationRecycler.setAdapter(notificationAdapter);
                toggleEmpty(allNotifications);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                toggleEmpty(null);
                Toast.makeText(NotificationActivity.this, "Failed to retrieve notifications", Toast.LENGTH_SHORT).show();
                Log.i(TAG, databaseError.getMessage());
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Network.isInternetAvailable(this, true)) {
            notificationRef.addValueEventListener(notificationEvent);
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
    protected void onDestroy() {
        super.onDestroy();
        if (notificationRef != null)
            notificationRef.removeEventListener(notificationEvent);
    }

    @Override
    public void onButtonClicked(Notification notification, int pos, boolean accept) {
        if (accept) {
            acceptInvite (notification);
        } else {
            rejectInvite (notification, false);
        }
    }

    private void acceptInvite (final Notification gNotification) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
        userRef.orderByChild("username").equalTo(user.name).getRef().runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseUserModel userModel = data.getValue(FirebaseUserModel.class);
                    if (userModel == null) return Transaction.success(mutableData);

                    if (userModel.getUsername().equals(user.name)) {
                        String currentKeys = userModel.getChatKeys();
                        if (currentKeys.equals("")) {
                            currentKeys = gNotification.getChatKey();
                        } else {
                            currentKeys = removeAnyDuplicateKey(userModel.getChatKeys().split(","), generateOppositeKey(gNotification.getChatKey()));
                        }

                        userModel.setChatKeys(currentKeys);
                        data.setValue(userModel);
                        break;
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    rejectInvite(gNotification, true);
                } else {
                    Toast.makeText(NotificationActivity.this, "Failed to remove pending invite notification", Toast.LENGTH_LONG).show();
                    Log.i(TAG, databaseError.getMessage());
                }
            }
        });
    }

    private String generateOppositeKey (String currentKey) {
        String [] keys = currentKey.split("-");
        return keys[1] + keys[0];
    }

    private String removeAnyDuplicateKey (String[] myKeys, String searchDup) {
        String newKeys = "";
        for (String key : myKeys) {
            if (!key.equals(searchDup)) {
                if (newKeys.equals("")) {
                    newKeys = key;
                } else {
                    newKeys += "," + key;
                }
            }
        }

        return newKeys;
    }

    private void rejectInvite (final Notification gNotification, final boolean home) {
        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(user.name);
        notificationRef.orderByChild("sender").equalTo(gNotification.getSender()).getRef().runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    Notification notification = data.getValue(Notification.class);
                    if (notification == null) return Transaction.success(mutableData);

                    if (gNotification.getSender().equals(notification.getSender())) {
                        notification = null;
                    }

                    data.setValue(notification);
                    break;
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                if (databaseError != null) {
                    Toast.makeText(NotificationActivity.this, "Failed to remove pending invite notification", Toast.LENGTH_LONG).show();
                    Log.i(TAG, databaseError.getMessage());
                } else {
                    if(home) {
                        Intent i = new Intent(NotificationActivity.this, ChatActivity.class);
                        i.putExtra("chatKey", gNotification.getChatKey());
                        startActivity(i);
                    }
                }
            }
        });
    }
}

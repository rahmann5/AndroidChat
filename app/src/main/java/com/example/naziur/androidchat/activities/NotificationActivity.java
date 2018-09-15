package com.example.naziur.androidchat.activities;

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
import com.example.naziur.androidchat.models.Notification;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Network;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
        notificationRef = FirebaseDatabase.getInstance().getReference("Notification").child(user.name);
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
            case R.id.action_fake :
                createFakeNotification ();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void createFakeNotification () {

        //notificationRef.setValue()
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
            System.out.println("ACCEPT");
        } else {
            System.out.println("REJECT");
        }
    }
}

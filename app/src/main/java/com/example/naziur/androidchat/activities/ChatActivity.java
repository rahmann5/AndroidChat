package com.example.naziur.androidchat.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.adapter.MessagesListAdapter;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.MessageCell;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.entity.StringEntity;
import de.hdodenhof.circleimageview.CircleImageView;


public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    User user = User.getInstance();

    ListView listView;

    EditText textComment;
    CircleImageView btnSend, btnMedia;

    List<FirebaseMessageModel> messages = new ArrayList<FirebaseMessageModel>();

    FirebaseDatabase database;
    DatabaseReference messagesRef;
    DatabaseReference usersRef;

    private ProgressDialog progressBar;

    private ActionBar actionBar;
    private FirebaseUserModel friend, me;

    public static ChatActivity chattingActivity;
    private String chatKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            friend = new FirebaseUserModel();
            friend.setUsername(extra.getString("username"));
        } else {
            Toast.makeText(this, "Error occured", Toast.LENGTH_LONG).show();
            finish();
        }
        createCustomActionBar();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        chattingActivity = this;

        listView = (ListView) findViewById(R.id.chattingList);

        textComment = (EditText) findViewById(R.id.comment_text);
        btnSend = (CircleImageView) findViewById(R.id.send_button);
        btnMedia = (CircleImageView) findViewById(R.id.media_button);

        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        progressBar.toggleDialog(true);
        //messagesRef.equalTo(user.name + "-" + friend.getUsername()).equalTo(friend.getUsername() + "-" + user.name);

        final com.google.firebase.database.ValueEventListener commentValueEventListener = new com.google.firebase.database.ValueEventListener() {

            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {

                messages.clear();

                for (com.google.firebase.database.DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    //System.out.println("Child: " + postSnapshot);
                    //Getting the data from snapshot
                    FirebaseMessageModel firebaseMessageModel = postSnapshot.getValue(FirebaseMessageModel.class);
                    messages.add(firebaseMessageModel);
                }

                updateListView();

                progressBar.toggleDialog(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                messages.clear();
                updateListView();
                progressBar.toggleDialog(false);
                Log.i(TAG, databaseError.getMessage());
            }
        };

        // improve for future search of users (need only the sender and receiver - currently looping through all users)
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                for (com.google.firebase.database.DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    System.out.println("Child: " + postSnapshot);
                    //Getting the data from snapshot
                    FirebaseUserModel firebaseUserModel = postSnapshot.getValue(FirebaseUserModel.class);
                    if (firebaseUserModel.getUsername().equals(friend.getUsername())) {
                        friend = firebaseUserModel;
                        ((TextView) actionBar.getCustomView().findViewById(R.id.profile_name)).setText(friend.getProfileName());
                        Glide.with(getApplicationContext())
                                .load(friend.getProfilePic())
                                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                                .into(((CircleImageView) actionBar.getCustomView().findViewById(R.id.profile_icon)));
                    }

                    if (firebaseUserModel.getUsername().equals(user.name)) {
                        me = firebaseUserModel;
                    }

                    if (me != null && !friend.getDeviceToken().equals("")) {
                        break;
                    }
                }

                if (me != null) {
                    if (!me.getChatKeys().equals("") && !friend.getChatKeys().equals("")) { // both with keys but maybe not same keys
                        String[] allMyKeys  = me.getChatKeys().split(",");
                        List<String> allFriendKeys  = Arrays.asList(friend.getChatKeys().split(","));
                        for(String key : allMyKeys) {
                            if (allFriendKeys.contains(key)) { // both make existing keys
                                messagesRef = database.getReference("messages")
                                        .child("single")
                                        .child(key);
                                chatKey = key;
                                break;
                            }
                        }

                    } else if (me.getChatKeys().equals("") && !friend.getChatKeys().equals("")) { // me without key but friend with key but not sure same key
                        verifyUserChatKeys(friend, me);

                    }  else if (!me.getChatKeys().equals("") && friend.getChatKeys().equals("")) { // friend without key but me with key but not sure same key
                        verifyUserChatKeys(me, friend);

                    } else { // no chat keys created yet for both
                        createMessageRefWithNewKey ();
                    }

                    if (messagesRef == null) {
                        createMessageRefWithNewKey ();
                    }

                    //Value event listener for realtime data update
                    messagesRef.addValueEventListener(commentValueEventListener);

                } else {
                    System.out.println("ME is null");
                    finish();
                }

                progressBar.toggleDialog(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.toggleDialog(false);
                System.out.println("The read failed: " + databaseError.getMessage());
            }
        });

        btnMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ChatActivity.this, "Adding media", Toast.LENGTH_SHORT).show();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                hideKeyboard();

                final String wishMessage = textComment.getText().toString().trim();
                if (wishMessage.isEmpty()) {
                    return;
                } else {
                    btnSend.setEnabled(false);
                    // send text as wish

                    final FirebaseMessageModel firebaseMessageModel = new FirebaseMessageModel();
                    firebaseMessageModel.setText(wishMessage);
                    firebaseMessageModel.setSenderDeviceId(user.deviceId);
                    firebaseMessageModel.setSenderName(user.name);
                    firebaseMessageModel.setReceiverName(friend.getUsername());
                    firebaseMessageModel.setIsReceived(Constants.MESSAGE_SENT);

                    progressBar.toggleDialog(true);

                    final DatabaseReference newRef = messagesRef.push();
                    newRef.setValue(firebaseMessageModel, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError != null) {
                                btnSend.setEnabled(true);
                                progressBar.toggleDialog(false);
                                Log.i(TAG, databaseError.getMessage());
                            } else {
                                textComment.setText("");

                                if (friend != null) {

                                    String url = "https://fcm.googleapis.com/fcm/send";
                                    AsyncHttpClient client = new AsyncHttpClient();

                                   //client.addHeader(HttpHeaders.AUTHORIZATION, "key=AIzaSyCl-lEfl7Rx9ZcDEyXX4sSpXhJYMS6PHfk");
                                    client.addHeader(HttpHeaders.AUTHORIZATION, "key=AAAAQmgvFoU:APA91bF8shJboV6QDRVUvy-8ZKhZ6c1eri8a6zlkSPLDosvPZ-MegfsPEOGeKUhoxmtMq3d11bzeOEWWIupjCuKW3rgbwmqZ8LqumrK_ldWYT_ipDExdy4J2OWnhYwvb9Y6pIx8vOWD8");
                                    client.addHeader(HttpHeaders.CONTENT_TYPE, RequestParams.APPLICATION_JSON);

                                    try {
                                        JSONObject params = new JSONObject();

                                        //params.put("registration_ids", registration_ids);
                                        params.put("to", friend.getDeviceToken());
                                        JSONObject payload = new JSONObject();
                                        payload.put("sender", user.name);
                                        JSONObject notificationObject = new JSONObject();
                                        notificationObject.put("click_action", ".MainActivity");
                                        notificationObject.put("body", wishMessage);
                                        notificationObject.put("title", user.name);
                                        notificationObject.put("tag", user.deviceId);
                                        params.put("data", payload);

                                        params.put("notification", notificationObject);

                                        StringEntity entity = new StringEntity(params.toString());

                                        client.post(getApplicationContext(), url, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
                                            @Override
                                            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                                                btnSend.setEnabled(true);
                                                progressBar.toggleDialog(false);
                                                Log.i(TAG, responseString);
                                            }

                                            @Override
                                            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                                                btnSend.setEnabled(true);
                                                progressBar.toggleDialog(false);
                                                Log.i(TAG, responseString);
                                            }
                                        });

                                    } catch (Exception e) {
                                        btnSend.setEnabled(true);
                                    }
                                } else {
                                    btnSend.setEnabled(true);
                                }

                            }
                        }
                    });

                    messagesRef.addValueEventListener(commentValueEventListener);

                }
            }
        });

    }

    private void createMessageRefWithNewKey () {
        String key = makeChatKey(me, friend);
        messagesRef = database.getReference("messages")
                .child("single")
                .child(key);
        chatKey = key;
    }

    private void verifyUserChatKeys (FirebaseUserModel withKeys, FirebaseUserModel withoutKeys) {
        String[] allKeys  = withKeys.getChatKeys().split(",");
        for(String key : allKeys) {
            String username1 = key.split("-")[0];
            String username2 = key.split("-")[1];
            if (username1.equals(withoutKeys.getUsername()) || username2.equals(withoutKeys.getUsername())) {
                String keyFinal = addChatKey(withoutKeys, key);
                messagesRef = database.getReference("messages")
                        .child("single")
                        .child(keyFinal);
                chatKey = keyFinal;
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_details :
                Intent chatDetailActivity = new Intent(this, ChatDetailActivity.class);
                chatDetailActivity.putExtra("username", friend.getUsername());
                startActivity(chatDetailActivity);
                return true;

            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent (ChatActivity.this, SessionActivity.class));
        finish();
        super.onBackPressed();
    }

    private String addChatKey (FirebaseUserModel user, String key) {
        stringVerification(user, key);
        return key;
    }

    private String makeChatKey (FirebaseUserModel sender, FirebaseUserModel receiver) {
        String key = sender.getUsername() + "-" + receiver.getUsername();
        stringVerification(sender, key);
        stringVerification(receiver, key);
        return key;
    }

    private void stringVerification(final FirebaseUserModel addKeyTo, final String key) {
        try {
            usersRef.orderByChild("username").equalTo(addKeyTo.getUsername()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        if (addKeyTo.getChatKeys().equals("")) {
                            snapshot.getRef().child("chatKeys").setValue(key);
                        } else {
                            snapshot.getRef().child("chatKeys").setValue(addKeyTo.getChatKeys() + "," + key);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.i(TAG, databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            Log.i(TAG,"FAILED " + addKeyTo.getUsername());
            e.printStackTrace();
        }

    }

    private void createCustomActionBar () {
        actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.toolbar);
        actionBar.getCustomView().findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent (ChatActivity.this, SessionActivity.class));
                finish();
            }
        });
    }

    public void hideKeyboard() {
        try  {
            InputMethodManager inputManager = (InputMethodManager) chattingActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(chattingActivity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {
            Log.i(TAG, "Exception while hiding keyboard");
        }
    }

    public void updateListView() {
        Log.i(TAG, "Inside prepareWishList()");

        int totalWishes = messages.size();

        Log.i(TAG, "Total Wishes : " + totalWishes);

        MessageCell[] messageCells;
        messageCells = new MessageCell[totalWishes];

        Map<Long, Map<String, Object>> messegesThatNeedUpdating = new HashMap<>();

        for (int counter = 0; counter < totalWishes; counter++) {
            final FirebaseMessageModel firebaseMessageModel = messages.get(counter);
            if(!firebaseMessageModel.getSenderName().equals(me.getUsername()) && firebaseMessageModel.getIsReceived() == Constants.MESSAGE_SENT) {
                firebaseMessageModel.setIsReceived(Constants.MESSAGE_RECEIVED);
                messegesThatNeedUpdating.put(firebaseMessageModel.getCreatedDateLong(), firebaseMessageModel.toMap());
            }
            MessageCell messageCell = new MessageCell(firebaseMessageModel.getSenderName() , firebaseMessageModel.getText(),  getDate(firebaseMessageModel.getCreatedDateLong()), firebaseMessageModel.getSenderDeviceId().equals(user.deviceId), firebaseMessageModel.getIsReceived());
            messageCell.setDateOnly(getDateOnly(firebaseMessageModel.getCreatedDateLong()));
            messageCells[counter] = messageCell;
        }

        MessagesListAdapter adapter = new MessagesListAdapter(this, messageCells);
        if(messegesThatNeedUpdating.size() > 0)
            updateMessageReceiveStatus(messegesThatNeedUpdating);
        // Assign adapter to ListView
        listView.setAdapter(adapter);

        listView.setSelection(listView.getCount() - 1);

        listView.requestFocus();
    }

    private void updateMessageReceiveStatus(final Map<Long, Map<String, Object>> messages){
        //System.out.println("Updating " +chatKey + " with size " + messages.size());

        database.getReference("messages").child("single").child(chatKey).limitToLast(messages.size()).addListenerForSingleValueEvent(new ValueEventListener() {
           @Override
           public void onDataChange(DataSnapshot dataSnapshot) {
               if (dataSnapshot.exists()) {
                   for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                       FirebaseMessageModel firebaseMessageModel = snapshot.getValue(FirebaseMessageModel.class);
                       System.out.println("Updating " + firebaseMessageModel.getText() + " with key " + snapshot.getKey());
                       if (messages.get(firebaseMessageModel.getCreatedDateLong()) != null)
                           messagesRef.child(snapshot.getKey()).updateChildren(messages.get(firebaseMessageModel.getCreatedDateLong()));
                   }
               } else {
                   System.out.println("Data doesn't exist");
               }
           }

           @Override
           public void onCancelled(DatabaseError databaseError) {

           }
       });
    }

    /**
     * Return date in specified format.
     * @param milliSeconds Date in milliseconds
     * @return String representing date in specified format
     */
    public static String getDate(long milliSeconds)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM, yyyy, hh:mm a");

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    public static String getDateOnly(long milliSeconds)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}

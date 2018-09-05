package com.example.naziur.androidchat.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.naziur.androidchat.adapter.MessagesListAdapter;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.MessageCell;
import com.example.naziur.androidchat.models.User;
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
import java.util.List;

import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.entity.StringEntity;
import de.hdodenhof.circleimageview.CircleImageView;


public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    User user = User.getInstance();

    ListView listView;

    EditText textComment;
    ImageView btnSend;

    List<FirebaseMessageModel> messages = new ArrayList<FirebaseMessageModel>();

    FirebaseDatabase database;
    DatabaseReference messagesRef;
    DatabaseReference usersRef;

    private ActionBar actionBar;
    private FirebaseUserModel friend, me;

    public static ChatActivity chattingActivity;


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
        btnSend = (ImageView) findViewById(R.id.send_button);
        btnSend.setEnabled(false);
        btnSend.setColorFilter(getResources().getColor(android.R.color.darker_gray));

        textComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 5) {
                    btnSend.setEnabled(false);
                    btnSend.setColorFilter(getResources().getColor(android.R.color.darker_gray));
                } else {
                    btnSend.setEnabled(true);
                    btnSend.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
                }
            }
        });

        final ProgressDialog Dialog = new ProgressDialog(this);
        Dialog.setMessage("Please wait..");
        Dialog.setCancelable(false);
        Dialog.show();

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

                if (Dialog.isShowing()) {
                    Dialog.dismiss();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                messages.clear();
                updateListView();

                if (Dialog.isShowing()) {
                    Dialog.dismiss();
                }
                System.out.println("The read failed: " + databaseError.getMessage());
            }
        };

        // improve for future search of users (need only the sender and receiver - currently looping through al users)
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
                                .apply(new RequestOptions().placeholder(R.drawable.unknown).error(R.drawable.unknown))
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
                        System.out.println("BOTH HAVE DATA");
                        String[] allMyKeys  = me.getChatKeys().split(",");
                        List<String> allFriendKeys  = Arrays.asList(friend.getChatKeys().split(","));
                        for(String key : allMyKeys) {
                            if (allFriendKeys.contains(key)) { // both make existing keys
                                messagesRef = database.getReference("messages")
                                        .child("single")
                                        .child(key);
                                break;
                            }
                        }

                    } else if (me.getChatKeys().equals("") && !friend.getChatKeys().equals("")) { // me without key but friend with key but not sure same key
                        System.out.println("FRIEND HAS DATA");
                        verifyUserChatKeys(friend, me);

                    }  else if (!me.getChatKeys().equals("") && friend.getChatKeys().equals("")) { // friend without key but me with key but not sure same key
                        System.out.println("ME HAS DATA");
                        verifyUserChatKeys(me, friend);

                    } else { // no chat keys created yet for both
                        System.out.println("NONE HAS DATA");
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

                if (Dialog.isShowing()) {
                    Dialog.dismiss();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (Dialog.isShowing()) {
                    Dialog.dismiss();
                }
                System.out.println("The read failed: " + databaseError.getMessage());
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
                    // send text as wish

                    final FirebaseMessageModel firebaseMessageModel = new FirebaseMessageModel();
                    firebaseMessageModel.setText(wishMessage);
                    firebaseMessageModel.setSenderDeviceId(user.deviceId);
                    firebaseMessageModel.setSenderName(user.name);
                    firebaseMessageModel.setReceiverName(friend.getUsername());

                    final ProgressDialog Dialog = new ProgressDialog(chattingActivity);
                    Dialog.setMessage("Please wait..");
                    Dialog.setCancelable(false);
                    Dialog.show();

                    final DatabaseReference newRef = messagesRef.push();
                    newRef.setValue(firebaseMessageModel, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            Dialog.dismiss();

                            if (databaseError != null) {
                                Log.i(TAG, databaseError.toString());
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
                                        params.put("data", payload);

                                        params.put("notification", notificationObject);

                                        StringEntity entity = new StringEntity(params.toString());

                                        client.post(getApplicationContext(), url, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
                                            @Override
                                            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                                                Dialog.dismiss();
                                                Log.i(TAG, responseString);
                                            }

                                            @Override
                                            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                                                Dialog.dismiss();
                                                Log.i(TAG, responseString);
                                            }
                                        });

                                    } catch (Exception e) {

                                    }
                                }

                            }
                        }
                    });

                    messagesRef.addValueEventListener(commentValueEventListener);

                }
            }
        });

    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void createMessageRefWithNewKey () {
        messagesRef = database.getReference("messages")
                .child("single")
                .child(makeChatKey(me, friend));
    }

    private void verifyUserChatKeys (FirebaseUserModel withKeys, FirebaseUserModel withoutKeys) {
        String[] allKeys  = withKeys.getChatKeys().split(",");
        for(String key : allKeys) {
            String username1 = key.split("-")[0];
            String username2 = key.split("-")[1];
            if (username1.equals(withoutKeys.getUsername()) || username2.equals(withoutKeys.getUsername())) {
                messagesRef = database.getReference("messages")
                        .child("single")
                        .child(addChatKey(withoutKeys, key));
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
                Toast.makeText(this, "View contact details for " + friend.getUsername(), Toast.LENGTH_SHORT).show();
                return true;

            default: return super.onOptionsItemSelected(item);
        }
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
                    System.out.println("The read failed: " + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            System.out.println("FAILED " + addKeyTo.getUsername());
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

        for (int counter = 0; counter < totalWishes; counter++) {
            final FirebaseMessageModel firebaseMessageModel = messages.get(counter);

            MessageCell messageCell = new MessageCell(firebaseMessageModel.getSenderName() , firebaseMessageModel.getText(),  getDate(firebaseMessageModel.getCreatedDateLong()), firebaseMessageModel.getSenderDeviceId().equals(user.deviceId));

            messageCells[counter] = messageCell;
        }

        MessagesListAdapter adapter = new MessagesListAdapter(this, messageCells);

        // Assign adapter to ListView
        listView.setAdapter(adapter);

        listView.setSelection(listView.getCount() - 1);

        listView.requestFocus();
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
}

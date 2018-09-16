package com.example.naziur.androidchat.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.adapter.MessagesListAdapter;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.fragment.ImageViewDialogFragment;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.MessageCell;
import com.example.naziur.androidchat.models.Notification;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.fragment.ErrorDialogFragment;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.entity.StringEntity;
import de.hdodenhof.circleimageview.CircleImageView;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;


public class ChatActivity extends AppCompatActivity implements ImageViewDialogFragment.ImageViewDialogListener{
    private static final String TAG = "ChatActivity";
    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    User user = User.getInstance();

    ListView listView;

    EditText textComment;
    CircleImageView btnSend, btnMedia, btnInvite;
    FloatingActionButton sendBottom;
    List<FirebaseMessageModel> messages = new ArrayList<FirebaseMessageModel>();

    FirebaseDatabase database;
    DatabaseReference messagesRef;
    DatabaseReference usersRef, notificationRef;

    private ValueEventListener commentValueEventListener;

    private ProgressDialog progressBar;

    private ActionBar actionBar;
    private FirebaseUserModel friend, me;

    private String chatKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            chatKey = extra.getString("chatKey");
            String [] usernamesInKey = chatKey.split("-");
            friend = new FirebaseUserModel();
            friend.setUsername(usernamesInKey[0].equals(user.name)? usernamesInKey[1] : usernamesInKey[0]);
        } else {
            Toast.makeText(this, "Error occurred", Toast.LENGTH_LONG).show();
            finish();
        }
        createCustomActionBar();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        notificationRef = database.getReference("notifications").child(friend.getUsername());

        listView = (ListView) findViewById(R.id.chattingList);

        textComment = (EditText) findViewById(R.id.comment_text);
        btnSend = (CircleImageView) findViewById(R.id.send_button);
        btnMedia = (CircleImageView) findViewById(R.id.media_button);
        btnInvite = (CircleImageView) findViewById(R.id.send_invite_button);
        sendBottom = (FloatingActionButton) findViewById(R.id.action_send_bottom);

        sendBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listView.setSelection(listView.getCount()-1);
                sendBottom.setVisibility(View.GONE);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                MessageCell c = (MessageCell) listView.getItemAtPosition(i);
                if (c.getMessageType().equals(Constants.MESSAGE_TYPE_PIC)) {
                    if (Network.isInternetAvailable(ChatActivity.this, true)) {
                        ImageViewDialogFragment imageViewDialog = ImageViewDialogFragment.newInstance(
                                c.getMessageText(),
                                Constants.ACTION_DOWNLOAD,
                                android.R.drawable.ic_menu_upload);
                        imageViewDialog.setCancelable(false);
                        imageViewDialog.show(getSupportFragmentManager(), "ImageViewDialogFragment");
                    }
                } else if (c.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("message", c.getMessageText());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(ChatActivity.this, "Copied", Toast.LENGTH_SHORT).show();
                }
            }
        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener(){
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                if(listView.getCount() != listView.getLastVisiblePosition() + 1) {
                    sendBottom.setVisibility(View.VISIBLE);
                } else {
                    sendBottom.setVisibility(View.GONE);
                }
            }
        });

        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        progressBar.toggleDialog(true);

        commentValueEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                messages.clear();

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
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

        if (Network.isInternetAvailable(this, true)) {
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
                        String myKey = findChatKey(me, friend);
                        String friendKey = findChatKey(friend, me);
                        btnInvite.setVisibility(View.GONE);
                        btnSend.setVisibility(View.VISIBLE);
                        assignMessageEventListener(chatKey);
                        if (friendKey.equals("")) {
                            progressBar.toggleDialog(false);
                            // change send button to be able to send notification instead
                            btnInvite.setVisibility(View.VISIBLE);
                            btnSend.setVisibility(View.GONE);
                        }

                    } else {
                        Log.i(TAG, "ME IS NULL");
                        progressBar.toggleDialog(false);
                        finish();
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    loadLocalData("Oops an error has occurred and we have no clue what it is.");
                    progressBar.toggleDialog(false);
                    Log.i(TAG, "The read failed: " + databaseError.getMessage());
                }
            });
        } else {
            loadLocalData("Please connect to the internet.");
        }
        btnInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Network.isInternetAvailable(ChatActivity.this, true)) {
                    return;
                } else {
                    btnInvite.setEnabled(false);

                    usersRef.orderByChild("username").equalTo(friend.getUsername()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                                    FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                                    List<String> list = Arrays.asList(firebaseUserModel.getChatKeys().split(","));
                                    if(list.contains(chatKey)){
                                        btnInvite.setVisibility(View.GONE);
                                        btnSend.setVisibility(View.VISIBLE);
                                        btnInvite.setEnabled(true);
                                    } else {
                                        sendInviteNotification();
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.i(TAG, "Could not check if friend had chat key before sending message, sending is aborted");
                        }
                    });

                }
            }
        });


        btnMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EasyImage.openChooserWithGallery(ChatActivity.this, getResources().getString(R.string.gallery_chooser), REQUEST_CODE_GALLERY_CAMERA);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                hideKeyboard();

                final String wishMessage = textComment.getText().toString().trim();
                if (!Network.isInternetAvailable(ChatActivity.this, true) || wishMessage.isEmpty()) {
                    return;
                } else {
                    btnSend.setEnabled(false);
                    // send text as wish
                    progressBar.toggleDialog(true);

                    usersRef.orderByChild("username").equalTo(friend.getUsername()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                                    FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                                    List<String> list = Arrays.asList(firebaseUserModel.getChatKeys().split(","));
                                    if(list.contains(chatKey))
                                        sendMessage(wishMessage);
                                    else {
                                        textComment.setText("");
                                        progressBar.toggleDialog(false);
                                        btnInvite.setEnabled(true);
                                        Toast.makeText(getApplicationContext(), "Recipient may have deleted this chat, so message could not be sent", Toast.LENGTH_SHORT).show();
                                        btnInvite.setVisibility(View.VISIBLE);
                                        btnSend.setVisibility(View.GONE);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.i(TAG, "Could not check if friend had chat key before sending message, sending is aborted");
                        }
                    });

                }
            }
        });

    }

    private void sendInviteNotification(){
        notificationRef.orderByChild("chatKey").equalTo(chatKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    DatabaseReference newRef = notificationRef.push();
                    Notification notificationObj = new Notification();
                    notificationObj.setSender(me.getUsername());
                    notificationObj.setChatKey(chatKey);
                    newRef.setValue(notificationObj).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "Successfully added notification to server");
                            btnInvite.setEnabled(true);
                            startActivity(new Intent(ChatActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            btnInvite.setEnabled(true);
                            Log.i(TAG, "Failed to add notification to server one may already exist");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                btnInvite.setEnabled(true);
                Log.i(TAG, "Error checking notification table in firebase server");
            }
        });
    }

    private void sendMessage(final String wishMessage){
        DatabaseReference newRef = messagesRef.push();
        newRef.setValue(makeNewMessageNode(Constants.MESSAGE_TYPE_TEXT,wishMessage), new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                    // show message failed to send (icon)
                    btnSend.setEnabled(true);
                    progressBar.toggleDialog(false);
                    Log.i(TAG, databaseError.getMessage());
                } else {
                    textComment.setText("");

                    if (friend != null) {

                        try {
                            StringEntity entity = generateEntity(Constants.MESSAGE_TYPE_TEXT, wishMessage);
                            if (entity == null){
                                Toast.makeText(ChatActivity.this, "Failed to make notification", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Network.createAsyncClient().post(getApplicationContext(), Constants.NOTIFICATION_URL, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
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

        messagesRef.removeEventListener(commentValueEventListener);
        messagesRef.addValueEventListener(commentValueEventListener);
    }

    private String findChatKey (FirebaseUserModel userModel, FirebaseUserModel withUser) {
        String lChatKey = "";
        if (!userModel.getChatKeys().equals("")) {
            String[] allKeys  = userModel.getChatKeys().split(",");
            for(String key : allKeys) {
                String username1 = key.split("-")[0];
                String username2 = key.split("-")[1];
                if (username1.equals(withUser.getUsername()) || username2.equals(withUser.getUsername())) {
                    lChatKey = key;
                    break;
                }
            }
        }
        return lChatKey ;
    }

    private void assignMessageEventListener (String key) {
        messagesRef = database.getReference("messages")
                .child("single")
                .child(key);
        //Value event listener for realtime data update
        messagesRef.addValueEventListener(commentValueEventListener);
    }




    private StringEntity generateEntity (String type, String wishMessage) {
        JSONObject params = new JSONObject();
        //params.put("registration_ids", registration_ids);
        StringEntity entity = null;
        try {
            params.put("to", friend.getDeviceToken());
            JSONObject payload = new JSONObject();
            payload.put("chatKey", chatKey); // used for extra intent in main activity
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("click_action", ".MainActivity");
            notificationObject.put("body", Constants.generateMediaText(this, type, wishMessage));
            notificationObject.put("title", user.profileName);
            notificationObject.put("tag", user.deviceId);
            params.put("data", payload);
            params.put("notification", notificationObject);

            entity = new StringEntity(params.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return entity;
    }

    private FirebaseMessageModel makeNewMessageNode (String type, String wishMessage) {
        final FirebaseMessageModel firebaseMessageModel = new FirebaseMessageModel();
        firebaseMessageModel.setText(wishMessage);
        firebaseMessageModel.setSenderDeviceId(user.deviceId);
        firebaseMessageModel.setSenderName(user.name);
        firebaseMessageModel.setReceiverName(friend.getUsername());
        firebaseMessageModel.setIsReceived(Constants.MESSAGE_SENT);
        firebaseMessageModel.setMediaType(type);
        return  firebaseMessageModel;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                Toast.makeText(ChatActivity.this, "Error choosing file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                // Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    Toast.makeText(ChatActivity.this, "Deleting captured image...", Toast.LENGTH_SHORT).show();
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(ChatActivity.this);
                    if (photoFile != null) photoFile.delete();
                }
            }

            @Override
            public void onImagesPicked(@NonNull List<File> imageFiles, EasyImage.ImageSource source, int type) {
                switch (type){
                    case REQUEST_CODE_GALLERY_CAMERA:
                        ImageViewDialogFragment imageViewDialog = ImageViewDialogFragment.newInstance(
                                imageFiles.get(0),
                                Constants.ACTION_SEND,
                                android.R.drawable.ic_menu_send);
                        imageViewDialog.setCancelable(false);
                        imageViewDialog.show(getSupportFragmentManager(), "ImageViewDialogFragment");
                        break;
                }
            }

        });
    }

    private void loadLocalData (String errorMsg) {
        progressBar.toggleDialog(false);
        ContactDBHelper db = new ContactDBHelper(this);
        if (db.isUserAlreadyInContacts(friend.getUsername())) {
            String[] friendInfo = db.getProfileNameAndPic(friend.getUsername());
            friend.setProfileName(friendInfo[0]);
            friend.setProfilePic(friendInfo[1]);
            ((TextView) actionBar.getCustomView().findViewById(R.id.profile_name)).setText(friend.getProfileName());
            Glide.with(getApplicationContext())
                    .load(friend.getProfilePic())
                    .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                    .into(((CircleImageView) actionBar.getCustomView().findViewById(R.id.profile_icon)));
        } else {
            ((TextView) actionBar.getCustomView().findViewById(R.id.profile_name)).setText(friend.getUsername());
            Glide.with(getApplicationContext())
                    .load(R.drawable.unknown)
                    .into(((CircleImageView) actionBar.getCustomView().findViewById(R.id.profile_icon)));
            ErrorDialogFragment errorDialog = ErrorDialogFragment.newInstance(errorMsg);
            errorDialog.setCancelable(false);
            errorDialog.show(getSupportFragmentManager(), "ErrorDialogFragment");
        }
        db.close();

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
        startActivity(new Intent (ChatActivity.this, SessionActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
        super.onBackPressed();
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
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
            MessageCell messageCell = new MessageCell(firebaseMessageModel.getSenderName() , firebaseMessageModel.getText(),
                    getDate(firebaseMessageModel.getCreatedDateLong()), firebaseMessageModel.getSenderDeviceId().equals(user.deviceId),
                    firebaseMessageModel.getIsReceived(), firebaseMessageModel.getMediaType());
            messageCell.setDateOnly(getDateOnly(firebaseMessageModel.getCreatedDateLong()));
            messageCells[counter] = messageCell;
        }

        MessagesListAdapter adapter = new MessagesListAdapter(this, messageCells);
        if(messegesThatNeedUpdating.size() > 0 && Network.isForeground(getApplicationContext()))
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
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM, yyyy, HH:mm a");

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

    @Override
    public void onActionPressed(String action, final Dialog dialog, final ProgressBar progressBar) {

        if (!Network.isInternetAvailable(this, true)) return;

        if (action.equals(Constants.ACTION_SEND)) {
            if (ImageViewDialogFragment.imageFile != null) {
                Uri imgUri = Uri.fromFile(ImageViewDialogFragment.imageFile);
                StorageReference mStorageRef = FirebaseStorage.getInstance()
                        .getReference().child("single/" + chatKey + "/pictures/" + imgUri.getLastPathSegment());
                mStorageRef.putFile(imgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress(0);
                            }
                        }, 5000);
                        dialog.dismiss();
                        ChatActivity.this.progressBar.toggleDialog(true);

                        DatabaseReference newRef = messagesRef.push();
                        @SuppressWarnings("VisibleForTests")
                        final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        newRef.setValue(makeNewMessageNode(Constants.MESSAGE_TYPE_PIC, downloadUrl.toString()), new DatabaseReference.CompletionListener() {

                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    // remove image from storage
                                    removeFailedImageUpload(downloadUrl.toString());
                                    // show message failed to send (icon)
                                    Log.i(TAG, databaseError.getMessage());
                                } else {
                                    StringEntity entity = generateEntity(Constants.MESSAGE_TYPE_PIC, downloadUrl.toString());
                                    if (entity == null){
                                        Toast.makeText(ChatActivity.this, "Failed to make notification", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    Network.createAsyncClient().post(getApplicationContext(), Constants.NOTIFICATION_URL, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                            ChatActivity.this.progressBar.toggleDialog(false);
                                            Log.i(TAG, responseString);
                                        }

                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, String responseString) {
                                            ChatActivity.this.progressBar.toggleDialog(false);
                                            Log.i(TAG, responseString);
                                        }
                                    });
                                }
                            }


                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, "Image Upload Failed", Toast.LENGTH_SHORT ).show();
                        dialog.dismiss();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        @SuppressWarnings("VisibleForTests")
                        double progresss = (100.0* taskSnapshot.getBytesTransferred()/ taskSnapshot.getTotalByteCount());
                        progressBar.setProgress((int)progresss);
                    }
                });

            }
        } else if (action.equals(Constants.ACTION_DOWNLOAD)) {
            if (ImageViewDialogFragment.imageFileString != null) {
                if (isStoragePermissionGranted()) {
                    Network.downloadImageToPhone(this, ImageViewDialogFragment.imageFileString);
                    dialog.dismiss();
                }
            }
        }

    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG,"Permission is granted");
                return true;
            } else {
                Toast.makeText(ChatActivity.this, "Please allow permission to write to storage.", Toast.LENGTH_SHORT).show();
                Log.i(TAG,"Permission is revoked");
                //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.i(TAG,"Permission is granted");
            return true;
        }
    }

    private void removeFailedImageUpload (String uri) {
        StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(uri);
        photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                ChatActivity.this.progressBar.toggleDialog(false);
                Log.i(TAG, "onSuccess: removed image from failed database update");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                ChatActivity.this.progressBar.toggleDialog(false);
                Toast.makeText(ChatActivity.this, "Error Removing old picture", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "onFailure: did not delete file in storage");
                e.printStackTrace();
            }
        });
    }

}

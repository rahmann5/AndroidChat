package com.example.naziur.androidchat.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MessagesListAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.fragment.ImageViewDialogFragment;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.MessageCell;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class GroupChatActivity extends AppCompatActivity implements ImageViewDialogFragment.ImageViewDialogListener, FirebaseHelper.FirebaseHelperListener{
    private static final String TAG = "GroupChatActivity";
    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    User user = User.getInstance();

    EditText textComment;
    CircleImageView btnSend, btnMedia;
    FloatingActionButton sendBottom;
    FirebaseDatabase database;
    ListView listView;
    List<FirebaseMessageModel> messages = new ArrayList<FirebaseMessageModel>();
    FirebaseGroupModel groupModel;
    JSONArray registeredIds;
    private ActionBar actionBar;
    private ProgressDialog progressBar;
    private String groupKey = "";
    private ValueEventListener msgValueEventListener;
    FirebaseHelper firebaseHelper;
    private ImageViewDialogFragment imageViewDialog;
    private ContactDBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Bundle extra = getIntent().getExtras();
        if (extra == null) {
            Toast.makeText(this, "Error occurred", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        db = new ContactDBHelper(this);
        listView = (ListView) findViewById(R.id.chattingList);
        textComment = (EditText) findViewById(R.id.comment_text);
        btnSend = (CircleImageView) findViewById(R.id.send_button);
        btnMedia = (CircleImageView) findViewById(R.id.media_button);
        sendBottom = (FloatingActionButton) findViewById(R.id.action_send_bottom);
        database = FirebaseDatabase.getInstance();
        groupKey = extra.getString("group_uid");
        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        // assuming that user is guaranteed member of group
        if (Network.isInternetAvailable(this, true)) {
            firebaseHelper.getGroupInfo(groupKey);
        } else {
            finish();
            // TO DO
        }

        msgValueEventListener = firebaseHelper.createMessageEventListener();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!Network.isInternetAvailable(GroupChatActivity.this, true)) {
                    return;
                }

                if(!textComment.getText().toString().trim().isEmpty()) {
                    hideKeyboard();
                    btnSend.setEnabled(false);
                    progressBar.toggleDialog(true);
                    firebaseHelper.checkGroupsKeys("users", FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_2, groupKey, getMembersThatNeedToReceiveMessage());
                } else {
                    Toast.makeText(GroupChatActivity.this, "You must enter some text before sending a message", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EasyImage.openChooserWithGallery(GroupChatActivity.this, getResources().getString(R.string.gallery_chooser), REQUEST_CODE_GALLERY_CAMERA);
            }
        });

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
                    if (Network.isInternetAvailable(GroupChatActivity.this, true)) {
                        imageViewDialog = ImageViewDialogFragment.newInstance(
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
                    Toast.makeText(GroupChatActivity.this, "Copied", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Network.isInternetAvailable(this, true)) {
            progressBar.toggleDialog(true);
            firebaseHelper.toggleMsgEventListeners("group", groupKey, msgValueEventListener, true);
        } else {
            Toast.makeText(this,"You need internet to view or send messages", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseHelper.toggleMsgEventListeners("single", groupKey, msgValueEventListener, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                Toast.makeText(GroupChatActivity.this, "Error choosing file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                // Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    Toast.makeText(GroupChatActivity.this, "Deleting captured image...", Toast.LENGTH_SHORT).show();
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(GroupChatActivity.this);
                    if (photoFile != null) photoFile.delete();
                }
            }

            @Override
            public void onImagesPicked(@NonNull List<File> imageFiles, EasyImage.ImageSource source, int type) {
                switch (type){
                    case REQUEST_CODE_GALLERY_CAMERA:
                        imageViewDialog = ImageViewDialogFragment.newInstance(
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

    private void createCustomActionBar () {
        actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.toolbar);
        actionBar.getCustomView().findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(GroupChatActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });
        TextView textView = (TextView) actionBar.getCustomView().findViewById(R.id.group_members);
        String members = "You, ";
        String[] membersArr = getMembersThatNeedToReceiveMessage();
        for(int i =0 ; i < membersArr.length; i++ ){
            if(db.isUserAlreadyInContacts(membersArr[i]))
                members += db.getProfileNameAndPic(membersArr[i])[0];
            else
                members += membersArr[i];

            if(i < membersArr.length-1){
                members += ", ";
            }
        }
        textView.setText(members);
        actionBar.getCustomView().findViewById(R.id.group_members).setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.view_details :
                break;
            case R.id.leave_group :
                leaveGroup();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void leaveGroup () {
        DatabaseReference groupRef = database.getReference("groups").orderByChild("groupKey").equalTo(groupKey).getRef();
        groupRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {

                for (MutableData data : mutableData.getChildren()) {
                    FirebaseGroupModel groupData = data.getValue(FirebaseGroupModel.class);

                    if (groupData == null) return Transaction.success(mutableData);

                    if (groupData.getGroupKey().equals(groupKey)) {
                        String [] membersNames = groupData.getMembers().split(",");
                        String newMembersList = "";
                        for (String username : membersNames) {
                            if (!username.equals(user.name))
                                newMembersList += (newMembersList.equals("")) ? username :  "," + username ;
                        }

                        groupData.setMembers(newMembersList);
                        data.setValue(groupData);
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    leaveGroupFromUser();
                } else {
                    Toast.makeText(GroupChatActivity.this, "Error leaving group", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, databaseError.getMessage());
                }
            }
        });
    }

    private void leaveGroupFromUser(){
        DatabaseReference userRef = database.getReference("users").orderByChild("username").equalTo(user.name).getRef();
        userRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {

                for(MutableData data : mutableData.getChildren()) {
                    FirebaseUserModel firebaseUser = data.getValue(FirebaseUserModel.class);

                    if (firebaseUser == null) return Transaction.success(mutableData);

                    if (firebaseUser.getUsername().equals(user.name)) {
                        String [] groupsKeys = firebaseUser.getGroupKeys().split(",");
                        String newKeys = "";
                        for (String gKey : groupsKeys) {
                            if (!gKey.equals(groupKey)) {
                                newKeys += (newKeys.equals(""))? gKey : ","  + gKey;
                            }
                        }

                        firebaseUser.setGroupKeys(newKeys);

                        data.setValue(firebaseUser);
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null){
                    finish();
                    Toast.makeText(GroupChatActivity.this, "Successfully left group", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(TAG, databaseError.getMessage());
                }
            }
        });
    }

    public void updateListView() {
        Log.i(TAG, "Inside prepareWishList()");

        int totalWishes = messages.size();

        Log.i(TAG, "Total Wishes : " + totalWishes);

        MessageCell[] messageCells;
        messageCells = new MessageCell[totalWishes];

        for (int counter = 0; counter < totalWishes; counter++) {
            final FirebaseMessageModel firebaseMessageModel = messages.get(counter);
            MessageCell messageCell = new MessageCell(firebaseMessageModel.getSenderName() , firebaseMessageModel.getText(),
                    ChatActivity.getDate(firebaseMessageModel.getCreatedDateLong()), firebaseMessageModel.getSenderDeviceId().equals(user.deviceId),
                    firebaseMessageModel.getIsReceived(), firebaseMessageModel.getMediaType());
            messageCell.setDateOnly( ChatActivity.getDateOnly(firebaseMessageModel.getCreatedDateLong()));
            messageCells[counter] = messageCell;
        }

        MessagesListAdapter adapter = new MessagesListAdapter(this, messageCells);
        // Assign adapter to ListView
        listView.setAdapter(adapter);

        listView.setSelection(listView.getCount() - 1);

        listView.requestFocus();
    }

    public void hideKeyboard() {
        try  {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {
            Log.i(TAG, "Exception while hiding keyboard");
        }
    }

    private void sendMessage(String wishMessage) {
        System.out.println("Sending messages to "+registeredIds.length());
        firebaseHelper.updateMessageNode(this, "group", groupKey, wishMessage, null, Constants.MESSAGE_TYPE_TEXT,registeredIds, groupModel.getTitle());
    }

    private String[] getMembersThatNeedToReceiveMessage(){
        String [] membersIngroup = groupModel.getMembers().split(",");
        String [] members = new String[membersIngroup.length];
        if(groupModel.getAdmin().equals(user.name))
            return membersIngroup;
        else {
            int counter = 0;
            members[counter] = groupModel.getAdmin();
            for (int i = 0; i < membersIngroup.length; i++) {
                if (!membersIngroup[i].equals(user.name)) {
                    counter++;
                    members[counter] = membersIngroup[i];
                }
            }
            return members;
        }
    }

    @Override
    public void onActionPressed() {
        firebaseHelper.checkGroupsKeys("users", FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_3 ,groupKey, getMembersThatNeedToReceiveMessage());
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch(tag){
            case "createMessageEventListener":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        messages.clear();
                        break;
                    case FirebaseHelper.CONDITION_2 :
                        updateListView();
                        progressBar.toggleDialog(false);
                        break;
                }
                break;
            case "getGroupInfo":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        groupModel = container.getGroupModel();
                        createCustomActionBar ();
                        ((TextView) actionBar.getCustomView().findViewById(R.id.profile_name)).setText(groupModel.getTitle());
                        Glide.with(getApplicationContext())
                                .load(groupModel.getPic())
                                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                                .into(((CircleImageView) actionBar.getCustomView().findViewById(R.id.profile_icon)));
                        List<String> members = Arrays.asList(getMembersThatNeedToReceiveMessage());
                        firebaseHelper.getDeviceTokensFor(members, groupModel.getTitle(), groupModel.getGroupKey());
                        break;
                }
                break;
            case "getDeviceTokensFor":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        registeredIds = container.getJsonArray();
                        break;
                }
                break;
            case "checkGroupsKeys":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        registeredIds = new JSONArray();
                        break;
                    case FirebaseHelper.CONDITION_2:
                        if(registeredIds.length() == 0) {
                            Toast.makeText(this, "Their are no more receivers on this chat: "+registeredIds.length(), Toast.LENGTH_SHORT).show();
                            progressBar.toggleDialog(false);
                        } else {
                            String wishMessage = textComment.getText().toString().trim();
                            sendMessage(wishMessage);
                        }
                        break;
                    case FirebaseHelper.CONDITION_3:
                        imageViewDialog.sendImageAndMessage(groupKey, null, this, groupModel, registeredIds);
                        break;
                }
                break;
            case "updateMessageNode":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                    case FirebaseHelper.CONDITION_2:
                        progressBar.toggleDialog(false);
                        textComment.setText("");
                        btnSend.setEnabled(true);
                        break;
                }
                break;
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch(tag){
            case "updateMessageNode":
            case "checkGroupsKeys":
                btnSend.setEnabled(true);
                progressBar.toggleDialog(false);
                break;
            case "createMessageEventListener":
                progressBar.toggleDialog(false);
                break;
        }
        Log.i(TAG, tag+" "+databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        switch(tag){
            case "createMessageEventListener":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        messages.add(container.getMsgModel());
                        break;
                }
                break;
            case "checkGroupsKeys":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        registeredIds.put(container.getString());
                        break;
                }
                break;
        }
    }
}

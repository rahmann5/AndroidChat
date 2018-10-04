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
import android.widget.LinearLayout;
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
    private ValueEventListener msgValueEventListener, groupListener;
    private LinearLayout chatControl, footerMsg;
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
        chatControl = (LinearLayout) findViewById(R.id.chat_control);
        footerMsg = (LinearLayout) findViewById(R.id.footer_message);
        database = FirebaseDatabase.getInstance();
        groupKey = extra.getString("group_uid");
        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        registeredIds = new JSONArray();
        // assuming that user is guaranteed member of group
        if (!Network.isInternetAvailable(this, true)) {
            finish();
        }

        msgValueEventListener = firebaseHelper.createMessageEventListener();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!Network.isInternetAvailable(GroupChatActivity.this, true)) {
                    return;
                }

                if(!textComment.getText().toString().trim().isEmpty()) {
                    if(registeredIds.length() > 0) {
                        hideKeyboard();
                        btnSend.setEnabled(false);
                        progressBar.toggleDialog(true);
                        firebaseHelper.checkGroupsKeys("users", FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_2, groupKey, getMembersThatNeedToReceiveMessage());
                    }else
                        Toast.makeText(GroupChatActivity.this, "Their is nobody else in this chat to speak to", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GroupChatActivity.this, "You must enter some text before sending a message", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EasyImage.openChooserWithGallery(GroupChatActivity.this, getResources().getString(R.string.chat_gallery_chooser), REQUEST_CODE_GALLERY_CAMERA);
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
            groupListener = firebaseHelper.getGroupInfo(groupKey);
            firebaseHelper.toggleMsgEventListeners("group", groupKey, msgValueEventListener, true);
        } else {
            Toast.makeText(this,"You need internet to view or send messages", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseHelper.toggleMsgEventListeners("single", groupKey, msgValueEventListener, false);
        firebaseHelper.toggleListenerFor("groups", "groupKey", groupKey, groupListener, false, false);
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

        textView.setText(getActionBarString());
        actionBar.getCustomView().findViewById(R.id.group_members).setVisibility(View.VISIBLE);
    }

    private String getActionBarString(){
        String[] membersArr = getMembersThatNeedToReceiveMessage();
        boolean isStillInGroup = false;
        String members = "";
        for(int i =0 ; i < membersArr.length; i++ ){
            if(membersArr[i].equals(user.name))
                isStillInGroup = true;
            members += db.getProfileInfoIfExists(membersArr[i])[0];
            if(i < membersArr.length-1)
                members+=", ";
        }
        if(isStillInGroup)
            members = "You, "+members;
        toggleFooterSection(isStillInGroup);
        return members;
    }

    private void toggleFooterSection(boolean show){
        if(show) {
            chatControl.setVisibility(View.VISIBLE);
            footerMsg.setVisibility(View.GONE);
        }else {
            chatControl.setVisibility(View.GONE);
            footerMsg.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_chat_menu, menu);
        if (groupModel != null) {
            if (groupModel.getAdmin().equals("")) {
                menu.findItem(R.id.admin).setVisible(true);
            } else {
                menu.findItem(R.id.admin).setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.view_details :
                Intent intent = new Intent(GroupChatActivity.this, GroupDetailActivity.class);
                intent.putExtra("g_uid", groupKey);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case R.id.admin :
                firebaseHelper.updateGroupMembers(user.name, null, groupKey, true);
                break;

        }

        return super.onOptionsItemSelected(item);
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
        firebaseHelper.updateMessageNode(this, "group", groupKey, wishMessage, null, Constants.MESSAGE_TYPE_TEXT,registeredIds, groupModel.getTitle());
    }

    private String[] getMembersThatNeedToReceiveMessage(){
        String [] membersIngroup = groupModel.getMembers().split(",");
        List<String> members = new ArrayList<>();
        if(groupModel.getAdmin().equals(user.name) && !membersIngroup[0].equals(""))
            return membersIngroup;
        else {
            if(!groupModel.getAdmin().isEmpty())
                members.add(groupModel.getAdmin());
            for (int i = 0; i < membersIngroup.length; i++) {
                if (!membersIngroup[i].equals(user.name)) {
                    members.add(membersIngroup[i]);
                }
            }
            String[] stockArr = new String[members.size()];

            return  members.toArray(stockArr);
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
                                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.ic_group_unknown))
                                .into(((CircleImageView) actionBar.getCustomView().findViewById(R.id.profile_icon)));
                        List<String> members = Arrays.asList(getMembersThatNeedToReceiveMessage());
                        if(members.size() > 0)
                            firebaseHelper.getDeviceTokensFor(members, groupModel.getTitle(), groupModel.getGroupKey());
                        invalidateOptionsMenu();
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
                        String wishMessage = textComment.getText().toString().trim();
                        sendMessage(wishMessage);
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
            case "updateGroupMembers" :
                String wishMessage = "New admin is " + container.getString();
                firebaseHelper.updateMessageNode(this, "group", groupKey, wishMessage , null, Constants.MESSAGE_TYPE_SYSTEM, null, groupModel.getTitle());
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
            case "updateGroupMembers":
                Toast.makeText(this, "Failed to become admin", Toast.LENGTH_SHORT).show();
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

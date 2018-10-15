package com.example.naziur.androidchat.activities;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class GroupChatActivity extends AuthenticatedActivity implements ImageViewDialogFragment.ImageViewDialogListener, FirebaseHelper.FirebaseHelperListener{
    private static final String TAG = "GroupChatActivity";
    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    User user = User.getInstance();

    EmojiconEditText textComment;
    CircleImageView btnSend, btnMedia, btnEmoji;
    FloatingActionButton sendBottom;
    FirebaseDatabase database;
    ListView listView;
    List<FirebaseMessageModel> messages = new ArrayList<FirebaseMessageModel>();
    FirebaseGroupModel groupModel;
    JSONArray registeredIds;
    private ActionBar actionBar;
    private ProgressDialog progressBar;
    private String groupKey = "";
    private  EmojIconActions emojIcon;
    private ValueEventListener msgValueEventListener, groupListener;
    private LinearLayout chatControl, footerMsg;
    FirebaseHelper firebaseHelper;
    private ImageViewDialogFragment imageViewDialog;
    private ContactDBHelper db;
    private IntentFilter mFilter = new IntentFilter("my.custom.action");
    public int currentFirstVisibleItem, currentVisibleItemCount, totalItem, currentScrollState;
    private List<FirebaseMessageModel> tempMsg;
    private boolean isScrolling = false;


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
        mFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        listView = (ListView) findViewById(R.id.chattingList);
        textComment = (EmojiconEditText) findViewById(R.id.comment_text);
        btnSend = (CircleImageView) findViewById(R.id.send_button);
        btnMedia = (CircleImageView) findViewById(R.id.media_button);
        btnEmoji = (CircleImageView) findViewById(R.id.emoji_button);
        sendBottom = (FloatingActionButton) findViewById(R.id.action_send_bottom);
        chatControl = (LinearLayout) findViewById(R.id.chat_control);
        footerMsg = (LinearLayout) findViewById(R.id.footer_message);
        database = FirebaseDatabase.getInstance();
        groupKey = extra.getString("group_uid");
        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);

        textComment.setUseSystemDefault(true);
        emojIcon = new EmojIconActions(this, findViewById(R.id.root_view), textComment, btnEmoji);
        emojIcon.setUseSystemEmoji(true);
        emojIcon.ShowEmojIcon();
        emojIcon.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {
                Log.e(TAG,"keyboard opened");
            }
            @Override
            public void onKeyboardClose() {
                Log.e(TAG,"Keyboard closed");
            }
        });

        textComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emojIcon.closeEmojIcon();
            }
        });

        btnEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emojIcon.ShowEmojIcon();
            }
        });


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

                if (!textComment.getText().toString().trim().isEmpty()) {

                    if(getMembersThatNeedToReceiveMessage().length > 0) {
                        hideKeyboard();
                        btnSend.setEnabled(false);
                        progressBar.toggleDialog(true);
                        firebaseHelper.checkGroupsKeys("users", FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_2, groupKey, getMembersThatNeedToReceiveMessage());
                    }

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
                currentFirstVisibleItem = firstVisibleItem;
                currentVisibleItemCount = visibleItemCount;
                totalItem = totalItemCount;
            }
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                if(listView.getCount() != listView.getLastVisiblePosition() + 1) {
                    sendBottom.setVisibility(View.VISIBLE);
                } else {
                    sendBottom.setVisibility(View.GONE);
                }
                currentScrollState = scrollState;
                if (!isScrolling) {
                    if (currentVisibleItemCount > 0 && currentScrollState == SCROLL_STATE_IDLE) {
                        if (currentFirstVisibleItem == 0) {
                            firebaseHelper.getNextNMessages("group", groupKey, messages.get(0).getId(), 4);
                            isScrolling = true;
                        }
                    }
                }
            }
        });
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extra = intent.getExtras();
            if (extra != null) {
                if(groupKey.equals(extra.getString("tag"))) {
                    abortBroadcast();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Network.isInternetAvailable(this, true)) {
            progressBar.toggleDialog(true);
            groupListener = firebaseHelper.getGroupInfo(groupKey);
            firebaseHelper.toggleListenerFor("groups", "groupKey", groupKey, groupListener, true, false);
            firebaseHelper.toggleMsgEventListeners("group", groupKey, msgValueEventListener, 1, true, false);
        } else {
            Toast.makeText(this,"You need internet to view or send messages", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseHelper.toggleMsgEventListeners("group", groupKey, msgValueEventListener,1, false, false);
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
    }

    private String getActionBarString(){
        boolean isStillInGroup = false;
        String members = "";
        if(!groupModel.getMembers().isEmpty()){
            String [] membersArr = groupModel.getMembers().split(",");

            if(!membersArr[0].isEmpty()) {
                ArrayList<String> test = new ArrayList<String>(Arrays.asList(membersArr));
                if(test.contains(user.name)){
                    isStillInGroup = true;
                    test.remove(user.name);
                }

                for (int i = 0; i < test.size(); i++) {

                    members += db.getProfileInfoIfExists(test.get(i))[0];

                    if (i < test.size() - 1)
                            members += ", ";
                }
            }
        }

        if(groupModel.getAdmin().equals(user.name))
            isStillInGroup = true;

        if(!groupModel.getAdmin().isEmpty() && !groupModel.getAdmin().equals(user.name)) {
            if(!members.isEmpty())
                members = members + ", " + db.getProfileInfoIfExists(groupModel.getAdmin())[0];
            else
                members = db.getProfileInfoIfExists(groupModel.getAdmin())[0];
        }

        if(isStillInGroup) {
            if(!members.isEmpty())
                members = "You, " + members;
            else
                members = "You";

        }

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

    private boolean isAMember () {
        List<String> members = Arrays.asList(groupModel.getMembers().split(","));
        return members.contains(user.name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_chat_menu, menu);
        if (groupModel != null) {
            if (groupModel.getAdmin().equals("") && isAMember ()) {
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

    public void updateListView(boolean scrollEnd) {
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

        if (scrollEnd) listView.setSelection(listView.getCount() - 1);

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
        if(groupModel.getAdmin().equals(user.name)) {
            if(!groupModel.getMembers().isEmpty())
                return membersIngroup;
            else {
                String[] stockArr = new String[members.size()];
                return  members.toArray(stockArr);
            }
        }else {
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
        if(getMembersThatNeedToReceiveMessage().length > 0)
            firebaseHelper.checkGroupsKeys("users", FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_3 ,groupKey, getMembersThatNeedToReceiveMessage());
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch(tag){
            case "createMessageEventListener":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        break;
                    case FirebaseHelper.CONDITION_2 :
                        updateListView(true);
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
                            firebaseHelper.getDeviceTokensFor(members, groupModel.getTitle(), groupModel.getGroupKey(), true);
                        invalidateOptionsMenu();
                        break;

                    case FirebaseHelper.CONDITION_2:
                        progressBar.toggleDialog(false);
                        Toast.makeText(this, "This group has been deleted." , Toast.LENGTH_LONG).show();
                        finish();
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
            case "getNextFiveMessages" :
                switch (condition) {
                    case FirebaseHelper.CONDITION_1 :
                        //lastKey = container.getString();
                        Collections.reverse(tempMsg);
                        for (FirebaseMessageModel fbm : tempMsg) {
                            messages.add(0, fbm);
                        }
                        if (messages.size() <= 6) {
                            updateListView(true);
                        } else {
                            updateListView(false);
                        }
                        progressBar.toggleDialog(false);
                        tempMsg = new ArrayList<>();
                        isScrolling = false;
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
                //updateListView(true);
                progressBar.toggleDialog(false);
                break;
            case "updateGroupMembers":
                Toast.makeText(this, "Failed to become admin", Toast.LENGTH_SHORT).show();
                break;
            case "getNextFiveMessages" :
                isScrolling = false;
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
                        FirebaseMessageModel firebaseMessageModel = container.getMsgModel();
                        if (firebaseMessageModel != null) {
                            if (!messages.isEmpty()) {
                                if ((!messages.get(messages.size()-1).getId().equals(firebaseMessageModel.getId())))
                                    messages.add(firebaseMessageModel);
                            } else {
                                messages.add(firebaseMessageModel);
                                tempMsg = new ArrayList<>();
                                firebaseHelper.getNextNMessages("group", groupKey, firebaseMessageModel.getId(), 5);
                            }
                        }
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
            case "getNextFiveMessages" :
                switch (condition) {
                    case FirebaseHelper.CONDITION_1 :
                        FirebaseMessageModel firebaseMessageModel = container.getMsgModel();
                        tempMsg.add(firebaseMessageModel);
                        break;
                }
                break;
        }
    }
}

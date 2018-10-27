package com.example.naziur.androidchat.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.adapter.MessagesListAdapter;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.fragment.ImageViewDialogFragment;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.MessageCell;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;


public class ChatActivity extends AuthenticatedActivity implements ImageViewDialogFragment.ImageViewDialogListener,
        FirebaseHelper.FirebaseHelperListener{
    private static final String TAG = "ChatActivity";
    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    private static final int LOAD_AMOUNT = 6 ;
    User user = User.getInstance();

    ListView listView;

    EmojiconEditText textComment;
    CircleImageView btnSend, btnMedia, btnInvite, btnEmoji;
    FloatingActionButton sendBottom;
    List<FirebaseMessageModel> messages = new ArrayList<FirebaseMessageModel>();

    private ValueEventListener commentValueEventListener, friendValueEvent;

    private ProgressDialog progressBar;

    private ActionBar actionBar;
    private FirebaseUserModel friend, me;

    private ImageViewDialogFragment imageViewDialog;

    private String chatKey;

    private FirebaseHelper firebaseHelper;

    private IntentFilter mFilter = new IntentFilter("my.custom.action");
    private  EmojIconActions emojIcon;
    public int currentFirstVisibleItem, currentVisibleItemCount, totalItem, currentScrollState;
    private List<FirebaseMessageModel> tempMsg;
    private boolean isScrolling = false;
    private boolean initialLoad = false;

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
        mFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        createCustomActionBar();


        listView = (ListView) findViewById(R.id.chattingList);

        textComment = (EmojiconEditText) findViewById(R.id.comment_text);
        btnSend = (CircleImageView) findViewById(R.id.send_button);
        btnMedia = (CircleImageView) findViewById(R.id.media_button);
        btnEmoji = (CircleImageView) findViewById(R.id.emoji_button);
        btnInvite = (CircleImageView) findViewById(R.id.send_invite_button);
        sendBottom = (FloatingActionButton) findViewById(R.id.action_send_bottom);
        textComment.setUseSystemDefault(true);

        final RelativeLayout rootView = (RelativeLayout)findViewById(R.id.root_view);
        emojIcon = new EmojIconActions(ChatActivity.this,rootView , textComment, btnEmoji);
        emojIcon.setIconsIds(hani.momanii.supernova_emoji_library.R.drawable.ic_action_keyboard,
                R.drawable.ic_emoji);
        emojIcon.setUseSystemEmoji(true);
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
                hideSoftKeyBoard(ChatActivity.this);
                emojIcon.ShowEmojIcon();
                showSoftKeyBoard();
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
                    if (Network.isInternetAvailable(ChatActivity.this, true)) {
                        imageViewDialog = ImageViewDialogFragment.newInstance(
                                c.getMessageText(),
                                Constants.ACTION_DOWNLOAD,
                                android.R.drawable.ic_menu_upload,
                                R.drawable.unknown);
                        imageViewDialog.setCancelable(true);
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
                currentFirstVisibleItem = firstVisibleItem;
                currentVisibleItemCount = visibleItemCount;
                totalItem = totalItemCount;
            }
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                toggleGoBottomArrow();

                currentScrollState = scrollState;
                if (!isScrolling) {
                    if (currentVisibleItemCount > 0 && currentScrollState == SCROLL_STATE_IDLE) {
                        if (currentFirstVisibleItem == 0) {
                            tempMsg = new ArrayList<>();
                            firebaseHelper.getNextNMessages("single", chatKey, messages.get(0).getId(), LOAD_AMOUNT);
                            isScrolling = true;
                        }
                    }
                }
            }
        });

        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);

        commentValueEventListener = firebaseHelper.createMessageEventListener(false);


        btnInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (!Network.isInternetAvailable(ChatActivity.this, true)) {
                return;
            } else {
                btnInvite.setEnabled(false);
                firebaseHelper.checkKeyListKey("users", FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_2 ,chatKey, friend.getUsername());
            }
            }
        });


        btnMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlOffline = false;
                emojIcon.closeEmojIcon();
                hideSoftKeyBoard(ChatActivity.this);
                EasyImage.openChooserWithGallery(ChatActivity.this, getResources().getString(R.string.chat_gallery_chooser), REQUEST_CODE_GALLERY_CAMERA);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendButtonClick ();
            }
        });


    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        hideSoftKeyBoard(ChatActivity.this);
        emojIcon.ShowEmojIcon();
    }

    private void sendButtonClick () {
        hideSoftKeyBoard(ChatActivity.this);

        final String wishMessage = textComment.getText().toString().trim();
        if (!Network.isInternetAvailable(ChatActivity.this, false) || wishMessage.isEmpty()) {
            return;
        } else {
            btnSend.setEnabled(false);
            // send text as wish
            progressBar.toggleDialog(true);
            firebaseHelper.checkKeyListKey("users", FirebaseHelper.CONDITION_3, FirebaseHelper.CONDITION_5 ,chatKey,  friend.getUsername());
        }
    }

    private void toggleGoBottomArrow() {
        if(listView.getCount() != listView.getLastVisiblePosition() + 1) {
            sendBottom.setVisibility(View.VISIBLE);
        } else {
            sendBottom.setVisibility(View.GONE);
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extra = intent.getExtras();
            if (extra != null) {
                if(friend != null) {
                    if(friend.getDeviceId().equals(extra.getString("tag"))) {
                        abortBroadcast();
                    }
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (Network.isInternetAvailable(this, false)) {
            progressBar.toggleDialog(true);
        } else {
            loadLocalData();
        }
        friendValueEvent = firebaseHelper.getValueEventListener(friend.getUsername(), FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION, FirebaseHelper.CONDITION_1, FirebaseUserModel.class);
        firebaseHelper.toggleListenerFor("users", "username" ,friend.getUsername(), friendValueEvent , true, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseHelper.toggleUnreadMsgEventListeners("single", chatKey, commentValueEventListener,false, false);
        firebaseHelper.toggleListenerFor("users", "username" ,friend.getUsername(), friendValueEvent , false, false);
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
                        imageViewDialog = ImageViewDialogFragment.newInstance(
                                imageFiles.get(0),
                                Constants.ACTION_SEND,
                                android.R.drawable.ic_menu_send,
                                R.drawable.unknown);
                        imageViewDialog.setCancelable(true);
                        imageViewDialog.show(getSupportFragmentManager(), "ImageViewDialogFragment");
                        break;
                }
            }

        });
    }

    private void loadLocalData () {
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
                Intent chatDetailActivity = new Intent(this, ChatDetailActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                chatDetailActivity.putExtra("username", friend.getUsername());
                Constants.animateTransition(this, chatDetailActivity,actionBar.getCustomView().findViewById(R.id.profile_icon), getResources().getString(R.string.picture_transition_name));

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
                startActivity(new Intent (ChatActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });
    }

    public void showSoftKeyBoard(){
        InputMethodManager imm = (InputMethodManager)   getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void hideSoftKeyBoard(Activity activity){
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    public void updateListView(boolean scrollToBottom) {
        Log.i(TAG, "Inside prepareWishList()");

        int totalWishes = messages.size();

        Log.i(TAG, "Total Wishes : " + totalWishes);

        MessageCell[] messageCells;
        messageCells = new MessageCell[totalWishes];

        List<String> messegesThatNeedUpdating = new ArrayList<>();
        for (int counter = 0; counter < totalWishes; counter++) {
            final FirebaseMessageModel firebaseMessageModel = messages.get(counter);
            if(!firebaseMessageModel.getSenderName().equals(me.getUsername()) && firebaseMessageModel.getIsReceived() == Constants.MESSAGE_SENT) {
                firebaseMessageModel.setIsReceived(Constants.MESSAGE_RECEIVED);
                messegesThatNeedUpdating.add(firebaseMessageModel.getId());
            }
            MessageCell messageCell = new MessageCell(firebaseMessageModel.getSenderName() , firebaseMessageModel.getText(),
                    getDate(firebaseMessageModel.getCreatedDateLong()), firebaseMessageModel.getSenderDeviceId().equals(user.deviceId),
                    firebaseMessageModel.getIsReceived(), firebaseMessageModel.getMediaType());
            messageCell.setDateOnly(getDateOnly(firebaseMessageModel.getCreatedDateLong()));
            messageCells[counter] = messageCell;
        }

        MessagesListAdapter adapter = new MessagesListAdapter(this, messageCells);
        if(messegesThatNeedUpdating.size() > 0 && Network.isForeground(getApplicationContext()))
            firebaseHelper.updateFirebaseMessageStatus(chatKey, messegesThatNeedUpdating);
        // Assign adapter to ListView
        listView.setAdapter(adapter);

        if (scrollToBottom) listView.setSelection(listView.getCount() - 1);

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
    public void onActionPressed() {
        firebaseHelper.checkKeyListKey("users", FirebaseHelper.CONDITION_4, FirebaseHelper.CONDITION_5 ,chatKey, friend.getUsername());
    }

    private boolean matchesMyKey(String friendKey, FirebaseUserModel me) {
        for(String key: me.getChatKeys().split(",")){
            if(key.equals(friendKey))
                return true;
        }
        return false;
    }

    private void setOnlineStatus (FirebaseUserModel returnedFriend) {
        int onlineRes = (returnedFriend.getOnline()) ? R.string.online_status_online : R.string.online_status_offline;
        ((TextView) actionBar.getCustomView().findViewById(R.id.group_members)).setText(getResources().getString(onlineRes));
    }

    private boolean isContainedIn (FirebaseMessageModel firebaseMessageModel) {
        for (int i = messages.size()-1; i > -1; i--) {
            if (messages.get(i).getId().equals(firebaseMessageModel.getId())) {
                return true;
            }
        }
        return false;
    }

    private void updateListViewOnChange () {
        boolean changed = false;
        for (int i = messages.size()-1; i > -1; i--) {
            if (messages.get(i).getIsReceived() == Constants.MESSAGE_SENT) {
                messages.get(i).setIsReceived(Constants.MESSAGE_RECEIVED);
                changed = true;
            }
        }
        if (changed) updateListView(true);
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("createMessageEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 : //WHEN UNREAD MESSAGES ARE FOUND
                    if (!messages.isEmpty() && messages.size() < LOAD_AMOUNT) { // not enough unread messages so load older messages
                        initialLoad = true;
                        tempMsg = new ArrayList<>();
                        firebaseHelper.getNextNMessages("single", chatKey, messages.get(0).getId(), LOAD_AMOUNT-messages.size());
                    } else {
                        updateListView(true);
                    }

                    progressBar.toggleDialog(false);
                    break;

                case FirebaseHelper.CONDITION_2 : //WHEN NO UNREAD MESSAGES ARE FOUND
                    if (messages.isEmpty()) {
                        initialLoad = true;
                        tempMsg = new ArrayList<>();
                        firebaseHelper.getNextNMessages("single", chatKey, "", LOAD_AMOUNT);
                    } else {
                        updateListViewOnChange ();
                        progressBar.toggleDialog(false);
                    }
                    break;
            }
        } else if (tag.equals("getValueEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1:
                    FirebaseUserModel returnedFriend = (FirebaseUserModel) container.getObject();
                    if (friend.getDeviceId().equals("")) {
                        friend = returnedFriend;
                        ((TextView) actionBar.getCustomView().findViewById(R.id.profile_name)).setText(friend.getProfileName());
                        Glide.with(getApplicationContext())
                                .load(friend.getProfilePic())
                                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                                .into(((CircleImageView) actionBar.getCustomView().findViewById(R.id.profile_icon)));
                        ValueEventListener singelEvent = firebaseHelper.getValueEventListener(user.name, FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION, FirebaseHelper.CONDITION_2, FirebaseUserModel.class);
                        firebaseHelper.toggleListenerFor("users", "username" ,user.name, singelEvent, true, true);
                    } else {
                        progressBar.toggleDialog(false);
                    }
                    setOnlineStatus(returnedFriend);
                    break;
                case FirebaseHelper.CONDITION_2:
                    me = (FirebaseUserModel) container.getObject();
                    if (me != null) {
                        firebaseHelper.toggleUnreadMsgEventListeners("single", chatKey, commentValueEventListener, true, false);
                        String friendKey = findChatKey(friend, me);
                        btnInvite.setVisibility(View.GONE);
                        btnSend.setVisibility(View.VISIBLE);
                        btnMedia.setEnabled(true);
                        if (friendKey.equals("") || !matchesMyKey(friendKey, me)) {
                            // change send button to be able to send notification instead
                            btnInvite.setVisibility(View.VISIBLE);
                            btnMedia.setEnabled(false);
                            btnSend.setVisibility(View.GONE);
                        }
                    } else {
                        Log.i(TAG, "ME IS NULL");
                        progressBar.toggleDialog(false);
                        finish();
                    }
                    break;
            }
        }  else if (tag.equals("checkKeyListKey" )) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    progressBar.toggleDialog(false);
                    btnInvite.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);
                    btnMedia.setEnabled(true);
                    btnInvite.setEnabled(true);
                    break;
                case FirebaseHelper.CONDITION_2:
                    FirebaseUserModel model = container.getUserModel();
                    if (model != null && !Network.isBlockListed(user.name, model.getBlockedUsers())) {
                        firebaseHelper.updateNotificationNode("chatKey", friend, chatKey);
                    } else {
                        Toast.makeText(this, getResources().getString(R.string.block_list_msg_blocked_by_them), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case FirebaseHelper.CONDITION_3:
                    String wishMessage = textComment.getText().toString().trim();
                    // send message
                    firebaseHelper.updateMessageNode(this, "single", chatKey, wishMessage, friend, Constants.MESSAGE_TYPE_TEXT,null, "");
                    break;
                case FirebaseHelper.CONDITION_4:
                    imageViewDialog.sendImageAndMessage(chatKey, friend, ChatActivity.this, null, null);
                    break;
                case FirebaseHelper.CONDITION_5:
                    textComment.setText("");
                    progressBar.toggleDialog(false);
                    btnInvite.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Recipient may have deleted this chat, so message could not be sent", Toast.LENGTH_SHORT).show();
                    btnInvite.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.GONE);
                    btnMedia.setEnabled(false);
                    if(imageViewDialog != null)
                        imageViewDialog.getDialog().dismiss();
                    break;
            }
        } else if (tag.equals("updateNotificationNode")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    Log.i(TAG, "Successfully added notification to server");
                    btnInvite.setEnabled(true);
                    startActivity(new Intent(ChatActivity.this, SessionActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                    break;
                case FirebaseHelper.CONDITION_2 :
                    progressBar.toggleDialog(false);
                    btnInvite.setEnabled(true);
                    Log.i(TAG, "Failed to add notification to server one may already exist");
                    break;
            }
        } else if (tag.equals("updateMessageNode")) {
            Log.i(TAG, container.getString());
            btnSend.setEnabled(true);
            progressBar.toggleDialog(false);
            textComment.setText("");
        }  else if (tag.equals("getNextNMessages")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    //lastKey = container.getString();
                    if (tempMsg.size() > 0) {
                        Collections.reverse(tempMsg);
                        for (FirebaseMessageModel fbm : tempMsg) {
                            messages.add(0, fbm);
                        }
                        if (initialLoad) {
                            initialLoad = false;
                            updateListView(true);
                        } else {
                            updateListView(false);
                            toggleGoBottomArrow();
                        }
                        tempMsg = new ArrayList<>();
                    } else if (!messages.isEmpty() && messages.size() < LOAD_AMOUNT) {
                        if (initialLoad) {
                            initialLoad = false;
                            updateListView(true);
                        }
                    }
                    progressBar.toggleDialog(false);
                    isScrolling = false;
                    break;

                case FirebaseHelper.CONDITION_2 :
                    progressBar.toggleDialog(false);
                    break;
            }
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag) {
            case "createMessageEventListener" :
                updateListView(false);
                break;

            case "getValueEventListener" :
                if (!friend.getDeviceId().equals("")) {
                    loadLocalData();
                    ValueEventListener singelEvent = firebaseHelper.getValueEventListener(user.name, FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION, FirebaseHelper.CONDITION_2, FirebaseUserModel.class);
                    firebaseHelper.toggleListenerFor("users", "username" ,user.name, singelEvent, true, true);
                } else if (!friend.getDeviceId().equals("") && me == null) {
                    Log.i(TAG, "ME IS NULL");
                    finish();
                }
                break;

            case "checkKeyListKey" :
                Log.i(TAG, "Could not check if friend had chat key before sending message, sending is aborted");
                break;
            case "updateNotificationNode" :
                btnInvite.setEnabled(true);
                Log.i(TAG, "Error checking notification table in firebase server");
                break;
            case "getNextNMessages" :
                isScrolling = false;
                break;

        }
        progressBar.toggleDialog(false);
        Log.i(TAG, tag + " " +databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        if (tag.equals("createMessageEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    FirebaseMessageModel firebaseMessageModel = container.getMsgModel();
                    if (friendValueEvent != null) {
                        if (!isContainedIn(firebaseMessageModel)) {
                            messages.add(firebaseMessageModel);
                        }

                    }
                    break;
            }
        }  else if (tag.equals("getNextNMessages")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    FirebaseMessageModel firebaseMessageModel = container.getMsgModel();
                    tempMsg.add(firebaseMessageModel);
                    break;
            }
        }
    }
}
package com.example.naziur.androidchat.fragment;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.activities.ChatActivity;
import com.example.naziur.androidchat.activities.ChatDetailActivity;
import com.example.naziur.androidchat.adapter.AllChatsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A fragment that allows users to manage all one to one chats.
 * Has two key thread(s): (1) to listen to any change in the users chat keys, (2) to get last message in each chat
 */
public class SingleSessionFragment extends Fragment implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = SingleSessionFragment.class.getSimpleName();
    private ValueEventListener userListener;
    private AllChatsAdapter myChatsdapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyChats;
    private User user = User.getInstance();
    private List<String> allChatKeys;
    private ContactDBHelper db;
    private ProgressDialog progressBar;
    private List<ValueEventListener> valueEventListeners;
    FirebaseHelper firebaseHelper;
    private SimpleDateFormat formatter;
    private ProgressBar chatProgress;
    private TextView textEmptyChat;
    public SingleSessionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_session_all_chats, container, false);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        valueEventListeners = new ArrayList<>();
        allChatKeys = new ArrayList<>();
        formatter = new SimpleDateFormat(getString(R.string.simple_date));
        emptyChats = (LinearLayout) rootView.findViewById(R.id.no_chats);
        chatProgress = (ProgressBar) rootView.findViewById(R.id.chat_progress);
        textEmptyChat = (TextView) rootView.findViewById(R.id.no_chats_text);
        recyclerView = rootView.findViewById(R.id.all_chats_list);
        progressBar = new ProgressDialog(getActivity(), R.layout.progress_dialog, true);
        db = new ContactDBHelper(getContext());
        firebaseHelper.updateAllLocalContactsFromFirebase(getActivity(), db);
        setUpRecyclerView();
        return rootView;
    }
    /*
    * All main execution starts here with first getting the users chat keys
    * */
    @Override
    public void onResume() {
        super.onResume();
        if (myChatsdapter.getItemCount() == 0) {
            toggleEmptyView(false, false);
            if (!Network.isInternetAvailable(getActivity(), true)) {
                return;
            }
        }
        toggleEmptyView(false, true);
        userListener = firebaseHelper.getValueEventListener(user.name, FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_2 , FirebaseHelper.CONDITION_3, FirebaseUserModel.class);
        firebaseHelper.toggleListenerFor("users", "username" , user.name, userListener, true, false);
    }


    private void setUpMsgEventListeners(){
        valueEventListeners.clear();
        myChatsdapter.clearAllChats ();
        progressBar.toggleDialog(true);
            for (int i = 0; i < allChatKeys.size(); i++) {
                final String chatKey = allChatKeys.get(i);
                valueEventListeners.add(firebaseHelper.getMessageEventListener(chatKey, i, "unread", user.name));
                //firebaseHelper.toggleLastMsgEventListener("single", allChatKeys.get(i), valueEventListeners.get(i), true);
                firebaseHelper.toggleUnreadMsgEventListeners("single", allChatKeys.get(i), valueEventListeners.get(i), true, false);
            }
        progressBar.toggleDialog(false);
        toggleEmptyView(false, false);
    }

    private void toggleEmptyView(boolean sort, boolean showProgress){
        if (showProgress) {
            chatProgress.setVisibility(View.VISIBLE);
            textEmptyChat.setText("");
        } else {
            chatProgress.setVisibility(View.GONE);
            if (myChatsdapter.getItemCount() == 0) {
                textEmptyChat.setText("No Chats Found");
                emptyChats.setVisibility(View.VISIBLE);
            } else {
                if (sort) myChatsdapter.sortAllChatsByDate(false, formatter);
                emptyChats.setVisibility(View.GONE);
            }
        }

    }

    private void setUpRecyclerView(){
        myChatsdapter = new AllChatsAdapter(getContext(), new AllChatsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Chat chat, int pos) {
                Intent chatActivity = new Intent(getActivity(), ChatActivity.class);
                chatActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                String chatKey = chat.getChatKey();
                chatActivity.putExtra("chatKey", chatKey);
                startActivity(chatActivity);
            }

            @Override
            public void onItemLongClicked(Chat chat, int pos) {
                createDialog(chat,pos).show();
            }

        });
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(OrientationHelper.VERTICAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(myChatsdapter);

    }

    private AlertDialog createDialog (final Chat chat, final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        int dialogId = (db.isUserAlreadyInContacts(chat.getUsernameOfTheOneBeingSpokenTo())) ? R.array.chat_dialog_actions : R.array.chat_dialog_actions_not_contact;
        final String [] commands = getResources().getStringArray(dialogId);
        builder.setTitle(R.string.dialog_chat_select_action)
                .setItems(commands, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // String[] actions = getResources().getStringArray(R.array.contact_dialog_actions);
                        onActionSelected(commands[which], chat, position);
                        dialog.dismiss();
                    }

                    private void onActionSelected(String which, Chat chat, int position) {
                        if (getResources().getString(R.string.dialog_op1).equals(which)) { // see profile
                            Intent chatDetailActivity = new Intent(getContext(), ChatDetailActivity.class);
                            chatDetailActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            chatDetailActivity.putExtra("username", chat.getUsernameOfTheOneBeingSpokenTo());
                            startActivity(chatDetailActivity);
                        } else if (getResources().getString(R.string.dialog_op2).equals(which)) { // add to contacts
                            addUserToContacts(chat, position);
                        } else if (getResources().getString(R.string.dialog_op3).equals(which)) { // see chat
                            Intent chatActivity = new Intent(getActivity(), ChatActivity.class);
                            chatActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            String chatKey = chat.getChatKey();
                            chatActivity.putExtra("chatKey", chatKey);
                            startActivity(chatActivity);
                        } else if (getResources().getString(R.string.dialog_op4).equals(which)){ // delete chat
                            if (Network.isInternetAvailable(getActivity(), true));
                            deleteChat(chat);
                        }
                    }
                });
        return builder.create();
    }

    private void deleteChat(final Chat chat){
        progressBar.toggleDialog(true);
        allChatKeys.remove(chat.getChatKey());
        //final String updatedKeys = getChatKeysAsString();
        firebaseHelper.updateChatKeys(user, chat.getChatKey(), chat, false); //  initiate deletion of chat
    }

    private void addUserToContacts(final Chat chat, final int position){

        if(!db.isUserAlreadyInContacts(chat.getUsernameOfTheOneBeingSpokenTo())){
            firebaseHelper.addUserToContacts(chat.getUsernameOfTheOneBeingSpokenTo(), db, position);
        } else {
            Toast.makeText(getActivity(), "That user may already exists in your contacts.", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onStop() {

        if (userListener != null) {
            firebaseHelper.toggleListenerFor("users", "username", user.name , userListener, false, false);
        }
        for(int i = 0; i < valueEventListeners.size(); i++){
            firebaseHelper.toggleUnreadMsgEventListeners("single", allChatKeys.get(i), valueEventListeners.get(i), false, false);
        }
        super.onStop();

    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }

    /**
     * Code that is executed when the thread detects a change in the node that it is listening to.
     * First it will do processing the thread and then send its result to onComplete() for further
     * bespoke processing by the requesting class. Also any result of firebase helper code are sent here.
     * @param tag - the name of the firebase code that was executed
     * @param condition - a specific condition was satisfied and a result was produced at this point that needs to be returned
     *                  to calling class
     * @param container - the result of the firebase code
     */
    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch(tag){
            case "getMessageEventListener":
                switch (condition) {
                    case FirebaseHelper.CONDITION_1 :
                        FirebaseMessageModel firebaseMessageModel = container.getMsgModel();
                        String isChattingTo = (firebaseMessageModel.getSenderName().equals(user.name)) ? db.getProfileNameAndPic(firebaseMessageModel.getReceiverName())[0] : db.getProfileNameAndPic(firebaseMessageModel.getSenderName())[0];
                        String username = (firebaseMessageModel.getSenderName().equals(user.name)) ? firebaseMessageModel.getReceiverName() : firebaseMessageModel.getSenderName();
                        String dateString = formatter.format(new Date(firebaseMessageModel.getCreatedDateLong()));
                        Chat chat = new Chat(firebaseMessageModel.getSenderName(), isChattingTo, username, firebaseMessageModel.getText(), db.getProfileNameAndPic(username)[1], dateString, container.getString(), firebaseMessageModel.getIsReceived(), firebaseMessageModel.getMediaType());
                        if (firebaseMessageModel.getIsReceived() == Constants.MESSAGE_SENT
                                && !firebaseMessageModel.getSenderName().equals(user.name)) {
                            chat.setUnreadCount(container.getInt());
                        }
                        myChatsdapter.addOrRemoveChat(chat, true);
                        toggleEmptyView(true, false);
                        break;
                    case FirebaseHelper.CONDITION_2:
                        int loc = container.getInt();
                        if (container.getString().equals("unread") && allChatKeys.size() > 0) {
                            firebaseHelper.toggleLastMsgSingleEventListener("single", allChatKeys.get(loc)
                                    , firebaseHelper.getMessageEventListener(allChatKeys.get(loc), loc, "last", user.name));
                        } else {
                            toggleEmptyView(true, false);
                        }
                        break;
                }
                break;
            case "checkKeyListKey":
                switch(condition){
                    case FirebaseHelper.CONDITION_2: // the other member did not have the chat key so collect any images for this chat for deletion
                        FirebaseGroupModel grp = new FirebaseGroupModel();
                        grp.setGroupKey(container.getString());
                        grp.setPic(null);
                        firebaseHelper.collectAllImagesForDeletionThenDeleteRelatedMessages("single", grp);
                        break;
                }
                break;

            case "getValueEventListener" :
                switch (condition) {
                    case FirebaseHelper.CONDITION_2:
                        toggleEmptyView(false, false);
                        break;
                    case FirebaseHelper.CONDITION_3: //Obtained chat keys now get the last message
                        setUpMsgEventListeners();
                        break;
                }
            case "updateChatKeys": // Initiating process of leaving chat (check if other member of chat has still got the chat key)
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        //verifying if all messages are deleteable
                        firebaseHelper.checkKeyListKey("users", FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_2, container.getChat().getChatKey(),container.getChat().getUsernameOfTheOneBeingSpokenTo());
                        break;
                }
                break;
            case "addUserToContacts":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        myChatsdapter.getAllMyChats().get(container.getInt()).setSpeakingTo(container.getString());
                        myChatsdapter.notifyDataSetChanged();
                        break;
                    case FirebaseHelper.CONDITION_2:
                        Toast.makeText(getActivity(), "That contact does not exist.", Toast.LENGTH_LONG).show();
                        break;
                }
                break;
            case "collectAllImagesForDeletionThenDeleteRelatedMessages": // now delete all identified images + messages
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        Network.deleteUploadImages(firebaseHelper, container.getStringList(), new String[]{container.getString()}, "single");
                        break;
                }
                break;
            case "cleanDeleteAllMessages": // completed process of leaving a chat
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        progressBar.toggleDialog(false);
                        Log.i(TAG, "Successfully removed all messages");
                        break;
                    case FirebaseHelper.CONDITION_2:
                        progressBar.toggleDialog(false);
                        Log.i(TAG, "Failed to removed all messages with error: " +container.getString());
                        break;
                }
                break;
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag){
            case "getMessageEventListener":
            case "getValueEventListener":
            case "updateChatKeys":
                progressBar.toggleDialog(false);
                break;
            case "checkKeyListKey":
                Log.i(TAG, tag + " Error verifying if all messages were deletable, aborted deletion");
                break;
            case "addUserToContacts":
                Toast.makeText(getActivity(), "Failed to add contact.", Toast.LENGTH_LONG).show();
                break;
        }
        toggleEmptyView(false, false);
        Log.i(TAG, tag + " "+ databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        switch(tag) {
            case "getValueEventListener":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        FirebaseUserModel userModel = (FirebaseUserModel) container.getObject();
                        if (userModel.getUsername().equals(container.getString())) {
                            String[] allKeys = userModel.getChatKeys().split(",");
                            allChatKeys.clear();
                            for(String key: allKeys){
                                if(!key.equals(""))
                                    allChatKeys.add(key);
                            }
                            //setUpMsgEventListeners();
                        }
                        break;
                }
                break;
        }
    }
}

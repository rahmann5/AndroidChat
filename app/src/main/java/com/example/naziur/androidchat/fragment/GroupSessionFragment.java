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
import android.widget.TextView;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.activities.GroupChatActivity;
import com.example.naziur.androidchat.adapter.AllChatsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
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
 * A simple {@link Fragment} subclass.
 */
public class GroupSessionFragment extends Fragment implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = GroupSessionFragment.class.getSimpleName();

    private ValueEventListener userListener;
    private AllChatsAdapter myChatsdapter;
    private RecyclerView recyclerView;
    private TextView emptyChats;
    private User user = User.getInstance();
    private List<String> allGroupKeys;
    private List<FirebaseGroupModel> allGroups;
    private ContactDBHelper db;
    private ProgressDialog progressBar;
    private List<ValueEventListener> grpValueEventListeners, grpMsgValueEventListeners;
    FirebaseHelper firebaseHelper;

    public GroupSessionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_session_all_chats, container, false);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        allGroups = new ArrayList<>();
        grpValueEventListeners = new ArrayList<>();
        grpMsgValueEventListeners = new ArrayList<>();
        allGroupKeys = new ArrayList<>();
        emptyChats = (TextView) rootView.findViewById(R.id.no_chats);
        recyclerView = rootView.findViewById(R.id.all_chats_list);
        progressBar = new ProgressDialog(getActivity(), R.layout.progress_dialog, true);
        setUpAdapterWithRecyclerView();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (myChatsdapter.getItemCount() == 0) {
            emptyChats.setVisibility(View.VISIBLE);
            if (!Network.isInternetAvailable(getActivity(), true)) {
                return;
            }
        }
        userListener = firebaseHelper.getValueEventListener(user.name, FirebaseHelper.CONDITION_1 ,FirebaseUserModel.class);
        firebaseHelper.toggleListenerFor("users", "username" , user.name, userListener, true, false);
    }

    private void setUpGrpEventListeners() {
        grpValueEventListeners.clear();
        grpMsgValueEventListeners.clear();
        allGroups.clear();
        for(int i = 0 ; i < allGroupKeys.size(); i++){
            final String currentGroupKey = allGroupKeys.get(i);
            ValueEventListener valueEventListener = firebaseHelper.getValueEventListener(currentGroupKey, FirebaseHelper.CONDITION_3, FirebaseGroupModel.class);
            grpValueEventListeners.add(valueEventListener);
            firebaseHelper.toggleListenerFor("groups", "groupKey" , currentGroupKey, valueEventListener, true, false);
        }
    }

    private void setUpGrpMSgEventListeners(String groupKey){
        ValueEventListener valueEventListener = firebaseHelper.getMessageEventListener(groupKey);
        grpMsgValueEventListeners.add(valueEventListener);
        firebaseHelper.attachOrRemoveMessageEventListener("group", groupKey, valueEventListener, true);
    }

    private int findIndexForGroup (String groupKey) {
        int index = -1;
        for (int i = 0; i < allGroups.size(); i++) {
            if (allGroups.get(i).getGroupKey().equals(groupKey)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void setUpAdapterWithRecyclerView(){
        myChatsdapter = new AllChatsAdapter(getContext(), new AllChatsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Chat chat, int pos) {
                Intent chatActivity = new Intent(getActivity(), GroupChatActivity.class);
                chatActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                String chatKey = chat.getChatKey();
                chatActivity.putExtra("group_uid", chatKey);
                startActivity(chatActivity);
            }

            @Override
            public void onItemLongClicked(Chat chat, int pos) {
                createDialog(chat, pos).show();
            }

            @Override
            public void onButtonClicked(Chat chat, int position) {}
        });

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(OrientationHelper.VERTICAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(myChatsdapter);
    }

    private AlertDialog createDialog (final Chat chat, final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_group_chat_select_action)
                .setItems(R.array.member_group_chat_dialog_actions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onActionSelected(which, chat, position);
                        dialog.dismiss();
                    }

                    private void onActionSelected(int which, Chat chat, int position) {
                        switch (which) {
                            case 0 : // see group info
                                Toast.makeText(getActivity(), "View Group Details", Toast.LENGTH_SHORT).show();
                                break;

                            case 1 : // chat with contact
                                Intent chatActivity = new Intent(getActivity(), GroupChatActivity.class);
                                chatActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                String chatKey = chat.getChatKey();
                                chatActivity.putExtra("group_uid", chatKey);
                                startActivity(chatActivity);
                                break;

                            case 2 : // leave/delete Chat
                                firebaseHelper.exitGroup(chat.getChatKey(), user.name, chat.getAdmin().equals(user.name));
                                break;

                        }
                    }
                });
        return builder.create();
    }

    private String getChatKeysAsString(){
        String keys = "";
        for(int i = 0; i < allGroupKeys.size(); i++){
            keys += allGroupKeys.get(i);
            if(i < allGroupKeys.size()-1)
                keys += ",";
        }
        return keys;
    }

    @Override
    public void onStop() {
        if (userListener != null) {
            firebaseHelper.toggleListenerFor("users", "username", user.name , userListener, false, false);
        }

        for(int i = 0; i < grpValueEventListeners.size(); i++){
            firebaseHelper.toggleListenerFor("groups", "groupKey", allGroupKeys.get(i) , grpValueEventListeners.get(i), false, false);
            firebaseHelper.attachOrRemoveMessageEventListener("group", allGroupKeys.get(i), grpMsgValueEventListeners.get(i), false);
        }
        super.onStop();
    }

    private void updateUserChatKeys (String chatKeyToRemove) {
        allGroupKeys.remove(chatKeyToRemove);
        String updatedKeys = getChatKeysAsString();
        firebaseHelper.updateChatKeys(user, updatedKeys, null, true);
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("getMessageEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_2:
                    if (myChatsdapter.getItemCount() == 0) {
                        emptyChats.setVisibility(View.VISIBLE);
                    } else {
                        emptyChats.setVisibility(View.GONE);
                    }
                    break;
            }
        } else if (tag.equals("getValueEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    FirebaseUserModel userModel = (FirebaseUserModel) container.getObject();
                    if (userModel.getUsername().equals(container.getString())) {
                        allGroups.clear(); // remove all previous groups
                        String[] allKeys = userModel.getGroupKeys().split(",");
                        allGroupKeys.clear();
                        for(String key: allKeys){
                            if(!key.equals(""))
                                allGroupKeys.add(key);
                        }
                        setUpGrpEventListeners();
                    }
                    break;

                case FirebaseHelper.CONDITION_2 :
                    Log.i(TAG, container.getString() + " does not exist");
                    break;

                case FirebaseHelper.CONDITION_3: // setUpGrpEventListeners comes here
                    FirebaseGroupModel firebaseGroupModel = (FirebaseGroupModel) container.getObject();
                    if(firebaseGroupModel.getGroupKey().equals(container.getString())) {
                        allGroups.add(firebaseGroupModel);
                        setUpGrpMSgEventListeners(container.getString());
                    }
                    break;

            }
        } else if (tag.equals("exitGroup")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    updateUserChatKeys(container.getString());
                    break;
            }
        } else if (tag.equals("updateChatKeys")) {
            firebaseHelper.deleteGroup(container.getChat().getChatKey());
        } else if (tag.equals("deleteGroup")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    // clean delete all messages + images
                    firebaseHelper.collectAllImagesForDeletionThenDeleteRelatedMessages("group", container.getString());
                    break;

                case FirebaseHelper.CONDITION_2 :
                    Toast.makeText(getActivity(), "Successfully left the group", Toast.LENGTH_SHORT).show();
                    break;
            }
        } else if (tag.equals("collectAllImagesForDeletionThenDeleteRelatedMessages")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1:
                    Network.deleteUploadImages(firebaseHelper, container.getStringList(), container.getString(), "group");
                    break;
            }
        } else if (tag.equals("cleanDeleteAllMessages")) {
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
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        Log.i(TAG, tag + ": " + databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        if (tag.equals("getMessageEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    String groupKey = container.getString();
                    String title = "ERROR";
                    String picUrl = "";
                    String admin = "ERROR";
                    int index = findIndexForGroup(groupKey);
                    if (index != -1) {
                        title = allGroups.get(index).getTitle();
                        picUrl = allGroups.get(index).getPic();
                        admin = allGroups.get(index).getAdmin();
                    }
                    FirebaseMessageModel groupMessageModel = container.getMsgModel();
                    db = new ContactDBHelper(getActivity());
                    String senderName = (groupMessageModel.getSenderName().equals(user.name)) ? user.profileName : db.getProfileNameAndPic(groupMessageModel.getSenderName())[0];
                    db.close();
                    SimpleDateFormat formatter = new SimpleDateFormat(getString(R.string.simple_date));
                    String dateString = formatter.format(new Date(groupMessageModel.getCreatedDateLong()));
                    Chat chat = new Chat(title, senderName, groupMessageModel.getText(), picUrl, dateString, groupKey, groupMessageModel.getMediaType(), admin);
                    myChatsdapter.addChat(chat);
                    break;
            }
        }
    }
}

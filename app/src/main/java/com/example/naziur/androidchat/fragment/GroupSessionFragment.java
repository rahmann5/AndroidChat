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
import com.example.naziur.androidchat.activities.GroupChatActivity;
import com.example.naziur.androidchat.activities.GroupDetailActivity;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class GroupSessionFragment extends Fragment implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = GroupSessionFragment.class.getSimpleName();

    private ValueEventListener userListener;
    private AllChatsAdapter myChatsdapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyChats;
    private User user = User.getInstance();
    private List<String> allGroupKeys;
    private List<FirebaseGroupModel> allGroups;
    private ContactDBHelper db;
    private ProgressDialog progressBar;
    private ProgressBar chatProgress;
    private Map<String, ValueEventListener> grpValueEventListeners;
    private Map<String, ValueEventListener> grpMsgValueEventListeners;
    private SimpleDateFormat formatter;
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
        grpValueEventListeners = new HashMap<>();
        grpMsgValueEventListeners = new HashMap<>();
        allGroupKeys = new ArrayList<>();
        emptyChats = (LinearLayout) rootView.findViewById(R.id.no_chats);
        chatProgress = (ProgressBar) rootView.findViewById(R.id.chat_progress);
        recyclerView = rootView.findViewById(R.id.all_chats_list);
        formatter = new SimpleDateFormat(getString(R.string.simple_date));
        progressBar = new ProgressDialog(getActivity(), R.layout.progress_dialog, true);
        db = new ContactDBHelper(getActivity());
        setUpAdapterWithRecyclerView();
        return rootView;
    }

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
        userListener = firebaseHelper.getValueEventListener(user.name, FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_2, FirebaseHelper.CONDITION_3 ,FirebaseUserModel.class);
        firebaseHelper.toggleListenerFor("users", "username" , user.name, userListener, true, false);
    }

    private void setUpGrpEventListeners(int index, boolean single, int loop, int exit ,int complete) {
        /*grpValueEventListeners.clear();
        grpMsgValueEventListeners.clear();
        myChatsdapter.clearAllChats ();*/
        if (index < allGroupKeys.size()) {
            final String currentGroupKey = allGroupKeys.get(index);
            ValueEventListener valueEventListener = firebaseHelper.getValueEventListener(currentGroupKey, loop, exit, complete, FirebaseGroupModel.class);
            grpValueEventListeners.put(currentGroupKey, valueEventListener);
            firebaseHelper.toggleListenerFor("groups", "groupKey" , currentGroupKey, valueEventListener, true, single);
        }

    }

    private void setUpGrpMSgEventListeners(String groupKey){
        ValueEventListener valueEventListener = firebaseHelper.getMessageEventListener(groupKey);
        grpMsgValueEventListeners.put(groupKey,valueEventListener);
        //System.out.println("Messages: " + grpMsgValueEventListeners.size());=
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
                if (allGroupKeys.contains(chat.getChatKey())) {
                    Intent chatActivity = new Intent(getActivity(), GroupChatActivity.class);
                    chatActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    String chatKey = chat.getChatKey();
                    chatActivity.putExtra("group_uid", chatKey);
                    startActivity(chatActivity);
                }  else {
                    Toast.makeText(getActivity(), "This is a empty group", Toast.LENGTH_LONG).show();
                }
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
                                if (allGroupKeys.contains(chat.getChatKey())) {
                                    Intent intent = new Intent(getActivity(), GroupDetailActivity.class);
                                    intent.putExtra("g_uid", chat.getChatKey());
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    break;
                                } else {
                                    Toast.makeText(getActivity(), "This is a empty group", Toast.LENGTH_LONG).show();
                                }

                            case 1 : // chat with contact
                                if (allGroupKeys.contains(chat.getChatKey())) {
                                    Intent chatActivity = new Intent(getActivity(), GroupChatActivity.class);
                                    chatActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    String chatKey = chat.getChatKey();
                                    chatActivity.putExtra("group_uid", chatKey);
                                    startActivity(chatActivity);
                                } else {
                                    Toast.makeText(getActivity(), "This is a empty group", Toast.LENGTH_LONG).show();
                                }
                                break;

                            case 2 : // leave/delete Chat
                                if (allGroupKeys.contains(chat.getChatKey())) {
                                    firebaseHelper.toggleListenerFor("groups", "groupKey" , chat.getChatKey(), grpValueEventListeners.get(chat.getChatKey()), false, false);
                                    List<String> groupToLeave = new ArrayList<String>();
                                    groupToLeave.add(chat.getChatKey());
                                    firebaseHelper.exitGroup(chat, user.name, chat.getAdmin().equals(user.name),groupToLeave);
                                } else {
                                    updateUserChatKeys(chat);
                                }
                                break;

                        }
                    }
                });
        return builder.create();
    }


    @Override
    public void onStop() {
        if (userListener != null) {
            firebaseHelper.toggleListenerFor("users", "username", user.name , userListener, false, false);
        }

        for(int i = 0; i < allGroupKeys.size(); i++){
            String key = allGroupKeys.get(i);
            firebaseHelper.toggleListenerFor("groups", "groupKey", key , grpValueEventListeners.get(key), false, false);
            firebaseHelper.attachOrRemoveMessageEventListener("group", key, grpMsgValueEventListeners.get(key), false);
        }
        super.onStop();
    }

    private void updateUserChatKeys (Chat chatToRemove) {
        //This doesn't include redundant keys so they are removed from server
        allGroupKeys.remove(chatToRemove.getChatKey());
       //String updatedKeys = getChatKeysAsString();
        firebaseHelper.updateChatKeys(user, chatToRemove.getChatKey(), chatToRemove, true);
    }

    private void updateGroupModel(FirebaseGroupModel grpModel) {
        for (int i = 0; i < allGroups.size(); i++) {
            if (allGroups.get(i).getGroupKey().equals(grpModel.getGroupKey())) {
                allGroups.remove(i);
                allGroups.add(i, grpModel);
                break;
            }
        }
    }

    private void toggleEmptyView(boolean sort, boolean showProgress){
        if (showProgress) {
            chatProgress.setVisibility(View.VISIBLE);
        } else {
            chatProgress.setVisibility(View.GONE);
        }
        if (myChatsdapter.getItemCount() == 0) {
            emptyChats.setVisibility(View.VISIBLE);
        } else {
            if (sort) myChatsdapter.sortAllChatsByDate(false, formatter);
            emptyChats.setVisibility(View.GONE);
        }
    }


    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("getMessageEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_2:
                    toggleEmptyView(true, false);
                    break;
            }
        } else if (tag.equals("getValueEventListener")) {
            switch (condition) {

                case FirebaseHelper.CONDITION_2 :
                    toggleEmptyView(true, false);
                    Log.i(TAG, container.getString() + " does not exist");
                    break;

                case FirebaseHelper.CONDITION_3 :
                    if (!allGroupKeys.isEmpty()) {
                        myChatsdapter.clearAllChats();
                        setUpGrpEventListeners(0, true, FirebaseHelper.CONDITION_4, FirebaseHelper.CONDITION_7 , FirebaseHelper.CONDITION_5);
                    } else {
                        toggleEmptyView(true, false);
                    }
                    break;

                case FirebaseHelper.CONDITION_5 :
                    if (allGroups.size() == allGroupKeys.size()) {
                        for (int i = 0; i< allGroups.size(); i++) {
                            setUpGrpEventListeners(i, false, FirebaseHelper.CONDITION_6, FirebaseHelper.NON_CONDITION ,FirebaseHelper.NON_CONDITION);
                        }
                    }
                    break;
                case FirebaseHelper.CONDITION_7:
                    if(allGroupKeys.contains(container.getString())) {
                        FirebaseGroupModel emptyGroup = new FirebaseGroupModel();
                        emptyGroup.setTitle("Empty Group");
                        emptyGroup.setGroupKey(container.getString());
                        emptyGroup.setPic("");
                        emptyGroup.setAdmin("");
                        emptyGroup.setMembers("");
                        //allGroups.add(emptyGroup);
                        Chat chat = new Chat(emptyGroup.getTitle(), "System", "This is a deleted group.", emptyGroup.getPic(), "", container.getString(), Constants.MESSAGE_TYPE_SYSTEM, "");
                        chat.setEmptyChat(true);
                        myChatsdapter.addOrRemoveChat(chat, true);
                        //myChatsdapter.notifyDataSetChanged();
                        allGroupKeys.remove(container.getString()); // remove redundent keys
                        // never reaches condition 4 need to find way to continue recursive function
                        if (allGroups.size() != allGroupKeys.size()) {
                            setUpGrpEventListeners(allGroups.size(), true, FirebaseHelper.CONDITION_4, FirebaseHelper.CONDITION_7 ,FirebaseHelper.CONDITION_5);
                        } else {
                            if(!allGroups.isEmpty()) {
                                for (int i = 0; i < allGroups.size(); i++) {
                                    setUpGrpEventListeners(i, false, FirebaseHelper.CONDITION_6, FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION);
                                }
                            } else {
                                myChatsdapter.notifyDataSetChanged();
                                toggleEmptyView(true, false);
                            }
                        }
                    }
                    break;

            }
        } else if (tag.equals("exitGroup")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    Chat chat = container.getChat();
                    updateUserChatKeys(chat);
                    break;

                case FirebaseHelper.CONDITION_2 : // failure reattach listener for that previously removed group listener
                    firebaseHelper.toggleListenerFor("groups", "groupKey" , container.getChat().getChatKey(), grpValueEventListeners.get(container.getString()), true, false);
                    break;
            }
        } else if (tag.equals("updateChatKeys")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    Chat chat = container.getChat();
                    for(int i = 0; i < allGroups.size(); i++){
                        if(allGroups.get(i).getGroupKey().equals(chat.getChatKey())) {
                            allGroups.remove(i);
                            break;
                        }
                    }
                    myChatsdapter.addOrRemoveChat(chat, false);
                    myChatsdapter.notifyDataSetChanged();
                    if (!chat.isEmptyChat()) {
                        toggleEmptyView(true, true);
                        firebaseHelper.deleteGroup(chat);
                    } else {
                        toggleEmptyView(true, false);
                    }
                    break;
            }
        } else if (tag.equals("deleteGroup")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    // clean delete all messages + images
                    firebaseHelper.collectAllImagesForDeletionThenDeleteRelatedMessages("group", container.getGroupModel());
                    break;

                case FirebaseHelper.CONDITION_2 :
                    FirebaseGroupModel fbgModel = container.getGroupModel();
                    String leaver = (fbgModel.getAdmin().equals(user.name)) ? "Admin " + user.name : user.name;
                    String wishMessage = leaver + " has left the group.";
                    firebaseHelper.updateMessageNode(getActivity(), "group", fbgModel.getGroupKey(), wishMessage , null, Constants.MESSAGE_TYPE_SYSTEM, null, fbgModel.getTitle());
                    Toast.makeText(getActivity(), "Successfully left the group", Toast.LENGTH_SHORT).show();
                    break;
            }
        } else if (tag.equals("collectAllImagesForDeletionThenDeleteRelatedMessages")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1:
                    Network.deleteUploadImages(firebaseHelper, container.getStringList(), new String[]{container.getString()}, "group");
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
        toggleEmptyView(false, false);
        Log.i(TAG, tag + ": " + databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        if (tag.equals("getMessageEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    String groupKey = container.getString();
                    int index = findIndexForGroup(groupKey);
                    if (index != -1) {
                        String title = allGroups.get(index).getTitle();
                        String picUrl = allGroups.get(index).getPic();
                        String admin = allGroups.get(index).getAdmin();
                        FirebaseMessageModel groupMessageModel = container.getMsgModel();
                        String senderName = (groupMessageModel.getSenderName().equals(user.name)) ? user.profileName : db.getProfileNameAndPic(groupMessageModel.getSenderName())[0];
                        db.close();
                        String dateString = formatter.format(new Date(groupMessageModel.getCreatedDateLong()));
                        Chat chat = new Chat(title, senderName, groupMessageModel.getText(), picUrl, dateString, groupKey, groupMessageModel.getMediaType(), admin);
                        myChatsdapter.addOrRemoveChat(chat, true);
                        //System.out.println(myChatsdapter.getItemCount());
                    }
                    break;
            }
        } if (tag.equals("getValueEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    FirebaseUserModel userModel = (FirebaseUserModel) container.getObject();
                    if (userModel.getUsername().equals(container.getString())) {
                        String[] allKeys = userModel.getGroupKeys().split(",");
                        allGroups.clear();
                        allGroupKeys.clear();
                        for(String key: allKeys){
                            if(!key.equals(""))
                                allGroupKeys.add(key);
                        }
                    }
                    break;

                case FirebaseHelper.CONDITION_4:
                    FirebaseGroupModel firebaseGroupModel = (FirebaseGroupModel) container.getObject();
                    if(firebaseGroupModel.getGroupKey().equals(container.getString())) {
                        //addToGroupIfNew(firebaseGroupModel);
                        allGroups.add(firebaseGroupModel);
                    }
                    if (allGroups.size() != allGroupKeys.size()) {
                        setUpGrpEventListeners(allGroups.size(), true, FirebaseHelper.CONDITION_4, FirebaseHelper.CONDITION_7 ,FirebaseHelper.CONDITION_5);
                    }
                    break;

                case FirebaseHelper.CONDITION_6: // setUpGrpEventListeners comes here once per group if that group has changed
                    FirebaseGroupModel firebaseModel = (FirebaseGroupModel) container.getObject();
                    if(firebaseModel.getGroupKey().equals(container.getString())) {
                        updateGroupModel(firebaseModel);
                        setUpGrpMSgEventListeners(firebaseModel.getGroupKey());
                    }
                    // called multiple times (based on the number of groups)
                    break;
            }
        }
    }
}
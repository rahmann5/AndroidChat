package com.example.naziur.androidchat.fragment;


import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.example.naziur.androidchat.activities.ChatActivity;
import com.example.naziur.androidchat.activities.ChatDetailActivity;
import com.example.naziur.androidchat.adapter.AllChatsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SingleSessionFragment extends Fragment implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = SingleSessionFragment.class.getSimpleName();

    private FirebaseDatabase database;
    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;
    private ValueEventListener userListener;
    private AllChatsAdapter myChatsdapter;
    private RecyclerView recyclerView;
    private TextView emptyChats;
    private User user = User.getInstance();
    private List<String> allChatKeys;
    private List<Chat> allChats;
    private ContactDBHelper db;
    private ProgressDialog progressBar;
    private List<ValueEventListener> valueEventListeners;

    public SingleSessionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getActivity().setTitle("All Chats");

        View rootView = inflater.inflate(R.layout.fragment_session_all_chats, container, false);
        FirebaseHelper.setFirebaseHelperListener(this);
        valueEventListeners = new ArrayList<>();
        allChats = new ArrayList<>();
        allChatKeys = new ArrayList<>();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        messagesRef = database.getReference("messages");
        emptyChats = (TextView) rootView.findViewById(R.id.no_chats);
        recyclerView = rootView.findViewById(R.id.all_chats_list);
        progressBar = new ProgressDialog(getActivity(), R.layout.progress_dialog, true);
        db = new ContactDBHelper(getContext());
        if (Network.isInternetAvailable(getActivity(), true)) {
            Cursor c = db.getAllMyContacts(null);
            if (c != null && c.getCount() > 0) {
                updateExistingContacts (c);
            }
        }
        setUpRecyclerView();
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
        userListener = FirebaseHelper.getUsersValueEventListener(user);
    }

    private void updateExistingContacts (Cursor c) {
        try{
            while (c.moveToNext()) {
                final FirebaseUserModel fbModel = new FirebaseUserModel();
                fbModel.setUsername(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME)));
                fbModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));
                // need one for profile picture
                FirebaseHelper.updateLocalContactsFromFirebase("users", fbModel, db);
            }
        } finally {
            c.close();
        }

    }

    private void setUpMsgEventListeners(){
        valueEventListeners.clear();
        allChats.clear();
        myChatsdapter.clearAllChats ();
        progressBar.toggleDialog(true);
            for (int i = 0; i < allChatKeys.size(); i++) {
                final String chatKey = allChatKeys.get(i);

                valueEventListeners.add(FirebaseHelper.getMessageEventListener(user, db, getString(R.string.simple_date), chatKey));
                FirebaseHelper.attachOrRemoveMessageEventListener("single", allChatKeys.get(i), valueEventListeners.get(i), true);
            }
        progressBar.toggleDialog(false);
        if (myChatsdapter.getItemCount() == 0) {
            emptyChats.setVisibility(View.VISIBLE);
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

            @Override
            public void onButtonClicked(Chat chat, int position) {
                addUserToContacts(chat, position);
            }
        });
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(OrientationHelper.VERTICAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(myChatsdapter);

    }

    private AlertDialog createDialog (final Chat chat, final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_chat_select_action)
                .setItems(R.array.chat_dialog_actions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // String[] actions = getResources().getStringArray(R.array.contact_dialog_actions);
                        onActionSelected(which, chat, position);
                        dialog.dismiss();
                    }

                    private void onActionSelected(int which, Chat chat, int position) {
                        switch (which) {
                            case 0 : // see profile info
                                Intent chatDetailActivity = new Intent(getContext(), ChatDetailActivity.class);
                                chatDetailActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                chatDetailActivity.putExtra("username", chat.getUsernameOfTheOneBeingSpokenTo());
                                startActivity(chatDetailActivity);
                                break;

                            case 1 : // chat with contact
                                Intent chatActivity = new Intent(getActivity(), ChatActivity.class);
                                chatActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                String chatKey = chat.getChatKey();
                                chatActivity.putExtra("chatKey", chatKey);
                                startActivity(chatActivity);
                                break;

                            case 2 : // delete Chat
                                if (Network.isInternetAvailable(getActivity(), true));
                                    deleteChat(chat);
                                break;
                        }
                    }
                });
        return builder.create();
    }

    private void deleteChat(final Chat chat){
        progressBar.toggleDialog(true);
        allChatKeys.remove(chat.getChatKey());
        final String updatedKeys = getChatKeysAsString();
        FirebaseHelper.updateChatKeys(user, updatedKeys, chat); //  initiate deletion of chat
    }

    private void addUserToContacts(final Chat chat, final int position){

        if(!db.isUserAlreadyInContacts(chat.getUsernameOfTheOneBeingSpokenTo())){
            FirebaseHelper.addUserToContacts(chat.getUsernameOfTheOneBeingSpokenTo(), db, position);
        } else {
            Toast.makeText(getActivity(), "That user may already exists in your contacts.", Toast.LENGTH_LONG).show();
        }

    }

    private String getChatKeysAsString(){
        String keys = "";
        for(int i = 0; i < allChatKeys.size(); i++){
            keys += allChatKeys.get(i);
            if(i < allChatKeys.size()-1)
                keys += ",";
        }
        return keys;
    }

    private void collectAllRemovableImagesForMessages (final String chatKey) {
        messagesRef
            .child("single").child(chatKey).orderByChild("mediaType")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final List<String> imageUri = new ArrayList<>();
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        FirebaseMessageModel model = data.getValue(FirebaseMessageModel.class);
                        if (model.getMediaType().equals(Constants.MESSAGE_TYPE_PIC)) {
                            imageUri.add(model.getText());
                        }
                    }
                    Log.i(TAG, "Number of images found "  + imageUri.size());
                    deleteUploadImages(imageUri, chatKey);

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    //Toast.makeText(getActivity(), "Failed to obtain reference to all previous messages", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Failed to obtain reference to all previous messages " + databaseError.getMessage());
                    cleanDeleteAllMessages(chatKey);
                }
            });

        FirebaseHelper.collectAllImagesForDeletionThenDeleteRelatedMessages("single", chatKey);
    }

    private void cleanDeleteAllMessages (String chatKey) {
        messagesRef.child("single").child(chatKey).setValue(null).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressBar.toggleDialog(false);
                Log.i(TAG, "Successfully removed all messages");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.toggleDialog(false);
                Log.i(TAG, "Failed to removed all messages");
            }
        });
    }

    private void deleteUploadImages (final List<String> allUris, final String chatKey) {
        if (!allUris.isEmpty()) {
            String uri = allUris.remove(0);
            StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(uri);
            photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                deleteUploadImages(allUris, chatKey);
                Log.i(TAG, "onSuccess: removed image from failed database update");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                Log.i(TAG, "onFailure: did not delete file in storage");
                // store that image uri in a log to remove manually
                e.printStackTrace();
                }
            });
        } else {
            cleanDeleteAllMessages(chatKey);
        }

    }

    @Override
    public void onStop() {

        if (userListener != null) {
            FirebaseHelper.removeListenerFor("users", userListener);
        }
        for(int i = 0; i < valueEventListeners.size(); i++){
            FirebaseHelper.attachOrRemoveMessageEventListener("single", allChatKeys.get(i), valueEventListeners.get(i), false);
        }
        super.onStop();

    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch(tag){
            case "getMessageEventListener":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        myChatsdapter.setAllMyChats(allChats);
                        myChatsdapter.notifyDataSetChanged();
                        break;
                    case FirebaseHelper.CONDITION_2:
                        if (myChatsdapter.getItemCount() == 0) {
                            emptyChats.setVisibility(View.VISIBLE);
                        } else {
                            emptyChats.setVisibility(View.GONE);
                        }
                        break;
                }
                break;
            case "getUsersValueEventListener":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        String[] allKeys = container.getString().split(",");
                        allChatKeys.clear();
                        for(String key: allKeys){
                            if(!key.equals(""))
                                allChatKeys.add(key);
                        }
                        setUpMsgEventListeners();
                        break;
                    case FirebaseHelper.CONDITION_2:
                        Toast.makeText(getContext(), "No chats found for this user, as the account may no longer exist", Toast.LENGTH_SHORT).show();
                        emptyChats.setVisibility(View.VISIBLE);
                        break;
                }
                break;
            case "checkKeyListKey":
                switch(condition){
                    case FirebaseHelper.CONDITION_2:
                        collectAllRemovableImagesForMessages (container.getString());
                        break;
                }
                break;
            case "updateChatKeys":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        //verifying if all messages are deleteable
                        FirebaseHelper.checkKeyListKey("users", container.getChat().getUsernameOfTheOneBeingSpokenTo(), FirebaseHelper.CONDITION_1, FirebaseHelper.CONDITION_2, container.getChat().getChatKey());
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
            case "collectAllImagesForDeletionThenDeleteRelatedMessages":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        deleteUploadImages(container.getStringList(), container.getString());
                        break;
                }
                break;
            case "cleanDeleteAllMessages":
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
            case "getUsersValueEventListener":
            case "updateChatKeys":
                progressBar.toggleDialog(false);
                break;
            case "checkKeyListKey":
                Log.i(TAG, tag + " Error verifying if all messages were deletable, aborted deletion");
                break;
            case "addUserToContacts":
                Toast.makeText(getActivity(), "Failed to add contact.", Toast.LENGTH_LONG).show();
                break;
            case "collectAllImagesForDeletionThenDeleteRelatedMessages":
                Log.i(TAG, tag + " Failed to obtain reference to all previous messages");
                break;
        }
        Log.i(TAG, tag + " "+ databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        switch(tag) {
            case "getMessageEventListener":
                switch (condition) {
                    case FirebaseHelper.CONDITION_1:
                        Chat chat = container.getChat();
                        for (int i = 0; i < allChats.size(); i++) {
                            if (allChats.get(i).getUsernameOfTheOneBeingSpokenTo().equals(chat.getUsernameOfTheOneBeingSpokenTo()))
                                allChats.remove(i);
                        }
                        if (chat.getIsSeen() == Constants.MESSAGE_SENT)
                            allChats.add(0, chat);
                        else
                            allChats.add(chat);
                        break;
                }
                break;
        }
    }
}

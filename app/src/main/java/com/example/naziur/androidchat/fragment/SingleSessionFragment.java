package com.example.naziur.androidchat.fragment;


import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import com.example.naziur.androidchat.activities.ChatActivity;
import com.example.naziur.androidchat.activities.ChatDetailActivity;
import com.example.naziur.androidchat.adapter.AllChatsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SingleSessionFragment extends Fragment {

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

        View rootView = inflater.inflate(R.layout.fragment_session_single, container, false);
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
        if (!Network.isInternetAvailable(getActivity(), true) && myChatsdapter.getItemCount() == 0) {
            emptyChats.setVisibility(View.VISIBLE);
            return;
        } else {
            emptyChats.setVisibility(View.GONE);
        }
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                    if(firebaseUserModel.getUsername().equals(user.name)){
                        String[] allKeys = firebaseUserModel.getChatKeys().split(",");
                        allChatKeys.clear();
                        for(String key: allKeys){
                            if(!key.equals(""))
                                allChatKeys.add(key);
                        }
                        setUpMsgEventListeners();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.toggleDialog(false);
                Log.i(TAG, databaseError.getMessage());
            }
        };

        usersRef.addValueEventListener(userListener);
    }

    private void updateExistingContacts (Cursor c) {
        try{
            while (c.moveToNext()) {
                final FirebaseUserModel fbModel = new FirebaseUserModel();
                fbModel.setUsername(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME)));
                fbModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));
                // need one for profile picture
                Query query = usersRef.orderByChild("username").equalTo(fbModel.getUsername());
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                                if(firebaseUserModel.getUsername().equals(fbModel.getUsername())) {
                                    db.updateProfile(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic());
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.i(TAG, databaseError.getMessage());
                    }
                });

            }
        } finally {
            c.close();
        }

    }

    private void setUpMsgEventListeners(){
        progressBar.toggleDialog(true);
            for (int i = 0; i < allChatKeys.size(); i++) {
                final String chatKey = allChatKeys.get(i);
                valueEventListeners.clear();
                valueEventListeners.add(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        allChats.clear();
                        myChatsdapter.clearAllChats ();
                        if (dataSnapshot.exists()) {
                            for (com.google.firebase.database.DataSnapshot msgSnapshot : dataSnapshot.getChildren()) {
                                FirebaseMessageModel firebaseMessageModel = msgSnapshot.getValue(FirebaseMessageModel.class);
                                String isChattingTo = (firebaseMessageModel.getSenderName().equals(user.name)) ? db.getProfileNameAndPic(firebaseMessageModel.getReceiverName())[0] : db.getProfileNameAndPic(firebaseMessageModel.getSenderName())[0];
                                String username = (firebaseMessageModel.getSenderName().equals(user.name)) ? firebaseMessageModel.getReceiverName() : firebaseMessageModel.getSenderName();
                                SimpleDateFormat formatter = new SimpleDateFormat(getString(R.string.simple_date));
                                String dateString = formatter.format(new Date(firebaseMessageModel.getCreatedDateLong()));
                                Chat chat = new Chat(isChattingTo, username, firebaseMessageModel.getText(), db.getProfileNameAndPic(username)[1], dateString, chatKey, firebaseMessageModel.getIsReceived());
                                allChats.add(chat);
                            }
                            myChatsdapter.setAllMyChats(allChats);
                        }

                        if (myChatsdapter.getItemCount() == 0) {
                            emptyChats.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        progressBar.toggleDialog(false);
                        Log.i(TAG, databaseError.getMessage());
                    }
                });

                messagesRef.child("single").child(allChatKeys.get(i)).limitToLast(1).addValueEventListener(valueEventListeners.get(i));
            }
        progressBar.toggleDialog(false);
    }

    private void setUpRecyclerView(){
        myChatsdapter = new AllChatsAdapter(getContext(), new AllChatsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Chat chat, int pos) {
                Intent chatActivity = new Intent(getActivity(), ChatActivity.class);
                String username = chat.getUsernameOfTheOneBeingSpokenTo();
                chatActivity.putExtra("username", username);
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
                                chatDetailActivity.putExtra("username", chat.getUsernameOfTheOneBeingSpokenTo());
                                startActivity(chatDetailActivity);
                                break;

                            case 1 : // chat with contact
                                Intent chatActivity = new Intent(getActivity(), ChatActivity.class);
                                String username = chat.getUsernameOfTheOneBeingSpokenTo();
                                chatActivity.putExtra("username", username);
                                startActivity(chatActivity);
                                break;

                            case 2 : // delete Chat
                                deleteChat(chat);
                                break;
                        }
                    }
                });
        return builder.create();
    }

    private void deleteChat(Chat chat){
        allChatKeys.remove(chat.getChatKey());
        final String updatedKeys = getChatKeysAsString();
        Query pendingTasks = usersRef.orderByChild("username").equalTo(user.name);
        pendingTasks.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    if(updatedKeys.equals(""))
                        snapshot.getRef().child("chatKeys").removeValue();
                    else
                        snapshot.getRef().child("chatKeys").setValue(updatedKeys);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Failed to delete chat try again later.", Toast.LENGTH_SHORT).show();
                Log.i(TAG, databaseError.getMessage());
            }
        });
    }

    private void addUserToContacts(final Chat chat, final int position){

        if(!db.isUserAlreadyInContacts(chat.getUsernameOfTheOneBeingSpokenTo())){
            Query query = usersRef.orderByChild("username").equalTo(chat.getUsernameOfTheOneBeingSpokenTo());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            //Getting the data from snapshot
                            FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);

                            if (firebaseUserModel.getUsername().equals(chat.getUsernameOfTheOneBeingSpokenTo())) {
                                Log.i(TAG, "Adding to contacts: " + firebaseUserModel.getUsername());
                                db.insertContact(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic(), firebaseUserModel.getDeviceToken());
                                myChatsdapter.getAllMyChats().get(position).setSpeakingTo(firebaseUserModel.getProfileName() );
                                myChatsdapter.notifyDataSetChanged();
                                break;
                            }
                        }

                    } else {
                        Toast.makeText(getActivity(), "That contact does not exist.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getActivity(), "Failed to add contact.", Toast.LENGTH_LONG).show();
                    Log.i(TAG, databaseError.getMessage());
                }
            });
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


    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {

        if (userListener != null) {
            usersRef.removeEventListener(userListener);
        }
        for(int i = 0; i < valueEventListeners.size(); i++){
            messagesRef.child("single").child(allChatKeys.get(i)).limitToLast(1).removeEventListener(valueEventListeners.get(i));
        }
        super.onStop();

    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }
}

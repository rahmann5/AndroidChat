package com.example.naziur.androidchat.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.activities.ChatActivity;
import com.example.naziur.androidchat.adapter.AllChatsAdapter;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.MessageCell;
import com.example.naziur.androidchat.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SessionFragment extends Fragment {

    private static final String TAG = SessionFragment.class.getSimpleName();

    private FirebaseDatabase database;
    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;
    private ValueEventListener userListener;
    private AllChatsAdapter myChatsdapter;
    private RecyclerView recyclerView;
    private User user = User.getInstance();
    private List<String> allChatKeys;
    private List<Chat> allChats;
    private List<ValueEventListener> valueEventListeners;

    public SessionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getActivity().setTitle("All Chats");

        View rootView = inflater.inflate(R.layout.fragment_session, container, false);
        valueEventListeners = new ArrayList<>();
        allChats = new ArrayList<>();
        allChatKeys = new ArrayList<>();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        messagesRef = database.getReference("messages");
        recyclerView = rootView.findViewById(R.id.all_chats_list);

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                    if(firebaseUserModel.getUsername().equals(user.name)){
                        String[] allKeys = firebaseUserModel.getChatKeys().split(",");
                        allChatKeys.clear();
                        allChats.clear();
                        for(String key: allKeys){
                            System.out.println("Adding key " + key);
                            allChatKeys.add(key);
                        }
                        setUpMsgEventListeners();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        usersRef.addValueEventListener(userListener);

        setUpRecyclerView();
        return rootView;
    }

    private void setUpMsgEventListeners(){
        for(int i= 0; i < allChatKeys.size(); i++){
            valueEventListeners.add(i, new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        for (com.google.firebase.database.DataSnapshot msgSnapshot : dataSnapshot.getChildren()) {
                            FirebaseMessageModel firebaseMessageModel = msgSnapshot.getValue(FirebaseMessageModel.class);
                            System.out.println("Sender: " + firebaseMessageModel.getSenderName() + " sent " + firebaseMessageModel.getText());

                            String isChattingTo = (firebaseMessageModel.getSenderName().equals(user.name)) ? firebaseMessageModel.getReceiverName() : firebaseMessageModel.getSenderName();

                            Chat chat = new Chat(isChattingTo, new MessageCell(
                                    firebaseMessageModel.getSenderName(),
                                    firebaseMessageModel.getText(),
                                    ChatActivity.getDate(firebaseMessageModel.getCreatedDateLong()),
                                    false
                            ));
                            allChats.add(chat);
                        }
                        myChatsdapter.setAllMyChats(allChats);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            messagesRef.child("single").child(allChatKeys.get(i)).limitToLast(1).addValueEventListener(valueEventListeners.get(i));
        }
    }

    private void setUpRecyclerView(){
        myChatsdapter = new AllChatsAdapter(new AllChatsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Chat chat, int pos) {

            }
        });
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);

        recyclerView.setAdapter(myChatsdapter);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

        if (userListener != null) {
            usersRef.removeEventListener(userListener);
        }

        for(ValueEventListener valueEventListener : valueEventListeners){
            messagesRef.removeEventListener(valueEventListener);
        }

    }


}

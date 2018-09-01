package com.example.naziur.androidchat.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
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
    private MyContactsAdapter myContactsAdapter;
    private RecyclerView recyclerView;
    private User user = User.getInstance();
    private List<String> allChatKeys;
    private List<Contact> allChats;

    public SessionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getActivity().setTitle("Contacts");

        View rootView = inflater.inflate(R.layout.fragment_session, container, false);

        allChats = new ArrayList<>();
        allChatKeys = new ArrayList<>();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        messagesRef = database.getReference("messages");
        recyclerView = rootView.findViewById(R.id.all_chats_list);
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                    if(firebaseUserModel.getUsername().equals(user.name)){
                        String[] allKeys = firebaseUserModel.getChatKeys().split(",");
                        for(String key: allKeys){
                            allChatKeys.add(key);
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        messagesRef.child("single").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(com.google.firebase.database.DataSnapshot msgSnapshot : dataSnapshot.getChildren()){
                    System.out.println(msgSnapshot.getKey());
                    if(allChatKeys.contains(msgSnapshot.getKey())){
                        System.out.println("Match found");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        setUpRecyclerView();
        return rootView;
    }

    private void setUpRecyclerView(){
        myContactsAdapter = new MyContactsAdapter(getContext(), new MyContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Contact contact, int pos) {

            }
        });


        recyclerView.setAdapter(myContactsAdapter);
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

    }


}

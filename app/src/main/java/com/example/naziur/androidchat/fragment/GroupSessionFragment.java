package com.example.naziur.androidchat.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.AllChatsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
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
public class GroupSessionFragment extends Fragment {

    private static final String TAG = SingleSessionFragment.class.getSimpleName();

    private FirebaseDatabase database;
    private DatabaseReference messagesRef, usersRef, groupsRef;
    private ValueEventListener userListener;
    private AllChatsAdapter myChatsdapter;
    private RecyclerView recyclerView;
    private TextView emptyChats;
    private User user = User.getInstance();
    private List<String> allGroupKeys;
    private List<Chat> allGroups;
    private ContactDBHelper db;
    private ProgressDialog progressBar;
    private List<ValueEventListener> grpValueEventListeners;

    public GroupSessionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_group_session, container, false);
        grpValueEventListeners = new ArrayList<>();
        allGroups = new ArrayList<>();
        allGroupKeys = new ArrayList<>();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        usersRef = database.getReference("groups");
        messagesRef = database.getReference("messages");
        emptyChats = (TextView) rootView.findViewById(R.id.no_chats);
        recyclerView = rootView.findViewById(R.id.all_group_chats_list);
        progressBar = new ProgressDialog(getActivity(), R.layout.progress_dialog, true);
        setUpAdapterWithRecyclerView();

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
        fetchUsersGroupKeys();
    }

    private void fetchUsersGroupKeys(){
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
                        String[] allKeys = firebaseUserModel.getGroupKeys().split(",");
                        allGroupKeys.clear();
                        for(String key: allKeys){
                            if(!key.equals(""))
                                allGroupKeys.add(key);
                        }
                        setUpGrpEventListeners();
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

    private void setUpGrpEventListeners() {

    }

    private void setUpAdapterWithRecyclerView(){
        myChatsdapter = new AllChatsAdapter(getContext(), new AllChatsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Chat chat, int pos) {

            }

            @Override
            public void onItemLongClicked(Chat chat, int pos) {

            }

            @Override
            public void onButtonClicked(Chat chat, int position) {

            }
        });

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(OrientationHelper.VERTICAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(myChatsdapter);
    }

}

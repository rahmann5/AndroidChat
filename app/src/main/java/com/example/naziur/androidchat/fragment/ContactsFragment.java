package com.example.naziur.androidchat.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.activities.SessionActivity;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseUserModel;
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
public class ContactsFragment extends Fragment {

    private static final String TAG = ContactsFragment.class.getSimpleName();

    private FirebaseDatabase database;
    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;
    private ValueEventListener userListener;

    private List<Contact> allContacts;

    public ContactsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getActivity().setTitle("Contacts");
        allContacts = new ArrayList<>();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        messagesRef = database.getReference("messages");


        return inflater.inflate(R.layout.fragment_contacts, container, false);
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

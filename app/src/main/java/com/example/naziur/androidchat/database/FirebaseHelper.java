package com.example.naziur.androidchat.database;

import android.util.Log;

import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Hamidur and Naziur on 21/09/2018.
 */

public class FirebaseHelper {

    private static FirebaseDatabase database = FirebaseDatabase.getInstance();

    public interface FirebaseHelperListener {
        void onCompleteTask (String tag, int condition, Container container);
        void onFailureTask(String tag, DatabaseError databaseError);
        void onChange(String tag, int condition, Container container);
    }

    public static final int CONDITION_1 = 0;
    public static final int CONDITION_2 = 1;

    private static FirebaseHelperListener listener;

    private FirebaseHelper () {};

    public static void setFirebaseHelperListener (FirebaseHelperListener fbListener) {
        listener = fbListener;
    }

    public static void autoLogin(String node, final String currentDeviceId, final User user) {
        DatabaseReference usersRef = database.getReference(node);
        usersRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                boolean fail = true;
                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    //Getting the data from snapshot
                    FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);

                    if (firebaseUserModel.getDeviceId().equals(currentDeviceId)) {
                        fail = false;
                        firebaseUserModel.setDeviceToken(FirebaseInstanceId.getInstance().getToken());
                        user.login(firebaseUserModel);
                        user.saveFirebaseKey(userSnapshot.getKey());
                        listener.onCompleteTask("autoLogin", CONDITION_1, null);
                        break;
                    }
                }

                if (fail) {
                    listener.onCompleteTask("autoLogin", CONDITION_2, null);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("autoLogin", databaseError);
            }
        });
    }


    public static void updateLocalContactsFromFirebase (String node, final FirebaseUserModel fbModel, final ContactDBHelper db) {
        DatabaseReference usersRef = database.getReference(node);
        Query query = usersRef.orderByChild("username").equalTo(fbModel.getUsername());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                        if(firebaseUserModel.getUsername().equals(fbModel.getUsername())) {
                            db.updateProfile(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic());
                            Container container = new Container();
                            container.setContact(new Contact(firebaseUserModel));
                            listener.onChange("updateLocalContactsFromFirebase",CONDITION_1, container);
                            break;
                        }
                    }
                } else {
                    Container container = new Container();
                    container.setContact(new Contact(fbModel, "", false));
                    listener.onCompleteTask("updateLocalContactsFromFirebase", CONDITION_1, container);
                }

                listener.onCompleteTask("updateLocalContactsFromFirebase", CONDITION_2, null);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
               listener.onFailureTask("updateLocalContactsFromFirebase", databaseError);
            }
        });
    }

    public static ValueEventListener createMessageEventListener () {
        return new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                listener.onChange("createMessageEventListener", CONDITION_1, null);

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    //System.out.println("Child: " + postSnapshot);
                    //Getting the data from snapshot
                    FirebaseMessageModel firebaseMessageModel = postSnapshot.getValue(FirebaseMessageModel.class);
                    Container container = new Container();
                    container.setMsgModel(firebaseMessageModel);
                    listener.onChange("createMessageEventListener", CONDITION_2, container);
                }

                listener.onCompleteTask("createMessageEventListener", CONDITION_1, null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("createMessageEventListener", databaseError);
            }
        };
    }

    public static ValueEventListener getMessageEventListener(final User user, final ContactDBHelper db, final String dateFormat, final String chatKey){
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot msgSnapshot : dataSnapshot.getChildren()) {
                        FirebaseMessageModel firebaseMessageModel = msgSnapshot.getValue(FirebaseMessageModel.class);
                        String isChattingTo = (firebaseMessageModel.getSenderName().equals(user.name)) ? db.getProfileNameAndPic(firebaseMessageModel.getReceiverName())[0] : db.getProfileNameAndPic(firebaseMessageModel.getSenderName())[0];
                        String username = (firebaseMessageModel.getSenderName().equals(user.name)) ? firebaseMessageModel.getReceiverName() : firebaseMessageModel.getSenderName();
                        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
                        String dateString = formatter.format(new Date(firebaseMessageModel.getCreatedDateLong()));
                        Chat chat = new Chat(isChattingTo, username, firebaseMessageModel.getText(), db.getProfileNameAndPic(username)[1], dateString, chatKey, firebaseMessageModel.getIsReceived(), firebaseMessageModel.getMediaType());
                        Container container = new Container();
                        container.setChat(chat);
                        listener.onChange("getMessageEventListener", CONDITION_1, container);
                    }
                    listener.onCompleteTask("getMessageEventListener", CONDITION_1, null);
                }
                listener.onCompleteTask("getMessageEventListener", CONDITION_2, null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getMessageEventListener", databaseError);
            }
        };

        return valueEventListener;
    }

    public static void attachOrRemoveMessageEventListener(String node, String chatKey, ValueEventListener valueEventListener, boolean add){
        DatabaseReference messagesRef = database.getReference("messages");
        Query pendingQuery = messagesRef.child(node).child(chatKey).limitToLast(1);
        if(add)
            pendingQuery.addValueEventListener(valueEventListener);
        else
            pendingQuery.removeEventListener(valueEventListener);

    }

    public static void removeListenerFor(String reference, ValueEventListener valueEventListener){
        DatabaseReference databaseReference = database.getReference(reference);
        databaseReference.removeEventListener(valueEventListener);
    }

    public static ValueEventListener getUsersValueEventListener(final User user){
        DatabaseReference usersRef = database.getReference("users");
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                        if (firebaseUserModel.getUsername().equals(user.name)) {
                            Container container = new Container();
                            container.setString(firebaseUserModel.getChatKeys());
                            listener.onCompleteTask("getUsersValueEventListener", CONDITION_1, container);
                            break;
                        }
                    }
                } else {
                    listener.onCompleteTask("getUsersValueEventListener", CONDITION_2, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getUsersValueEventListener", databaseError);
            }
        };
        usersRef.orderByChild("username").equalTo(user.name).addValueEventListener(valueEventListener);

        return valueEventListener;
    }

}

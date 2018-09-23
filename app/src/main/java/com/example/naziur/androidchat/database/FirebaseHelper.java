package com.example.naziur.androidchat.database;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.content.Context;
import android.widget.Toast;

import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.Notification;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
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
import com.google.firebase.iid.FirebaseInstanceId;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

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
    public static final int CONDITION_3 = 2;
    public static final int CONDITION_4 = 3;
    public static final int CONDITION_5 = 4;

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

    public static void manualLogin(final User user, final String username, final String profileName, final String currentDeviceId){
        DatabaseReference reference = database.getReference("users");
        reference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Container container = new Container();
                if(!dataSnapshot.exists()){
                    List<String> contents = new ArrayList<>();
                    contents.add(profileName);
                    contents.add(username);
                    container.setStringList(contents);
                    listener.onCompleteTask("manualLogin", CONDITION_1, container);
                } else {
                    boolean foundMatch = false;

                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        container.setUserModel(firebaseUserModel);
                        if(firebaseUserModel.getDeviceId().equals(currentDeviceId)){
                            listener.onCompleteTask("manualLogin", CONDITION_2, container);
                            user.saveFirebaseKey(snapshot.getKey());
                            foundMatch = true;
                            break;
                        } else {
                            listener.onCompleteTask("manualLogin", CONDITION_3, null);
                        }
                    }
                    if(!foundMatch)
                        listener.onCompleteTask("manualLogin", CONDITION_4, null);
                    else
                        listener.onCompleteTask("manualLogin", CONDITION_5, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("manualLogin", databaseError);
            }
        });
    }

    public static void registerNewUser(final FirebaseUserModel firebaseUserModel){
        DatabaseReference reference = database.getReference("users");
        final DatabaseReference newRef = reference.push();
        newRef.setValue(firebaseUserModel, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    Container c = new Container();
                    c.setUserModel(firebaseUserModel);
                    listener.onCompleteTask("registerNewUser", CONDITION_1, c);
                } else {
                    listener.onFailureTask("registerNewUser", databaseError);
                }
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

                listener.onCompleteTask("createMessageEventListener", CONDITION_1, null);

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    //System.out.println("Child: " + postSnapshot);
                    //Getting the data from snapshot
                    FirebaseMessageModel firebaseMessageModel = postSnapshot.getValue(FirebaseMessageModel.class);
                    Container container = new Container();
                    container.setMsgModel(firebaseMessageModel);
                    listener.onChange("createMessageEventListener", CONDITION_1, container);
                }

                listener.onCompleteTask("createMessageEventListener", CONDITION_2, null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("createMessageEventListener", databaseError);
            }
        };
    }

    public static void setUpSingleChat(String node, final String chatKey, final String friendUsername, final String usersUsername, final ValueEventListener commentValueEventListener) {
        DatabaseReference usersRef = database.getReference(node);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                FirebaseUserModel friend = null;
                FirebaseUserModel me = null;
                for (com.google.firebase.database.DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    System.out.println("Child: " + postSnapshot);
                    //Getting the data from snapshot
                    FirebaseUserModel firebaseUserModel = postSnapshot.getValue(FirebaseUserModel.class);
                    Container container = new Container();
                    container.setUserModel(firebaseUserModel);
                    if (firebaseUserModel.getUsername().equals(friendUsername)) {
                        listener.onChange("setUpSingleChat", CONDITION_1, container);
                        friend = firebaseUserModel;
                    } else if (firebaseUserModel.getUsername().equals(usersUsername)) {
                        listener.onChange("setUpSingleChat", CONDITION_2, container);
                        me = firebaseUserModel;
                    }

                    if (me != null && friend != null) {
                        break;
                    }
                }

                listener.onCompleteTask("setUpSingleChat", CONDITION_1, null);
                if (me != null) {
                    DatabaseReference messagesRef = database.getReference("messages")
                            .child("single")
                            .child(chatKey);
                    //Value event listener for realtime data update
                    messagesRef.addValueEventListener(commentValueEventListener);
                    listener.onCompleteTask("setUpSingleChat", CONDITION_2, null);
                } else {
                    listener.onCompleteTask("setUpSingleChat", CONDITION_3, null);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("setUpSingleChat", databaseError);
            }
        });
    }

    public static void removeMsgEventListeners (String node, String chatKey, ValueEventListener commentValueEventListener) {
        DatabaseReference messagesRef = database.getReference("messages")
                .child(node)
                .child(chatKey);
        //Value event listener for realtime data update
        messagesRef.removeEventListener(commentValueEventListener);
    }

    public static void checkKeyListKey (String node, String username, final int myCondition1, final int myCondition2 , final String chatKey) {
        DatabaseReference usersRef = database.getReference(node);
        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean found = false;
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        List<String> list = Arrays.asList(firebaseUserModel.getChatKeys().split(","));
                        if(list.contains(chatKey)){
                            found = true;
                            listener.onCompleteTask("checkKeyListKey", myCondition1, null);
                            break;
                        }
                    }
                }

                if (!found) {
                    Container container = new Container();
                    container.setString(chatKey);
                    listener.onCompleteTask("checkKeyListKey", myCondition2, container);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("checkKeyListKey", databaseError);
            }
        });
    }

    public static void createImageUploadMessageNode (String node, final String chatKey, final Context context, final String downloadUrl, final FirebaseUserModel friend) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages")
                .child(node)
                .child(chatKey);
        DatabaseReference newRef = messagesRef.push();
        newRef.setValue(Network.makeNewMessageNode(Constants.MESSAGE_TYPE_PIC, downloadUrl,friend), new DatabaseReference.CompletionListener() {

            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    // remove image from storage
                    Network.removeFailedImageUpload(downloadUrl.toString(), context);
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    // show message failed to send (icon)
                    listener.onFailureTask("createImageUploadMessageNode", databaseError);
                } else {
                    StringEntity entity = Network.generateEntity(context, Constants.MESSAGE_TYPE_PIC, downloadUrl, friend, chatKey);
                    if (entity == null){
                        Toast.makeText(context, "Failed to make notification", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Network.createAsyncClient().post(context, Constants.NOTIFICATION_URL, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            Container container = new Container();
                            container.setString(responseString);
                            listener.onCompleteTask("createImageUploadMessageNode", CONDITION_1, container);
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String responseString) {
                            Container container = new Container();
                            container.setString(responseString);
                            listener.onCompleteTask("createImageUploadMessageNode", CONDITION_2, container);
                        }
                    });
                }
            }


        });
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

    public static void updateChatKeys(final User user, final String updatedKeys, final Chat chat){
        DatabaseReference pendingTasks = database.getReference("users").orderByChild("username").equalTo(user.name).getRef();
        pendingTasks.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for(MutableData data : mutableData.getChildren()){
                    FirebaseUserModel firebaseUserModel = data.getValue(FirebaseUserModel.class);
                    if(firebaseUserModel == null)
                        return Transaction.success(mutableData);

                    if(firebaseUserModel.getUsername().equals(user.name)){
                        firebaseUserModel.setChatKeys(updatedKeys);
                        data.setValue(firebaseUserModel);
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if(databaseError != null) {
                    listener.onFailureTask("updateChatKeys", databaseError);
                } else {
                    Container container = new Container();
                    container.setChat(chat);
                    listener.onCompleteTask("updateChatKeys", CONDITION_1, container);
                }
            }

        });
    }

    public static void addUserToContacts(final String contactsUsername, final ContactDBHelper db, final int positionInAdapter){
        Query query = database.getReference("users").orderByChild("username").equalTo(contactsUsername);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        //Getting the data from snapshot
                        FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);

                        if (firebaseUserModel.getUsername().equals(contactsUsername)) {
                            db.insertContact(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic(), firebaseUserModel.getDeviceToken());
                            Container container = new Container();
                            container.setString(firebaseUserModel.getProfileName());
                            container.setInt(positionInAdapter);
                            listener.onCompleteTask("addUserToContacts", CONDITION_1, container);
                            break;
                        }
                    }

                } else {
                    listener.onCompleteTask("addUserToContacts", CONDITION_2, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("addUserToContacts", databaseError);
            }
        });
    }

    public static void collectAllImagesForDeletionThenDeleteRelatedMessages(String node, final String key){
        final DatabaseReference reference = database.getReference("messages").child(node).child(key);
        reference.orderByChild("mediaType").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final List<String> imageUri = new ArrayList<>();
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    FirebaseMessageModel model = data.getValue(FirebaseMessageModel.class);
                    if (model.getMediaType().equals(Constants.MESSAGE_TYPE_PIC)) {
                        imageUri.add(model.getText());
                    }
                }
                Container container = new Container();
                container.setString(key);
                container.setStringList(imageUri);
                listener.onCompleteTask("collectAllImagesForDeletionThenDeleteRelatedMessages", CONDITION_1, container);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("collectAllImagesForDeletionThenDeleteRelatedMessages", databaseError);
                cleanDeleteAllMessages(reference);
            }
        });
    }

    public static void cleanDeleteAllMessages(DatabaseReference reference){
        reference.setValue(null).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                listener.onCompleteTask("cleanDeleteAllMessages", CONDITION_1, null);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Container container = new Container();
                container.setString(e.getMessage());
                listener.onCompleteTask("cleanDeleteAllMessages", CONDITION_2, container);
            }
        });
    }


    public static void updateNotificationNode (String node, String targetUsername, final String chatKey) {
        final User user = User.getInstance();
        final DatabaseReference notificationRef = database.getReference("notifications").child(targetUsername);
        notificationRef.orderByChild(node).equalTo(chatKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    DatabaseReference newRef = notificationRef.push();
                    Notification notificationObj = new Notification();
                    notificationObj.setSender(user.name);
                    notificationObj.setChatKey(chatKey);
                    newRef.setValue(notificationObj).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            listener.onCompleteTask("updateNotificationNode", CONDITION_1, null);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            listener.onCompleteTask("updateNotificationNode", CONDITION_2, null);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("updateNotificationNode", databaseError);
            }
        });
    }

    public static void updateFirebaseMessageStatus (final String node, final String chatKey, final Map<Long, Map<String, Object>> messages) {
        final DatabaseReference messagesRef = database.getReference("messages").child(node).child(chatKey);
        messagesRef.limitToLast(messages.size()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        FirebaseMessageModel firebaseMessageModel = snapshot.getValue(FirebaseMessageModel.class);
                        if (messages.get(firebaseMessageModel.getCreatedDateLong()) != null)
                            messagesRef.child(snapshot.getKey()).updateChildren(messages.get(firebaseMessageModel.getCreatedDateLong()));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("updateFirebaseMessageStatus", databaseError);
            }
        });
    }

    public static void updateMessageNode (final Context context, final String node, final String chatKey, final String wishMessage, final FirebaseUserModel friend) {
        final DatabaseReference messagesRef = database.getReference("messages").child(node).child(chatKey);
        DatabaseReference newRef = messagesRef.push();
        newRef.setValue(Network.makeNewMessageNode(Constants.MESSAGE_TYPE_TEXT,wishMessage, friend), new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                   listener.onFailureTask("updateMessageNode", databaseError);
                } else {
                    final Container container = new Container();

                    if (friend != null) {
                        try {
                            StringEntity entity = Network.generateEntity(context, Constants.MESSAGE_TYPE_TEXT, wishMessage, friend, chatKey);
                            if (entity == null){
                                container.setString("Failed to make notification");
                                listener.onCompleteTask("updateMessageNode", CONDITION_1, container);
                                return;
                            }

                            Network.createAsyncClient().post(context, Constants.NOTIFICATION_URL, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
                                @Override
                                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                                    container.setString("Failed: " + responseString);
                                    listener.onCompleteTask("updateMessageNode", CONDITION_1, container);
                                }

                                @Override
                                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                                    container.setString("Success: " + responseString);
                                    listener.onCompleteTask("updateMessageNode", CONDITION_2, container);
                                }
                            });

                        } catch (Exception e) {
                            container.setString(e.toString());
                            listener.onCompleteTask("updateMessageNode", CONDITION_1, container);
                        }
                    } else {
                        container.setString("Friend not found");
                        listener.onCompleteTask("updateMessageNode", CONDITION_1, container);
                    }

                }
            }
        });
    }

    public static void getOnlineInfoForUser(final String userBeingViewed){
        DatabaseReference reference = database.getReference("users");
        reference.orderByChild("username").equalTo(userBeingViewed).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                        if (userBeingViewed.equals(userBeingViewed)) {
                            Container container = new Container();
                            container.setUserModel(firebaseUserModel);
                            listener.onCompleteTask("getOnlineInfoForUser", CONDITION_1, container);
                            break;
                        }
                    }
                } else {
                    listener.onCompleteTask("getOnlineInfoForUser", CONDITION_2, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getOnlineInfoForUser", databaseError);
            }
        });
    }


}

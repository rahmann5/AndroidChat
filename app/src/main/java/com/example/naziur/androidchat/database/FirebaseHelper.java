package com.example.naziur.androidchat.database;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.content.Context;
import android.widget.Toast;

import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
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

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    private FirebaseHelperListener listener;

    private FirebaseHelper () {};

    public static FirebaseHelper getInstance () {
        return new FirebaseHelper();
    }

    public void setFirebaseHelperListener (FirebaseHelperListener fbListener) {
        listener = fbListener;
    }

    public void autoLogin(String node, final String currentDeviceId, final User user) {
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

    public void manualLogin(final User user, final String username, final String profileName, final String currentDeviceId){
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

    public void registerNewUser(final FirebaseUserModel firebaseUserModel){
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


    public void updateLocalContactsFromFirebase (String node, final FirebaseUserModel fbModel, final ContactDBHelper db) {
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

    public ValueEventListener createMessageEventListener () {
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

    // get users and contacts latest chat keys (needs to be optimised)
    public void setUpSingleChat(String node, final String friendUsername, final String usersUsername) {
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
                        friend = firebaseUserModel;
                        listener.onChange("setUpSingleChat", CONDITION_1, container);
                    } else if (firebaseUserModel.getUsername().equals(usersUsername)) {
                        me = firebaseUserModel;
                        listener.onChange("setUpSingleChat", CONDITION_2, container);
                    }

                    if (me != null && friend != null) {
                        break;
                    }
                }

                listener.onCompleteTask("setUpSingleChat", CONDITION_1, null);
                if (me != null && friend != null) {
                    Container container = new Container();
                    container.setUserModel(me);
                    container.setContact(new Contact(friend));
                    listener.onCompleteTask("setUpSingleChat", CONDITION_2, container);
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

    public void toggleMsgEventListeners (String node, String chatKey, ValueEventListener commentValueEventListener, boolean add) {
        DatabaseReference messagesRef = database.getReference("messages")
                .child(node)
                .child(chatKey);
        //Value event listener for realtime data update
        if (add) {
            messagesRef.addValueEventListener(commentValueEventListener);
        } else {
            messagesRef.removeEventListener(commentValueEventListener);
        }
    }

    public void checkKeyListKey (String node, final int myCondition1, final int myCondition2 , final String msgId, final String username) {
        database.getReference(node).orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean found = false;
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        List<String> list = Arrays.asList(firebaseUserModel.getChatKeys().split(","));
                        if (list.contains(msgId)) {
                            found = true;
                            listener.onCompleteTask("checkKeyListKey", myCondition1, null);
                            break;
                        }
                    }
                }

                if (!found) {
                    Container container = new Container();
                    container.setString(msgId);
                    listener.onCompleteTask("checkKeyListKey", myCondition2, container);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("checkKeyListKey", databaseError);
            }
        });
    }

    public void checkGroupsKeys(String node, final int myCondition1, final int myCondition2 , final String msgId, final String... usernames){
        final List<String> allMembers = Arrays.asList(usernames);
        Collections.sort(allMembers);

        database.getReference(node).orderByChild("username").startAt(allMembers.get(0)).endAt(allMembers.get(allMembers.size()-1)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    listener.onCompleteTask("checkGroupsKeys", myCondition1, null);
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        for(int i = 0; i < allMembers.size(); i++){
                            if(allMembers.contains(firebaseUserModel.getUsername())){
                                if(Arrays.asList(firebaseUserModel.getGroupKeys().split(",")).contains(msgId)) {
                                    Container container = new Container();
                                    container.setString(firebaseUserModel.getDeviceToken());
                                    listener.onChange("checkGroupsKeys", myCondition1,container);
                                }
                            }
                        }
                    }
                    listener.onCompleteTask("checkGroupsKeys", myCondition2, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("checkKeyListKey", databaseError);
            }
        });

    }

    public void createImageUploadMessageNode (String node, final String chatKey, final Context context, final String downloadUrl, FirebaseMessageModel messageModel, final StringEntity entity) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages")
                .child(node)
                .child(chatKey);
        DatabaseReference newRef = messagesRef.push();
        newRef.setValue(messageModel, new DatabaseReference.CompletionListener() {

            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    // remove image from storage
                    Network.removeFailedImageUpload(downloadUrl.toString(), context);
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    // show message failed to send (icon)
                    listener.onFailureTask("createImageUploadMessageNode", databaseError);
                } else {
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

    public ValueEventListener getMessageEventListener(final String chatKey){
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    listener.onCompleteTask("getMessageEventListener", CONDITION_3, null);
                    for (com.google.firebase.database.DataSnapshot msgSnapshot : dataSnapshot.getChildren()) {
                        FirebaseMessageModel firebaseMessageModel = msgSnapshot.getValue(FirebaseMessageModel.class);
                        Container container = new Container();
                        container.setMsgModel(firebaseMessageModel);
                        container.setString(chatKey);
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

    public void attachOrRemoveMessageEventListener(String node, String chatKey, ValueEventListener valueEventListener, boolean add){
        DatabaseReference messagesRef = database.getReference("messages");
        Query pendingQuery = messagesRef.child(node).child(chatKey).limitToLast(1);
        if(add)
            pendingQuery.addValueEventListener(valueEventListener);
        else
            pendingQuery.removeEventListener(valueEventListener);

    }

    public void toggleListenerFor(String reference, String child, String target, ValueEventListener valueEventListener, boolean add, boolean single){
        DatabaseReference databaseReference = database.getReference(reference);
        if (!single) {
            if (add) {
                databaseReference.orderByChild(child).equalTo(target).addValueEventListener(valueEventListener);
            } else {
                databaseReference.orderByChild(child).equalTo(target).removeEventListener(valueEventListener);
            }
        } else {
            databaseReference.orderByChild(child).equalTo(target).addListenerForSingleValueEvent(valueEventListener);
        }
    }

    public ValueEventListener getValueEventListener(final String target, final int condition ,final Class obj){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Container container = new Container();
                container.setString(target);
                if(dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        container.setObject(userSnapshot.getValue(obj));
                        listener.onCompleteTask("getValueEventListener", condition, container);
                    }
                } else {
                    listener.onCompleteTask("getValueEventListener", CONDITION_2, container);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getValueEventListener", databaseError);
            }
        };
    }

    public void updateChatKeys(final User user, final String updatedKeys, final Chat chat, final boolean isGroup){
        DatabaseReference pendingTasks = database.getReference("users").orderByChild("username").equalTo(user.name).getRef();
        pendingTasks.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for(MutableData data : mutableData.getChildren()){
                    FirebaseUserModel firebaseUserModel = data.getValue(FirebaseUserModel.class);
                    if(firebaseUserModel == null)
                        return Transaction.success(mutableData);

                    if(firebaseUserModel.getUsername().equals(user.name)){
                        if (!isGroup) {
                            firebaseUserModel.setChatKeys(updatedKeys);
                        } else {
                            firebaseUserModel.setGroupKeys(updatedKeys);
                        }

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

    public void addUserToContacts(final String contactsUsername, final ContactDBHelper db, final int positionInAdapter){
        Query query = database.getReference("users").orderByChild("username").equalTo(contactsUsername);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        //Getting the data from snapshot
                        FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);

                        if (firebaseUserModel.getUsername().equals(contactsUsername)) {
                            Container container = new Container();
                            if(db != null) {
                                db.insertContact(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic(), firebaseUserModel.getDeviceToken());
                                container.setString(firebaseUserModel.getProfileName());
                                container.setInt(positionInAdapter);
                            } else {
                                container.setString(contactsUsername);
                            }
                            container.setUserModel(firebaseUserModel);
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

    public void collectAllImagesForDeletionThenDeleteRelatedMessages(final String node, final String key){
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
                cleanDeleteAllMessages(node, key);
            }
        });
    }

    public void cleanDeleteAllMessages(String node, String key){
        DatabaseReference reference = database.getReference("messages").child(node).child(key);
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


    public void updateNotificationNode (String node, final FirebaseUserModel target, final String chatKey) {
        final User user = User.getInstance();
        final DatabaseReference notificationRef = database.getReference("notifications").child(target.getUsername());
        notificationRef.orderByChild(node).equalTo(chatKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    DatabaseReference newRef = notificationRef.push();
                    Notification notificationObj = new Notification();
                    notificationObj.setSender(user.name);
                    notificationObj.setChatKey(chatKey);
                    final Container container = new Container();
                    container.setContact(new Contact(target));
                    container.setString(chatKey);
                    newRef.setValue(notificationObj).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            listener.onCompleteTask("updateNotificationNode", CONDITION_1, container);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            listener.onCompleteTask("updateNotificationNode", CONDITION_2, container);
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

    public void removeNotificationNode (final String target, final String chatKey, final boolean action) {
        User user = User.getInstance();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(user.name);
        notificationRef.orderByChild("sender").equalTo(target).getRef().runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    Notification notification = data.getValue(Notification.class);
                    if (notification == null) return Transaction.success(mutableData);

                    if (target.equals(notification.getSender())) {
                        notification = null;
                    }

                    data.setValue(notification);
                    break;
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    Container container = new Container();
                    container.setString(chatKey);
                    if (action) {
                        listener.onCompleteTask("removeNotificationNode", CONDITION_1, container );
                    }
                } else {
                    listener.onFailureTask("removeNotificationNode", databaseError);
                }
            }
        });
    }

    public void notificationNodeExists(final String target, final String chatKey, ValueEventListener eventListener) {
        User user = User.getInstance();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(user.name);
        if (eventListener == null)
            notificationRef.orderByChild("sender").equalTo(target).addListenerForSingleValueEvent(getNotificationChecker(target, chatKey));
        else
            notificationRef.addValueEventListener(eventListener);
    }

    public static void removeNotificationListener (ValueEventListener eventListener) {
        User user = User.getInstance();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(user.name);
        notificationRef.removeEventListener(eventListener);
    }

    public ValueEventListener getNotificationChecker (final String target, final String chatKey) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean exists = false;
                boolean invite = false;
                List<Notification> notifications = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot notiSnapshot : dataSnapshot.getChildren()) {
                        Notification notification = notiSnapshot.getValue(Notification.class);
                        if (notification.getSender().equals(target)) {
                            invite = true;
                            break;
                        }
                        notifications.add(notification);
                    }
                    exists = true;
                }

                Container container = new Container();
                container.setString(chatKey);
                container.setBoolean(exists);
                container.setNotifications(notifications);
                if (invite) {
                    listener.onCompleteTask("notificationNodeExists", CONDITION_1, container);
                } else {
                    listener.onCompleteTask("notificationNodeExists", CONDITION_2, container);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("notificationNodeExists", databaseError);
            }
        };
    }

    public void updateFirebaseMessageStatus (final String node, final String chatKey, final Map<Long, Map<String, Object>> messages) {
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

    public void updateMessageNode (final Context context, final String node, final String chatKey, final String wishMessage, final FirebaseUserModel friend, final String msgType, final JSONArray membersDeviceTokens, final String grpTitle) {
        final DatabaseReference messagesRef = database.getReference("messages").child(node).child(chatKey);
        DatabaseReference newRef = messagesRef.push();
            newRef.setValue(
                    (membersDeviceTokens == null)? Network.makeNewMessageNode(msgType,wishMessage, friend) : Network.makeNewGroupMessageModel(chatKey, wishMessage, msgType),
                    new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                   listener.onFailureTask("updateMessageNode", databaseError);
                } else {
                    final Container container = new Container();
                    container.setString(chatKey);
                    if ((friend != null && membersDeviceTokens == null) || (friend == null && membersDeviceTokens != null)) {
                        try {
                            StringEntity entity;
                            if(membersDeviceTokens == null)
                               entity = Network.generateSingleMsgEntity(context,msgType, wishMessage, friend, chatKey);
                            else
                               entity = Network.generateGroupMsgEntity(context, msgType, membersDeviceTokens, grpTitle, chatKey, wishMessage);

                            if (entity == null){
                                if(membersDeviceTokens == null)
                                    container.setString("Failed to make notification");
                                listener.onCompleteTask("updateMessageNode", CONDITION_2, container);
                                return;
                            }

                            Network.createAsyncClient().post(context, Constants.NOTIFICATION_URL, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
                                @Override
                                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                                    if(membersDeviceTokens == null) {
                                        container.setString("Failed: " + responseString);
                                        listener.onCompleteTask("updateMessageNode", CONDITION_1, container);
                                    } else {
                                        listener.onCompleteTask("updateMessageNode", CONDITION_2, container);
                                    }
                                }

                                @Override
                                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                                    if(membersDeviceTokens == null) {
                                        container.setString("Success: " + responseString);
                                        listener.onCompleteTask("updateMessageNode", CONDITION_2, container);
                                    }
                                    else
                                        listener.onCompleteTask("updateMessageNode", CONDITION_1, container);
                                }
                            });

                        } catch (Exception e) {
                            if(membersDeviceTokens == null) {
                                container.setString(e.toString());
                                listener.onCompleteTask("updateMessageNode", CONDITION_1, container);
                            }
                        }
                    } else {
                        if(membersDeviceTokens == null) {
                            container.setString("Friend not found");
                            listener.onCompleteTask("updateMessageNode", CONDITION_1, container);
                        }
                    }
                }
            }
        });
    }

    public void updateChatKeyFromContact (final Contact c, final String chatKey, final boolean invite, final boolean duplicationCheck) {
        final User user = User.getInstance();
        database.getReference("users").orderByChild("username").equalTo(user.name).getRef().runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseUserModel currentUser = data.getValue(FirebaseUserModel.class);
                    if (currentUser == null) return Transaction.success(mutableData);
                    if (currentUser.getUsername().equals(user.name)) {
                        String currentKeys = currentUser.getChatKeys();
                        if (currentKeys.equals("")) {
                            currentKeys = chatKey;
                        } else {
                            currentKeys = (duplicationCheck) ? removeAnyDuplicateKey(currentUser.getChatKeys().split(","), chatKey ,generateOppositeKey(chatKey))
                                                    : currentKeys + "," + chatKey; ;
                        }

                        currentUser.setChatKeys(currentKeys);
                        data.setValue(currentUser);
                        break;
                    }
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    Container container = new Container();
                    container.setContact(c);
                    container.setString(chatKey);
                    if (invite) {
                        listener.onCompleteTask("updateChatKeyFromContact", CONDITION_1, container);
                    } else {
                        listener.onCompleteTask("updateChatKeyFromContact", CONDITION_2, container);
                    }
                } else {
                    listener.onFailureTask("updateChatKeyFromContact", databaseError);
                }
            }
        });
    }

    private static String generateOppositeKey (String currentKey) {
        String [] keys = currentKey.split("-");
        return keys[1] + "-" + keys[0];
    }

    private static String removeAnyDuplicateKey (String[] myKeys, String keyToAdd , String searchDup) {
        String newKeys = keyToAdd;
        for (String key : myKeys) {
            if (!key.equals(searchDup) || !key.equals(keyToAdd)) {
                newKeys += "," + key;
            }
        }

        return newKeys;
    }

    public void getOnlineInfoForUser(final String userBeingViewed){
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

    public void createGroup(final FirebaseGroupModel firebaseGroupModel){
        DatabaseReference newRef = database.getReference("groups").push();
        newRef.setValue(firebaseGroupModel, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if(databaseError != null){
                    listener.onFailureTask("createGroup", databaseError);
                } else {
                    Container container = new Container();
                    container.setString(firebaseGroupModel.getTitle()+","+firebaseGroupModel.getGroupKey());
                    listener.onCompleteTask("createGroup", CONDITION_1, container);
                }
            }
        });
    }

    public void getDeviceTokensFor(final List<String> allMembers, final String title, final String uniqueId){
        Collections.sort(allMembers);
        DatabaseReference reference = database.getReference("users");
        reference.orderByChild("username").startAt(allMembers.get(0)).endAt(allMembers.get(allMembers.size()-1)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    JSONArray membersDeviceTokens = new JSONArray();
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        if(allMembers.contains(firebaseUserModel.getUsername())){
                            membersDeviceTokens.put(firebaseUserModel.getDeviceToken());
                        }
                    }
                    Container container = new Container();
                    container.setJsonArray(membersDeviceTokens);
                    container.setString(title+","+uniqueId);
                    listener.onCompleteTask("getDeviceTokensFor", CONDITION_1, container);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getDeviceTokensFor", databaseError);
            }
        });
    }

    public void updateGroupKeyForMembers(final List<String> allMembers, final String uniqueID, final User user){
        DatabaseReference reference = database.getReference("users");
        reference.orderByChild("username").getRef().runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for(MutableData data : mutableData.getChildren()){
                    FirebaseUserModel firebaseUserModel = data.getValue(FirebaseUserModel.class);
                    if(firebaseUserModel == null)
                        return Transaction.success(mutableData);

                    if(firebaseUserModel.getUsername().equals(user.name) || allMembers.contains(firebaseUserModel.getUsername())){
                        if(firebaseUserModel.getGroupKeys().equals(""))
                            firebaseUserModel.setGroupKeys(uniqueID);
                        else {
                            List<String> membersKeys = Arrays.asList(firebaseUserModel.getGroupKeys().split(","));
                            if(!membersKeys.contains(uniqueID))
                                firebaseUserModel.setGroupKeys(firebaseUserModel.getGroupKeys() + "," + uniqueID);
                        }
                        data.setValue(firebaseUserModel);
                    }
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if(databaseError != null){
                    listener.onFailureTask("updateGroupKeyForMembers", databaseError);
                } else {
                    Container container = new Container();
                    container.setString(uniqueID);
                    listener.onCompleteTask("updateGroupKeyForMembers", CONDITION_1, container);
                }
            }
        });
    }

    public void updateUserInfo (String target, final Uri uploadedImgUri, final String status, final String profileName, final boolean reset) {
        DatabaseReference userRef = database.getReference("users");
        userRef.orderByChild("username").equalTo(target).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    final FirebaseUserModel updatedUser = snapshot.getValue(FirebaseUserModel.class);
                    updatedUser.setStatus(status);
                    updatedUser.setProfileName(profileName);

                    if (uploadedImgUri != null && !reset) { // new profile pic upload
                        updatedUser.setProfilePic(uploadedImgUri.toString());
                    } else if (uploadedImgUri == null && reset) { // reset image back to unknown
                        updatedUser.setProfilePic("");
                    } else if (uploadedImgUri == null && !reset){ // keep current image but change other information
                        // do nothing
                    }

                    snapshot.getRef().setValue(updatedUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Container container = new Container();
                            container.setUserModel(updatedUser);
                            listener.onCompleteTask("updateUserInfo", CONDITION_1, container);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            listener.onCompleteTask("updateUserInfo", CONDITION_2, null);
                        }
                    });

                    break;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("updateUserInfo", databaseError);
            }
        });
    }

    public void exitGroup (final String chatKey, final String leavingUser, final boolean admin) {
        DatabaseReference reference = database.getReference("groups");
        reference.orderByChild("groupKey").equalTo(chatKey).getRef().runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseGroupModel groupModel = data.getValue(FirebaseGroupModel.class);

                    if (groupModel == null) return Transaction.success(mutableData);
                    if (groupModel.getGroupKey().equals(chatKey)) {
                        String newMembers = groupModel.getMembers();
                        if (admin) {
                            groupModel.setAdmin("");
                        } else {
                            String[] members = groupModel.getMembers().split(",");
                            newMembers = "";
                            for (String member : members) {
                                if (!member.equals(leavingUser)) {
                                    newMembers += (newMembers.equals("")) ? member : "," + member;
                                }
                            }
                        }

                        groupModel.setMembers(newMembers);
                        data.setValue(groupModel);
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    Container container = new Container();
                    container.setString(chatKey); // key to remove
                    listener.onCompleteTask("exitGroup", CONDITION_1, container);
                } else {
                    listener.onFailureTask("exitGroup", databaseError);
                }
            }
        });
    }

    public void deleteGroup (final String chatKey) {
        DatabaseReference groupRef = database.getReference("groups").orderByChild("groupKey").equalTo(chatKey).getRef();
        groupRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseGroupModel groupModel = data.getValue(FirebaseGroupModel.class);

                    if (groupModel == null) return Transaction.success(mutableData);

                    if (groupModel.getGroupKey().equals(chatKey)) {
                        if (groupModel.getAdmin().equals("") && groupModel.getMembers().equals("")) { // no admin and members left
                            data = null;
                        }
                    }

                    mutableData.setValue(data);
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    Container container = new Container();
                    container.setString(chatKey); // key to remove
                    if (!dataSnapshot.exists()) {
                        // complete delete
                        listener.onCompleteTask("deleteGroup", CONDITION_1, container);
                    } else {
                        listener.onCompleteTask("deleteGroup", CONDITION_2, container);
                    }
                } else {
                    listener.onFailureTask("deleteGroup", databaseError);
                }
            }
        });
    }

    public ValueEventListener getGroupInfo(final String groupKey){
        return database.getReference("groups").orderByChild("groupKey").equalTo(groupKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    FirebaseGroupModel groupModel = postSnapshot.getValue(FirebaseGroupModel.class);
                    if (groupModel.getGroupKey().equals(groupKey)) {
                        Container container = new Container();
                        container.setGroupModel(groupModel);
                        listener.onCompleteTask("getGroupInfo", CONDITION_1, container);
                        break;
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getGroupInfo", databaseError);
            }
        });
    }



}

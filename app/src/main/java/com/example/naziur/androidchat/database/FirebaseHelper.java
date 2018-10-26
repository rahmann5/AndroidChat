package com.example.naziur.androidchat.database;

import android.database.Cursor;
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
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;

import java.util.Arrays;
import java.util.Iterator;
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


    public static final int NON_CONDITION = -1;
    public static final int CONDITION_1 = 0;
    public static final int CONDITION_2 = 1;
    public static final int CONDITION_3 = 2;
    public static final int CONDITION_4 = 3;
    public static final int CONDITION_5 = 4;
    public static final int CONDITION_6 = 5;
    public static final int CONDITION_7 = 6;

    private FirebaseHelperListener listener;

    private FirebaseHelper () {};

    public static FirebaseHelper getInstance () {
        return new FirebaseHelper();
    }

    public void setFirebaseHelperListener (FirebaseHelperListener fbListener) {
        listener = fbListener;
    }

    public static DatabaseReference setOnlineStatusListener (String uid, final boolean single) {
        final DatabaseReference databaseRef = database.getReference().child("users").child(uid);
        ValueEventListener v =new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot !=  null) {
                    if (single) {
                        databaseRef.child("online").setValue(false);
                    } else {
                        databaseRef.child("online").onDisconnect().setValue(false);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        if (single) {
            databaseRef.addListenerForSingleValueEvent(v);
        } else {
            databaseRef.addValueEventListener(v);
        }
        return databaseRef;
    }

    public void updateUserDeviceToken (final String strToken) {
        final User user = User.getInstance();
        final DatabaseReference usersRef = database.getReference("users").orderByChild("username").equalTo(user.name).getRef();
        final Container container = new Container();
        container.setString(strToken);
        usersRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseUserModel userModel = data.getValue(FirebaseUserModel.class);

                    if (userModel == null) return Transaction.success(mutableData);

                    if (strToken != null && userModel.getDeviceId().equals(user.deviceId) && !strToken.equals(userModel.getDeviceToken())) {
                        userModel.setDeviceToken(strToken);
                        data.setValue(userModel);
                        break;
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    listener.onCompleteTask("updateUserDeviceToken", CONDITION_1, container);
                } else {
                    listener.onCompleteTask("updateUserDeviceToken", CONDITION_2, container);
                }
            }
        });
    }

    public void autoLogin(String node, final String currentDeviceId, final User user) {
        database.getReference(node).orderByChild("deviceId").equalTo(currentDeviceId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                        String deviceToken = FirebaseInstanceId.getInstance().getToken();
                        if (firebaseUserModel.getDeviceId().equals(currentDeviceId)) {
                            if (firebaseUserModel.getDeviceToken().equals(deviceToken)) {
                                listener.onCompleteTask("autoLogin", CONDITION_1, null);
                            } else{
                                Container container = new Container();
                                container.setString(deviceToken);
                                listener.onCompleteTask("autoLogin", CONDITION_3, container);
                            }
                            firebaseUserModel.setDeviceToken(deviceToken);
                            user.login(firebaseUserModel);
                            user.saveFirebaseKey(userSnapshot.getKey());
                            break;
                        }
                    }
                } else {
                    listener.onCompleteTask("autoLogin", CONDITION_2, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("autoLogin", databaseError);
            }
        });
    }

    public void manualLogin(final String username,final String currentDeviceId){
        final User user = User.getInstance();
        DatabaseReference reference = database.getReference("users");
        reference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Container container = new Container();
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        container.setUserModel(firebaseUserModel);
                        if(firebaseUserModel.getDeviceId().equals(currentDeviceId)){
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();
                            user.saveFirebaseKey(snapshot.getKey());
                            user.login(container.getUserModel());
                            if (firebaseUserModel.getDeviceToken().equals(deviceToken)) {
                                listener.onCompleteTask("manualLogin", CONDITION_1, container);
                            } else {
                                container.setString(deviceToken);
                                listener.onCompleteTask("manualLogin", CONDITION_4, container);
                            }
                        } else {
                            listener.onCompleteTask("manualLogin", CONDITION_2, null);
                        }
                    }
                } else {
                    listener.onCompleteTask("manualLogin", CONDITION_3, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("manualLogin", databaseError);
            }
        });
    }

    public void isDeviceAlreadyRegistered(final String deviceId){
        DatabaseReference reference = database.getReference("users");
        reference.orderByChild("deviceId").equalTo(deviceId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    Container container = new Container();
                    container.setString(deviceId);
                    listener.onCompleteTask("isDeviceAlreadyRegistered", CONDITION_1, container);
                } else {
                    System.out.println(deviceId+" VS "+dataSnapshot.getValue());
                    listener.onCompleteTask("isDeviceAlreadyRegistered", CONDITION_2, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("isDeviceAlreadyRegistered", databaseError);
            }
        });
    }

    public void registerNewUser(final FirebaseUserModel firebaseUserModel, String uid){
        DatabaseReference reference = database.getReference("users").child(uid);
        reference.setValue(firebaseUserModel, new DatabaseReference.CompletionListener() {
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

    public void updateAllLocalContactsFromFirebase (Context context, final ContactDBHelper db) {
        Cursor cursor = db.getAllMyContacts(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME);
        final List<Contact> allContactsUpToDate = new ArrayList<>();
        final List<String> allContactsByUsername = new ArrayList<>();
        final Container container = new Container();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                final FirebaseUserModel fbModel = new FirebaseUserModel();
                fbModel.setUsername(cursor.getString(cursor.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME)));
                fbModel.setProfileName(cursor.getString(cursor.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));
                fbModel.setProfilePic(cursor.getString(cursor.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC)));
                allContactsUpToDate.add(new Contact(fbModel, false)); // updated with new info
                allContactsByUsername.add(fbModel.getUsername()); // list of usernames
            }
            cursor.close();
        }
        if (Network.isInternetAvailable(context, false) && !allContactsByUsername.isEmpty()) {
            DatabaseReference usersRef = database.getReference("users");
            Query query = usersRef.orderByChild("username").equalTo(allContactsByUsername.get(0));
            if (allContactsByUsername.size() > 1) {
                query = usersRef.orderByChild("username").startAt(allContactsByUsername.get(0))
                        .endAt(allContactsByUsername.get(allContactsByUsername.size()-1));
            }
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                            int indexOf = allContactsByUsername.indexOf(firebaseUserModel.getUsername());
                            if (indexOf != -1) {
                                db.updateProfile(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic());
                                allContactsUpToDate.remove(indexOf);
                                allContactsUpToDate.add(indexOf, new Contact(firebaseUserModel));
                            }
                        }

                    }
                    container.setContacts(allContactsUpToDate);
                    listener.onCompleteTask("updateAllLocalContactsFromFirebase", CONDITION_1, container);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    container.setContacts(allContactsUpToDate);
                    listener.onCompleteTask("updateAllLocalContactsFromFirebase", CONDITION_1, container);
                    listener.onFailureTask("updateAllLocalContactsFromFirebase", databaseError);
                }
            });
        } else {
            container.setContacts(allContactsUpToDate);
            listener.onCompleteTask("updateAllLocalContactsFromFirebase", CONDITION_1, container);
        }

    }

    public ValueEventListener createGroupMessageEventListener(final List<FirebaseMessageModel> currentMessages, final int loadAmount, final int currentKnownMessageCount) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Container container = new Container();
                    List<FirebaseMessageModel> dataInReverse = new ArrayList<>();
                    if (currentMessages.size() == 0) {
                        List<FirebaseMessageModel> tempMessages = new ArrayList<>();
                        //ITERATING ALL DATA BECAUSE GOOGLE WAS TOO LAZY TO ALLOW US TO GET DATA IN REVERSE
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            FirebaseMessageModel firebaseMessageModel = postSnapshot.getValue(FirebaseMessageModel.class);
                            firebaseMessageModel.setId(postSnapshot.getKey());
                            dataInReverse.add(firebaseMessageModel);
                        }

                        for (int i = dataInReverse.size() - 1; i >= 0; i--) {
                            if (tempMessages.size() < loadAmount)
                                tempMessages.add(0, dataInReverse.get(i));
                            else if (tempMessages.size() == loadAmount)
                                break;
                        }
                        container.setMessages(tempMessages);
                        container.setInt(dataInReverse.size());
                        listener.onCompleteTask("createGroupMessageEventListener", CONDITION_1, container);
                    } else {
                        container.setLong( dataSnapshot.getChildrenCount());
                        listener.onCompleteTask("createGroupMessageEventListener", CONDITION_2, container);
                    }
                } else {
                    listener.onCompleteTask("createGroupMessageEventListener", CONDITION_3, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("createGroupMessageEventListener", databaseError);
            }
        };
    }

    public void getNewMessages(String key, long amountToGet){
        database.getReference("messages").child("group").child(key).orderByKey()
                .limitToLast((int) Math.max(Math.min(Integer.MAX_VALUE, amountToGet), Integer.MIN_VALUE))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            List<FirebaseMessageModel> tempMessages = new ArrayList<>();
                            for(DataSnapshot postSnapshot : dataSnapshot.getChildren()){
                                FirebaseMessageModel firebaseMessageModel = postSnapshot.getValue(FirebaseMessageModel.class);
                                firebaseMessageModel.setId(postSnapshot.getKey());
                                tempMessages.add(firebaseMessageModel);
                            }
                            Container container = new Container();
                            container.setMessages(tempMessages);
                            listener.onCompleteTask("getNewMessages", CONDITION_1, container);
                        } else {
                            listener.onCompleteTask("getNewMessages", CONDITION_2, null);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        listener.onFailureTask("getNewMessages", databaseError);
                    }
                });
    }

    public ValueEventListener createMessageEventListener () {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        FirebaseMessageModel firebaseMessageModel = postSnapshot.getValue(FirebaseMessageModel.class);
                        Container container = new Container();
                        firebaseMessageModel.setId(postSnapshot.getKey());
                        container.setMsgModel(firebaseMessageModel);
                        listener.onChange("createMessageEventListener", CONDITION_1, container);
                    }
                    listener.onCompleteTask("createMessageEventListener", CONDITION_1, null);
                } else {
                    listener.onCompleteTask("createMessageEventListener", CONDITION_2, null);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("createMessageEventListener", databaseError);
            }
        };
    }

    public void getNextNMessages (String child, String key, final String start, int amount) {
        Query query;
        if (!start.equals("")) {
            query = FirebaseDatabase.getInstance().getReference("messages").child(child)
                    .child(key).orderByKey().endAt(start);;
        } else {
            query = FirebaseDatabase.getInstance().getReference("messages").child(child)
                    .child(key).orderByKey();
        }

        query.limitToLast(amount).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        FirebaseMessageModel firebaseMessageModel = child.getValue(FirebaseMessageModel.class);
                        if (!child.getKey().equals(start)) {
                            firebaseMessageModel.setId(child.getKey());
                            Container container = new Container();
                            container.setMsgModel(firebaseMessageModel);
                            listener.onChange("getNextNMessages", CONDITION_1, container);
                        }
                    }
                    listener.onCompleteTask("getNextNMessages", CONDITION_1, null);
                } else {
                    listener.onCompleteTask("getNextNMessages", CONDITION_2, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getNextNMessages", databaseError);
            }
        });
    }

    public void toggleGrpMsgEventListeners(String node, String chatKey, ValueEventListener commentValueEventListener, boolean add, boolean single){
        Query messagesRef = database.getReference("messages")
                .child(node)
                .child(chatKey);

        if (!single) {
            if (add) {
                messagesRef.addValueEventListener(commentValueEventListener);
            } else {
                messagesRef.removeEventListener(commentValueEventListener);
            }
        } else {
            messagesRef.addListenerForSingleValueEvent(commentValueEventListener);
        }

    }

    public void toggleUnreadMsgEventListeners(String node, String chatKey, ValueEventListener commentValueEventListener, boolean add, boolean single) {
        Query messagesRef = database.getReference("messages")
                .child(node)
                .child(chatKey)
                .orderByChild("isReceived")
                .equalTo(Constants.MESSAGE_SENT);

        if (!single) {
            if (add) {
                messagesRef.addValueEventListener(commentValueEventListener);
            } else {
                messagesRef.removeEventListener(commentValueEventListener);
            }
        } else {
            messagesRef.addListenerForSingleValueEvent(commentValueEventListener);
        }
    }

    public void checkKeyListKey (String node, final int myCondition1, final int myCondition2 , final String msgId, final String username) {
        database.getReference(node).orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean found = false;
                Container container = new Container();
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        container.setUserModel(firebaseUserModel);
                        List<String> list = Arrays.asList(firebaseUserModel.getChatKeys().split(","));
                        if (list.contains(msgId)) {
                            found = true;
                            listener.onCompleteTask("checkKeyListKey", myCondition1, null);
                            break;
                        }
                    }
                }

                if (!found) {
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

    public ValueEventListener getMessageEventListener(final String chatKey, final int loc, final String message, final String username){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Container container = new Container();
                container.setInt(loc);
                int unreadCounter = 0;
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot msgSnapshot : dataSnapshot.getChildren()) {
                        FirebaseMessageModel firebaseMessageModel = msgSnapshot.getValue(FirebaseMessageModel.class);
                        container.setMsgModel(firebaseMessageModel);
                        container.setString(chatKey);
                        if (firebaseMessageModel.getIsReceived() == Constants.MESSAGE_SENT
                                && !firebaseMessageModel.getSenderName().equals(username)) {
                            unreadCounter++;
                        }
                        listener.onChange("getMessageEventListener", CONDITION_1, container);
                    }
                    if (container.getMsgModel().getIsReceived() == Constants.MESSAGE_SENT && unreadCounter > 0) {
                        container.setInt(unreadCounter);
                    }
                    listener.onCompleteTask("getMessageEventListener", CONDITION_1, container);
                } else {
                    container.setString(message);
                    listener.onCompleteTask("getMessageEventListener", CONDITION_2, container);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getMessageEventListener", databaseError);
            }
        };
    }

    public void toggleLastMsgSingleEventListener(String node, String chatKey, ValueEventListener valueEventListener){
        DatabaseReference messagesRef = database.getReference("messages");
        Query pendingQuery = messagesRef.child(node).child(chatKey).limitToLast(1);
        if(valueEventListener != null) {
            pendingQuery.addListenerForSingleValueEvent(valueEventListener);
        }

    }

    public void toggleLastMsgEventListener(String node, String chatKey, ValueEventListener valueEventListener, boolean add){
        DatabaseReference messagesRef = database.getReference("messages");
        Query pendingQuery = messagesRef.child(node).child(chatKey).limitToLast(1);
        if(valueEventListener != null) {
            if (add)
                pendingQuery.addValueEventListener(valueEventListener);
            else
                pendingQuery.removeEventListener(valueEventListener);
        }

    }

    public void toggleListenerFor(String reference, String child, String target, ValueEventListener valueEventListener, boolean add, boolean single){
        DatabaseReference databaseReference = database.getReference(reference);
        if(valueEventListener != null) {
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
    }

    public ValueEventListener getValueEventListener(final String target, final int loopCond, final int notExistCond, final int completeCond ,final Class obj){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Container container = new Container();
                container.setString(target);
                if(dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        container.setObject(userSnapshot.getValue(obj));
                        listener.onChange("getValueEventListener", loopCond, container);
                    }
                    listener.onCompleteTask("getValueEventListener", completeCond, container);
                } else {
                    listener.onCompleteTask("getValueEventListener", notExistCond, container);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getValueEventListener", databaseError);
            }
        };
    }

    public void updateChatKeys(final User user, final String keyToRemove, final Chat chat, final boolean isGroup){
        DatabaseReference pendingTasks = database.getReference("users").orderByChild("username").equalTo(user.name).getRef();
        pendingTasks.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for(MutableData data : mutableData.getChildren()){
                    FirebaseUserModel firebaseUserModel = data.getValue(FirebaseUserModel.class);
                    if(firebaseUserModel == null)
                        return Transaction.success(mutableData);

                    if(firebaseUserModel.getUsername().equals(user.name)){
                        String [] keys = (isGroup) ? firebaseUserModel.getGroupKeys().split(",") : firebaseUserModel.getChatKeys().split(",");
                        String newKeys = "";
                        for (String key : keys) {
                            if (!key.equals(keyToRemove)) {
                                newKeys += (newKeys.equals("")) ? key : "," + key;
                            }
                        }
                        if (!isGroup) {
                            firebaseUserModel.setChatKeys(newKeys);
                        } else {
                            firebaseUserModel.setGroupKeys(newKeys);
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

    public void collectAllImagesForDeletionThenDeleteRelatedMessages(final String node, final FirebaseGroupModel firebaseGroupModel){
        final DatabaseReference reference = database.getReference("messages").child(node).child(firebaseGroupModel.getGroupKey());
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
                if (firebaseGroupModel.getPic() != null && !firebaseGroupModel.getPic().isEmpty())
                    imageUri.add(firebaseGroupModel.getPic());
                Container c = new Container();
                c.setString(firebaseGroupModel.getGroupKey());
                c.setStringList(imageUri);
                listener.onCompleteTask("collectAllImagesForDeletionThenDeleteRelatedMessages", CONDITION_1, c);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                cleanDeleteAllMessages(node, new String[]{firebaseGroupModel.getGroupKey()});
            }
        });
    }

    public void collectAllImagesForDeletionThenDeleteAllRelatedMessages(final String node, final List<String> keys, final List<String> picUrls){
        final DatabaseReference reference = database.getReference("messages");
        Query pendingQuery = reference.child(node).orderByKey().startAt(keys.get(0)).endAt(keys.get(keys.size()-1));

        pendingQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final List<String> imageUri = new ArrayList<>();
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    for(DataSnapshot snapshot : data.getChildren()) {
                        FirebaseMessageModel model = snapshot.getValue(FirebaseMessageModel.class);
                        if(model.getMediaType().equals(Constants.MESSAGE_TYPE_PIC))
                            imageUri.add(model.getText());
                    }
                }
                if(picUrls != null) {
                    for (String url : picUrls) {
                        imageUri.add(url);
                    }
                }
                Container c = new Container();
                Container innerContainer = new Container();
                innerContainer.setStringList(keys);
                c.setStringList(imageUri);
                c.setContainer(innerContainer);
                c.setString(node);
                listener.onCompleteTask("collectAllImagesForDeletionThenDeleteAllRelatedMessages", CONDITION_1, c);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Container container = new Container();
                container.setStringList(keys);
                container.setString(databaseError.getMessage());
                listener.onCompleteTask("collectAllImagesForDeletionThenDeleteAlleRelatedMessages", CONDITION_2, container);
            }
        });
    }

    public void cleanDeleteAllMessages(String node, String[] keys){
        for(String key: keys){
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

    public void updateFirebaseMessageStatus (final String chatKey, final List<String> messages) {
        final Query messagesRef = database.getReference("messages").child("single").child(chatKey).orderByKey().startAt(messages.get(0)).endAt(messages.get(messages.size()-1));
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (messages.contains(snapshot.getKey()))
                            database.getReference("messages").child("single").child(chatKey).child(snapshot.getKey()).child("isReceived").setValue(Constants.MESSAGE_RECEIVED);
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
                    (grpTitle.isEmpty())? Network.makeNewMessageNode(msgType,wishMessage, friend) : Network.makeNewGroupMessageModel(chatKey, wishMessage, msgType),
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
            if (!key.equals(searchDup) && !key.equals(keyToAdd)) {
                newKeys += "," + key;
            }
        }

        return newKeys;
    }

    public void createGroup(final FirebaseGroupModel firebaseGroupModel, final JSONArray allMembersDeviceToken){
        DatabaseReference newRef = database.getReference("groups").push();
        newRef.setValue(firebaseGroupModel, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if(databaseError != null){
                    listener.onFailureTask("createGroup", databaseError);
                } else {
                    Container container = new Container();
                    container.setString(firebaseGroupModel.getTitle()+","+firebaseGroupModel.getGroupKey());
                    container.setJsonArray(allMembersDeviceToken);
                    listener.onCompleteTask("createGroup", CONDITION_1, container);
                }
            }
        });
    }

    public void getDeviceTokensFor(final List<String> allMembers, final String title, final String uniqueId, final boolean noBlockList){
        Collections.sort(allMembers);
        DatabaseReference reference = database.getReference("users");
        final List<String> addedMembers = new ArrayList<>();
        final User user = User.getInstance();
        reference.orderByChild("username").startAt(allMembers.get(0)).endAt(allMembers.get(allMembers.size()-1)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    JSONArray membersDeviceTokens = new JSONArray();
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        // or condition if either true enter if statement (creator activity - false || ?), (chat activity - true - ?)
                        if (noBlockList || !Network.isBlockListed(user.name, firebaseUserModel.getBlockedUsers())) {
                            if(allMembers.contains(firebaseUserModel.getUsername())){
                                membersDeviceTokens.put(firebaseUserModel.getDeviceToken());
                                addedMembers.add(firebaseUserModel.getUsername());
                            }
                        }
                    }
                    Container container = new Container();
                    container.setJsonArray(membersDeviceTokens);
                    container.setString(title+","+uniqueId);
                    container.setStringList(addedMembers);
                    listener.onCompleteTask("getDeviceTokensFor", CONDITION_1, container);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getDeviceTokensFor", databaseError);
            }
        });
    }

    public void updateGroupKeyForMembers(final List<String> allMembers, final String uniqueID, final int condition){
        Collections.sort(allMembers);
        DatabaseReference reference = database.getReference("users");
        if(allMembers.size() > 1)
            reference = reference.orderByChild("username").startAt(allMembers.get(0)).endAt(allMembers.get(allMembers.size()-1)).getRef();
        else
            reference = reference.orderByChild("username").equalTo(allMembers.get(0)).getRef();

        reference.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for(MutableData data : mutableData.getChildren()){
                    FirebaseUserModel firebaseUserModel = data.getValue(FirebaseUserModel.class);
                    if(firebaseUserModel == null)
                        return Transaction.success(mutableData);

                    if(allMembers.contains(firebaseUserModel.getUsername())){
                        if(firebaseUserModel.getGroupKeys().equals(""))
                            firebaseUserModel.setGroupKeys(uniqueID);
                        else {
                            List<String> allGroupKeys = Arrays.asList(firebaseUserModel.getGroupKeys().split(","));
                            if (!allGroupKeys.contains(uniqueID)) {
                                firebaseUserModel.setGroupKeys(firebaseUserModel.getGroupKeys() + "," + uniqueID);
                            }
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
                    container.setStringList(allMembers);
                    listener.onCompleteTask("updateGroupKeyForMembers", condition, container);
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

    public void updateGroupInfo(final FirebaseGroupModel group){
        DatabaseReference groupsRef = database.getReference("groups");
        groupsRef.orderByChild("groupKey").equalTo(group.getGroupKey()).getRef().runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseGroupModel groupModel = data.getValue(FirebaseGroupModel.class);
                    if (groupModel == null) return Transaction.success(mutableData);
                    if(groupModel.getGroupKey().equals(group.getGroupKey())){
                        groupModel = group;
                        data.setValue(groupModel);
                    }
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    listener.onCompleteTask("updateGroupInfo", CONDITION_1, null);
                } else {
                    listener.onFailureTask("updateGroupInfo", databaseError);
                }
            }
        });
    }

    public void exitGroup (final Chat chat, final String leavingUser, final boolean admin, final List<String> groupsToExit) {
        DatabaseReference reference = database.getReference("groups");
        Collections.sort(groupsToExit);
        reference.orderByChild("groupKey").startAt(groupsToExit.get(0)).endAt(groupsToExit.get(groupsToExit.size()-1)).getRef().runTransaction(new Transaction.Handler() {
            List<FirebaseGroupModel> groupsToDelete = new ArrayList<FirebaseGroupModel>();
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseGroupModel groupModel = data.getValue(FirebaseGroupModel.class);

                    if (groupModel == null) return Transaction.success(mutableData);
                    if (groupsToExit.contains(groupModel.getGroupKey())) {
                        String newMembers = groupModel.getMembers();
                        if (admin) {
                            groupModel.setAdmin("");
                        } else {
                            if(chat == null){
                                if(groupModel.getAdmin().equals(leavingUser))
                                    groupModel.setAdmin("");
                            }


                            String[] members = groupModel.getMembers().split(",");
                            newMembers = "";
                            for (String member : members) {
                                if (!member.equals(leavingUser)) {
                                    newMembers += (newMembers.equals("")) ? member : "," + member;
                                }
                            }
                        }

                        groupModel.setMembers(newMembers);
                        if(groupModel.getMembers().isEmpty() && groupModel.getAdmin().isEmpty())
                            groupsToDelete.add(groupModel);
                        data.setValue(groupModel);
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Container container = new Container();
                if(chat != null)
                    container.setChat(chat); // key to remove
                else {
                    container.setGroups(groupsToDelete);//groups to delete completely
                }
                if (databaseError == null) {
                    listener.onCompleteTask("exitGroup", CONDITION_1, container);
                } else {
                    listener.onCompleteTask("exitGroup", CONDITION_2, container);
                }
            }
        });
    }

    public void removeFromGroup(final String groupKey, final String username){
        DatabaseReference groupRef = database.getReference("groups").orderByChild("groupKey").equalTo(groupKey).getRef();
        groupRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseGroupModel groupModel = data.getValue(FirebaseGroupModel.class);

                    if (groupModel == null) return Transaction.success(mutableData);

                    if (groupModel.getGroupKey().equals(groupKey)) {
                        String[] members = groupModel.getMembers().split(",");
                        String newMembers = "";
                        for (String member : members) {
                            if (!member.equals(username)) {
                                newMembers += (newMembers.equals("")) ? member : "," + member;
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
                Container container = new Container();
                List<String> data = new ArrayList<String>();
                data.add(groupKey);
                data.add(username);
                container.setStringList(data);
                if (databaseError == null) {
                    listener.onCompleteTask("removeFromGroup", CONDITION_1, container);
                } else {
                    listener.onFailureTask("removeFromGroup", databaseError);
                }
            }
        });
    }

    public void deleteGroup (final Chat chat) {
        DatabaseReference groupRef = database.getReference("groups").orderByChild("groupKey").equalTo(chat.getChatKey()).getRef();
        groupRef.runTransaction(new Transaction.Handler() {
            boolean isDeleted = false;
            private FirebaseGroupModel groupRemoved;
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseGroupModel groupModel = data.getValue(FirebaseGroupModel.class);

                    if (groupModel == null) return Transaction.success(mutableData);

                    if (groupModel.getGroupKey().equals(chat.getChatKey())) {
                        groupRemoved = new FirebaseGroupModel();
                        groupRemoved.setTitle(groupModel.getTitle());
                        groupRemoved.setGroupKey(chat.getChatKey());
                        groupRemoved.setPic(groupModel.getPic());
                        groupRemoved.setAdmin(chat.getAdmin());
                        if (groupModel.getAdmin().equals("") && groupModel.getMembers().equals("")) { // no admin and members left
                            isDeleted = true;
                            groupModel = null;
                            data.setValue(groupModel);
                            break;
                        }
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    Container container = new Container();
                    container.setGroupModel(groupRemoved);
                    if (isDeleted) {
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

    public void deleteGroups(final List<FirebaseGroupModel> groups){
        if(groups.size() > 0) {
            Collections.sort(groups, FirebaseGroupModel.groupKeyComparator);
            DatabaseReference groupRef = database.getReference("groups").orderByChild("groupKey")
                    .startAt(groups.get(0).getGroupKey())
                    .endAt(groups.get(groups.size() - 1).getGroupKey()).getRef();
            groupRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    for (MutableData data : mutableData.getChildren()) {
                        FirebaseGroupModel groupModel = data.getValue(FirebaseGroupModel.class);

                        if (groupModel == null) return Transaction.success(mutableData);
                        int pos = containsGroup(groups, groupModel);
                        if (pos >= 0) {
                            //groups.remove(pos);

                            groupModel = null;
                            data.setValue(groupModel);
                        }

                        //if(groups.size() == 0)break;

                    }

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    if (databaseError == null) {
                        Container container = new Container();
                        container.setGroups(groups);
                        listener.onCompleteTask("deleteGroups", CONDITION_1, container);
                    } else {
                        listener.onFailureTask("deleteGroups", databaseError);
                    }
                }
            });
        } else {
            listener.onCompleteTask("deleteGroups", CONDITION_2, null);
        }
    }

    public void accumulateAllChatsForDeletion(final List<String> allUsersInChatWith, final String username){
        if(allUsersInChatWith.size() > 0) {
            Collections.sort(allUsersInChatWith);
            Query query = database.getReference("users").orderByChild("username")
                    .startAt(allUsersInChatWith.get(0))
                    .endAt(allUsersInChatWith.get(allUsersInChatWith.size() - 1));
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                List<String> chatsToDeleteWith = new ArrayList<String>();

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            FirebaseUserModel user = snapshot.getValue(FirebaseUserModel.class);
                            if(allUsersInChatWith.contains(user.getUsername())){
                                if (!user.getChatKeys().isEmpty()) {
                                    boolean keepChatWith = false;
                                    for (String key : user.getChatKeys().split(",")) {
                                        String p1 = key.split("-")[0];
                                        String p2 = key.split("-")[1];
                                        if(p1.equals(username) || p2.equals(username))
                                            keepChatWith=true;
                                    }
                                    if(!keepChatWith && !chatsToDeleteWith.contains(user.getUsername())) {
                                        System.out.println("Adding name final: "+user.getUsername());
                                        chatsToDeleteWith.add(user.getUsername());
                                    }
                                } else {
                                    System.out.println("Adding name final: "+user.getUsername());
                                    chatsToDeleteWith.add(user.getUsername());
                                }
                            }
                        }
                        Container container = new Container();
                        container.setStringList(chatsToDeleteWith);
                        listener.onCompleteTask("accumulateAllChatsForDeletion", CONDITION_1, container);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    listener.onFailureTask("accumulateAllChatsForDeletion", databaseError);
                }
            });
        } else {
            listener.onCompleteTask("accumulateAllChatsForDeletion", CONDITION_2, null);
        }

    }

    private int containsGroup(List<FirebaseGroupModel> list, FirebaseGroupModel searchItem){
        int i = 0;
        for(FirebaseGroupModel item : list){
            if(item.getGroupKey().equals(searchItem.getGroupKey()))
                return i;
            i++;
        }
        return -1;
    }

    public ValueEventListener getGroupInfo(final String groupKey){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        FirebaseGroupModel groupModel = postSnapshot.getValue(FirebaseGroupModel.class);
                        if (groupModel.getGroupKey().equals(groupKey)) {
                            Container container = new Container();
                            container.setGroupModel(groupModel);
                            listener.onCompleteTask("getGroupInfo", CONDITION_1, container);
                            break;
                        }
                    }
                } else {
                    listener.onCompleteTask("getGroupInfo", CONDITION_2, null);
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("getGroupInfo", databaseError);
            }
        };
    }

    public void updateGroupMembers (final String singleUser, final List<String> usernames, final String chatKey, final boolean admin) {
        DatabaseReference groupRef = database.getReference("groups").orderByChild("groupKey").equalTo(chatKey).getRef();
        groupRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseGroupModel groupModel = data.getValue(FirebaseGroupModel.class);

                    if (groupModel == null) return Transaction.success(mutableData);

                    if (groupModel.getGroupKey().equals(chatKey)) {
                        String updatedMembers = groupModel.getMembers();
                        if (admin) {
                            if (groupModel.getAdmin().equals("")) {
                                groupModel.setAdmin(singleUser);
                                String [] allMembers = groupModel.getMembers().split(",");
                                updatedMembers = "";
                                for (String member : allMembers) {
                                    if (!member.equals(singleUser)) {
                                        updatedMembers += (updatedMembers.equals("")) ? member : "," + member;
                                    }
                                }
                            }
                        } else {
                            updatedMembers += (updatedMembers.equals("")) ? singleUser : "," + singleUser;
                        }

                        groupModel.setMembers(updatedMembers);
                        data.setValue(groupModel);
                        break;
                    }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    Container container = new Container();
                    container.setStringList(usernames);
                    container.setString(singleUser);
                    container.setBoolean(admin);
                    listener.onCompleteTask("updateGroupMembers", CONDITION_1, container);
                } else {
                    listener.onFailureTask("updateGroupMembers", databaseError);
                }
            }
        });
    }

    public void allUnblockedMembers (final List<String> membersToAdd) {
        final User user = User.getInstance();
        Collections.sort(membersToAdd);
        DatabaseReference userRef = database.getReference("users");
        if (membersToAdd.size() > 1) {
            userRef.orderByChild("username").startAt(membersToAdd.get(0)).endAt(membersToAdd.get(membersToAdd.size()-1)).getRef();
        } else {
            userRef.orderByChild("username").equalTo(membersToAdd.get(0)).getRef();
        }
        final List<String> addableMembers = new ArrayList<>();
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapShot : dataSnapshot.getChildren()) {
                        FirebaseUserModel model = snapShot.getValue(FirebaseUserModel.class);
                        if (membersToAdd.contains(model.getUsername())) {
                            if (!Network.isBlockListed(user.name, model.getBlockedUsers())) {
                                addableMembers.add(model.getUsername());
                            }
                        }
                    }
                    Container container = new Container();
                    container.setStringList(addableMembers);
                    listener.onCompleteTask("allUnblockedMembers", CONDITION_1, container);
                } else {
                    listener.onCompleteTask("allUnblockedMembers", CONDITION_2, null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask("allUnblockedMembers", databaseError);
            }
        });
    }

    public void updateBlockList (final String [] users, final boolean add) {
        final User user = User.getInstance();
        DatabaseReference userRef = database.getReference("users").orderByChild("username").equalTo(user.name).getRef();
        userRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseUserModel fModel = data.getValue(FirebaseUserModel.class);

                    if (fModel == null) return Transaction.success(mutableData);

                    if (fModel.getUsername().equals(user.name)) {
                        JSONArray jsonArray;
                        try {
                            if (!fModel.getBlockedUsers().trim().equals("")) {
                                jsonArray = new JSONArray(fModel.getBlockedUsers());
                                if (!add) {
                                    JSONArray newBlockList = new JSONArray();
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        boolean found = false;
                                        for (String user : users) {
                                            if (jsonArray.getString(i).equals(user)) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            newBlockList.put(jsonArray.getString(i));
                                        }
                                    }
                                    jsonArray = newBlockList;
                                }
                            } else {
                                jsonArray = new JSONArray();
                            }

                            if (add) {
                                if (jsonArray.length() == 0) {
                                    jsonArray.put(users[0]);
                                } else {
                                    boolean found = false;
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        if (jsonArray.getString(i).equals(users[0])) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) jsonArray.put(users[0]);
                                }
                            }
                            fModel.setBlockedUsers(jsonArray.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        data.setValue(fModel);
                        break;
                    }
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    listener.onCompleteTask("updateBlockList", CONDITION_1, null);
                } else {
                    listener.onFailureTask("updateBlockList", databaseError);
                }
            }
        });
    }

    public void deleteUserFromDatabase(final String username){
        DatabaseReference databaseReference = database.getReference("users").orderByChild("username").equalTo(username).getRef();
        databaseReference.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                   FirebaseUserModel firebaseUserModel = data.getValue(FirebaseUserModel.class);
                   if(firebaseUserModel.getUsername().equals(username)) {
                       firebaseUserModel = null;
                       data.setValue(firebaseUserModel);
                   }

                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError == null) {
                    listener.onCompleteTask("deleteUserFromDatabase", CONDITION_1, null);
                } else {
                    listener.onFailureTask("deleteUserFromDatabase", databaseError);
                }
            }
        });
    }

}

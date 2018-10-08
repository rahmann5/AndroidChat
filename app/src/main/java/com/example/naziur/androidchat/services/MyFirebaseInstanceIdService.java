package com.example.naziur.androidchat.services;

import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceIdService;

import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;


/**
 * Created by Naziur on 27/08/2018.
 */

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = "MyFirebaseIIDService";
    private FirebaseHelper firebaseHelper;
    User user = com.example.naziur.androidchat.models.User.getInstance();

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if (refreshedToken != null) {
            Log.d(TAG, "Refreshed token: " + refreshedToken);

            // If you want to send messages to this application instance or
            // manage this apps subscriptions on the server side, send the
            // Instance ID token to your app server.
            sendTokenToServer(refreshedToken);
        }
    }

    public void sendTokenToServer(final String strToken) {
        // API call to send token to Server
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        ValueEventListener listener = firebaseHelper.getValueEventListener(strToken, FirebaseHelper.CONDITION_1, FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION, FirebaseUserModel.class);
        firebaseHelper.toggleListenerFor("users", "username", user.name, listener, true, true);
    }


    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("updateUserDeviceToken")){
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    Log.i(TAG, tag + ": Successfully updated new device token " + container.getString());
                    break;

                case FirebaseHelper.CONDITION_2 :
                    Log.i(TAG, tag + ": Failed to update new device token " + container.getString());
                    break;
            }
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        Log.i(TAG, tag + ": " + databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        if (tag.equals("getValueEventListener")){
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    FirebaseUserModel firebaseUserModel = (FirebaseUserModel) container.getObject();
                    String strToken = container.getString();
                    if (strToken != null && firebaseUserModel.getDeviceId().equals(user.deviceId) && !strToken.equals(firebaseUserModel.getDeviceToken())) {
                        firebaseHelper.updateUserDeviceToken(strToken);
                    }
                    break;
            }
        }
    }
}

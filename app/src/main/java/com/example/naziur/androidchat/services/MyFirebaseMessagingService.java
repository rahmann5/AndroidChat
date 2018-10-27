package com.example.naziur.androidchat.services;

import android.content.Intent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;

/**
 * Created by Naziur on 27/08/2018.
 *
 * Firebase service that listens for broadcast sent from firebase and then fires an ordered broadcast:
 * this will either fire a push notification or cancel the push notification due to user being in the
 * chat page already hence not requiring to receive a notification.
 *
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        //Calling method to show notification
        Intent intent = new Intent();
        //If the below action is caught by the chat activities the push notification is aborted else it is fired
        intent.setAction("my.custom.action");
        intent.putExtra("body",  remoteMessage.getNotification().getBody());
        intent.putExtra("tag",  remoteMessage.getNotification().getTag());
        intent.putExtra("title",  remoteMessage.getNotification().getTitle());
        Map<String, String> map = remoteMessage.getData();
        JSONObject object = new JSONObject(map);
        try {
            if(object.has("chatKey")) {
                intent.putExtra("payLoad", object.getString("chatKey"));
                intent.putExtra("key", "chatKey");
            }else if(object.has("group_uid")) {
                intent.putExtra("payLoad", object.getString("group_uid"));
                intent.putExtra("key", "group_uid");
            }else if(object.has("notification")) {
                intent.putExtra("payLoad", object.getString("notification"));
                intent.putExtra("key", "notification");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendOrderedBroadcast(intent,null);

    }

}

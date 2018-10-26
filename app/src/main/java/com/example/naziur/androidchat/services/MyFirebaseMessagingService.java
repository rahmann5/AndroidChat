package com.example.naziur.androidchat.services;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.naziur.androidchat.activities.ChatActivity;
import com.example.naziur.androidchat.activities.MainActivity;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.utils.Network;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Naziur on 27/08/2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        //Calling method to show notification
        Intent intent = new Intent();
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

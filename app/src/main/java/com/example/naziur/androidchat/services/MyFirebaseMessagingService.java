package com.example.naziur.androidchat.services;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.naziur.androidchat.activities.MainActivity;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Network;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Naziur on 27/08/2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    private static final int NOTIFICATION_ID = 100;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());

        //Calling method to show notification
        if (!Network.isForeground(getApplicationContext())){
            String type = Constants.MESSAGE_TYPE_TEXT;
            try {
                JSONObject objType = new JSONObject(remoteMessage.getData());
                type = objType.getString("type");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            showNotification(remoteMessage.getNotification().getBody(),
                    remoteMessage.getNotification().getTitle(),
                    type,
                    remoteMessage.getNotification().getTag());
        }

    }

    private void showNotification(String messageBody, String to, String type ,String dToken) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("sender", to);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.ic_launcher_round))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getContent(type, messageBody))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        mNotificationManager.notify(dToken, NOTIFICATION_ID, notificationBuilder.build());
    }

    private String getContent (String type, String body) {
        switch (type) {
            case Constants.MESSAGE_TYPE_TEXT :
                return body;

            case Constants.MESSAGE_TYPE_PIC :
                return "Picture";

            default: return body;

        }
    }

}

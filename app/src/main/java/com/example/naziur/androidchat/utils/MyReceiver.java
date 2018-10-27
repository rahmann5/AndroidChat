package com.example.naziur.androidchat.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.activities.MainActivity;


/**
 * Created by Naziur on 08/10/2018.
 *
 * if the ordered intent (MyFirebaseMessagingService) isn't aborted it will reach here and fire a push notification
 */

public class MyReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 100;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extra = intent.getExtras();
        if (extra != null) {
            showNotification(context, extra.getString("body"),
                    extra.getString("tag"),extra.getString("title"),extra.getString("payLoad"),extra.getString("key"));
        }
    }

    private void showNotification(Context context, String messageBody,String dToken, String title, String payLoadData, String payloadKey) {

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(payloadKey,payLoadData);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(context.getApplicationContext().getResources(),
                        R.mipmap.ic_launcher_round))
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);


        //dToken to ensure same chat doesn't create different notification each time
        mNotificationManager.notify(dToken, NOTIFICATION_ID, notificationBuilder.build());
    }

}

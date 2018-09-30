package com.example.naziur.androidchat.utils;

import android.content.Context;

import com.example.naziur.androidchat.R;

/**
 * Created by Naziur on 08/09/2018.
 */

public class Constants {

    public static final String NOTIFICATION_URL = "https://fcm.googleapis.com/fcm/send";

    public static final int MESSAGE_SENT = 0;
    public static final int MESSAGE_RECEIVED = 1;
    public static final int MESSAGE_ERROR = 2;

    public static final String MESSAGE_TYPE_TEXT = "TEXT";
    public static final String MESSAGE_TYPE_PIC = "PIC";
    public static final String MESSAGE_TYPE_SYSTEM = "SYSTEM";
    public static final String ACTION_SEND = "Send";
    public static final String ACTION_DOWNLOAD = "Download";


    public static String generateMediaText (Context c, String type, String wishMessage) {
        switch (type) {
            case Constants.MESSAGE_TYPE_SYSTEM :
            case Constants.MESSAGE_TYPE_TEXT :
                return wishMessage;

            case Constants.MESSAGE_TYPE_PIC :
                return c.getResources().getString(R.string.media_message);

            default: return Constants.MESSAGE_TYPE_TEXT;
        }
    }
}

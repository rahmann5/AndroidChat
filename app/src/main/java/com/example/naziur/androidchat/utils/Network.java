package com.example.naziur.androidchat.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.widget.Toast;

import com.example.naziur.androidchat.models.Contact;

/**
 * Created by Hamidur on 07/09/2018.
 */

public class Network {

    public static boolean isInternetAvailable(Context c, boolean showMsg) {
        ConnectivityManager conMgr = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        // ARE WE CONNECTED TO THE NET
        if (conMgr.getActiveNetworkInfo() != null
                && conMgr.getActiveNetworkInfo().isAvailable()
                && conMgr.getActiveNetworkInfo().isConnected()) {

            return true;
        }
        if (showMsg) Toast.makeText(c, "No Internet Available", Toast.LENGTH_SHORT).show();
        return false;
    }

}

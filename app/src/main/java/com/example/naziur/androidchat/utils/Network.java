package com.example.naziur.androidchat.utils;

import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.example.naziur.androidchat.activities.ChatActivity;
import com.example.naziur.androidchat.models.Contact;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.util.List;

import cz.msebera.android.httpclient.HttpHeaders;

/**
 * Created by Hamidur on 07/09/2018.
 */

public class Network {
    private static final String LOG_TAG = "Network";
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE = 2;
    public static final int TYPE_NOT_CONNECTED = 0;
    public static final int NETWORK_STATUS_NOT_CONNECTED = 0;
    public static final int NETWORK_STATUS_WIFI = 1;
    public static final int NETWORK_STATUS_MOBILE = 2;

    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public static int getConnectivityStatusString(Context context) {
        int conn = Network.getConnectivityStatus(context);
        int status = 0;
        if (conn == Network.TYPE_WIFI) {
            status = NETWORK_STATUS_WIFI;
        } else if (conn == Network.TYPE_MOBILE) {
            status = NETWORK_STATUS_MOBILE;
        } else if (conn == Network.TYPE_NOT_CONNECTED) {
            status = NETWORK_STATUS_NOT_CONNECTED;
        }
        return status;
    }

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

    public static boolean isForeground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : tasks) {
            if (ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND == appProcess.importance && packageName.equals(appProcess.processName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInForegroundAndInChatScreen(Context context){
        if(isForeground(context)) {
            System.out.println(context.getClass().getSimpleName() + " is being compared to " + ChatActivity.class.getSimpleName());
            return context.getClass().getSimpleName().equals(ChatActivity.class.getSimpleName());
        } else
            return false;
    }


    public static void downloadImageToPhone (Context context, String downloadUrlOfImage) {
        String DIR_NAME = "Android Chat";
        String filename = "img_"+System.currentTimeMillis()+".jpg";
        
        //external storage availability check
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(context, "External Storage not mounted", Toast.LENGTH_SHORT).show();
            return;
        }

        File direct =
                new File(Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        .getAbsolutePath() + "/" + DIR_NAME + "/");


        if (!direct.exists()) {
            direct.mkdir();
            Log.i(LOG_TAG, "dir created for first time");
        }

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri downloadUri = Uri.parse(downloadUrlOfImage);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(filename)
                .setMimeType("image/jpeg")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES,
                        File.separator + DIR_NAME + File.separator + filename);

        dm.enqueue(request);
    }

    public static AsyncHttpClient createAsyncClient () {

        AsyncHttpClient client = new AsyncHttpClient();

        //client.addHeader(HttpHeaders.AUTHORIZATION, "key=AIzaSyCl-lEfl7Rx9ZcDEyXX4sSpXhJYMS6PHfk");
        client.addHeader(HttpHeaders.AUTHORIZATION, "key=AAAAQmgvFoU:APA91bF8shJboV6QDRVUvy-8ZKhZ6c1eri8a6zlkSPLDosvPZ-MegfsPEOGeKUhoxmtMq3d11bzeOEWWIupjCuKW3rgbwmqZ8LqumrK_ldWYT_ipDExdy4J2OWnhYwvb9Y6pIx8vOWD8");
        client.addHeader(HttpHeaders.CONTENT_TYPE, RequestParams.APPLICATION_JSON);
        return client;
    }
}

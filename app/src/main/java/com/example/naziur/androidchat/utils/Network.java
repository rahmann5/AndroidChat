package com.example.naziur.androidchat.utils;

import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.example.naziur.androidchat.activities.ChatActivity;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.entity.StringEntity;

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

    public static void deleteUploadImages (final FirebaseHelper firebaseHelper, final List<String> allUris, final String[] chatKeys, final String loc) {
        if (!allUris.isEmpty()) {
            String uri = allUris.remove(0);
            StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(uri);
            photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    deleteUploadImages(firebaseHelper , allUris, chatKeys, loc);
                    Log.i(LOG_TAG, "onSuccess: removed image from failed database update");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.i(LOG_TAG, "onFailure: did not delete file in storage");
                    // store that image uri in a log to remove manually
                    e.printStackTrace();
                }
            });
        } else {
            if(!loc.equals("profile"))
                firebaseHelper.cleanDeleteAllMessages(loc, chatKeys);
            else
                firebaseHelper.deleteUserFromDatabase(chatKeys[0]);
        }

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

    public static void removeFailedImageUpload (String uri, final Context context) {
        StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(uri);
        photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(LOG_TAG, "onSuccess: removed image from failed database update");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Error Removing old picture", Toast.LENGTH_SHORT).show();
                Log.i(LOG_TAG, "onFailure: did not delete file in storage");
                e.printStackTrace();
            }
        });
    }

    public static AsyncHttpClient createAsyncClient () {

        AsyncHttpClient client = new AsyncHttpClient();

        //client.addHeader(HttpHeaders.AUTHORIZATION, "key=AIzaSyCl-lEfl7Rx9ZcDEyXX4sSpXhJYMS6PHfk");
        client.addHeader(HttpHeaders.AUTHORIZATION, "key=AAAAQmgvFoU:APA91bF8shJboV6QDRVUvy-8ZKhZ6c1eri8a6zlkSPLDosvPZ-MegfsPEOGeKUhoxmtMq3d11bzeOEWWIupjCuKW3rgbwmqZ8LqumrK_ldWYT_ipDExdy4J2OWnhYwvb9Y6pIx8vOWD8");
        client.addHeader(HttpHeaders.CONTENT_TYPE, RequestParams.APPLICATION_JSON);
        return client;
    }

    public static StringEntity generateSingleMsgEntity(Context c, String type, String wishMessage, FirebaseUserModel friend, String chatKey) {
        User user = User.getInstance();
        JSONObject params = new JSONObject();
        //params.put("registration_ids", registration_ids);
        StringEntity entity = null;
        try {
            params.put("to", friend.getDeviceToken());
            JSONObject payload = new JSONObject();
            payload.put("chatKey", chatKey); // used for extra intent in main activity
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("click_action", ".MainActivity");
            notificationObject.put("body", Constants.generateMediaText(c, type, wishMessage));
            notificationObject.put("title", user.profileName);
            notificationObject.put("tag", user.deviceId);
            params.put("data", payload);
            params.put("notification", notificationObject);

            entity = new StringEntity(params.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return entity;
    }

    public static StringEntity generateGroupMsgEntity (Context c, String type, JSONArray membersDeviceTokens, String title, String uniqueId, String wishMessage) {
        JSONObject params = new JSONObject();
        // params.put("to", c.getContact().getDeviceToken());
        StringEntity entity = null;

        try {

            params.put("registration_ids", membersDeviceTokens);
            JSONObject payload = new JSONObject();
            payload.put("group_uid", uniqueId); // used for extra intent in main activity
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("click_action", ".MainActivity");
            notificationObject.put("body", Constants.generateMediaText(c, type, wishMessage));
            notificationObject.put("title", title);
            notificationObject.put("tag", uniqueId);
            params.put("data", payload);
            params.put("notification", notificationObject);

            entity = new StringEntity(params.toString());
        }catch (Exception e) {
            e.printStackTrace();
        }

        return entity;
    }

    public static FirebaseMessageModel makeNewMessageNode (String type, String wishMessage, FirebaseUserModel friend) {
        User user = User.getInstance();
        final FirebaseMessageModel firebaseMessageModel = new FirebaseMessageModel();
        firebaseMessageModel.setText(wishMessage);
        firebaseMessageModel.setSenderDeviceId(user.deviceId);
        firebaseMessageModel.setSenderName(user.name);
        firebaseMessageModel.setReceiverName(friend.getUsername());
        firebaseMessageModel.setIsReceived(Constants.MESSAGE_SENT);
        firebaseMessageModel.setMediaType(type);
        return  firebaseMessageModel;
    }

    public static FirebaseMessageModel makeNewGroupMessageModel(String uniqueId, String text, String type){
        User user = User.getInstance();
        FirebaseMessageModel firebaseMessageModel = new FirebaseMessageModel();
        firebaseMessageModel.setCreatedDate(System.currentTimeMillis());
        firebaseMessageModel.setMediaType(type);
        firebaseMessageModel.setIsReceived(0);
        firebaseMessageModel.setSenderDeviceId(user.deviceId);
        firebaseMessageModel.setSenderName(user.name);
        firebaseMessageModel.setId(uniqueId);
        firebaseMessageModel.setText(text);
        return firebaseMessageModel;
    }

    public static String getMembersText (Context context, String[] members, String admin, String currentUser) {
        ContactDBHelper db = new ContactDBHelper(context);
        String newMembersList = "You";
        if (!admin.equals(currentUser) && !admin.equals("")) {
            newMembersList += ", "+db.getProfileInfoIfExists(admin)[0];
        }
        for (String m : members) {
            if (!m.equals(currentUser) && !m.equals("")) {
                newMembersList += ", " +db.getProfileInfoIfExists(m)[0];
            }
        }
        db.close();
        return newMembersList;
    }

    public static boolean isBlockListed (String username, String blockList) {
        if (!blockList.equals("")) {
            try {
                JSONArray jsonArray = new JSONArray(blockList);
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (jsonArray.getString(i).equals(username)) {
                        return true;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static String escapeJavaString(String st){
        StringBuilder builder = new StringBuilder();
        try {
            for (int i = 0; i < st.length(); i++) {
                char c = st.charAt(i);
                if(!Character.isLetterOrDigit(c) && !Character.isSpaceChar(c)&& !Character.isWhitespace(c) ){
                    String unicode = String.valueOf(c);
                    int code = (int)c;
                    if(!(code >= 0 && code <= 255)){
                        unicode = "\\u"+Integer.toHexString(c);
                    }
                    builder.append(unicode);
                }
                else{
                    builder.append(c);
                }
            }
            Log.i("Unicode Block", builder.toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static String getUniCode(String st){
        StringBuilder builder = new StringBuilder();
        try {
            for (int i = 0; i < st.length(); i++) {
                char c = st.charAt(i);
                if(!Character.isLetterOrDigit(c) && !Character.isSpaceChar(c)&& !Character.isWhitespace(c) ){
                    String unicode = String.valueOf(c);
                    int code = (int)c;
                    if(!(code >= 0 && code <= 255)){
                        unicode =  Character.toString((char)code);
                    }
                    builder.append(unicode);
                }
                else{
                    builder.append(c);
                }
            }
            Log.i("Unicode Block", builder.toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return builder.toString();
    }

}

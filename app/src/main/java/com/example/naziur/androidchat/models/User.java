package com.example.naziur.androidchat.models;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.naziur.androidchat.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hamidur on 27/08/2018.
 */

public class User {
    private static final User user = new User();

    public static User getInstance() {
        return user;
    }

    public String firebaseKey = "firebaseKey";
    public String name = "Owner";
    public String profileName = "Profile";
    public String deviceId = "";
    public String status = "";
    public String profilePic = "";

    public static final String appPreferences = "ChattingAppPreferences" ;
    public static final String Key  = "keyKey";
    public static final String Name = "nameKey";
    public static final String ProfileName = "profileNameKey";
    public static final String DeviceToken = "deviceTokenKey";
    public static final String DeviceId = "deviceIdKey";

    public SharedPreferences sharedpreferences;

    private User() {
    }

    public Boolean login(FirebaseUserModel firebaseUserModel) {
        name = firebaseUserModel.getUsername();
        profileName = firebaseUserModel.getProfileName();
        deviceId = firebaseUserModel.getDeviceId();
        status = firebaseUserModel.getStatus();
        profilePic = firebaseUserModel.getProfilePic();

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Key, firebaseKey);
        editor.putString(Name, name);
        editor.putString(ProfileName, profileName);
        editor.putString(DeviceId, deviceId);

        editor.apply();

        return true;
    }

    public void saveFirebaseKey(String key) {
        this.firebaseKey = key;

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Key, firebaseKey);
        editor.apply();
    }

    public void logout() {
        firebaseKey = "";
        name = "";
        deviceId = "";
        profileName = "";
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Key, firebaseKey);
        editor.putString(Name, name);
        editor.putString(DeviceId, deviceId);
        editor.putString(ProfileName, profileName);

        editor.apply();
    }

    public void setUserAuthentication (Context context,String email) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(context.getResources().getString(R.string.key_email), email);
        editor.apply();
    }

    public String getUserAuthentication (Context context) {
        return sharedpreferences.getString(context.getResources().getString(R.string.key_email), "");
    }

    public void setAutoLogin(Context context, boolean login) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(context.getResources().getString(R.string.key_auto_login), login);
        editor.apply();
    }

    public boolean getAutoLogin(Context context) {
        return sharedpreferences.getBoolean(context.getResources().getString(R.string.key_auto_login), false);
    }
}

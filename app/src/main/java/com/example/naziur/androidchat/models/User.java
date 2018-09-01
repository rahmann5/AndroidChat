package com.example.naziur.androidchat.models;

import android.content.SharedPreferences;

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
    public String deviceToken = "";

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
        deviceToken = firebaseUserModel.getDeviceToken();

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
        deviceToken = "";

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Key, firebaseKey);
        editor.putString(Name, name);
        editor.putString(DeviceId, deviceId);

        editor.apply();
    }
}

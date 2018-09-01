package com.example.naziur.androidchat.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hamidur on 27/08/2018.
 */

public class FirebaseUserModel {

    String deviceId = "";
    String deviceToken = "";
    String username = "";
    String profileName = "";
    String profilePic = "";
    String chatKeys = "";

    public FirebaseUserModel() {
      /*Blank default constructor essential for Firebase*/
    }

    public String getChatKeys() {
        return chatKeys;
    }

    public void setChatKeys(String chatKeys) {
        this.chatKeys = chatKeys;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

}

package com.example.naziur.androidchat.utils;

import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;

/**
 * Created by Hamidur on 21/09/2018.
 */

public class Container {

    private Contact contact;
    private FirebaseMessageModel msgModel;
    private FirebaseUserModel userModel;
    private String simpleString;

    public Container () {
        // empty
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public FirebaseMessageModel getMsgModel() {
        return msgModel;
    }

    public void setMsgModel(FirebaseMessageModel msgModel) {
        this.msgModel = msgModel;
    }

    public FirebaseUserModel getUserModel() {
        return userModel;
    }

    public void setUserModel(FirebaseUserModel userModel) {
        this.userModel = userModel;
    }

    public String getString() {
        return simpleString;
    }

    public void setString(String simpleString) {
        this.simpleString = simpleString;
    }
}

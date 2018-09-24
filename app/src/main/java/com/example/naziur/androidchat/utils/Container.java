package com.example.naziur.androidchat.utils;

import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by Hamidur on 21/09/2018.
 */

public class Container {

    private Contact contact;
    private FirebaseMessageModel msgModel;
    private FirebaseUserModel userModel;
    private String simpleString;
    private int simpleInt;
    private Chat chat;
    private List<String> stringList;
    private JSONArray jsonArray;

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

    public int getInt() {
        return simpleInt;
    }

    public void setInt(int simpleInt) {
        this.simpleInt = simpleInt;
    }

    public void setChat(Chat chat){
        this.chat = chat;
    }

    public Chat getChat(){
        return chat;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public JSONArray getJsonArray() {
        return jsonArray;
    }

    public void setJsonArray(JSONArray jsonArray) {
        this.jsonArray = jsonArray;
    }
}

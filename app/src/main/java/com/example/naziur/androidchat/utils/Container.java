package com.example.naziur.androidchat.utils;

import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseMessageModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.Notification;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by Hamidur on 21/09/2018.
 */

public class Container {

    private Contact contact;
    private FirebaseMessageModel msgModel;
    private FirebaseUserModel userModel;
    private FirebaseGroupModel groupModel;
    private String simpleString;
    private int simpleInt;
    private boolean simpleBoolean;
    private Chat chat;
    private List<String> stringList;
    private List<Notification> notifications;
    private JSONArray jsonArray;
    private Object object;
    private List<FirebaseGroupModel> groups;
    private List<FirebaseMessageModel> messages;
    private List<Contact> contacts;
    private Container container;

    public Container () {
        // empty
    }

    public List<FirebaseMessageModel> getMessages() {
        return messages;
    }

    public void setMessages(List<FirebaseMessageModel> messages) {
        this.messages = messages;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
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

    public boolean getBoolean() {
        return simpleBoolean;
    }

    public void setBoolean(boolean simpleBoolean) {
        this.simpleBoolean = simpleBoolean;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public FirebaseGroupModel getGroupModel() {
        return groupModel;
    }

    public void setGroupModel(FirebaseGroupModel groupModel) {
        this.groupModel = groupModel;
    }

    public List<FirebaseGroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<FirebaseGroupModel> groups) {
        this.groups = groups;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }
}

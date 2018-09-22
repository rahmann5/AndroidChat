package com.example.naziur.androidchat.utils;

import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseMessageModel;

/**
 * Created by Hamidur on 21/09/2018.
 */

public class Container {

    private Contact contact;
    private FirebaseMessageModel msgModel;
    private Chat chat;

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

    public void setChat(Chat chat){
        this.chat = chat;
    }

    public Chat getChat(){
        return chat;
    }
}

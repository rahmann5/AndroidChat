package com.example.naziur.androidchat.models;

/**
 * Created by Hamidur on 14/09/2018.
 */

public class Notification {

    private String sender;
    private String chatKey;

    public Notification(String sender, String chatKey) {
        this.sender = sender;
        this.chatKey = chatKey;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getChatKey() {
        return chatKey;
    }

    public void setChatKey(String chatKey) {
        this.chatKey = chatKey;
    }
}

package com.example.naziur.androidchat.models;

import com.bumptech.glide.annotation.Excludes;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

/**
 * Created by Hamidur on 14/09/2018.
 */

public class Notification {

    private String sender;
    private String chatKey;
    private Long date;

    public Notification() {

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

    public java.util.Map<String, String> getCreatedDate() {
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public Long getCreatedDateLong() {
        return date;
    }

    public void setCreatedDate(Long createdDate) {
        this.date = createdDate;
    }
}

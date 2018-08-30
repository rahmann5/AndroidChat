package com.example.naziur.androidchat.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

/**
 * Created by Hamidur on 27/08/2018.
 */

public class FirebaseMessageModel {

    private String msgId;
    private String senderDeviceId;
    private String text;
    private Long createdDate;
    private String senderName;
    private String receiverName;
    private String Id;

    public FirebaseMessageModel() {
      /*Blank default constructor essential for Firebase*/
    }

    public String getSenderDeviceId() {
        return senderDeviceId;
    }

    public void setSenderDeviceId(String senderDeviceId) {
        this.senderDeviceId = senderDeviceId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public java.util.Map<String, String> getCreatedDate() {
        return ServerValue.TIMESTAMP;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    @Exclude
    public Long getCreatedDateLong() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

}
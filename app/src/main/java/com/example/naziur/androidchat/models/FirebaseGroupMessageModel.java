package com.example.naziur.androidchat.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

/**
 * Created by Hamidur on 13/09/2018.
 */

public class FirebaseGroupMessageModel {

    private String senderDeviceId;
    private String text;
    private Long createdDate;
    private String senderName;
    private String mediaType;
    private String Id;

    public FirebaseGroupMessageModel() {
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


    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public java.util.Map<String, String> getCreatedDate() {
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public Long getCreatedDateLong() {
        return createdDate;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
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
}

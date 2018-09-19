package com.example.naziur.androidchat.models;

/**
 * Created by Hamidur on 28/08/2018.
 */

public class Contact {

    private FirebaseUserModel contact;
    private String lastMsg;
    private boolean isActive;

    public Contact () {

    }

    public Contact (FirebaseUserModel contact, String lastMsg, boolean isActive) {
        this.contact = contact;
        this.lastMsg = lastMsg;
        this.isActive = isActive;
    }

    public Contact (FirebaseUserModel contact) {
        this.contact = contact;
        this.lastMsg = "";
        isActive = true;
    }

    public FirebaseUserModel getContact() {
        return contact;
    }

    public void setContact(FirebaseUserModel contact) {
        this.contact = contact;
    }

    public String getLastMsg() {
        return lastMsg;
    }

    public void setLastMsg(String lastMsg) {
        this.lastMsg = lastMsg;
    }

    public boolean isActive() {
        return isActive;
    }

}

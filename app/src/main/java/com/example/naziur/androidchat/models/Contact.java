package com.example.naziur.androidchat.models;

/**
 * Created by Hamidur on 28/08/2018.
 */

public class Contact {

    private FirebaseUserModel contact;
    private String lastMsg;

    public Contact () {

    }

    public Contact (FirebaseUserModel contact, String lastMsg) {
        this.contact = contact;
        this.lastMsg = lastMsg;
    }

    public Contact (FirebaseUserModel contact) {
        this.contact = contact;
        this.lastMsg = "";
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
}

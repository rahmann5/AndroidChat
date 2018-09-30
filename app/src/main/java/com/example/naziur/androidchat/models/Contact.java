package com.example.naziur.androidchat.models;

/**
 * Created by Hamidur on 28/08/2018.
 */

public class Contact {

    private FirebaseUserModel contact;
    private boolean isActive;

    public Contact () {

    }

    public Contact(FirebaseUserModel contact, boolean isActive) {
        this.contact = contact;
        this.isActive = isActive;
    }

    public Contact (FirebaseUserModel contact) {
        this.contact = contact;
        isActive = true;
    }

    public FirebaseUserModel getContact() {
        return contact;
    }

    public void setContact(FirebaseUserModel contact) {
        this.contact = contact;
    }

    public boolean isActive() {
        return isActive;
    }

}

package com.example.naziur.androidchat.utils;

import com.example.naziur.androidchat.models.Contact;

/**
 * Created by Hamidur on 21/09/2018.
 */

public class Container {

    private Contact contact;

    public Container () {
        // empty
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }
}

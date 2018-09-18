package com.example.naziur.androidchat.models;

/**
 * Created by Hamidur on 13/09/2018.
 */

public class FirebaseGroupModel {

    String title = "";
    String pic = "";
    String admin = "";
    String members = "";

    public FirebaseGroupModel () {
        /*Blank default constructor essential for Firebase*/
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public String getMembers() {
        return members;
    }

    public void setMembers(String members) {
        this.members = members;
    }
}

package com.example.naziur.androidchat.models;

/**
 * Created by Hamidur on 13/09/2018.
 */

public class FirebaseGroupModel {

    String title = "";
    String pic = "";
    String groupKey = "";
    String admin = "";

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

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }
}

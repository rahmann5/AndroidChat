package com.example.naziur.androidchat.models;

import java.util.Comparator;

/**
 * Created by Hamidur on 13/09/2018.
 */

public class FirebaseGroupModel {

    String title = "";
    String pic = "";
    String admin = "";
    String groupKey = "";
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

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public static Comparator<FirebaseGroupModel> groupKeyComparator = new Comparator<FirebaseGroupModel>() {

        public int compare(FirebaseGroupModel s1, FirebaseGroupModel s2) {
            String groupKey1 = s1.getGroupKey().toUpperCase();
            String groupKey2 = s2.getGroupKey().toUpperCase();

            //ascending order
            return groupKey1.compareTo(groupKey2);

        }};

}

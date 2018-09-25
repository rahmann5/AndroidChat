package com.example.naziur.androidchat.fragment;

import android.support.v4.app.Fragment;

import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.utils.Container;
import com.google.firebase.database.DatabaseError;

/**
 * Created by Hamidur on 25/09/2018.
 */

public abstract class SessionFragment  extends Fragment implements FirebaseHelper.FirebaseHelperListener{

    public SessionFragment () {
        FirebaseHelper.setFirebaseHelperListener(this);
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        handleOnCompleteTask(tag, condition, container);
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        handleOnFailureTask(tag, databaseError);
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        handleOnChange(tag, condition, container);
    }

    public abstract void handleOnCompleteTask (String tag, int condition, Container container);
    public abstract void handleOnFailureTask (String tag, DatabaseError databaseError);
    public abstract void handleOnChange (String tag, int condition, Container container);

}

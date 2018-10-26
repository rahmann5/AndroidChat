package com.example.naziur.androidchat.utils;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;
/**
 * Created by Hamidur on 24/10/2018.
 */
public class LogoutLoader extends AsyncTaskLoader<Void> {

    public LogoutLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public Void loadInBackground() {
        try {
            FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

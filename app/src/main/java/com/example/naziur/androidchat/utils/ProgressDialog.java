package com.example.naziur.androidchat.utils;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

/**
 * Created by Naziur on 24/04/2018.
 */

public class ProgressDialog {

    private AlertDialog builder;


    public ProgressDialog(Activity context, int layoutId, boolean transparent) {
        builder = new AlertDialog.Builder(context).create();
        // Get the layout inflater
        if (transparent) {
            builder.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            builder.setCancelable(false);
        }
        LayoutInflater inflater = context.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(layoutId, null));
    }

    public void toggleDialog(boolean show){
        if(show)
            builder.show();
        else
            builder.dismiss();
    }

}

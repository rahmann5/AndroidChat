package com.example.naziur.androidchat.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.naziur.androidchat.R;

/**
 * Created by Naziur on 24/04/2018.
 */

public class ProgressDialog {

    private AlertDialog builder;
    private Context context;
    private TextView infoTv;

    public ProgressDialog(Activity context, int layoutId, boolean transparent) {
        builder = new AlertDialog.Builder(context).create();
        this. context = context;
        // Get the layout inflater
        if (transparent) {
            builder.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            builder.setCancelable(false);
        }
        LayoutInflater inflater = context.getLayoutInflater();
        View root = inflater.inflate(layoutId, null);
        infoTv = (TextView) root.findViewById(R.id.info);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(root);
    }

    public void toggleInfoDisplay(boolean show){
        if(show)
            infoTv.setVisibility(View.VISIBLE);
        else
            infoTv.setVisibility(View.GONE);
    }

    public void setInfo(String info){
        infoTv.setText(info);
    }

    public void toggleDialog(boolean show){
        if(((Activity) context).isFinishing())
        {
            return;
        }

        if(show)
            builder.show();
        else
            builder.dismiss();
    }

}

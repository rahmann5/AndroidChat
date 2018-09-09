package com.example.naziur.androidchat.utils;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.naziur.androidchat.R;

/**
 * Created by Hamidur on 09/09/2018.
 */

public class ErrorDialog {
    private AlertDialog builder;

    public ErrorDialog(final Activity context) {
        builder = new AlertDialog.Builder(context).create();
        builder.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        builder.setCancelable(false);
        LayoutInflater inflater = context.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.error_dialog, null));
        AppCompatButton exit = (AppCompatButton) builder.findViewById(R.id.error_exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.exit(1);
            }
        });
        AppCompatButton refresh = (AppCompatButton) builder.findViewById(R.id.error_refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.recreate();
            }
        });
    }

    public void setErrorMessage (String errorMessage) {
        TextView errorText = (TextView) builder.findViewById(R.id.error_message);
        errorText.setText(errorMessage);
    }

    public void toggleDialog(boolean show){
        if(show)
            builder.show();
        else
            builder.dismiss();
    }
}

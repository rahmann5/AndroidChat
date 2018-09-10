package com.example.naziur.androidchat.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.naziur.androidchat.R;

/**
 * Created by Hamidur on 09/09/2018.
 */

public class ErrorDialogFragment extends DialogFragment {
    private static  String mMessageToDisplay;


    public static ErrorDialogFragment newInstance(
            String message) {

        mMessageToDisplay = message;
        return new ErrorDialogFragment();
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View v = inflater.inflate(R.layout.error_dialog, null);

        TextView errorText = (TextView) v.findViewById(R.id.error_message);
        errorText.setText(mMessageToDisplay);

        builder.setView(v) // Add action buttons
                .setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        getActivity().recreate();
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        System.exit(1);
                    }
                });;
       return builder.create();
    }

}
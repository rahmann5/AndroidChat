package com.example.naziur.androidchat.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;

import java.io.File;

/**
 * Created by Hamidur on 09/09/2018.
 */

public class ImageViewDialogFragment extends DialogFragment {

    public interface ImageViewDialogListener {
        void onSendPressed (Dialog dialog, File f, ProgressBar bar);
    }

    private static ImageViewDialogListener listener;

    private static File imageFile;

    public static ImageViewDialogFragment newInstance (File f) {
        imageFile = f;
        return new ImageViewDialogFragment();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.image_viewer_dialog, null);

        final ProgressBar bar = (ProgressBar) v.findViewById(R.id.upload_progress);

        ImageView display = (ImageView) v.findViewById(R.id.image_viewer);
        Glide.with(getActivity()).load(imageFile)
                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                .into(display);

        AppCompatImageButton cancelBtn = (AppCompatImageButton) v.findViewById(R.id.cancel_image);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        final AppCompatImageButton sendBtn = (AppCompatImageButton) v.findViewById(R.id.send_image);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBtn.setEnabled(false);
                listener.onSendPressed(getDialog(), imageFile, bar);
            }
        });

        builder.setView(v);

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (ImageViewDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement ContactDialogListener");
        }
    }
}
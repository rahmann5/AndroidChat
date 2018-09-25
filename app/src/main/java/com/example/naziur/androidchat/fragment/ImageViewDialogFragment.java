package com.example.naziur.androidchat.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.activities.ChatActivity;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by Hamidur on 09/09/2018.
 */

public class ImageViewDialogFragment extends DialogFragment implements FirebaseHelper.FirebaseHelperListener{

    private static final String TAG = "ImageViewDialogFragment";
    private FirebaseHelper firebaseHelper;

    public interface ImageViewDialogListener {
        void onActionPressed();
    }

    private static ImageViewDialogListener listener;

    public static File imageFile;
    public static String imageFileString;
    private static String action;
    private static int actionIcon;
    ProgressBar progressBar;

    public static ImageViewDialogFragment newInstance (File f, String a, int icon) {
        imageFile = f;
        imageFileString = null;
        action = a;
        actionIcon = icon;
        return new ImageViewDialogFragment();
    }

    public static ImageViewDialogFragment newInstance (String f, String a, int icon) {
        imageFileString = f;
        imageFile = null;
        action = a;
        actionIcon = icon;
        return new ImageViewDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.image_viewer_dialog, null);

        progressBar = (ProgressBar) v.findViewById(R.id.upload_progress);

        ImageView display = (ImageView) v.findViewById(R.id.image_viewer);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        if (imageFile != null) {
            Glide.with(getActivity()).load(imageFile)
                    .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                    .into(display);
        } else if (imageFileString != null) {
            Glide.with(getActivity()).load(imageFileString)
                    .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                    .into(display);
        }


        AppCompatImageButton cancelBtn = (AppCompatImageButton) v.findViewById(R.id.cancel_image);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        final AppCompatImageButton sendBtn = (AppCompatImageButton) v.findViewById(R.id.send_image);
        sendBtn.setImageResource(actionIcon);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Network.isInternetAvailable(getActivity(), true)) return;
                sendBtn.setEnabled(false);
                if (action.equals(Constants.ACTION_DOWNLOAD)) {
                    if (imageFileString != null) {
                        if (isStoragePermissionGranted(getActivity())) {
                            Network.downloadImageToPhone(getActivity(), imageFileString);
                            getDialog().dismiss();
                        }
                    }
                } else if (action.equals(Constants.ACTION_SEND)) {
                    listener.onActionPressed();
                }
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

    public void sendImageAndMessage(final String chatKey, final FirebaseUserModel friend, final Context context){
        if (ImageViewDialogFragment.imageFile != null) {
            Uri imgUri = Uri.fromFile(ImageViewDialogFragment.imageFile);
            StorageReference mStorageRef = FirebaseStorage.getInstance()
                    .getReference().child("single/" + chatKey + "/pictures/" + imgUri.getLastPathSegment());
            mStorageRef.putFile(imgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(0);
                        }
                    }, 5000);
                    @SuppressWarnings("VisibleForTests")
                    final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    getDialog().dismiss();
                    firebaseHelper.createImageUploadMessageNode("single", chatKey, context, downloadUrl.toString(), friend);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(context, "Image Upload Failed", Toast.LENGTH_SHORT ).show();
                    getDialog().dismiss();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    @SuppressWarnings("VisibleForTests")
                    double progresss = (100.0* taskSnapshot.getBytesTransferred()/ taskSnapshot.getTotalByteCount());
                    progressBar.setProgress((int)progresss);
                }
            });

        }
    }
    
    public  boolean isStoragePermissionGranted(final Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG,"Permission is granted");
                return true;
            } else {
                Toast.makeText(context, "Please allow permission to write to storage.", Toast.LENGTH_SHORT).show();
                Log.i(TAG,"Permission is revoked");
                //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.i(TAG,"Permission is granted");
            return true;
        }
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("createImageUploadMessageNode")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    Log.i(TAG, "FAILED NOTIFICATION: " + container.getString());
                    break;

                case FirebaseHelper.CONDITION_2 :
                    Log.i(TAG, "SUCCESSFUL NOTIFICATION: " + container.getString());
                    break;

            }
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag) {
            case "createImageUploadMessageNode" :
                Log.i(TAG, databaseError.getMessage());
                break;
        }
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

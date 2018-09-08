package com.example.naziur.androidchat.activities;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.utils.FadingActionBarHelper;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    private static final String TAG = ProfileActivity.class.getSimpleName();

    private StorageReference mStorageRef;
    private FirebaseDatabase database;
    private DatabaseReference userRef;

    private ContactDBHelper db;

    User user = User.getInstance();
    private RecyclerView myGroups, myContacts;
    private TextView emptyGroupsList, emptyContactsList, myUsername;
    private AppCompatButton saveButton;
    private ImageView editToggle, profilePic;
    private ImageButton updatePic, resetPic, revertPic;
    private AppCompatEditText profileName;
    private Spinner profileStatus;
    private File myImageFile;
    private ProgressDialog progressBar;
    private boolean reset = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(user.profileName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        database = FirebaseDatabase.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        userRef = database.getReference("users");
        db = new ContactDBHelper(getApplicationContext());
        progressBar = new ProgressDialog(ProfileActivity.this, R.layout.progress_dialog, true);
        FadingActionBarHelper helper = new FadingActionBarHelper()
                .actionBarBackground(R.color.colorPrimaryDark)
                .headerLayout(R.layout.header)
                .contentLayout(R.layout.activity_profile)
                .allowHeaderTouchEvents(true);
        setContentView(helper.createView(this));
        helper.initActionBar(this);
        EasyImage.configuration(this).setAllowMultiplePickInGallery(false);
        profilePic = (ImageView) findViewById(R.id.image_header);
        Glide.with(ProfileActivity.this).load(user.profilePic)
                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                .into(profilePic);
        updatePic = (ImageButton) findViewById(R.id.update_profile_pic);

        revertPic = (ImageButton) findViewById(R.id.undo_profile_pic);
        revertPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reset(false);
                Glide.with(ProfileActivity.this).load(user.profilePic)
                        .apply(new RequestOptions().error(R.drawable.unknown))
                        .into(profilePic);
            }
        });

        resetPic = (ImageButton) findViewById(R.id.reset_profile_pic);
        resetPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!user.profilePic.equals("") || myImageFile != null) {
                    reset(true);
                    Glide.with(ProfileActivity.this).load(R.drawable.unknown).into(profilePic);
                }
            }
        });

        updatePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EasyImage.openChooserWithGallery(ProfileActivity.this, getResources().getString(R.string.gallery_chooser), REQUEST_CODE_GALLERY_CAMERA);
            }
        });

        profileStatus = (Spinner) findViewById(R.id.prof_status);
        if (!user.status.equals(""))
            profileStatus.setSelection(((ArrayAdapter<String>)profileStatus.getAdapter()).getPosition(user.status));

        profileName = (AppCompatEditText) findViewById(R.id.edit_prof_name);
        profileName.setText(user.profileName);
        editToggle = (ImageView) findViewById(R.id.edit_button_prof_name);
        myUsername = (TextView) findViewById(R.id.my_username);

        myUsername.setText("Username : " + user.name);

        saveButton = (AppCompatButton) findViewById(R.id.save_profile_btn);

        showContacts();
        //showGroups(); do inside showContacts for before hiding progress dialog

        editToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!profileName.isEnabled()) {
                    profileName.setEnabled(true);
                } else {
                    profileName.setEnabled(false);
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Network.isInternetAvailable(ProfileActivity.this, true)) {
                    if (hasChanged()) {
                        progressBar.toggleDialog(true);
                        deletePrevImage();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Changes must be made and edit must be disabled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void showGroups() {
        ArrayAdapter<String> adapterGroup = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        emptyGroupsList = (TextView) findViewById(R.id.no_groups);
        myGroups = (RecyclerView) findViewById(R.id.profile_groups_list);
        emptyGroupsList.setVisibility(View.VISIBLE);
        //myGroups.setAdapter(adapterGroup);
    }


    private void showContacts() {
        progressBar.toggleDialog(true);
        //ArrayAdapter<String> adapterContacts = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        final List<Contact> allContacts = new ArrayList<>();
        emptyContactsList = (TextView) findViewById(R.id.no_contacts);
        myContacts = (RecyclerView) findViewById(R.id.profile_contacts_list);
        MyContactsAdapter contactsAdapter = new MyContactsAdapter(ProfileActivity.this, updateContacts (), null);
        LinearLayoutManager l = new LinearLayoutManager(ProfileActivity.this);
        myContacts.setLayoutManager(l);
        myContacts.setAdapter(contactsAdapter);


        if (myContacts.getAdapter() == null || myContacts.getAdapter().getItemCount() == 0) {
            emptyContactsList.setVisibility(View.VISIBLE);
        }

    }

    private List<Contact> updateContacts () {
        final List<Contact> allContacts = new ArrayList<>();
        Cursor c = db.getAllMyContacts(null);
        boolean hasInternet = Network.isInternetAvailable(this, true);
        try {
            while (c.moveToNext()) {
                final String friendUsername = c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME));

                if (hasInternet) {
                    userRef.orderByChild("username").equalTo(friendUsername)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists()){
                                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                            FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                                            boolean res = db.updateProfile(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic());
                                            if (!res) {
                                                Log.i(TAG, "Failed to update local data for : " + firebaseUserModel.getUsername());
                                            }
                                            allContacts.add(new Contact(firebaseUserModel));
                                        }

                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.i(TAG, "Failed to connect to real time database for: " + friendUsername + " reason: " + databaseError.getMessage());
                                }
                            });
                } else {
                    FirebaseUserModel firebaseUserModel = new FirebaseUserModel();
                    firebaseUserModel.setUsername(friendUsername);
                    firebaseUserModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));
                    firebaseUserModel.setProfilePic(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC)));
                    allContacts.add(new Contact(firebaseUserModel));
                }
            }

        } finally {
            if (!hasInternet) Toast.makeText(this, "Data maybe outdated", Toast.LENGTH_LONG).show();
            progressBar.toggleDialog(false);
            c.close();
        }
        return allContacts;
    }

    private void uploadImageToCloud () {
        if (myImageFile != null) {
            Uri fileUri = Uri.fromFile(myImageFile);
            StorageReference fileRef = mStorageRef.child("profile/" + fileUri.getLastPathSegment());

            fileRef.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Get a URL to the uploaded content
                            @SuppressWarnings("VisibleForTests")
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            updateUserInfo(downloadUrl);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressBar.toggleDialog(false);
                            Toast.makeText(ProfileActivity.this, "Error Uploading File", Toast.LENGTH_SHORT).show();
                            exception.printStackTrace();
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    //@SuppressWarnings("VisibleForTests")
                    //double progresss = (100.0* taskSnapshot.getBytesTransferred()/ taskSnapshot.getTotalByteCount());
                }
            });
        } else {
            updateUserInfo(null);
        }

    }

    private void updateUserInfo(final Uri uploadedImgUri) {
        userRef.orderByChild("username").equalTo(user.name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    final FirebaseUserModel updatedUser = snapshot.getValue(FirebaseUserModel.class);
                    updatedUser.setStatus(profileStatus.getSelectedItem().toString());
                    updatedUser.setProfileName(profileName.getText().toString());

                    if (uploadedImgUri != null && !reset) { // new profile pic upload
                        updatedUser.setProfilePic(uploadedImgUri.toString());
                    } else if (uploadedImgUri == null && reset) { // reset image back to unknown
                        updatedUser.setProfilePic("");
                    } else if (uploadedImgUri == null && !reset){ // keep current image but change other information
                        // do nothing
                    }

                    snapshot.getRef().setValue(updatedUser, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError == null) {
                                Toast.makeText(ProfileActivity.this, "Successfully updated profile", Toast.LENGTH_SHORT).show();
                                user.login(updatedUser);
                                finish();
                            } else {
                                Toast.makeText(ProfileActivity.this, "Error Uploading to Database", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, databaseError.toString());
                            }
                            progressBar.toggleDialog(false);

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.toggleDialog(false);
                Toast.makeText(ProfileActivity.this, "Error Uploading to Database", Toast.LENGTH_SHORT).show();
                Log.i(TAG, databaseError.toString());
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                Toast.makeText(ProfileActivity.this, "Error choosing file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                // Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    Toast.makeText(ProfileActivity.this, "Deleting captured image...", Toast.LENGTH_SHORT).show();
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(ProfileActivity.this);
                    if (photoFile != null) photoFile.delete();
                }
            }

            @Override
            public void onImagesPicked(@NonNull List<File> imageFiles, EasyImage.ImageSource source, int type) {
                switch (type){
                    case REQUEST_CODE_GALLERY_CAMERA:
                        reset(false);
                        myImageFile = imageFiles.get(0);
                        Glide.with(ProfileActivity.this)
                                .load(myImageFile)
                                .apply(new RequestOptions().error(R.drawable.unknown))
                                .into(profilePic);
                        break;
                }
            }

        });
    }

    private boolean hasChanged () {
        boolean changed = false;
        if (!profileName.isEnabled() && !profileName.getText().toString().equals("")) { // edit disabled && profile has been set
            if (!profileName.getText().toString().trim().equals(user.profileName) // profile name is different
                    || !user.status.equals(profileStatus.getSelectedItem().toString()) // status is different
                    || reset // image reset from previously set image
                    || myImageFile != null) { // new image set
                changed = true;
            }
        }

        return changed;
    }

    private void reset(boolean value) {
        myImageFile = null;
        reset = value;
    }

    private void deletePrevImage () {
        StorageReference photoRef = null;
       if (!user.profilePic.equals("")) {
            if (reset) { // reverting back to original picture
                photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(user.profilePic);
            } else if (myImageFile != null) { // uploading new picture
                photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(user.profilePic);
            }
        }

        if (photoRef != null) {
            photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    uploadImageToCloud ();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    progressBar.toggleDialog(false);
                    Toast.makeText(ProfileActivity.this, "Error Removing old picture", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onFailure: did not delete file");
                    exception.printStackTrace();
                }
            });
        } else {
            uploadImageToCloud ();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){
            case android.R.id.home:
                finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}

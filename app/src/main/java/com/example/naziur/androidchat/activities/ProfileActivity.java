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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.AllGroupsAdapter;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.FadingActionBarHelper;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class ProfileActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{

    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    private static final String TAG = ProfileActivity.class.getSimpleName();
    private FirebaseHelper firebaseHelper;
    private StorageReference mStorageRef;

    private ContactDBHelper db;

    User user = User.getInstance();
    private RecyclerView myGroups, myContacts;
    private List<String> groupKeys;
    private TextView emptyGroupsList, emptyContactsList, myUsername, resetPic, revertPic, profileInfoName, profileInfoStatus;
    private AppCompatButton saveButton;
    private ImageView editToggle;
    private CircleImageView updatePic;
    private AppCompatEditText profileName;
    private Spinner profileStatus;
    private File myImageFile;
    private ProgressDialog progressBar;
    private boolean reset = false;
    private MyContactsAdapter contactsAdapter;
    private AllGroupsAdapter groupsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(user.profileName);
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        db = new ContactDBHelper(getApplicationContext());
        progressBar = new ProgressDialog(ProfileActivity.this, R.layout.progress_dialog, true);
        FadingActionBarHelper helper = new FadingActionBarHelper()
                .actionBarBackground(R.color.colorPrimaryDark)
                .headerLayout(R.layout.header)
                .contentLayout(R.layout.activity_profile)
                .allowHeaderTouchEvents(true);
        setContentView(helper.createView(this));
        helper.initActionBar(this);
        updatePic = (CircleImageView) findViewById(R.id.update_profile_pic);
        EasyImage.configuration(this).setAllowMultiplePickInGallery(false);
        Glide.with(ProfileActivity.this).load(user.profilePic)
                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                .into(updatePic);
        profileInfoName = (TextView)  findViewById(R.id.prof_info_name);
        profileInfoStatus = (TextView)  findViewById(R.id.prof_info_status);
        revertPic = (TextView) findViewById(R.id.undo_profile_pic);
        revertPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reset(false);
                Glide.with(ProfileActivity.this).load(user.profilePic)
                        .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                        .into(updatePic);
            }
        });

        resetPic = (TextView) findViewById(R.id.reset_profile_pic);
        resetPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!user.profilePic.equals("") || myImageFile != null) {
                    reset(true);
                    Glide.with(ProfileActivity.this).load(R.drawable.unknown).into(updatePic);
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

        profileInfoName.setText(user.profileName);
        profileInfoStatus.setText("Status: " + user.status);

        saveButton = (AppCompatButton) findViewById(R.id.save_profile_btn);

        showContacts();
        showGroups();

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
        groupKeys = new ArrayList<>();
        groupsAdapter = new AllGroupsAdapter(this);
        emptyGroupsList = (TextView) findViewById(R.id.no_groups);
        myGroups = (RecyclerView) findViewById(R.id.profile_groups_list);
        LinearLayoutManager l = new LinearLayoutManager(ProfileActivity.this);
        myGroups.setLayoutManager(l);
        myGroups.setAdapter(groupsAdapter);
        ValueEventListener userListener = firebaseHelper.getValueEventListener(user.name, FirebaseHelper.CONDITION_1 , FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION, FirebaseUserModel.class);
        firebaseHelper.toggleListenerFor("users", "username" , user.name, userListener, true, true); //  single event

    }


    private void getGroupInfo (String key) {
        ValueEventListener userListener = firebaseHelper.getValueEventListener(key, FirebaseHelper.CONDITION_3, FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION, FirebaseGroupModel.class);
        firebaseHelper.toggleListenerFor("groups", "groupKey" , key, userListener, true, true); //  single event
    }


    private void showContacts() {
        emptyContactsList = (TextView) findViewById(R.id.no_contacts);
        myContacts = (RecyclerView) findViewById(R.id.profile_contacts_list);
        updateContacts ();
    }

    private void updateContacts () {
        contactsAdapter = new MyContactsAdapter(this, null);
        final Cursor c = db.getAllMyContacts(null);
        boolean hasInternet = Network.isInternetAvailable(this, true);
        try {
            while (c.moveToNext()) {
                FirebaseUserModel fbModel = new FirebaseUserModel();
                fbModel.setUsername(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME)));
                fbModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));
                fbModel.setProfilePic(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC)));
                if (hasInternet) {
                    firebaseHelper.updateLocalContactsFromFirebase("users", fbModel, db);
                } else {
                    contactsAdapter.addNewItemContact(new Contact(fbModel, false));
                    toggleEmptyState(emptyContactsList, contactsAdapter);
                }
            }

        } finally {
            if (!hasInternet) Toast.makeText(this, "Data maybe outdated", Toast.LENGTH_LONG).show();
            c.close();
        }
    }

    private void toggleEmptyState (TextView emptystate, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        if (adapter.getItemCount() == 0)
            emptystate.setVisibility(View.VISIBLE);
        else
            emptystate.setVisibility(View.GONE);
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
                            firebaseHelper.updateUserInfo(user.name, downloadUrl, profileStatus.getSelectedItem().toString(), profileName.getText().toString(), reset);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressBar.toggleDialog(false);
                            Toast.makeText(ProfileActivity.this, "Error Uploading File", Toast.LENGTH_SHORT).show();
                            exception.printStackTrace();
                        }
                    });
        } else {
            firebaseHelper.updateUserInfo(user.name, null, profileStatus.getSelectedItem().toString(), profileName.getText().toString(), reset);
        }

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
                                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                                .into(updatePic);
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
                    Log.i(TAG, "onFailure: did not delete file");
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

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("updateLocalContactsFromFirebase")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    contactsAdapter.addNewItemContact(container.getContact());
                    break;

                case FirebaseHelper.CONDITION_2 :
                    LinearLayoutManager l = new LinearLayoutManager(ProfileActivity.this);
                    myContacts.setLayoutManager(l);
                    myContacts.setAdapter(contactsAdapter);
                    toggleEmptyState(emptyContactsList, contactsAdapter);
                    break;
            }
        } else if (tag.equals("updateUserInfo")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    Toast.makeText(ProfileActivity.this, "Successfully updated profile", Toast.LENGTH_SHORT).show();
                    user.login(container.getUserModel());
                    finish();
                    break;

                case FirebaseHelper.CONDITION_2 :
                    Toast.makeText(ProfileActivity.this, "Error Uploading to Database", Toast.LENGTH_SHORT).show();
                    break;
            }
            progressBar.toggleDialog(false);
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag) {
            case "updateLocalContactsFromFirebase" :
                toggleEmptyState(emptyContactsList, contactsAdapter);
                break;

            case "updateUserInfo" :
                progressBar.toggleDialog(false);
                Toast.makeText(ProfileActivity.this, "Error Uploading to Database", Toast.LENGTH_SHORT).show();
                break;
        }
        Log.i(TAG, databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {
        if (tag.equals("updateLocalContactsFromFirebase")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    contactsAdapter.addNewItemContact(container.getContact());
                    break;
            }
        } else if (tag.equals("getValueEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    FirebaseUserModel currentUser = (FirebaseUserModel) container.getObject();
                    String[] allKeys = currentUser.getGroupKeys().split(",");
                    for(String key: allKeys){
                        if(!key.equals(""))
                            groupKeys.add(key);
                    }
                    if (!groupKeys.isEmpty())
                        getGroupInfo(groupKeys.get(0)); // first item
                    else
                        toggleEmptyState(emptyGroupsList, groupsAdapter);

                    break;

                case FirebaseHelper.CONDITION_3 :
                    FirebaseGroupModel groupModel = (FirebaseGroupModel) container.getObject();
                    groupsAdapter.addGroupItem(groupModel);
                    toggleEmptyState(emptyGroupsList, groupsAdapter);
                    int currentIndex = groupKeys.indexOf(groupModel.getGroupKey());
                    if ((currentIndex + 1) < groupKeys.size())
                        getGroupInfo(groupKeys.get(currentIndex + 1)); // subsequent item
                    break;
            }
        }
    }
}

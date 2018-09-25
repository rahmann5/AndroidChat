package com.example.naziur.androidchat.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseGroupMessageModel;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import de.hdodenhof.circleimageview.CircleImageView;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class GroupCreatorActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{
    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    private final String TAG = getClass().getSimpleName();

    private List<String> membersSelectedFromContacts;
    private List<String> newUsersNotInContacts;
    private RecyclerView contactsRecyclerView;
    private RecyclerView choiceRecyclerView;
    private MyContactsAdapter myContactsAdapter;
    private StorageReference mStorageRef;
    private ChoiceAdapter allChosenMembersAdapter;
    private File myImageFile;
    private User user = User.getInstance();
    private CircleImageView groupImage;
    private ProgressDialog progressBar;
    private FirebaseHelper firebaseHelper;

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch(tag){
            case "addUserToContacts":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        FirebaseUserModel firebaseUserModel = container.getUserModel();
                        if (!allChosenMembersAdapter.isUserAlreadyInContacts(container.getString())) {
                            newUsersNotInContacts.add(firebaseUserModel.getUsername());
                            updateChosenList();
                            break;
                        }
                        break;
                    case FirebaseHelper.CONDITION_2:
                        Toast.makeText(GroupCreatorActivity.this, "The username you entered is not valid", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
            case "createGroup":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        String title = container.getString().split(",")[0];
                        String uniqueID = container.getString().split(",")[1];
                        final List<String> allMembers = getAllMembersTogether();
                        Collections.sort(allMembers);
                        firebaseHelper.getDeviceTokensFor(allMembers, title, uniqueID);
                        break;
                }
                break;
            case "updateMessageNode":
                switch (condition){
                    case FirebaseHelper.CONDITION_2:
                        Toast.makeText(GroupCreatorActivity.this, "Failed to make background notification", Toast.LENGTH_SHORT).show();
                        break;
                }
                firebaseHelper.updateGroupKeyForMembers(getAllMembersTogether(), container.getString(), user);
                break;
            case "getDeviceTokensFor":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        String title = container.getString().split(",")[0];
                        String uniqueID = container.getString().split(",")[1];
                        sendAdminMsg(container.getJsonArray(), title, uniqueID);
                        break;
                }
                break;
            case "updateGroupKeyForMembers":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        moveToGroupChatActivity(container.getString());
                        progressBar.toggleDialog(false);
                        break;
                }
                break;
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch(tag){
            case "addUserToContacts":
                Log.i(TAG, tag+": Failed to retrieve user from server with error:" + databaseError.getMessage());
                break;
            case "createGroup":
                progressBar.toggleDialog(false);
                Toast.makeText(GroupCreatorActivity.this, "Error Uploading to Database", Toast.LENGTH_SHORT).show();
                Log.e(TAG,  tag+" "+databaseError.toString());
                break;
            case "updateMessageNode":
                Log.i(TAG,  tag+" "+databaseError.toString());
                break;
            case "getDeviceTokensFor":
                Log.i(TAG, tag+" "+databaseError.getMessage());
                progressBar.toggleDialog(false);
                break;
            case "updateGroupKeyForMembers":
                Log.e(TAG, tag+" "+databaseError.getMessage());
                finish();
                progressBar.toggleDialog(false);
                break;
        }
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }

    public interface OnItemClickListener {
        void onItemClick (String user, int pos);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_creator);
        setTitle("Group Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        membersSelectedFromContacts = new ArrayList<>();
        newUsersNotInContacts = new ArrayList<>();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        groupImage = (CircleImageView) findViewById(R.id.group_photo);
        CircleImageView refreshImage = (CircleImageView) findViewById(R.id.refresh);
        final EditText groupNameEt = (EditText) findViewById(R.id.group_name);
        final EditText searchedUsernameEt = (EditText) findViewById(R.id.user_not_in_contacts);
        progressBar = new ProgressDialog(GroupCreatorActivity.this, R.layout.progress_dialog, true);
        ImageButton searchAddUserBtn = (ImageButton) findViewById(R.id.search_add_contact);
        Button createGroupBtn = (Button) findViewById(R.id.make_group_btn);
        ContactDBHelper contactDbHelper = new ContactDBHelper(this);
        TextView emptyViewTv = (TextView) findViewById(R.id.no_groups);
        contactsRecyclerView = (RecyclerView) findViewById(R.id.chat_groups_list);
        choiceRecyclerView = (RecyclerView) findViewById(R.id.chosen_group_list);
        allChosenMembersAdapter = new ChoiceAdapter(this, new OnItemClickListener() {
            @Override
            public void onItemClick(String user, int pos) {
                if(membersSelectedFromContacts.contains(user))
                    membersSelectedFromContacts.remove(user);
                else if(newUsersNotInContacts.contains(user))
                    newUsersNotInContacts.remove(user);

                updateChosenList();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        choiceRecyclerView.setLayoutManager(layoutManager);
        choiceRecyclerView.setAdapter(allChosenMembersAdapter);
        myContactsAdapter = new MyContactsAdapter(this, readCursorData(contactDbHelper.getAllMyContacts(null)), new MyContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Contact contact, int pos) {
                if (membersSelectedFromContacts.contains(contact.getContact().getUsername())) {
                    membersSelectedFromContacts.remove(contact.getContact().getUsername());
                } else {
                    if(!allChosenMembersAdapter.isUserAlreadyInContacts(contact.getContact().getUsername())) {
                        membersSelectedFromContacts.add(contact.getContact().getUsername());
                    }
                }
                updateChosenList();
            }
        });

        searchAddUserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Network.isInternetAvailable(GroupCreatorActivity.this, true)) {
                    Toast.makeText(GroupCreatorActivity.this, "You need internet access to do this", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!searchedUsernameEt.getText().toString().trim().isEmpty() && !searchedUsernameEt.getText().toString().trim().equals(user.name)){
                    firebaseHelper.addUserToContacts(searchedUsernameEt.getText().toString().trim(), null, 0); // TEST
                }else {
                    Toast.makeText(GroupCreatorActivity.this, "You must enter a username before searching for one", Toast.LENGTH_SHORT).show();
                }
            }
        });

        groupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EasyImage.openChooserWithGallery(GroupCreatorActivity.this, getResources().getString(R.string.gallery_chooser), REQUEST_CODE_GALLERY_CAMERA);
            }
        });

        refreshImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myImageFile = null;
                Glide.with(GroupCreatorActivity.this)
                        .load(R.drawable.ic_group_unknown)
                        .into(groupImage);
            }
        });

        createGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Network.isInternetAvailable(GroupCreatorActivity.this, true)) {
                    Toast.makeText(GroupCreatorActivity.this, "You need internet access to do this", Toast.LENGTH_SHORT).show();
                    return;
                }

                String title = groupNameEt.getText().toString().trim();
                if(title.equals("")){
                    Toast.makeText(GroupCreatorActivity.this, "You need to enter a group name", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(membersSelectedFromContacts.size() > 0 || newUsersNotInContacts.size() > 0){
                    progressBar.toggleDialog(true);
                    if(myImageFile == null){
                        createGroupNode(title, "");
                    } else {
                        uploadImageThenCreateNode(title);
                    }
                } else {
                    Toast.makeText(GroupCreatorActivity.this, "You need to at least select one other person to be in the group", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (myContactsAdapter.getItemCount() > 0){
            emptyViewTv.setVisibility(View.GONE);
            contactsRecyclerView.setVisibility(View.VISIBLE);
        }
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        contactsRecyclerView.setLayoutManager(mLayoutManager);
        contactsRecyclerView.setAdapter(myContactsAdapter);

    }

    private void uploadImageThenCreateNode(final String title) {
        Uri fileUri = Uri.fromFile(myImageFile);
        StorageReference fileRef = mStorageRef.child("groupProf/"+ fileUri.getLastPathSegment());

        fileRef.putFile(fileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        @SuppressWarnings("VisibleForTests")
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        if(downloadUrl != null)
                            createGroupNode(title, downloadUrl.toString());
                        else
                            progressBar.toggleDialog(false);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        progressBar.toggleDialog(false);
                        Toast.makeText(GroupCreatorActivity.this, "Error Uploading File", Toast.LENGTH_SHORT).show();
                        exception.printStackTrace();
                    }
                });
    }


    private void createGroupNode(final String title, String imgUrl){
        final String uniqueID = System.currentTimeMillis()+getRandomCharactersForKey();
        FirebaseGroupModel firebaseGroupModel = new FirebaseGroupModel();
        firebaseGroupModel.setTitle(title);
        firebaseGroupModel.setAdmin(user.name);
        firebaseGroupModel.setPic(imgUrl);
        firebaseGroupModel.setGroupKey(uniqueID);
        firebaseGroupModel.setMembers(getAllMembersAsString());
        firebaseHelper.createGroup(firebaseGroupModel);
    }

    private void sendAdminMsg(JSONArray membersDeviceTokens, String title, final String uniqueId){
        String inviteMessage = "Group invite to: " + title + " by " + user.name;
        firebaseHelper.updateMessageNode(this, "group", uniqueId, inviteMessage, null, membersDeviceTokens, title);
    }

    private String getAllMembersAsString(){
        String members = "";
        List<String> allMembers = getAllMembersTogether();
        int i = 0;
        for(String user : allMembers){
            members += user;

            if(i < allMembers.size()-1){
                members += ",";
            }
            i++;
        }

        return members;
    }

    private void moveToGroupChatActivity(String id){
        Intent intent = new Intent(this, GroupChatActivity.class);
        intent.putExtra("group_uid", id);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private List<String> getAllMembersTogether(){
        List<String> allMembers = new ArrayList<String>();
        allMembers.addAll(membersSelectedFromContacts);
        allMembers.addAll(newUsersNotInContacts);
        return allMembers;
    }

    private void updateChosenList(){
        allChosenMembersAdapter.clear();
        allChosenMembersAdapter.setChoices(getAllMembersTogether());
        choiceRecyclerView.invalidate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback(){

            @Override
            public void onImagesPicked(@NonNull List<File> imageFiles, EasyImage.ImageSource source, int type) {
                switch (type){
                    case REQUEST_CODE_GALLERY_CAMERA:
                        myImageFile = imageFiles.get(0);
                        Glide.with(GroupCreatorActivity.this)
                                .load(myImageFile)
                                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                                .into(groupImage);
                        break;
                }
            }

            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                Toast.makeText(GroupCreatorActivity.this, "Error choosing file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                // Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    Toast.makeText(GroupCreatorActivity.this, "Deleting captured image...", Toast.LENGTH_SHORT).show();
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(GroupCreatorActivity.this);
                    if (photoFile != null) photoFile.delete();
                }
            }
        });


    }

    private List<Contact> readCursorData (Cursor c) {
        List<Contact> myContacts = new ArrayList<>();
        try{
            while (c.moveToNext()) {
                FirebaseUserModel fbModel = new FirebaseUserModel();
                fbModel.setUsername(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME)));
                fbModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));
                fbModel.setProfilePic(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC)));
                myContacts.add(new Contact(fbModel));
            }
        } finally {
            c.close();
        }

        return myContacts;
    }

    private String getRandomCharactersForKey(){
        Random rand = new Random();
        String name = user.name;
        return ""+name.charAt(rand.nextInt(name.length()))+name.charAt(rand.nextInt(name.length()))+rand.nextInt(name.length());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class ChoiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public Context context;
        private List<String> choices;
        private GroupCreatorActivity.OnItemClickListener onItemClickListener;

        public ChoiceAdapter(Context c, GroupCreatorActivity.OnItemClickListener onItemClickListener){
            context = c;
            choices = new ArrayList();
            this.onItemClickListener = onItemClickListener;
        }

        public void setChoices(List<String> choices){
            this.choices = choices;
            notifyDataSetChanged();
        }

        public boolean isUserAlreadyInContacts(String username){
            return choices.contains(username);
        }

        public void clear(){
            choices = new ArrayList();
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new MyChoiceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((MyChoiceViewHolder) holder).bind(choices.get(position), onItemClickListener, position);
        }

        @Override
        public int getItemCount() {
            return choices.size();
        }

        private class MyChoiceViewHolder extends RecyclerView.ViewHolder {

            public MyChoiceViewHolder(View itemView) {
                super(itemView);
            }

            public void bind(final String username, final GroupCreatorActivity.OnItemClickListener onItemClickListener, final int pos){
                TextView usernameTv = (TextView) itemView.findViewById(android.R.id.text1);
                usernameTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onItemClickListener.onItemClick(username ,pos);
                    }
                });
                usernameTv.setText(username);
            }
        }
    }
}

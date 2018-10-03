package com.example.naziur.androidchat.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.ProgressDialog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class GroupDetailActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener{
    private static final String TAG = GroupDetailActivity.class.getSimpleName();
    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    public static final int MEMBER_REQUEST_CODE = 1;
    private final String NO_IMAGE_CODE = "NO_IMAGE_CODE";
    private FirebaseHelper firebaseHelper;
    private FirebaseGroupModel groupModel;
    private ValueEventListener groupListener;
    private Toolbar toolbar;
    private ProgressDialog progressBar;
    private File myImageFile;
    private ImageView groupIv;
    private EditText titleEt;
    private TextView emptyTv;
    private User user = User.getInstance();
    private StorageReference mStorageRef;
    private String pic = "";
    private Menu menu;
    private ArrayAdapter membersAdapter;
    private ListView membersListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        membersListView = (ListView) findViewById(R.id.members_list_view);
        membersAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, getEveryOneBesidesYou()){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(Color.WHITE);
                return textView;
            }
        };
        membersListView.setAdapter(membersAdapter);
        EasyImage.configuration(this).setAllowMultiplePickInGallery(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            progressBar.toggleDialog(true);
            groupListener = firebaseHelper.getGroupInfo(extra.getString("g_uid"));
        } else {
            Toast.makeText(this, "Error occurred", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void populateWithGroupData(){
        toolbar.setTitle(groupModel.getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView adminTv = (TextView) findViewById(R.id.admin_tv);
        groupIv = (ImageView) findViewById(R.id.expandedImage);
        updateGroupListAdapter();
        emptyTv = (TextView) findViewById(R.id.empty_view);

        if(membersAdapter.getCount() == 0)
            emptyTv.setVisibility(View.VISIBLE);
        else
            emptyTv.setVisibility(View.GONE);

        if(pic.isEmpty() || pic.equals(groupModel.getPic()))
            pic = groupModel.getPic();

        if(!groupModel.getAdmin().isEmpty())
            adminTv.setText(groupModel.getAdmin());
        else
            adminTv.setText(getResources().getString(R.string.no_admin));

        if(!groupModel.getAdmin().equals(user.name)) {
            TextView titleTv = (TextView) findViewById(R.id.title_tv);
            titleTv.setVisibility(View.VISIBLE);
            titleTv.setText(groupModel.getTitle());
        } else {
            invalidateOptionsMenu();

            membersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String username = (String) adapterView.getItemAtPosition(i);
                    createDialog(username).show();
                }
            });

            titleEt = (EditText) findViewById(R.id.group_name_et);
            titleEt.setText(groupModel.getTitle());
            titleEt.setVisibility(View.VISIBLE);

            ImageView editIv = (ImageView) findViewById(R.id.edit_btn_group_title);
            editIv.setVisibility(View.VISIBLE);
            editIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   titleEt.setEnabled(titleEt.isEnabled() ? false : true);
                   if(titleEt.isEnabled())
                       titleEt.requestFocus();
                   else
                       titleEt.clearFocus();
                }
            });

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            FloatingActionButton fabDelete = (FloatingActionButton) findViewById(R.id.fab_delete);
            fab.setVisibility(View.VISIBLE);
            fabDelete.setVisibility(View.VISIBLE);
            fabDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pic = NO_IMAGE_CODE;
                    myImageFile = null;
                    Glide.with(GroupDetailActivity.this)
                            .load(R.drawable.ic_group_unknown)
                            .into(groupIv);
                }
            });
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EasyImage.openChooserWithGallery(GroupDetailActivity.this, getResources().getString(R.string.group_gallery_chooser), REQUEST_CODE_GALLERY_CAMERA);
                }
            });

        }
        if(pic.equals(groupModel.getPic()))
            Glide.with(GroupDetailActivity.this).load(pic).apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown)).into(groupIv);

    }

    private void updateGroupListAdapter() {
        membersAdapter.clear();
        membersAdapter.addAll(getEveryOneBesidesYou());
        membersAdapter.notifyDataSetChanged();
    }

    private List<String> getEveryOneBesidesYou(){
        List<String> members = new ArrayList<>();
        if(groupModel == null)
            return members;

        String [] membersIngroup = groupModel.getMembers().split(",");
        if(groupModel.getAdmin().equals(user.name))
            if(!membersIngroup[0].equals(""))
                return Arrays.asList(membersIngroup);
            else return members;
        else {
            for (int i = 0; i < membersIngroup.length; i++) {
                if (!membersIngroup[i].equals(user.name)) {
                    members.add(membersIngroup[i]);
                }
            }
            return members;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MEMBER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    List<String> members = data.getStringArrayListExtra("members");
                    if (members.size() > 0) {
                        firebaseHelper.updateGroupMembers(members.get(0), members , groupModel.getGroupKey(), false);
                    }
                }
            }

        }

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback(){

            @Override
            public void onImagesPicked(@NonNull List<File> imageFiles, EasyImage.ImageSource source, int type) {
                switch (type){
                    case REQUEST_CODE_GALLERY_CAMERA:
                        myImageFile = imageFiles.get(0);
                        Glide.with(GroupDetailActivity.this)
                                .load(myImageFile)
                                .apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown))
                                .into(groupIv);
                        pic = "groupProf/"+ Uri.fromFile(myImageFile).getLastPathSegment();
                        break;
                }
            }

            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                Toast.makeText(GroupDetailActivity.this, "Error choosing file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                // Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    Toast.makeText(GroupDetailActivity.this, "Deleting captured image...", Toast.LENGTH_SHORT).show();
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(GroupDetailActivity.this);
                    if (photoFile != null) photoFile.delete();
                }
            }
        });

    }

    private AlertDialog createDialog (final String username) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_group_details)
                .setItems(R.array.group_detail_dialog_actions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // String[] actions = getResources().getStringArray(R.array.contact_dialog_actions);
                        onActionSelected(username);
                        dialog.dismiss();
                    }

                    private void onActionSelected(String username) {
                        progressBar.toggleDialog(true);
                        firebaseHelper.removeFromGroup(groupModel.getGroupKey(), username);
                    }
                });
        return builder.create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.chat_detail_menu, menu);
        if(groupModel != null && groupModel.getAdmin().equals(user.name)) {
            menu.findItem(R.id.save_change).setVisible(true);
            menu.findItem(R.id.action_info).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case android.R.id.home:
                finish();
                break;
            case R.id.action_info:
                Intent intent = new Intent(GroupDetailActivity.this, MemberSelectorActivity.class);
                intent.putExtra("current_members", groupModel.getMembers());
                startActivityForResult(intent, MEMBER_REQUEST_CODE);
                break;
            case R.id.save_change:
                checkAndSaveChanges();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkAndSaveChanges(){
        if(titleEt.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Title field cannot be left empty", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.toggleDialog(true);
        if(pic.equals(groupModel.getPic()) || pic.isEmpty()){
            updateGroupModel("");
        } else{
            deletePrevImage(); // DELETE AN IMAGE: THEN EITHER JUST UPDATE OR ADD A NEW IMAGE THEN UPDATE
        }
    }

    private void updateGroupModel(String uri){
        boolean changing = false;
        if(!titleEt.getText().toString().trim().equals(groupModel.getTitle())){
            groupModel.setTitle(titleEt.getText().toString().trim());
            changing = true;
        }

        if(pic.equals(NO_IMAGE_CODE)) {
            changing = true;
            groupModel.setPic("");
        } else if(!uri.isEmpty()){
            changing = true;
            groupModel.setPic(uri);
        }
        if(changing)
            firebaseHelper.updateGroupInfo(groupModel);
        else
            progressBar.toggleDialog(false);
    }

    private void deletePrevImage () {
        StorageReference photoRef = null;
        if (!groupModel.getPic().equals("")) {
            photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(groupModel.getPic());
        }

        if (photoRef != null) {
            photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    if(!pic.equals(NO_IMAGE_CODE)) {
                        uploadImageToCloud();//DELETE THEN ADD NEW IMAGE THEN UPDATE
                    } else {
                        updateGroupModel("");//JUST DELETE THEN PERFORM UPDATE
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    progressBar.toggleDialog(false);
                    Toast.makeText(GroupDetailActivity.this, "Error Removing old picture", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onFailure: did not delete file");
                    exception.printStackTrace();
                }
            });
        } else {
            if(!pic.equals(NO_IMAGE_CODE)) {
                uploadImageToCloud();//NOTHING TO DELETE ADD NEW IMAGE THEN UPDATE
            } else {
                updateGroupModel("");//NOTHING TO DELETE JUST UPDATE
            }
        }

    }

    private void uploadImageToCloud () {
        if (myImageFile != null) {
            Uri fileUri = Uri.fromFile(myImageFile);
            StorageReference fileRef = mStorageRef.child(pic);

            fileRef.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Get a URL to the uploaded content
                            @SuppressWarnings("VisibleForTests")
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            updateGroupModel(downloadUrl.toString());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressBar.toggleDialog(false);
                            Toast.makeText(GroupDetailActivity.this, "Error Uploading File", Toast.LENGTH_SHORT).show();
                            exception.printStackTrace();
                        }
                    });
        } else {
            updateGroupModel("");
        }

    }

    private String getNextMember (String member, List<String> members) {
        String nextMember = member;
        for (String next : members) {
            if (!next.equals(member)) {
                nextMember = next;
            }
        }
        return nextMember;
    }

    @Override
    protected void onStop() {
        firebaseHelper.toggleListenerFor("groups", "groupKey", groupModel.getGroupKey(), groupListener, false, false);
        super.onStop();
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch (tag){
            case "getGroupInfo":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        System.out.println("getGroupInfo");
                        groupModel = container.getGroupModel();
                        populateWithGroupData();
                        progressBar.toggleDialog(false);
                        break;
                }
                break;
            case "updateGroupInfo":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        progressBar.toggleDialog(false);
                        pic = "";
                        finish();
                        break;
                }
                break;
            case "removeFromGroup":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        /*Container is a list where 0=>groupKey, 1=>username*/
                        List<String> memberToRemove = new ArrayList<>();
                        memberToRemove.add(container.getStringList().get(1));
                        firebaseHelper.updateGroupKeyForMembers(memberToRemove, container.getStringList().get(0), FirebaseHelper.CONDITION_1);
                        break;
                }
                break;
            case "updateGroupKeyForMembers":
                switch(condition){
                    case FirebaseHelper.CONDITION_2:
                        Toast.makeText(this, "Successfully added all members", Toast.LENGTH_SHORT).show();
                        break;
                }
                progressBar.toggleDialog(false);
                break;

            case "updateGroupMembers" :
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        String addedMember = container.getString();
                        List<String> allMembers = container.getStringList();
                        String nextMember = getNextMember(addedMember, allMembers);
                        if (!nextMember.equals(addedMember)) {
                            firebaseHelper.updateGroupMembers(nextMember, allMembers, groupModel.getGroupKey(), false);
                        } else {
                            firebaseHelper.updateGroupKeyForMembers(allMembers, groupModel.getGroupKey(), FirebaseHelper.CONDITION_2);
                        }
                        break;
                }
                break;

        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        switch (tag){
            case "getGroupInfo":
            case "updateGroupInfo":
            case "removeFromGroup":
            case "updateGroupKeyForMembers":
            case "updateGroupMembers":
                progressBar.toggleDialog(false);
                break;
        }
        Log.i(TAG, tag+" "+databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

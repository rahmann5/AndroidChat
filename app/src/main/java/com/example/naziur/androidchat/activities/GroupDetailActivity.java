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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.fragment.ImageViewDialogFragment;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;
import com.example.naziur.androidchat.utils.Container;
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

public class GroupDetailActivity extends AuthenticatedActivity implements FirebaseHelper.FirebaseHelperListener, ImageViewDialogFragment.ImageViewDialogListener{
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
    private CircleImageView groupIv;
    private EditText titleEt;
    private User user = User.getInstance();
    private StorageReference mStorageRef;
    private String pic = "";
    private ArrayAdapter membersAdapter;
    private ListView membersListView;
    private ContactDBHelper db;
    private ImageViewDialogFragment imageViewDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        progressBar = new ProgressDialog(this, R.layout.progress_dialog, true);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        db = new ContactDBHelper(this);
        membersListView = (ListView) findViewById(R.id.members_list_view);
        membersAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, getEveryMember(true)){
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
        if (!Network.isInternetAvailable(this, true)) {
            setUpActionBar ("");
            return;
        }

        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            progressBar.toggleDialog(true);
            groupListener = firebaseHelper.getGroupInfo(extra.getString("g_uid"));
            firebaseHelper.toggleListenerFor("groups", "groupKey", extra.getString("g_uid"), groupListener, true, false);
        } else {
            Toast.makeText(this, "Error occurred", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setUpActionBar (String title) {
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void populateWithGroupData(){
        setUpActionBar (groupModel.getTitle());
        TextView adminTv = (TextView) findViewById(R.id.admin_tv);
        Button beAdmin = (Button) findViewById(R.id.be_admin);
        groupIv = (CircleImageView) findViewById(R.id.expandedImage);
        updateGroupListAdapter();
        TextView emptyTv = (TextView) findViewById(R.id.empty_view);

        if(membersAdapter.getCount() == 0)
            emptyTv.setVisibility(View.VISIBLE);
        else
            emptyTv.setVisibility(View.GONE);

        if(pic.isEmpty() || pic.equals(groupModel.getPic()))
            pic = groupModel.getPic();

        if(!groupModel.getAdmin().isEmpty()){
            String name = (!groupModel.getAdmin().equals(user.name)) ? db.getProfileInfoIfExists(groupModel.getAdmin())[0] : user.profileName;
            adminTv.setText(name);
        } else {
            adminTv.setText(getResources().getString(R.string.no_admin));
        }


        beAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseHelper.updateGroupMembers(user.name, null, groupModel.getGroupKey(), true);
            }
        });

        if(!groupModel.getAdmin().equals(user.name)) {
            TextView titleTv = (TextView) findViewById(R.id.title_tv);
            titleTv.setVisibility(View.VISIBLE);
            titleTv.setText(groupModel.getTitle());

            List<String> allMembers = getEveryMember(false);
            if (allMembers.contains(user.name) && groupModel.getAdmin().equals("")) {
                beAdmin.setVisibility(View.VISIBLE);
                adminTv.setVisibility(View.GONE);
            } else {
                beAdmin.setVisibility(View.GONE);
                adminTv.setVisibility(View.VISIBLE);
            }

        } else {
            invalidateOptionsMenu();
            adminTv.setVisibility(View.VISIBLE);
            beAdmin.setVisibility(View.GONE);
            membersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (!groupModel.getMembers().isEmpty()) {
                        String [] membersIngroup = groupModel.getMembers().split(",");
                        createDialog(membersIngroup[i]).show();
                    }
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

            FloatingActionButton fabDelete = (FloatingActionButton) findViewById(R.id.fab_delete);
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
            groupIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (myImageFile != null) {
                        imageViewDialog = ImageViewDialogFragment.newInstance(
                                myImageFile,
                                Constants.ACTION_SEND,
                                android.R.drawable.ic_menu_upload);
                    } else {
                        imageViewDialog = ImageViewDialogFragment.newInstance(
                                (pic != NO_IMAGE_CODE) ? groupModel.getPic() : "",
                                Constants.ACTION_SEND,
                                android.R.drawable.ic_menu_upload);
                    }
                    imageViewDialog.setCancelable(true);
                    imageViewDialog.show(getSupportFragmentManager(), "ImageViewDialogFragment");
                }
            });

        }
        if(pic.equals(groupModel.getPic()))
            Glide.with(getApplicationContext()).load(pic).apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.ic_group_unknown)).into(groupIv);

        progressBar.toggleDialog(false);
    }

    private void updateGroupListAdapter() {
        membersAdapter.clear();
        membersAdapter.addAll(getEveryMember(true));
        membersAdapter.notifyDataSetChanged();
    }

    private List<String> getEveryMember(boolean useProfile){
        List<String> members = new ArrayList<>();
        if(groupModel != null) {
            if (!groupModel.getMembers().isEmpty()) {
                String [] membersIngroup = groupModel.getMembers().split(",");
                for (int i = 0; i < membersIngroup.length; i++) {
                    String name =  membersIngroup[i];
                    if (useProfile) {
                        name = (!membersIngroup[i].equals(user.name)) ? db.getProfileInfoIfExists(membersIngroup[i])[0] : user.profileName;
                    }
                    members.add(name);
                }
            }
        }
        return members;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MEMBER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    List<String> members = data.getStringArrayListExtra("members");
                    if (members.size() > 0) {
                        firebaseHelper.allUnblockedMembers(members);
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
        super.onStop();
        if (groupModel != null)
            firebaseHelper.toggleListenerFor("groups", "groupKey", groupModel.getGroupKey(), groupListener, false, false);

        if (db != null)  db.close();
    }


    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        switch (tag){
            case "getGroupInfo":
                switch (condition){
                    case FirebaseHelper.CONDITION_1:
                        groupModel = container.getGroupModel();
                        populateWithGroupData();
                        break;

                    case FirebaseHelper.CONDITION_2:
                        if (groupModel == null) {
                            Toast.makeText(this, "this group does not exist", Toast.LENGTH_SHORT).show();
                            progressBar.toggleDialog(false);
                            finish();
                        }
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
                        String msg = "Admin has removed "+ container.getStringList().get(1) + " from the group";
                        firebaseHelper.updateMessageNode(this, "group", groupModel.getGroupKey(), msg, null, Constants.MESSAGE_TYPE_SYSTEM, null, groupModel.getTitle());
                        break;
                }
                break;
            case "updateGroupKeyForMembers":
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        String msg = "Admin has add "+ container.getStringList().get(0) +" to the group";
                        firebaseHelper.updateMessageNode(this, "group", groupModel.getGroupKey(), msg, null, Constants.MESSAGE_TYPE_SYSTEM, null, groupModel.getTitle());
                        Toast.makeText(this, "Successfully added all members", Toast.LENGTH_SHORT).show();
                        break;
                }
                progressBar.toggleDialog(false);
                break;

            case "updateGroupMembers" :
                switch(condition){
                    case FirebaseHelper.CONDITION_1:
                        if (!container.getBoolean()) { // not admin
                            String addedMember = container.getString();
                            List<String> allMembers = container.getStringList();
                            String nextMember = getNextMember(addedMember, allMembers);
                            if (!nextMember.equals(addedMember)) {
                                firebaseHelper.updateGroupMembers(nextMember, allMembers, groupModel.getGroupKey(), false);
                            } else {
                                firebaseHelper.updateGroupKeyForMembers(allMembers, groupModel.getGroupKey(), FirebaseHelper.CONDITION_1);
                            }
                        } else {
                            String wishMessage = "New admin is " + container.getString();
                            firebaseHelper.updateMessageNode(this, "group",  groupModel.getGroupKey(), wishMessage , null, Constants.MESSAGE_TYPE_SYSTEM, null, groupModel.getTitle());
                        }
                        break;
                }
                break;

            case "allUnblockedMembers" :
                switch (condition) {
                    case FirebaseHelper.CONDITION_1 :
                        List<String> members = container.getStringList();
                        if (members.size() > 0) {
                            firebaseHelper.updateGroupMembers(members.get(0), members , groupModel.getGroupKey(), false);
                        } else {
                            Toast.makeText(this, getResources().getString(R.string.block_list_msg_blocked_by_them), Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case FirebaseHelper.CONDITION_2 :
                        progressBar.toggleDialog(false);
                        Toast.makeText(this, "Sorry the users selected does not exist", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;

        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        progressBar.toggleDialog(false);
        Log.i(TAG, tag+" "+databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }

    @Override
    public void onActionPressed() {
        controlOffline = false;
        imageViewDialog.getDialog().dismiss();
        EasyImage.openChooserWithGallery(GroupDetailActivity.this, getResources().getString(R.string.group_gallery_chooser), REQUEST_CODE_GALLERY_CAMERA);
    }
}

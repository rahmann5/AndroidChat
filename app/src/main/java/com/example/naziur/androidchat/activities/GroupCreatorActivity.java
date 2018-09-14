package com.example.naziur.androidchat.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class GroupCreatorActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_GALLERY_CAMERA = 0;
    private final String TAG = getClass().getSimpleName();

    private List<String> membersSelectdForGroup;
    private List<String> newUsersNotInContacts;
    private RecyclerView contactsRecyclerView;
    private RecyclerView choiceRecyclerView;
    private MyContactsAdapter myContactsAdapter;
    private DatabaseReference userRef;
    private ChoiceAdapter allChosenMembersAdapter;
    private File myImageFile;
    private CircleImageView groupImage;

    public interface OnItemClickListener {
        void onItemClick (String user, int pos);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_creator);
        setTitle("Group Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        membersSelectdForGroup = new ArrayList<>();
        newUsersNotInContacts = new ArrayList<>();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");
        groupImage = (CircleImageView) findViewById(R.id.group_photo);
        CircleImageView refreshImage = (CircleImageView) findViewById(R.id.refresh);
        EditText groupNameEt = (EditText) findViewById(R.id.group_name);
        final EditText searchedUsernameEt = (EditText) findViewById(R.id.user_not_in_contacts);
        ImageButton searchAddUserBtn = (ImageButton) findViewById(R.id.search_add_contact);
        Button createGroupBtn = (Button) findViewById(R.id.make_group_btn);
        ContactDBHelper contactDbHelper = new ContactDBHelper(this);
        TextView emptyViewTv = (TextView) findViewById(R.id.no_groups);
        contactsRecyclerView = (RecyclerView) findViewById(R.id.chat_groups_list);
        choiceRecyclerView = (RecyclerView) findViewById(R.id.chosen_group_list);
        allChosenMembersAdapter = new ChoiceAdapter(this, new OnItemClickListener() {
            @Override
            public void onItemClick(String user, int pos) {
                if(membersSelectdForGroup.contains(user))
                    membersSelectdForGroup.remove(user);
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
                if (membersSelectdForGroup.contains(contact.getContact().getUsername())) {
                    membersSelectdForGroup.remove(contact.getContact().getUsername());
                } else {
                    if(!allChosenMembersAdapter.isUserAlreadyInContacts(contact.getContact().getUsername())) {
                        membersSelectdForGroup.add(contact.getContact().getUsername());
                    }
                }
                updateChosenList();
            }
        });

        searchAddUserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!searchedUsernameEt.getText().toString().trim().isEmpty()){
                    userRef.orderByChild("username").equalTo(searchedUsernameEt.getText().toString().trim()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                for(DataSnapshot snapShot : dataSnapshot.getChildren()) {
                                    FirebaseUserModel firebaseUserModel = snapShot.getValue(FirebaseUserModel.class);
                                    if (firebaseUserModel.getUsername().equals(searchedUsernameEt.getText().toString().trim())
                                            && !allChosenMembersAdapter.isUserAlreadyInContacts(searchedUsernameEt.getText().toString().trim())) {
                                        newUsersNotInContacts.add(firebaseUserModel.getUsername());
                                        updateChosenList();
                                    }
                                }
                            } else {
                                Toast.makeText(GroupCreatorActivity.this, "The username you entered is not valid", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.i(TAG, "Failed to retrieve user from server");
                        }
                    });
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
                        .load(R.drawable.unknown)
                        .into(groupImage);
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

    private List<String> getAllMembersTogether(){
        List<String> allMembers = new ArrayList<String>();
        allMembers.addAll(membersSelectdForGroup);
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

    private void makeGroup(){

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

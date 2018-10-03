package com.example.naziur.androidchat.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.adapter.MyContactsAdapter;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.database.FirebaseHelper;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.fragment.AddContactDialogFragment;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.example.naziur.androidchat.utils.Network;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MemberSelectorActivity extends AppCompatActivity implements FirebaseHelper.FirebaseHelperListener, MyContactsAdapter.OnItemClickListener,  AddContactDialogFragment.ContactDialogListener{
    private static final String TAG = "MemberSelectorActivity";
    private FirebaseHelper firebaseHelper;
    private LinearLayout emptyState;
    private MyContactsAdapter myContactsAdapter;
    private ArrayList<String> selectedContacts;
    private String [] currentMembers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_contacts);
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            if (extra.getString("current_members") != null && !extra.getString("current_members").equals("") ){
                currentMembers = extra.getString("current_members").split(",");
            }
        }
        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.setFirebaseHelperListener(this);
        selectedContacts = new ArrayList<>();
        emptyState = (LinearLayout) findViewById(R.id.empty);
        FloatingActionButton floatingActionButton  = (FloatingActionButton) findViewById(R.id.add_contact);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showContctsDialog();
            }
        });
        loadContacts ();
        createActionBar ();
    }

    public void showContctsDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new AddContactDialogFragment();
        dialog.show(getSupportFragmentManager(), "AddContactDialogFragment");
    }

    private void loadContacts () {
        ContactDBHelper db = new ContactDBHelper(getApplicationContext());
        RecyclerView myContactsRecycler = (RecyclerView) findViewById(R.id.contacts_recycler);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        myContactsRecycler.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                mLayoutManager.getOrientation());
        myContactsRecycler.addItemDecoration(dividerItemDecoration);

        Cursor c = db.getAllMyContacts(null);
        List<Contact> allContacts = new ArrayList<>();
        if (c != null && c.getCount() > 0) {
           while (c.moveToNext()) {
               String username = c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME));
               if (!isAlreadyMember(username)) {
                   FirebaseUserModel fbModel = new FirebaseUserModel();
                   fbModel.setUsername(username);
                   fbModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));
                   fbModel.setProfilePic(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE_PIC)));

                   allContacts.add(new Contact(fbModel));
               }

           }
            myContactsAdapter = new MyContactsAdapter(this, allContacts, this);
        } else {
            myContactsAdapter = new MyContactsAdapter(this, this);
        }
        myContactsRecycler.setAdapter(myContactsAdapter);
        toggleEmptyState();
        db.close();
    }

    private void toggleEmptyState () {
        if (myContactsAdapter.getItemCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void createActionBar () {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP| ActionBar.DISPLAY_SHOW_TITLE| ActionBar.DISPLAY_SHOW_CUSTOM);
        ImageView imageView = new ImageView(actionBar.getThemedContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setImageResource(R.drawable.ic_thumb_up);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.END
                | Gravity.CENTER_VERTICAL);
        imageView.setLayoutParams(layoutParams);
        actionBar.setCustomView(imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent data = new Intent();
                data.putStringArrayListExtra("members", selectedContacts);
                setResult(RESULT_OK,data);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(Contact contact, int pos, View itemView) {
        if (!selectedContacts.contains(contact.getContact().getUsername())) {
            selectedContacts.add(contact.getContact().getUsername());
            itemView.setBackgroundResource(R.color.green);
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
            selectedContacts.remove(contact.getContact().getUsername());
        }
    }

    private boolean isAlreadyMember (String username) {
        if (currentMembers != null) {
            for (String member : currentMembers) {
                if (member.equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, final String username) {

        if (!Network.isInternetAvailable(this, true)) {
            dialog.dismiss();
            return;
        }

        User user = User.getInstance();
        ContactDBHelper db = new ContactDBHelper(getApplicationContext());
        if(!db.isUserAlreadyInContacts(username) && !username.equals(user.name) && !isAlreadyMember(username)){
            ValueEventListener valueEventListener = firebaseHelper.getValueEventListener(username,
                    FirebaseHelper.NON_CONDITION, FirebaseHelper.NON_CONDITION, FirebaseHelper.CONDITION_1, FirebaseUserModel.class);
            firebaseHelper.toggleListenerFor("users", "username", username, valueEventListener, true, true);
        } else {
            Toast.makeText(MemberSelectorActivity.this, "User cannot be added as they may already exist or it is your username", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
    }

    @Override
    public void onCompleteTask(String tag, int condition, Container container) {
        if (tag.equals("getValueEventListener")) {
            switch (condition) {
                case FirebaseHelper.CONDITION_1 :
                    FirebaseUserModel fbModel = (FirebaseUserModel) container.getObject();
                    myContactsAdapter.addNewItem(fbModel);
                    toggleEmptyState();
                    break;
            }
        }
    }

    @Override
    public void onFailureTask(String tag, DatabaseError databaseError) {
        Log.i(TAG, tag + ": " + databaseError.getMessage());
    }

    @Override
    public void onChange(String tag, int condition, Container container) {

    }
}

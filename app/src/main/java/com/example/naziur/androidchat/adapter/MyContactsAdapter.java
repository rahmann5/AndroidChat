package com.example.naziur.androidchat.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseUserModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hamidur on 29/08/2018.
 */

public class MyContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<Contact> allMyContacts;

    public MyContactsAdapter (Context context, Cursor c) {
        this.context = context;
        readCursorData(c);
    }

    public MyContactsAdapter (Context context){
        allMyContacts = new ArrayList<>();
        this.context = context;;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_my_contact, parent, false);
        return new MyContactViewHolder(view);
    }

    private void readCursorData (Cursor c) {
        allMyContacts = new ArrayList<>();
        try{
            while (c.moveToNext()) {
                FirebaseUserModel fbModel = new FirebaseUserModel();
                fbModel.setUsername(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_USERNAME)));
                fbModel.setProfileName(c.getString(c.getColumnIndex(MyContactsContract.MyContactsContractEntry.COLUMN_PROFILE)));
                // need one for profile picture
                allMyContacts.add(new Contact(fbModel));
            }
        } finally {
            c.close();
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((MyContactViewHolder) holder).bind(allMyContacts.get(position));
    }

    @Override
    public int getItemCount() {
        return allMyContacts.size();
    }

    public void updateState (int pos) {

    }

    public void addNewItem(FirebaseUserModel fbModel){
        allMyContacts.add(new Contact(fbModel));
        notifyDataSetChanged();
    }

    private static class MyContactViewHolder extends RecyclerView.ViewHolder {

        private TextView usernameTv, profileTv;
        private ImageView profPicIv;

        public MyContactViewHolder(View itemView) {
            super(itemView);
            usernameTv = (TextView) itemView.findViewById(R.id.username);
            profileTv = (TextView) itemView.findViewById(R.id.prof_name);
            profPicIv = (ImageView) itemView.findViewById(R.id.prof_pic);
        }

        public void bind (Contact contact) {
            usernameTv.setText(contact.getContact().getUsername());
            profileTv.setText(contact.getContact().getProfileName());
        }
    }
}

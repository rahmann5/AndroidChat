package com.example.naziur.androidchat.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.database.MyContactsContract;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseUserModel;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Hamidur on 29/08/2018.
 */

public class MyContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public Context context;
    private List<Contact> allMyContacts;

    public OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick (Contact contact, int pos);
    }

    public MyContactsAdapter (Context context, List<Contact> contacts, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        //readCursorData(c);
        setAllMyContacts(contacts);
    }

    public MyContactsAdapter (Context context, OnItemClickListener listener){
        allMyContacts = new ArrayList<>();
        this.context = context;;
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_my_contact, parent, false);
        return new MyContactViewHolder(view);
    }


    private void setAllMyContacts(List<Contact> contacts){
        allMyContacts = contacts;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((MyContactViewHolder) holder).bind(allMyContacts.get(position), position ,listener, context);
    }

    @Override
    public int getItemCount() {
        return allMyContacts.size();
    }

    public void updateState (int pos) {
        allMyContacts.remove(pos);
        notifyItemRemoved(pos);
    }

    public void addNewItem(FirebaseUserModel fbModel){
        allMyContacts.add(new Contact(fbModel));
        notifyDataSetChanged();
    }

    private static class MyContactViewHolder extends RecyclerView.ViewHolder {

        private TextView usernameTv, profileTv, activeTv;
        private CircleImageView profPicIv;

        public MyContactViewHolder(View itemView) {
            super(itemView);
            usernameTv = (TextView) itemView.findViewById(R.id.username);
            profileTv = (TextView) itemView.findViewById(R.id.prof_name);
            activeTv = (TextView) itemView.findViewById(R.id.active);
            profPicIv = (CircleImageView) itemView.findViewById(R.id.prof_pic);

        }

        public void bind (final Contact contact, final int position, final OnItemClickListener listener, final Context context) {
            if (listener != null ) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onItemClick(contact, position);
                    }
                });
            }

            usernameTv.setText(contact.getContact().getUsername());
            profileTv.setText(contact.getContact().getProfileName());
            if (contact.isActive()) {
                activeTv.setText(context.getResources().getString(R.string.contact_active));
                setTextColor(activeTv, R.color.green, context);
            } else {
                activeTv.setText(context.getResources().getString(R.string.contact_inactive));
                setTextColor(activeTv, R.color.red, context);
                contact.getContact().setProfilePic("");
            }

            Glide.with(context).load(contact.getContact().getProfilePic()).apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown)).into(profPicIv);
        }

        private void setTextColor (TextView textView, int res, Context context) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textView.setTextColor(context.getColor(res));
            } else {
                textView.setTextColor(context.getResources().getColor(res));
            }
        }
    }



}

package com.example.naziur.androidchat.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.activities.GroupChatActivity;
import com.example.naziur.androidchat.database.ContactDBHelper;
import com.example.naziur.androidchat.models.MessageCell;
import com.example.naziur.androidchat.utils.Constants;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;

/**
 * Created by Hamidur on 27/08/2018.
 */

public class MessagesListAdapter extends ArrayAdapter<MessageCell> {
    MessageCell[] cellItem = null;
    Context context;
    private ContactDBHelper db;
    public MessagesListAdapter(Context context, MessageCell[] resource) {
        super(context, R.layout.receiving_message_cell, resource);
        // TODO Auto-generated constructor stub
        this.context = context;
        this.cellItem = resource;
        db = new ContactDBHelper(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();
        if(cellItem[position].getMessageType().equals(Constants.MESSAGE_TYPE_SYSTEM)) {
            convertView = inflater.inflate(R.layout.system_message_cell, parent, false);
            setTextMsg(position, convertView);
        } else if (cellItem[position].getSender()) {
            if (cellItem[position].getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)) {
                convertView = inflater.inflate(R.layout.my_message_cell, parent, false);
                setTextMsg(position, convertView);
            } else if (cellItem[position].getMessageType().equals(Constants.MESSAGE_TYPE_PIC)) {
                convertView = inflater.inflate(R.layout.my_image_message_cell, parent, false);
                ImageView msgPic =  (ImageView) convertView.findViewById(R.id.sending_img_cell);
                putImageUsingGlide(msgPic, cellItem[position].getMessageText());
            }

            ImageView receiveStatusIv = (ImageView) convertView.findViewById(R.id.receive_status);
            if(context.getClass().getSimpleName().equals(GroupChatActivity.class.getSimpleName())){
                receiveStatusIv.setVisibility(View.GONE);
            } else if(cellItem[position].getRecieved() == Constants.MESSAGE_RECEIVED)
                receiveStatusIv.setImageResource(R.drawable.ic_thumb_up_green);

        } else {

            if (cellItem[position].getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)) {
                convertView = inflater.inflate(R.layout.receiving_message_cell, parent, false);
                setTextMsg(position, convertView);
                setSenderName(convertView, position);
            } else if (cellItem[position].getMessageType().equals(Constants.MESSAGE_TYPE_PIC)) {
                convertView = inflater.inflate(R.layout.receiving_image_message_cell, parent, false);
                setSenderName(convertView, position);
                ImageView msgPic =  (ImageView) convertView.findViewById(R.id.receiving_img_cell);
                putImageUsingGlide(msgPic, cellItem[position].getMessageText());
            }
        }

        if(position == 0){
            TextView dateTime = (TextView) convertView.findViewById(R.id.date_seperator);
            dateTime.setText(cellItem[position].getDateOnly());
            dateTime.setVisibility(View.VISIBLE);
        } else if (!cellItem[position].getDateOnly().equals(cellItem[position-1].getDateOnly())){
            TextView dateTime = (TextView) convertView.findViewById(R.id.date_seperator);
            dateTime.setText(cellItem[position].getDateOnly());
            dateTime.setVisibility(View.VISIBLE);
        }

        TextView dateTime = (TextView) convertView.findViewById(R.id.dateTime);
        dateTime.setText(cellItem[position].getMessageDateTime());

        return convertView;
    }

    private void setTextMsg (int position, View convertView) {
        EmojiconTextView wish = (EmojiconTextView) convertView.findViewById(R.id.wishMessage);
        wish.setUseSystemDefault(true);
        wish.setText(cellItem[position].getMessageText());
    }

    private void putImageUsingGlide (ImageView img, String pic) {
        Glide.with(getContext()).load(pic)
                .apply(new RequestOptions().centerCrop().transform(new RoundedCorners(20)).placeholder(R.drawable.placeholder).error(R.drawable.placeholder))
                //.apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.placeholder))
                .into(img);
    }

    private void setSenderName(View convertView, int position){
        if(context.getClass().getSimpleName().equals(GroupChatActivity.class.getSimpleName())){
            TextView senderName = convertView.findViewById(R.id.sender_id);
            senderName.setText(db.getProfileNameAndPic(cellItem[position].getMessageSender())[0]);
            senderName.setVisibility(View.VISIBLE);
        }
    }


}
package com.example.naziur.androidchat.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.MessageCell;

/**
 * Created by Hamidur on 27/08/2018.
 */

public class MessagesListAdapter extends ArrayAdapter<MessageCell> {
    MessageCell[] cellItem = null;
    Context context;
    public MessagesListAdapter(Context context, MessageCell[] resource) {
        super(context, R.layout.receiving_message_cell, resource);
        // TODO Auto-generated constructor stub
        this.context = context;
        this.cellItem = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();

        if (cellItem[position].getSender()) {
            convertView = inflater.inflate(R.layout.my_message_cell, parent, false);
        } else {
            convertView = inflater.inflate(R.layout.receiving_message_cell, parent, false);
        }

        TextView wish = (TextView) convertView.findViewById(R.id.wishMessage);
        wish.setText(cellItem[position].getMessageText());

        TextView dateTime = (TextView) convertView.findViewById(R.id.dateTime);
        dateTime.setText(cellItem[position].getMessageDateTime());

        return convertView;
    }
}
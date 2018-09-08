package com.example.naziur.androidchat.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.MessageCell;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

        if(position == 0){
            TextView dateTime = (TextView) convertView.findViewById(R.id.date_seperator);
            dateTime.setText(cellItem[position].getDateOnly());
            dateTime.setVisibility(View.VISIBLE);
        } else if (!cellItem[position].getDateOnly().equals(cellItem[position-1].getDateOnly())){
            TextView dateTime = (TextView) convertView.findViewById(R.id.date_seperator);
            dateTime.setText(cellItem[position].getDateOnly());
            dateTime.setVisibility(View.VISIBLE);
        }

        ImageView receiveStatusIv = (ImageView) convertView.findViewById(R.id.receive_status);
        if(cellItem[position].getRecieved() == 1)
            receiveStatusIv.setVisibility(View.VISIBLE);

        TextView wish = (TextView) convertView.findViewById(R.id.wishMessage);
        wish.setText(cellItem[position].getMessageText());

        TextView dateTime = (TextView) convertView.findViewById(R.id.dateTime);
        dateTime.setText(cellItem[position].getMessageDateTime());

        return convertView;
    }

}
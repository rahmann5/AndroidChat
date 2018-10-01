package com.example.naziur.androidchat.adapter;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Hamidur on 14/09/2018.
 */

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener {
        void onButtonClicked(Notification notification, int pos, boolean accept);
    }

    private List<Notification> allMyNotification;

    public OnItemClickListener listener;
    public Context context;

    public NotificationAdapter (Context context, OnItemClickListener listener, List<Notification> currentNotifications) {
        this.context = context;
        allMyNotification = currentNotifications;
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((NotificationViewHolder)holder).bind(allMyNotification.get(position), position, listener, context);
    }

    @Override
    public int getItemCount() {
        return allMyNotification.size();
    }

    public void removeItem(int pos) {
        allMyNotification.remove(pos);
        notifyDataSetChanged();
    }

    private static class NotificationViewHolder extends RecyclerView.ViewHolder {

        private TextView notificationMsg, notificationDate;
        private ImageButton accept, reject;

        public NotificationViewHolder(View itemView) {
            super(itemView);
            notificationMsg = (TextView) itemView.findViewById(R.id.notification_msg);
            notificationDate = (TextView) itemView.findViewById(R.id.notification_date);
            accept = (ImageButton) itemView.findViewById(R.id.notification_accept);
            reject = (ImageButton) itemView.findViewById(R.id.notification_reject);
        }

        void bind(final Notification notification, final int position, final OnItemClickListener listener, Context context) {
            notificationMsg.setText(notification.getSender() + ": " + context.getResources().getString(R.string.invitation_message));
            SimpleDateFormat formatter = new SimpleDateFormat(context.getString(R.string.simple_date));
            String dateString = formatter.format(new Date(notification.getCreatedDateLong()));
            notificationDate.setText(dateString);
            accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onButtonClicked(notification, position, true);// needs doing
                }
            });

            reject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onButtonClicked(notification, position, false);// needs doing
                }
            });
        }
    }

}

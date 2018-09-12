package com.example.naziur.androidchat.adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Naziur on 01/09/2018.
 */

public class AllChatsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private List<Chat> allMyChats;

    public OnItemClickListener listener;
    public Context context;

    public AllChatsAdapter (Context context, OnItemClickListener listener) {
        this.context = context;
        allMyChats = new ArrayList<>();
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_chat, parent, false);
        return new MyChatsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((MyChatsViewHolder) holder).bind(allMyChats.get(position), position ,listener, context);
    }

    @Override
    public int getItemCount() {
        return allMyChats.size();
    }

    public List<Chat> getAllMyChats(){
        return allMyChats;
    }

    public void setAllMyChats(List<Chat> allMyChats){
        this.allMyChats = allMyChats;
        notifyDataSetChanged();
    }

    public void clearAllChats () {
        this.allMyChats.clear();
        notifyDataSetChanged();
    }


    public interface OnItemClickListener {
        void onItemClick (Chat chat, int pos);
        void onItemLongClicked(Chat chat, int pos);
        void onButtonClicked(Chat chat, int pos);
    }

    private static class MyChatsViewHolder extends RecyclerView.ViewHolder {
        User user = User.getInstance();
        private TextView dateTimeTv, profileTv, lastMsgTv;
        private CircleImageView profPicIv;
        private Button addContactBtn;

        public MyChatsViewHolder(View itemView) {
            super(itemView);
            profileTv = (TextView) itemView.findViewById(R.id.prof_name);
            lastMsgTv = (TextView) itemView.findViewById(R.id.last_msg);
            profPicIv = (CircleImageView) itemView.findViewById(R.id.prof_pic);
            dateTimeTv = (TextView) itemView.findViewById(R.id.dateTime);
            addContactBtn = (Button) itemView.findViewById(R.id.add_contact_btn);
        }

        public void bind (final Chat chat, final int position, final OnItemClickListener listener, Context context) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(chat, position);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    listener.onItemLongClicked(chat, position);
                    return true;
                }
            });

            if(chat.getUsernameOfTheOneBeingSpokenTo().equals(chat.getSpeakingTo())) {
                addContactBtn.setVisibility(View.VISIBLE);
                addContactBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onButtonClicked(chat, position);
                    }
                });
            } else {
                addContactBtn.setVisibility(View.GONE);
            }

            profileTv.setText(chat.getSpeakingTo());
            dateTimeTv.setText(chat.getTimeOfMsg());
            lastMsgTv.setText(Constants.generateMediaText(context, chat.getMsgType(), chat.getLastMsgInThisChat()));
            if (user.name.equals(chat.getUsernameOfTheOneBeingSpokenTo())) {
                if (chat.getIsSeen() == Constants.MESSAGE_SENT){
                    lastMsgTv.setTextColor(ContextCompat.getColor(context, R.color.red));
                } else {
                    lastMsgTv.setTextColor(ContextCompat.getColor(context, R.color.black));
                }
            }else {
                lastMsgTv.setTextColor(ContextCompat.getColor(context, R.color.black));
            }

            Glide.with(context).load(chat.getProfilePic()).apply(new RequestOptions().placeholder(R.drawable.unknown).error(R.drawable.unknown)).into(profPicIv);
        }

    }





}

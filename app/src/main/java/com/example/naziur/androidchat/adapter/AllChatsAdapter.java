package com.example.naziur.androidchat.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.Chat;
import com.example.naziur.androidchat.models.MessageCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Naziur on 01/09/2018.
 */

public class AllChatsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private List<Chat> allMyChats;

    public OnItemClickListener listener;

    public AllChatsAdapter (OnItemClickListener listener) {
        allMyChats = new ArrayList<>();
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_contact, parent, false);
        return new MyChatsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((MyChatsViewHolder) holder).bind(allMyChats.get(position), position ,listener);
    }

    @Override
    public int getItemCount() {
        return allMyChats.size();
    }

    public void setAllMyChats(List<Chat> allMyChats){
        this.allMyChats = allMyChats;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick (Chat chat, int pos);
    }

    private static class MyChatsViewHolder extends RecyclerView.ViewHolder {

        private TextView usernameTv, profileTv, lastMsgTv;
        private ImageView profPicIv;

        public MyChatsViewHolder(View itemView) {
            super(itemView);
            profileTv = (TextView) itemView.findViewById(R.id.prof_name);
            lastMsgTv = (TextView) itemView.findViewById(R.id.last_msg);
            profPicIv = (ImageView) itemView.findViewById(R.id.prof_pic);
        }

        public void bind (final Chat chat, final int position, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(chat, position);
                }
            });
            profileTv.setText("@"+chat.getSpeakingTo());
            //profileTv.setVisibility(View.GONE);
            lastMsgTv.setText(chat.getMessageCell().getMessageText());
        }
    }


}

package com.example.naziur.androidchat.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.naziur.androidchat.R;
import com.example.naziur.androidchat.models.FirebaseGroupModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Network;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Hamidur on 27/09/2018.
 */

public class AllGroupsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<FirebaseGroupModel> allMyGroups;
    private Context context;

    public AllGroupsAdapter (Context context) {
        this.allMyGroups = new ArrayList<>();
        this.context = context;
    }

    public void addGroupItem(FirebaseGroupModel model) {
        allMyGroups.add(model);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_my_contact, parent, false);
        return new MyGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((MyGroupViewHolder) holder).bind(context, allMyGroups.get(position));
    }

    @Override
    public int getItemCount() {
        return allMyGroups.size();
    }

    private static class MyGroupViewHolder extends RecyclerView.ViewHolder {
        private User user = User.getInstance();
        private TextView membersTv, titleTv, activeTv;
        private CircleImageView groupPicIv;

        public MyGroupViewHolder(View itemView) {
            super(itemView);
            membersTv = (TextView) itemView.findViewById(R.id.username);
            titleTv = (TextView) itemView.findViewById(R.id.prof_name);
            activeTv = (TextView) itemView.findViewById(R.id.active);
            groupPicIv = (CircleImageView) itemView.findViewById(R.id.prof_pic);
        }

        public void bind (Context context, FirebaseGroupModel group) {
            membersTv.setText(Network.getMembersText(context, group.getMembers().split(","), group.getAdmin()));
            titleTv.setText(group.getTitle());
            Glide.with(context).load(group.getPic()).apply(new RequestOptions().placeholder(R.drawable.placeholder).error(R.drawable.unknown)).into(groupPicIv);
        }



    }
}

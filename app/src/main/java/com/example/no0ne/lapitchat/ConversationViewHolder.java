package com.example.no0ne.lapitchat;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by no0ne on 1/2/18.
 */

public class ConversationViewHolder extends RecyclerView.ViewHolder {

    public View mView;

    public ConversationViewHolder(View itemView) {
        super(itemView);
        mView = itemView;
    }

    public void setMessage(String message, boolean isSeen){
        TextView userStatusView = mView.findViewById(R.id.text_view_user_status);
        userStatusView.setText(message);

        if(!isSeen){
            userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
        } else {
            userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.NORMAL);
        }
    }

    public void setName(String name){
        TextView userNameView = mView.findViewById(R.id.text_view_user_name);
        userNameView.setText(name);
    }

    public void setUserImage(String thumb_image, Context ctx){
        CircleImageView userImageView = mView.findViewById(R.id.circle_image_view_user_list);
        Picasso.with(ctx).load(thumb_image).placeholder(R.drawable.default_image).into(userImageView);
    }

    public void setUserOnline(String onlineStatus) {
        ImageView userOnlineView = mView.findViewById(R.id.image_view_user_online);

        if(onlineStatus.equals("true")){
            userOnlineView.setVisibility(View.VISIBLE);
        } else {
            userOnlineView.setVisibility(View.INVISIBLE);
        }
    }
}

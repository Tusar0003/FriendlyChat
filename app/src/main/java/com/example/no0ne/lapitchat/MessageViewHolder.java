package com.example.no0ne.lapitchat;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by no0ne on 10/13/17.
 */

public class MessageViewHolder extends RecyclerView.ViewHolder {

    public CircleImageView mProfileImage;
    public TextView mMessageText;
    public TextView mNameTextView;
    public ImageView mMessageImageView;

    public MessageViewHolder(View itemView) {
        super(itemView);

        mProfileImage = itemView.findViewById(R.id.circle_image_view_profile);
        mMessageText = itemView.findViewById(R.id.text_view_message);
        mNameTextView = itemView.findViewById(R.id.text_view_display_name);
        mMessageImageView = itemView.findViewById(R.id.image_view_message);
    }


}

package com.example.no0ne.lapitchat;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by no0ne on 9/21/17.
 */

public class UsersViewHolder extends RecyclerView.ViewHolder {

    public View mView;

    public UsersViewHolder(View itemView) {
        super(itemView);
        mView = itemView;
    }

    public void setName(String name) {
        TextView mUserNameTextView = mView.findViewById(R.id.text_view_user_name);
        mUserNameTextView.setText(name);
    }

    public void setStatus(String status) {
        TextView mUserStatusTextView = mView.findViewById(R.id.text_view_user_status);
        mUserStatusTextView.setText(status);
    }

    public void setImage(final String image) {
        final CircleImageView mUserCircleImageView = mView.findViewById(R.id.circle_image_view_user_list);
//        Picasso.with(mView.getContext()).load(image).placeholder(R.drawable.default_image).into(mUserCircleImageView);
        Picasso.with(mView.getContext()).load(image).networkPolicy(NetworkPolicy.OFFLINE)
                .placeholder(R.drawable.default_image).into(mUserCircleImageView, new Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError() {
                Picasso.with(mView.getContext()).load(image).placeholder(R.drawable.default_image).into(mUserCircleImageView);
            }
        });

    }
}

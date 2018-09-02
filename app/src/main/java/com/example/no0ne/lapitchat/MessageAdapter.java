package com.example.no0ne.lapitchat;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by no0ne on 10/13/17.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private List<Messages> mMessageList;

    private DatabaseReference mUserReference;

    public MessageAdapter(List<Messages> mMessageList) {
        this.mMessageList = mMessageList;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_single_layout, parent, false);

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        Messages messages = mMessageList.get(position);
        String fromUser = messages.getFrom();
        String messageType = messages.getType();

        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUser);

        mUserReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String image = dataSnapshot.child("thumb_image").getValue().toString();

                holder.mNameTextView.setText(name);

                Picasso.with(holder.mProfileImage.getContext()).load(image).placeholder(R.drawable.default_image)
                        .into(holder.mProfileImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

//        if (fromUser.equals(currentUserId)) {
//            holder.mMessageText.setBackgroundColor(Color.WHITE);
//            holder.mMessageText.setTextColor(Color.BLACK);
//        } else {
//            holder.mMessageText.setBackgroundResource(R.drawable.message_text_background);
//            holder.mMessageText.setTextColor(Color.WHITE);
//        }

        if (messageType.equals("text")) {
            holder.mMessageText.setText(messages.getMessage());
            holder.mNameTextView.setText(fromUser);
            holder.mMessageImageView.setVisibility(View.INVISIBLE);
        } else {
            holder.mMessageText.setVisibility(View.INVISIBLE);
            Picasso.with(holder.mProfileImage.getContext()).load(messages.getMessage()).placeholder(R.drawable.default_image)
                    .into(holder.mMessageImageView);
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }
}

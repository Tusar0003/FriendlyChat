package com.example.no0ne.lapitchat.Fragments;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.no0ne.lapitchat.ChatActivity;
import com.example.no0ne.lapitchat.Friends;
import com.example.no0ne.lapitchat.ProfileActivity;
import com.example.no0ne.lapitchat.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {

    private RecyclerView mFriendsListRecyclerView;

    private DatabaseReference mFriendsReference;
    private DatabaseReference mUserReference;
    private FirebaseAuth mAuth;

    private String mCurrentUserId;

    private View mMainView;

    public FriendsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_friends, container, false);

        mFriendsListRecyclerView = mMainView.findViewById(R.id.recycler_view_friends_list);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUserId = mAuth.getCurrentUser().getUid();
        mFriendsReference = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrentUserId);
        mFriendsReference.keepSynced(true); // Firebase offline feature
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users");
        mUserReference.keepSynced(true);

        mFriendsListRecyclerView.setHasFixedSize(true);
        mFriendsListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Friends, FriendsViewHolder> friendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(
                Friends.class,
                R.layout.user_single_layout,
                FriendsViewHolder.class,
                mFriendsReference) {
            @Override
            protected void populateViewHolder(final FriendsViewHolder friendsViewHolder, Friends friends, int position) {
                friendsViewHolder.setDate(friends.getDate());

                final String listUserId = getRef(position).getKey();

                mUserReference.child(listUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userThumbImage = dataSnapshot.child("thumb_image").getValue().toString();

                        friendsViewHolder.setName(userName);
                        friendsViewHolder.setImage(userThumbImage);

                        if (dataSnapshot.hasChild("online")) {
                            String userOnlineStatus = dataSnapshot.child("online").getValue().toString();
                            friendsViewHolder.setUserOnlineStatus(userOnlineStatus);
                        }

                        friendsViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CharSequence options[] = new CharSequence[]{"Open Profile", "Send Message"};

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setTitle("Select Options");
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        switch (i) {
                                            case 0:
                                                Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                                profileIntent.putExtra("user_id", listUserId);
                                                startActivity(profileIntent);
                                                break;
                                            case 1:
                                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                                chatIntent.putExtra("user_id", listUserId);
                                                chatIntent.putExtra("user_name", userName);
                                                startActivity(chatIntent);
                                                break;
                                            default:
                                                return;
                                        }
                                    }
                                });

                                builder.show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        };

        mFriendsListRecyclerView.setAdapter(friendsRecyclerViewAdapter);
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {

        public View mView;

        public FriendsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setName(String name) {
            TextView userNameTextView = mView.findViewById(R.id.text_view_user_name);
            userNameTextView.setText(name);
        }

        public void setDate(String date) {
            TextView userStatusTextView = mView.findViewById(R.id.text_view_user_status);
            userStatusTextView.setText(date);
        }

        public void setImage(String image) {
            CircleImageView userImageView = mView.findViewById(R.id.circle_image_view_user_list);
            Picasso.with(mView.getContext()).load(image).placeholder(R.drawable.default_image).into(userImageView);
        }

        public void setUserOnlineStatus(String onlineStatus) {
            ImageView userOnlineImageView = mView.findViewById(R.id.image_view_user_online);

            if (onlineStatus.equals("true")) {
                userOnlineImageView.setVisibility(View.VISIBLE);
            } else {
                userOnlineImageView.setVisibility(View.INVISIBLE);
            }
        }
    }
}

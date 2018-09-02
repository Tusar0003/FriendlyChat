package com.example.no0ne.lapitchat.Fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.no0ne.lapitchat.ConversationViewHolder;
import com.example.no0ne.lapitchat.ProfileActivity;
import com.example.no0ne.lapitchat.R;
import com.example.no0ne.lapitchat.Request;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private static final String TAG = "RequestsFragment";

    private String mCurrentUserId;

    private View mMainView;

    private RecyclerView mRecyclerView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseReference;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_requests, container, false);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            mCurrentUserId = mAuth.getCurrentUser().getUid();
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mRecyclerView = mMainView.findViewById(R.id.recycler_view_friend_request);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Query query = FirebaseDatabase.getInstance().getReference().child("Friend_Request").child(mCurrentUserId);

        FirebaseRecyclerAdapter<Request, ConversationViewHolder> recyclerAdapter = new FirebaseRecyclerAdapter<Request, ConversationViewHolder>(
                Request.class,
                R.layout.user_single_layout,
                ConversationViewHolder.class,
                query
        ) {
            @Override
            protected void populateViewHolder(final ConversationViewHolder viewHolder, final Request model, int position) {
                final String userId = getRef(position).getKey();
                mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);

                mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        viewHolder.setUserImage(dataSnapshot.child("thumb_image").getValue().toString(), getContext());
                        viewHolder.setName(dataSnapshot.child("name").getValue().toString());

                        if (model.getRequest_type().equalsIgnoreCase("sent")) {
                            viewHolder.setMessage("Request sent", true);
                        } else {
                            viewHolder.setMessage("Request received", true);
                        }

                        if (dataSnapshot.hasChild("online")) {
                            String userOnline = dataSnapshot.child("online").getValue().toString();
                            viewHolder.setUserOnline(userOnline);
                        }

                        viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(getContext(), ProfileActivity.class);
                                intent.putExtra("user_id", userId);
                                intent.putExtra("user_name", dataSnapshot.child("name").getValue().toString());
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        };

        mRecyclerView.setAdapter(recyclerAdapter);
    }
}

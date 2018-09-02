package com.example.no0ne.lapitchat;

import android.icu.text.DateFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImageView;
    private TextView mProfileNameTextView;
    private TextView mProfileStatusTextView;
    private TextView mTotalFriendsTextView;
    private Button mSendFriendRequestButton;
    private Button mDeclineFriendRequestButton;

    private String mCurrentState;

    private DatabaseReference mRootReference;
    private DatabaseReference mUserReference;
    private DatabaseReference mFriendRequestReference;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mFriendReference;
    private DatabaseReference mNotificationReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getSupportActionBar().hide();

        mProfileImageView = (ImageView) findViewById(R.id.image_view_profile_image);
        mProfileNameTextView = (TextView) findViewById(R.id.text_view_profile_name);
        mProfileStatusTextView = (TextView) findViewById(R.id.text_view_profile_status);
        mTotalFriendsTextView = (TextView) findViewById(R.id.text_view_total_friends);
        mSendFriendRequestButton = (Button) findViewById(R.id.button_send_friend_request);
        mDeclineFriendRequestButton = (Button) findViewById(R.id.button_decline_friend_request);

        mCurrentState = "not_friend";

        mDeclineFriendRequestButton.setVisibility(View.INVISIBLE);
        mDeclineFriendRequestButton.setEnabled(false);

        final String userId = getIntent().getStringExtra("user_id");

        mRootReference = FirebaseDatabase.getInstance().getReference();
        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);
        mFriendRequestReference = FirebaseDatabase.getInstance().getReference().child("Friend_Request");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mFriendReference = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationReference = FirebaseDatabase.getInstance().getReference().child("Notifications");

        mUserReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mProfileNameTextView.setText(name);
                mProfileStatusTextView.setText(status);

                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.default_image).into(mProfileImageView);

                /**
                 * FRIEND LIST / REQUEST FEATURE
                 *
                 * addListenerForSingleValueEvent is used for retrieving single value
                 */
                mFriendRequestReference.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(userId)) {
                            String requestType = dataSnapshot.child(userId).child("request_type").getValue().toString();

                            if (requestType.equals("received")) {
                                mCurrentState = "request_received";
                                mSendFriendRequestButton.setText(R.string.button_accept_friend_request);

                                mDeclineFriendRequestButton.setVisibility(View.VISIBLE);
                                mDeclineFriendRequestButton.setEnabled(true);
                            } else if (requestType.equals("sent")) {
                                mCurrentState = "request_sent";
                                mSendFriendRequestButton.setText(R.string.button_cancel_friend_request);

                                mDeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                                mDeclineFriendRequestButton.setEnabled(false);
                            }
                        } else {
                            mFriendReference.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(userId)) {
                                        mCurrentState = "friends";
                                        mSendFriendRequestButton.setText(R.string.button_unfriend);

                                        mDeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                                        mDeclineFriendRequestButton.setEnabled(false);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mSendFriendRequestButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                mSendFriendRequestButton.setEnabled(false);

                // NOT FRIEND STATE
                if (mCurrentState.equals("not_friend")) {
                    DatabaseReference newNotificationReference = mRootReference.child("notifications").child(userId).push();

                    String newNotificationId = newNotificationReference.getKey();

                    HashMap<String, String> notificationData = new HashMap<>();
                    notificationData.put("from", mCurrentUser.getUid());
                    notificationData.put("type", "request");

                    Map requestMap = new HashMap();
                    // '/' for getting into another child
                    requestMap.put("Friend_Request/" + mCurrentUser.getUid() + "/" + userId + "/request_type", "sent");
                    requestMap.put("Friend_Request/" + userId + "/" + mCurrentUser.getUid() + "/request_type", "received");
                    requestMap.put("notifications/" + userId + "/" + newNotificationId, notificationData);

                    mRootReference.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Toast.makeText(ProfileActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                            }

                            mSendFriendRequestButton.setEnabled(true);
                            mCurrentState = "request_sent";
                            mSendFriendRequestButton.setText(R.string.button_cancel_friend_request);
                        }
                    });
                }

                // CANCEL REQUEST STATE
                if (mCurrentState.equals("request_sent")) {
                    mFriendRequestReference.child(mCurrentUser.getUid()).child(userId).removeValue()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mFriendRequestReference.child(userId).child(mCurrentUser.getUid()).removeValue()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mSendFriendRequestButton.setEnabled(true);
                                                    mCurrentState = "not_friend";
                                                    mSendFriendRequestButton.setText(R.string.button_send_friend_request);

                                                    mDeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                                                    mDeclineFriendRequestButton.setEnabled(false);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // For handling errors
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // For handling errors.
                                }
                            });
                }

                // REQUEST ACCEPTED STATE
                if (mCurrentState.equals("request_received")) {
                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap = new HashMap();
                    friendsMap.put("Friends/" + mCurrentUser.getUid() + "/" + userId + "/date", currentDate);
                    friendsMap.put("Friends/" + userId + "/" + mCurrentUser.getUid() + "/date", currentDate);

                    friendsMap.put("Friend_Request/" + mCurrentUser.getUid() + "/" + userId, null);
                    friendsMap.put("Friend_Request/" + userId + "/" + mCurrentUser.getUid(), null);

                    mRootReference.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError == null) {
                                mSendFriendRequestButton.setEnabled(true);
                                mCurrentState = "friends";
                                mSendFriendRequestButton.setText(R.string.button_unfriend);

                                mDeclineFriendRequestButton.setEnabled(false);
                                mDeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                // UNFRIEND STATE
                if (mCurrentState.equals("friends")) {
                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + userId, null);
                    unfriendMap.put("Friends/" + userId + "/" + mCurrentUser.getUid(), null);

                    mRootReference.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError == null) {
                                mCurrentState = "not_friend";
                                mSendFriendRequestButton.setText(R.string.button_send_friend_request);

                                mDeclineFriendRequestButton.setEnabled(false);
                                mDeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }

                            mSendFriendRequestButton.setEnabled(true);
                        }
                    });
                }
            }
        });

        mDeclineFriendRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDeclineFriendRequestButton.setEnabled(false);
                mDeclineFriendRequestButton.setVisibility(View.INVISIBLE);

                mSendFriendRequestButton.setEnabled(true);
                mSendFriendRequestButton.setText(R.string.button_send_friend_request);

                mFriendRequestReference.child(mCurrentUser.getUid()).child(userId).removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mFriendRequestReference.child(userId).child(mCurrentUser.getUid()).removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                mSendFriendRequestButton.setEnabled(true);
                                                mCurrentState = "not_friend";
                                                mSendFriendRequestButton.setText(R.string.button_send_friend_request);

                                                mDeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                                                mDeclineFriendRequestButton.setEnabled(false);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // For handling errors
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // For handling errors.
                            }
                        });
            }
        });
    }
}

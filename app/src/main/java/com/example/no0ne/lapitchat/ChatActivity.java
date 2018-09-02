package com.example.no0ne.lapitchat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final int TOTAL_MESSAGE_TO_LOAD = 10;
    private static final int GALLERY_PICK = 1;

    // Loading messages for a single time
    private int mCurrentPage = 1;
    private int mItemPosition = 0;
    private String mLastKey = "";
    private String mPreviousKey = "";

    private DatabaseReference mRootReference;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserPushReference;
    private StorageReference mImageStorage; // Storage Firebase

    private TextView mProfileName;
    private TextView mLastSeen;
    private CircleImageView mProfileImage;
    private EditText mChatMessageEditText;
    private ImageButton mChatAddButton;
    private ImageButton mChatSendButton;
    private RecyclerView mMessagesList;
    private SwipeRefreshLayout mRefreshLayout;

    private String mCurrentUserId;
    private String mChatUser;

    private final List<Messages> mList = new ArrayList<>();

    private LinearLayoutManager mLayoutManager;
    private MessageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true); // For custom action bar

        mRootReference = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mCurrentUserId = mAuth.getCurrentUser().getUid();
        mChatUser = getIntent().getStringExtra("user_id");
        String userName = getIntent().getStringExtra("user_name");

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = inflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(actionBarView);

        mProfileName = (TextView) findViewById(R.id.custom_bar_profile_name);
        mLastSeen = (TextView) findViewById(R.id.custom_bar_last_seen);
        mProfileImage = (CircleImageView) findViewById(R.id.custom_bar_profile_image);
        mChatMessageEditText = (EditText) findViewById(R.id.edit_text_message);
        mChatAddButton = (ImageButton) findViewById(R.id.image_button_add);
        mChatSendButton = (ImageButton) findViewById(R.id.image_button_send);
        mMessagesList = (RecyclerView) findViewById(R.id.recycler_view_messages_list);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mAdapter = new MessageAdapter(mList);
        mLayoutManager = new LinearLayoutManager(this);

        mMessagesList.setHasFixedSize(true);
        mMessagesList.setLayoutManager(mLayoutManager);
        mMessagesList.setAdapter(mAdapter);

        mProfileName.setText(userName);

        mRootReference.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String online = dataSnapshot.child("online").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                if (online.equals("true")) {
                    mLastSeen.setText("Online");
                } else {
                    GetTimeAgo timeAgo = new GetTimeAgo();

                    long lastTime = Long.parseLong(online);
                    String lastSeenTime = timeAgo.getTimeAgo(lastTime, getApplicationContext());

                    mLastSeen.setText(lastSeenTime);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRootReference.child("Chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(mChatUser)) {
                    Map chatMap = new HashMap();
                    chatMap.put("seen", false);
                    chatMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/" + mCurrentUserId + "/" + mChatUser, chatMap);
                    chatUserMap.put("Chat/" + mChatUser + "/" + mCurrentUserId, chatMap);

                    mRootReference.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Log.d("CHAT_LOG", databaseError.getMessage().toString());
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        loadMessages();

        mChatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        mChatAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;
                mItemPosition = 0;

                loadMoreMessages();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();

            final String currentUserReference = "Messages/" + mCurrentUserId + "/" + mChatUser;
            final String chatUserReference = "Messages/" + mChatUser + "/" + mCurrentUserId;

            DatabaseReference userMessagePush = mRootReference.child("Messages").child(mCurrentUserId).child(mChatUser).push();
            final String pushId = userMessagePush.getKey();
            StorageReference filePath = mImageStorage.child("message_images").child(pushId + ".jpg");

            filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        String downloadUrl = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("message", downloadUrl);
                        messageMap.put("seen", false);
                        messageMap.put("type", "image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from", mCurrentUserId);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(currentUserReference + "/" + pushId, messageMap);
                        messageUserMap.put(chatUserReference + "/" + pushId, messageMap);

                        mChatMessageEditText.setText("");

                        mRootReference.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null){
                                    Log.d("CHAT_LOG", databaseError.getMessage().toString());
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void sendMessage() {
        String message = mChatMessageEditText.getText().toString();

        if (!TextUtils.isEmpty(message)) {
            mUserPushReference = mRootReference.child("Messages").child(mCurrentUserId).child(mChatUser).push();

            String currentUserRef = "Messages/" + mCurrentUserId + "/" + mChatUser;
            String chatUserRef = "Messages/" + mChatUser + "/" + mCurrentUserId;
            String pushId = mUserPushReference.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserId);   // The person sending the message

            Map messageUserMap = new HashMap();
            messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
            messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

            mChatMessageEditText.setText(null);

            mRootReference.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Log.d("CHAT_LOG", databaseError.getMessage().toString());
                    }
                }
            });
        }
    }

    private void loadMessages() {
        DatabaseReference messageRef = mRootReference.child("Messages").child(mCurrentUserId).child(mChatUser);

        // It will load only 10 messages
        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_MESSAGE_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);

                mItemPosition++;

                if (mItemPosition == 1) {
                    String messageKey = dataSnapshot.getKey();
                    mLastKey = messageKey;
                    mPreviousKey = messageKey;
                }

                mList.add(messages);
                mAdapter.notifyDataSetChanged();

                mMessagesList.scrollToPosition(mList.size() - 1);

                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadMoreMessages() {
        DatabaseReference messageRef = mRootReference.child("Messages").child(mCurrentUserId).child(mChatUser);

        // It will load only 10 messages
        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();

                if (!mPreviousKey.equals(messageKey)) {
                    mList.add(mItemPosition++, messages);
                } else {
                    mPreviousKey = mLastKey;
                }

                if (mItemPosition == 1) {
                    mLastKey = messageKey;
                }

                mAdapter.notifyDataSetChanged();

//                mMessagesList.scrollToPosition(mList.size() - 1);

                mRefreshLayout.setRefreshing(false);
                mLayoutManager.scrollToPositionWithOffset(10, 0);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}

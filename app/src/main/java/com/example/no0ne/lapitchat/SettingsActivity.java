 package com.example.no0ne.lapitchat;

 import android.content.Intent;
 import android.graphics.Bitmap;
 import android.net.Uri;
 import android.os.Bundle;
 import android.support.annotation.NonNull;
 import android.support.v7.app.AppCompatActivity;
 import android.view.View;
 import android.widget.Button;
 import android.widget.TextView;
 import android.widget.Toast;

 import com.google.android.gms.tasks.OnCompleteListener;
 import com.google.android.gms.tasks.Task;
 import com.google.firebase.auth.FirebaseAuth;
 import com.google.firebase.auth.FirebaseUser;
 import com.google.firebase.database.DataSnapshot;
 import com.google.firebase.database.DatabaseError;
 import com.google.firebase.database.DatabaseReference;
 import com.google.firebase.database.FirebaseDatabase;
 import com.google.firebase.database.ValueEventListener;
 import com.google.firebase.storage.FirebaseStorage;
 import com.google.firebase.storage.StorageReference;
 import com.google.firebase.storage.UploadTask;
 import com.squareup.picasso.Callback;
 import com.squareup.picasso.NetworkPolicy;
 import com.squareup.picasso.Picasso;
 import com.theartofdev.edmodo.cropper.CropImage;

 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.Map;

 import de.hdodenhof.circleimageview.CircleImageView;
 import id.zelory.compressor.Compressor;

 public class SettingsActivity extends AppCompatActivity {

     private static final int GALLERY_PICK = 1;

     private CircleImageView mCircleImageView;
     private TextView mDisplayNameTextView;
     private TextView mStatusTextView;
     private Button mChangeImageButton;
     private Button mChangeStatusButton;

     private View mLoadingIndicator;

     // Firebase
     private DatabaseReference mUserReference;
     private FirebaseUser mCurrentUser;
     private StorageReference mImageStorage;

     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_settings);

         getSupportActionBar().hide();

         mLoadingIndicator = findViewById(R.id.loading_indicator);
         mLoadingIndicator.setVisibility(View.INVISIBLE);

         mCircleImageView = (CircleImageView) findViewById(R.id.circle_image_view);
         mDisplayNameTextView = (TextView) findViewById(R.id.text_view_display_name);
         mStatusTextView = (TextView) findViewById(R.id.text_view_status);
         mChangeImageButton = (Button) findViewById(R.id.button_change_image);
         mChangeStatusButton = (Button) findViewById(R.id.button_change_status);

         mImageStorage = FirebaseStorage.getInstance().getReference();
         mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
         String uid = mCurrentUser.getUid();
         mUserReference = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

         // For offline activity
         mUserReference.keepSynced(true);

         // addValueEventListener is used for retrieving multiple things
         mUserReference.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(DataSnapshot dataSnapshot) {
                 String name = dataSnapshot.child("name").getValue().toString();
                 String status = dataSnapshot.child("status").getValue().toString();
                 final String image = dataSnapshot.child("image").getValue().toString();
                 String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();

                 if (!image.equals("default")) {
//                     Picasso.with(SettingsActivity.this).load(thumb_image).placeholder(R.drawable.default_image).into(mCircleImageView);

                     Picasso.with(SettingsActivity.this).load(image).networkPolicy(NetworkPolicy.OFFLINE)
                             .placeholder(R.drawable.default_image).into(mCircleImageView, new Callback() {
                         @Override
                         public void onSuccess() {

                         }

                         @Override
                         public void onError() {
                             // If can not load the image, then it will load online
                             Picasso.with(SettingsActivity.this).load(image).placeholder(R.drawable.default_image)
                                     .into(mCircleImageView);
                         }
                     });
                 }

                 mDisplayNameTextView.setText(name);
                 mStatusTextView.setText(status);
             }

             @Override
             public void onCancelled(DatabaseError databaseError) {
             }
         });

         mChangeStatusButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 String status = mStatusTextView.getText().toString();

                 Intent intent = new Intent(SettingsActivity.this, StatusActivity.class);
                 intent.putExtra("status", status);
                 startActivity(intent);
             }
         });

         mChangeImageButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent intent = new Intent();
                 intent.setType("image/*");
                 intent.setAction(Intent.ACTION_GET_CONTENT);
                 startActivityForResult(Intent.createChooser(intent, "Select Image"), GALLERY_PICK);
             }
         });
     }

     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);

         if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
             Uri imageUri = data.getData();
             CropImage.activity(imageUri)
                     .setAspectRatio(1, 1)
                     .setMinCropWindowSize(500, 500)
                     .start(this);
         }

         if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
             CropImage.ActivityResult result = CropImage.getActivityResult(data);

             if (resultCode == RESULT_OK) {
                 mLoadingIndicator.setVisibility(View.VISIBLE);

                 Uri imageUri = result.getUri();
                 File thumbFile = new File(imageUri.getPath());

                 String uid = mCurrentUser.getUid();

                 final byte[] thumbByte = compressingImage(thumbFile);

                 StorageReference reference = mImageStorage.child("profile_images").child(uid + ".jpg");
                 final StorageReference thumbReference = mImageStorage.child("profile_images").child("thumbs").child(uid + ".jpg");

                 // Storing the original image to the Firebase Storage.
                 reference.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                     @Override
                     public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                         if (task.isSuccessful()) {
                             // Downloading the original image url.
                             final String imageUrl = task.getResult().getDownloadUrl().toString();

                             UploadTask uploadTask = thumbReference.putBytes(thumbByte);

                             // Storing the compressed image to the Firebase Storage.
                             uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                 @Override
                                 public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                     if (task.isSuccessful()) {
                                         // Downloading the compressed image url.
                                         String thumbUrl = task.getResult().getDownloadUrl().toString();

                                         Map map = new HashMap<String, String>();
                                         map.put("image", imageUrl);
                                         map.put("thumb_image", thumbUrl);

                                         // Storing the original and compressed images url to the database
                                         mUserReference.updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                             @Override
                                             public void onComplete(@NonNull Task<Void> task) {
                                                 if (task.isSuccessful()) {
                                                     mLoadingIndicator.setVisibility(View.GONE);

                                                     Toast.makeText(SettingsActivity.this, "Profile Picture Updated Successfully!", Toast.LENGTH_SHORT)
                                                             .show();
                                                 } else {
                                                     mLoadingIndicator.setVisibility(View.GONE);

                                                     Toast.makeText(SettingsActivity.this, "Profile Picture Updated Failed!", Toast.LENGTH_SHORT)
                                                             .show();
                                                 }
                                             }
                                         });
                                     } else {
                                         mLoadingIndicator.setVisibility(View.GONE);
                                         Toast.makeText(SettingsActivity.this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                                     }
                                 }
                             });
                         } else {
                             mLoadingIndicator.setVisibility(View.GONE);
                             Toast.makeText(SettingsActivity.this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                         }
                     }
                 });
             } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                 Exception error = result.getError();
             }
         }
     }

     // Compressing the original image
     private byte[] compressingImage(File thumbFile) {
         Bitmap thumbBitmap = null;
         try {
             thumbBitmap = new Compressor(this)
                     .setMaxHeight(200)
                     .setMaxWidth(200)
                     .setQuality(50)
                     .compressToBitmap(thumbFile);
         } catch (IOException e) {
             e.printStackTrace();
         }

         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
         byte[] thumbByte = outputStream.toByteArray();

         return thumbByte;
     }
 }

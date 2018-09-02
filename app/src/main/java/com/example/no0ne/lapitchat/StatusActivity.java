package com.example.no0ne.lapitchat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StatusActivity extends AppCompatActivity {

    private EditText mStatusEditText;
    private Button mSaveButton;

    private View mLoadingIndicator;

    private DatabaseReference mDatabase;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        getSupportActionBar().setTitle(R.string.account_status);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLoadingIndicator = findViewById(R.id.loading_indicator);
        mLoadingIndicator.setVisibility(View.INVISIBLE);

        mStatusEditText = (EditText) findViewById(R.id.edit_text_status);
        mSaveButton = (Button) findViewById(R.id.button_save);

        String status = getIntent().getStringExtra("status");
        mStatusEditText.setText(status);

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = mUser.getUid();

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLoadingIndicator.setVisibility(View.VISIBLE);

                String status = mStatusEditText.getText().toString();

                mDatabase.child("status").setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mLoadingIndicator.setVisibility(View.GONE);
                            Toast.makeText(StatusActivity.this, "Status Changed Successfully!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(StatusActivity.this, SettingsActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(StatusActivity.this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}

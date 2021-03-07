package com.example.sandesh.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.sandesh.Adapters.GroupMessageAdapter;
import com.example.sandesh.Adapters.MessageAdapter;
import com.example.sandesh.Model.Message;
import com.example.sandesh.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class GroupChatActivity extends AppCompatActivity {

    GroupMessageAdapter adapter;
    ArrayList<Message> messages;

    ImageView sendButton;
    ImageView attachment;
    TextView messageBox;
    TextView nameToolbar;
    TextView statusToolbar;
    ImageView profileToolbar;
    ImageView left_arrow;
    RecyclerView recyclerView;
    String senderUid;
    Toolbar toolbar;

    URL serverURL;
    JitsiMeetConferenceOptions ondefaultOptions;

    FirebaseDatabase database;
    FirebaseStorage storage;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        sendButton = findViewById(R.id.sendBtn);
        messageBox = findViewById(R.id.messageBox);
        recyclerView = findViewById(R.id.recyclerView);
        attachment = findViewById(R.id.attachment);
        nameToolbar = findViewById(R.id.name);
        statusToolbar = findViewById(R.id.status_typing);
        profileToolbar = findViewById(R.id.profile);
        left_arrow = findViewById(R.id.left_arrow);
        toolbar = findViewById(R.id.toolbar);

        //setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Group Chat");
        // <- back arrow icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        senderUid = FirebaseAuth.getInstance().getUid();

        dialog = new ProgressDialog(this);
        dialog.setMessage("uploading image...");
        dialog.setCancelable(false); // so that dialog will not stop while touching anywhere


        messages = new ArrayList<>();
        adapter = new GroupMessageAdapter(this, messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


        database.getReference().child("public")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for(DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Message message = postSnapshot.getValue(Message.class);
                            message.setMessageId(postSnapshot.getKey());
                            messages.add(message);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageBox.getText().toString().trim();
                Date date = new Date();
                Message message = new Message(messageText, senderUid, date.getTime());
                messageBox.setText("");
                database.getReference().child("public").push()
                        .setValue(message);
            }
        });


        attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,25);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 25) {
            if(data != null) {// some images are present
                if(data.getData() != null) { // some image got select
                    Uri selectedImage = data.getData();

                    // taking time which is unique in millisecond
                    Calendar calendar = Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("chats")
                            .child(calendar.getTimeInMillis()+"");
                    dialog.show();

                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                dialog.dismiss();
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filePath = uri.toString();

                                        String messageText = messageBox.getText().toString().trim();
                                        Date date = new Date();
                                        Message message = new Message(messageText, senderUid, date.getTime());
                                        message.setMessage("photo");
                                        messageBox.setText("");
                                        message.setImageUrl(filePath);

                                        database.getReference().child("public")
                                                .push().setValue(message);

//                                         Toast.makeText(ChatActivity.this,filePath,
//                                                 Toast.LENGTH_SHORT).show() ;
                                    }
                                });
                            }
                        }
                    });


                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
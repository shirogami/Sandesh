package com.example.sandesh.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;


import com.bumptech.glide.Glide;
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

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;




public class ChatActivity extends AppCompatActivity {

    MessageAdapter adapter;
    ArrayList<Message> messages;

    String senderRoom, recieverRoom;
    ImageView sendButton;
    ImageView attachment;
    TextView messageBox;
    TextView nameToolbar;
    TextView statusToolbar;
    ImageView profileToolbar;
    ImageView left_arrow;
    RecyclerView recyclerView;
    String senderUid;
    String recieverUid;
    Toolbar toolbar;

    URL serverURL;
    JitsiMeetConferenceOptions ondefaultOptions;

    FirebaseDatabase database;
    FirebaseStorage storage;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sendButton = findViewById(R.id.sendBtn);
        messageBox = findViewById(R.id.messageBox);
        recyclerView = findViewById(R.id.recyclerView);
        attachment = findViewById(R.id.attachment);
        nameToolbar = findViewById(R.id.name);
        statusToolbar = findViewById(R.id.status_typing);
        profileToolbar = findViewById(R.id.profile);
        left_arrow = findViewById(R.id.left_arrow);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        String name = getIntent().getStringExtra("USER_NAME");
        String profile = getIntent().getStringExtra("USER_IMAGE");



        try {
            serverURL = new URL("https://meet.jit.si");
            ondefaultOptions = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL).setWelcomePageEnabled(false)
                    .build();
            JitsiMeet.setDefaultConferenceOptions(ondefaultOptions);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }



        // set name and profile on tool bar
        nameToolbar.setText(name);
        Glide.with(ChatActivity.this).load(profile)
                .placeholder(R.drawable.avatar).into(profileToolbar);

        // left arrow finishes current activity
        left_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        recieverUid = getIntent().getStringExtra("USER_ID");
        senderUid = FirebaseAuth.getInstance().getUid();


        messages = new ArrayList<>();

        dialog = new ProgressDialog(this);
        dialog.setMessage("uploading image...");
        dialog.setCancelable(false); // so that dialog will not stop while touching anywhere

        // find the status of reciever
        database.getReference().child("presence").child(recieverUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()) {
                            String status = snapshot.getValue(String.class);
                            if(!status.isEmpty()) {
                                if(status.equals("Offline")) {
                                    statusToolbar.setVisibility(View.GONE);
                                }
                                else {
                                    statusToolbar.setText(status);
                                    statusToolbar.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        senderRoom = senderUid + recieverUid;
        recieverRoom = recieverUid + senderUid;

        adapter = new MessageAdapter(this, messages,senderRoom, recieverRoom);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        database.getReference().child("chats").child(senderRoom).child("messages")
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

                // random key is from to make tha id of message in sender chat and
                // reciever chat same so that it can get feelings in both message databses
                // it also helps us in deleting the message from both sender and reciever

                String randomKey = database.getReference().push().getKey();

                // updating last Message and time

                HashMap <String, Object> lastMessageObject = new HashMap<>();
                lastMessageObject.put("lastMsg", message.getMessage());
                lastMessageObject.put("lastMsgTime", date.getTime());

                database.getReference().child("chats").child(senderRoom)
                        .updateChildren(lastMessageObject);
                database.getReference().child("chats").child(recieverRoom)
                        .updateChildren(lastMessageObject);


                database.getReference().child("chats").child(senderRoom)
                        .child("messages").child(randomKey)
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        database.getReference().child("chats").child(recieverRoom)
                                .child("messages").child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {


                            }
                        });

                    }
                });
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

        // identifies whether textbox is typing or not
        final Handler handler = new Handler();
        messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                database.getReference().child("presence").child(senderUid)
                        .setValue("typing ...");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping,1000);

            }

            Runnable userStoppedTyping = new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("presence").child(senderUid)
                            .setValue("Online");
                }
            };
        });


        // used to hide title (sandesh from tool bar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // set name as title
         //getSupportActionBar().setTitle(name);
        // set back button (<-)
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

                                             // random key is from to make tha id of message in sender chat and
                                             // reciever chat same so that it can get feelings in both message databses
                                             // it also helps us in deleting the message from both sender and reciever

                                             String randomKey = database.getReference().push().getKey();

                                             // updating last Message and time

                                             HashMap <String, Object> lastMessageObject = new HashMap<>();
                                             lastMessageObject.put("lastMsg", message.getMessage());
                                             lastMessageObject.put("lastMsgTime", date.getTime());

                                             database.getReference().child("chats").child(senderRoom)
                                                     .updateChildren(lastMessageObject);
                                             database.getReference().child("chats").child(recieverRoom)
                                                     .updateChildren(lastMessageObject);


                                             database.getReference().child("chats").child(senderRoom)
                                                     .child("messages").child(randomKey)
                                                     .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                 @Override
                                                 public void onSuccess(Void aVoid) {

                                                     database.getReference().child("chats").child(recieverRoom)
                                                             .child("messages").child(randomKey)
                                                             .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                         @Override
                                                         public void onSuccess(Void aVoid) {


                                                         }
                                                     });

                                                  }
                                             });
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
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");

    }

    // onclick ( <- ) we cam move from chat activity to main activity
    @Override
    public boolean onSupportNavigateUp() {
        // backs to main activity
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.video_call :
                videoCall();
                break;

            case R.id.settings :
                Toast.makeText(this, "Settings Clicked",
                        Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void videoCall() {
        String code = maxString(senderUid, recieverUid) + minString(senderUid, recieverUid);

        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                .setRoom(code)
                .setWelcomePageEnabled(false)
                .build();

        JitsiMeetActivity.launch(ChatActivity.this, options);
    }

    String maxString(String a, String b) {
        if(a.compareTo(b) > 0) {
            return a;
        }
        else{
            return b;
        }
    }
    String minString(String a, String b) {
        if(a.compareTo(b) < 0) {
            return a;
        }
        else{
            return b;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu_chat, menu);
        return super.onCreateOptionsMenu(menu);

    }
}
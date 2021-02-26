  package com.example.sandesh.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.sandesh.Activities.ChatActivity;
import com.example.sandesh.R;
import com.example.sandesh.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    Context context;
    ArrayList<User> users;
    public UsersAdapter(Context context, ArrayList<User> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public UsersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_conversation,parent,false);

        return new ViewHolder(view,context);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersAdapter.ViewHolder viewholder, int position) {
        User user = users.get(position);

        String senderId = FirebaseAuth.getInstance().getUid();
        String senderRoom = senderId+ user.getUserid();
    
        FirebaseDatabase.getInstance().getReference().child("chats").
                child(senderRoom).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    String lastMsg = snapshot.child("lastMsg").getValue(String.class);
                    long time = snapshot.child("lastMsgTime").getValue(Long.class);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
                    viewholder.msgTime.setText(dateFormat.format(new Date(time)));
                    viewholder.lastMessage.setText(lastMsg);
                }
                else{
                    viewholder.lastMessage.setText("Tap to chat");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        viewholder.userName.setText(user.getName());

        // if profile is not present it shows avatar image instead of no image

        Glide.with(context).load(user.getProfileImage())
                .placeholder(R.drawable.avatar)
                .into(viewholder.profile);

        viewholder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("USER_NAME",user.getName());
                intent.putExtra("USER_IMAGE",user.getProfileImage());
                intent.putExtra("USER_ID",user.getUserid());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView  userName;
        TextView  lastMessage;
        TextView  msgTime;
        ImageView profile;

        public ViewHolder(@NonNull View itemView,Context ctx) {
            super(itemView);
            context = ctx;
            userName = itemView.findViewById(R.id.user_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            msgTime = itemView.findViewById(R.id.msg_time);
            profile = itemView.findViewById(R.id.profile);
        }
    }
}

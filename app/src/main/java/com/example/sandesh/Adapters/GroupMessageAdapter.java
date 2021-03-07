 package com.example.sandesh.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.sandesh.Model.Message;
import com.example.sandesh.Model.User;
import com.example.sandesh.R;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

 public class GroupMessageAdapter extends RecyclerView.Adapter{
     private Context context;
     ArrayList<Message> messages;
     final int ITEM_SENT = 1;
     final int ITEM_RECIEVE = 2;
     boolean isTouch = false;

     public GroupMessageAdapter(Context context, ArrayList<Message> messages) {
         this.context = context;
         this.messages = messages;

     }

     @NonNull
     @Override
     public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
         if(viewType == ITEM_SENT) {
             View view = LayoutInflater.from(context).inflate(R.layout.item_sent_group,parent,false);
             return new SentViewHolder(view);
         }
         else {
             View view = LayoutInflater.from(context).inflate(R.layout.item_recieve_group,parent,false);
             return new RecievrHolder(view);
         }

     }

     @Override
     public int getItemViewType(int position) {
         Message message = messages.get(position);
         Log.d("MESSAGE_ID", "getItemViewType: "+ message.getSenderId());
         Log.d("MESSAGE_ID", "getItemViewType: "+ FirebaseAuth.getInstance().getUid());
         if(FirebaseAuth.getInstance().getUid().equals(message.getSenderId())) {
             return ITEM_SENT;
         }else{
             return ITEM_RECIEVE;
         }

     }

     @Override
     public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
         Message message = messages.get(position);

         int reactions[] = new int[]{ // array of feeling icons
                 R.drawable.ic_fb_like,
                 R.drawable.ic_fb_love,
                 R.drawable.ic_fb_laugh,
                 R.drawable.ic_fb_sad,
                 R.drawable.ic_fb_angry,
                 R.drawable.ic_fb_cry,
                 R.drawable.ic_fb_dead,
                 R.drawable.ic_fb_embarrass,
                 R.drawable.ic_fb_joy,
                 R.drawable.ic_fb_sleepy,
                 R.drawable.ic_fb_surprise

         };

         // code copied form https://github.com/pgreze/android-reactions
         ReactionsConfig config = new ReactionsConfigBuilder(context)
                 .withReactions(reactions)
                 .build();

         ReactionPopup popup = new ReactionPopup(context, config, (pos) -> {

             if(holder.getClass() == SentViewHolder.class) {
                 SentViewHolder viewHolder = (SentViewHolder)holder;
                 viewHolder.feeling_sent.setImageResource(reactions[pos]);
                 viewHolder.feeling_sent.setVisibility(View.VISIBLE);
             }
             else{
                 RecievrHolder viewHolder = (RecievrHolder)holder;
                 viewHolder.feeling_recieve.setImageResource(reactions[pos]);
                 viewHolder.feeling_recieve.setVisibility(View.VISIBLE);
             }

             message.setFeeling(pos);

             FirebaseDatabase.getInstance().getReference().child("public")
                     .child(message.getMessageId()).setValue(message);



             return true; // true is closing popup, false is requesting a new selection
         });


         if(holder.getClass() == SentViewHolder.class) {
             SentViewHolder viewHolder = (SentViewHolder)holder;

             if(message.getMessage().equals("photo")) {
                 viewHolder.image_sent.setVisibility(View.VISIBLE);
                 viewHolder.message_sent.setVisibility(View.GONE );
                 Glide.with(context).load(message.getImageUrl())
                         .placeholder(R.drawable.placeholder).into(viewHolder.image_sent);
             }

             if(message.getSenderId() !=null) {
                 FirebaseDatabase.getInstance().getReference().child("users")
                         .child(message.getSenderId())
                         .addListenerForSingleValueEvent(new ValueEventListener() {
                             @Override
                             public void onDataChange(@NonNull DataSnapshot snapshot) {
                                 if (snapshot.exists()) {
                                     User user = snapshot.getValue(User.class);
                                     viewHolder.name.setText("@ " + user.getName());
                                 }
                             }

                             @Override
                             public void onCancelled(@NonNull DatabaseError error) {

                             }
                         });
             }

             viewHolder.message_sent.setText(message.getMessage());

             if(message.getFeeling() >=0 ) {
                 //message.setFeeling(reactions[(int) message.getFeeling()]);
                 viewHolder.feeling_sent.setImageResource(reactions[message.getFeeling()]);
                 viewHolder.feeling_sent.setVisibility(View.VISIBLE);
             }
             else {
                 viewHolder.feeling_sent.setVisibility(View.GONE);
             }

             viewHolder.message_sent.setOnTouchListener(new View.OnTouchListener() {
                 @Override
                 public boolean onTouch(View v, MotionEvent event) {
                     if(event.getAction() == MotionEvent.ACTION_MOVE) {
                         popup.onTouch(v,event);
                         isTouch = true;
                     }
                     if(event.getAction()==MotionEvent.ACTION_UP){
                         isTouch = false;
                     }

                     return false;
                 }
             });

             viewHolder.image_sent.setOnTouchListener(new View.OnTouchListener() {
                 @Override
                 public boolean onTouch(View v, MotionEvent event) {
                     popup.onTouch(v, event);
                     return false;
                 }
             });


             viewHolder.message_sent.setOnLongClickListener(new View.OnLongClickListener() {
                 @Override
                 public boolean onLongClick(View v) {
                     if (!isTouch) {
                         View view = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                         Button delete, everyone, cancel;
                         AlertDialog dialog = new AlertDialog.Builder(context)
                                 .setTitle("Delete Message")
                                 .setView(R.layout.delete_dialog)
                                 .create();
                         Log.d("TAG", "onLongClick: " + "Ashish");
                         delete = view.findViewById(R.id.delete);
                         everyone = view.findViewById(R.id.everyone);
                         cancel = view.findViewById(R.id.cancel);

                         dialog.setView(view);
                         dialog.show();

                         everyone.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 message.setMessage("This message is removed.");
                                 message.setFeeling(-1);
                                 FirebaseDatabase.getInstance().getReference()
                                         .child("public")
                                         .child(message.getMessageId()).setValue(message);

                                 dialog.dismiss();
                             }
                         });

                         delete.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 FirebaseDatabase.getInstance().getReference()
                                         .child("public")
                                         .child(message.getMessageId()).setValue(null);
                                 dialog.dismiss();
                             }
                         });

                         cancel.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 dialog.dismiss();
                             }
                         });

                         dialog.show();
                     }
                     return  true;
                 }
             });




         }
         else{
             RecievrHolder viewHolder = (RecievrHolder)holder;

             if(message.getMessage().equals("photo")) {
                 viewHolder.image_recieve.setVisibility(View.VISIBLE);
                 viewHolder.message_recieve.setVisibility(View.GONE );
                 Glide.with(context).load(message.getImageUrl())
                         .placeholder(R.drawable.placeholder).into(viewHolder.image_recieve);
             }

             if(message.getSenderId() !=null) {
                 FirebaseDatabase.getInstance().getReference().child("users")
                         .child(message.getSenderId())
                         .addListenerForSingleValueEvent(new ValueEventListener() {
                             @Override
                             public void onDataChange(@NonNull DataSnapshot snapshot) {
                                 if (snapshot.exists()) {
                                     User user = snapshot.getValue(User.class);
                                     viewHolder.name.setText("@ " + user.getName());
                                 }
                             }

                             @Override
                             public void onCancelled(@NonNull DatabaseError error) {

                             }
                         });
             }

             viewHolder.message_recieve.setText(message.getMessage());

             if(message.getFeeling() >=0 ) {
                 //message.setFeeling(reactions[(int) message.getFeeling()]);
                 viewHolder.feeling_recieve.setImageResource(reactions[message.getFeeling()]);
                 viewHolder.feeling_recieve.setVisibility(View.VISIBLE);
             }
             else {
                 viewHolder.feeling_recieve.setVisibility(View.GONE);
             }


             viewHolder.message_recieve.setOnTouchListener(new View.OnTouchListener() {
                 @Override
                 public boolean onTouch(View v, MotionEvent event) {
                     if(event.getAction() == MotionEvent.ACTION_MOVE) {
                         popup.onTouch(v,event);
                         isTouch = true;
                     }
                     if(event.getAction()==MotionEvent.ACTION_UP){
                         isTouch = false;
                     }

                     return false;
                 }
             });

             viewHolder.image_recieve.setOnTouchListener(new View.OnTouchListener() {
                 @Override
                 public boolean onTouch(View v, MotionEvent event) {
                     popup.onTouch(v, event);
                     return false;
                 }
             });

             viewHolder.message_recieve.setOnLongClickListener(new View.OnLongClickListener() {
                 @Override
                 public boolean onLongClick(View v) {
                     if (!isTouch) {
                         View view = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                         Button delete, everyone, cancel;
                         delete = view.findViewById(R.id.delete);
                         everyone = view.findViewById(R.id.everyone);
                         cancel = view.findViewById(R.id.cancel);

                         AlertDialog dialog = new AlertDialog.Builder(context)
                                 .setTitle("Delete Message")
                                 .setView(R.layout.delete_dialog)
                                 .create();

                         dialog.setView(view);
                         dialog.show();


                         everyone.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 message.setMessage("This message is removed.");
                                 message.setFeeling(-1);
                                 FirebaseDatabase.getInstance().getReference()
                                         .child("public")
                                         .child(message.getMessageId()).setValue(message);
                                 dialog.dismiss();
                             }
                         });

                         delete.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 FirebaseDatabase.getInstance().getReference()
                                         .child("public")
                                         .child(message.getMessageId()).setValue(null);
                                 dialog.dismiss();
                             }
                         });

                         cancel.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 dialog.dismiss();
                             }
                         });

                         dialog.show();


                     }
                     return true;
                 }
             });



         }
     }

     @Override
     public int getItemCount() {
         return messages.size();
     }

     public class SentViewHolder extends RecyclerView.ViewHolder {
         TextView message_sent;
         TextView name;
         ImageView feeling_sent;
         ImageView image_sent;
         public SentViewHolder(@NonNull View itemView) {
             super(itemView);
             message_sent = itemView.findViewById(R.id.message_sent);
             name = itemView.findViewById(R.id.name);
             feeling_sent = itemView.findViewById(R.id.feeling_sent);
             image_sent = itemView.findViewById(R.id.image_sent);
         }
     }

     public class RecievrHolder extends RecyclerView.ViewHolder {
         TextView message_recieve;
         TextView name;
         ImageView feeling_recieve;
         ImageView image_recieve;
          public RecievrHolder(@NonNull View itemView) {
              super(itemView);
              message_recieve = itemView.findViewById(R.id.message_recieve);
              name = itemView.findViewById(R.id.name);
              feeling_recieve = itemView.findViewById(R.id.feeling_recieve);
              image_recieve = itemView.findViewById(R.id.image_recieve);

         }
     }
 }
 package com.example.sandesh.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.sandesh.Model.Message;
import com.example.sandesh.R;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MessageAdapter extends RecyclerView.Adapter{
    private Context context;
    private ArrayList<Message> messages;
    String senderRoom, recieverRoom;

    final int ITEM_SENT = 1;
    final int ITEM_RECIEVE = 2;

    public MessageAdapter(Context context, ArrayList<Message> messages, String senderRoom, String recieverRoom) {
        this.context = context;
        this.messages = messages;
        this.senderRoom = senderRoom;
        this.recieverRoom = recieverRoom;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == ITEM_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_sent,parent,false);
            return new SentViewHolder(view);
        }
        else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_recieve,parent,false);
            return new RecievrHolder(view);
        }

    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
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

            FirebaseDatabase.getInstance().getReference().child("chats").child(senderRoom)
                    .child("messages").child(message.getMessageId()).setValue(message);

            FirebaseDatabase.getInstance().getReference().child("chats").child(recieverRoom)
                    .child("messages").child(message.getMessageId()).setValue(message);


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
                    popup.onTouch(v,event);
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

        }
        else{
            RecievrHolder viewHolder = (RecievrHolder)holder;

            if(message.getMessage().equals("photo")) {
                viewHolder.image_recieve.setVisibility(View.VISIBLE);
                viewHolder.message_recieve.setVisibility(View.GONE );
                Glide.with(context).load(message.getImageUrl())
                        .placeholder(R.drawable.placeholder).into(viewHolder.image_recieve);
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
                    popup.onTouch(v,event);
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
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class SentViewHolder extends RecyclerView.ViewHolder {
        TextView message_sent;
        ImageView feeling_sent;
        ImageView image_sent;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            message_sent = itemView.findViewById(R.id.message_sent);
            feeling_sent = itemView.findViewById(R.id.feeling_sent);
            image_sent = itemView.findViewById(R.id.image_sent);
        }
    }

    public class RecievrHolder extends RecyclerView.ViewHolder {
        TextView message_recieve;
        ImageView feeling_recieve;
        ImageView image_recieve;
         public RecievrHolder(@NonNull View itemView) {
             super(itemView);
             message_recieve = itemView.findViewById(R.id.message_recieve);
             feeling_recieve = itemView.findViewById(R.id.feeling_recieve);
             image_recieve = itemView.findViewById(R.id.image_recieve);

        }
    }
}
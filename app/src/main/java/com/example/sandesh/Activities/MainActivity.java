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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.sandesh.Adapters.TopStatusAdapter;
import com.example.sandesh.Adapters.UsersAdapter;
import com.example.sandesh.Model.Status;
import com.example.sandesh.Model.User;
import com.example.sandesh.Model.UserStatus;
import com.example.sandesh.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase database;
    ArrayList<User> users;
    UsersAdapter usersAdapter;
    RecyclerView recyclerView;
    RecyclerView statusList;
    TopStatusAdapter topStatusAdapter;
    ArrayList<UserStatus> userStatuses;
    BottomNavigationView bottomNavigationView;

    User user;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = FirebaseDatabase.getInstance();
        users = new ArrayList<>();
        userStatuses = new ArrayList<>();
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        dialog = new ProgressDialog(this);
        dialog.setMessage("uploading image..");
        dialog.setCancelable(false);

        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        user = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });



        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        statusList = findViewById(R.id.statusList);
        statusList.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        statusList.setLayoutManager(layoutManager);


        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for(DataSnapshot datasnapshot : snapshot.getChildren()) {
                    User user = datasnapshot.getValue(User.class);
                    if (!user.getUserid().equals(FirebaseAuth.getInstance().getUid())) {
                        users.add(user);

                    }
                    Log.d("USER", "onDataChange: "+user.getName());
                }

                usersAdapter = new UsersAdapter(MainActivity.this,users);
                topStatusAdapter = new TopStatusAdapter(MainActivity.this,userStatuses);

                statusList.setAdapter(topStatusAdapter);

                recyclerView.setAdapter(usersAdapter);
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    for(DataSnapshot storySnapshot : snapshot.getChildren()) {
                        userStatuses.clear();
                        UserStatus status = new UserStatus();
                        status.setName(storySnapshot.child("name").getValue(String.class));
                        status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                        status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));

                        ArrayList<Status> statuses = new ArrayList<>();
                        for(DataSnapshot statusSnapShot : storySnapshot.child("statuses").getChildren()) {
                            Status sampleStatus = statusSnapShot.getValue(Status.class);
                            statuses.add(sampleStatus);
                        }

                        status.setStatuses(statuses);
                        userStatuses.add(status);
                    }

                    topStatusAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.status:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(intent,75);
                        break;
                }
                return false;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null) {

            // if image is selected
            if (data.getData() != null) {
                dialog.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                Date date = new Date();
                StorageReference reference = storage.getReference().child(date.getTime()+"");

                reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()) {
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    dialog.dismiss();

                                    UserStatus userStatus = new UserStatus();
                                    userStatus.setName(user.getName());
                                    user.setProfileImage(user.getProfileImage());
                                    userStatus.setLastUpdated(date.getTime());

                                    // updates statuses

                                    HashMap<String, Object> map = new HashMap<>();
                                    map.put("name", userStatus.getName());
                                    map.put("profileImage", userStatus.getProfileImage());
                                    map.put("lastUpdated", userStatus.getLastUpdated());

                                    String imageUrl = uri.toString();
                                    Status status = new Status(imageUrl, userStatus.getLastUpdated());


                                    database.getReference().child("stories")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .updateChildren(map);

                                    database.getReference().child("stories")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .child("statuses").push().setValue(status);

                                }
                            });
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        String currentId = FirebaseAuth.getInstance().getUid();
//        database.getReference().child("presence").child(currentId).setValue("Offline");
//
//    }

    //    @Override
//    protected void onStop() {
//
//
//    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search :
                Toast.makeText(this, "Search Clicked",
                        Toast.LENGTH_SHORT).show();
                break;

            case R.id.settings :
                Toast.makeText(this, "Settings Clicked",
                        Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // add top menu to main activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);
        return super.onCreateOptionsMenu(menu);

    }
}
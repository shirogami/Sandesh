package com.example.sandesh.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.sandesh.R;
import com.google.firebase.auth.FirebaseAuth;


public class PhoneNumberActivity extends AppCompatActivity {

    private EditText phoneBox;
    private Button continueButton;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number);
        phoneBox = findViewById(R.id.nameBox);
        continueButton = findViewById(R.id.continue_button);
        auth = FirebaseAuth.getInstance();

        if(auth.getCurrentUser() !=null) {
            Intent intent = new Intent(PhoneNumberActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        getSupportActionBar().hide();
        phoneBox.requestFocus();

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhoneNumberActivity.this,OTPActivity.class);
                intent.putExtra("phoneNumber",phoneBox.getText().toString());
                startActivity(intent);
            }
        });
    }
}
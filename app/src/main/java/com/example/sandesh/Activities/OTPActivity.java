package com.example.sandesh.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.example.sandesh.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;


import java.util.concurrent.TimeUnit;

public class OTPActivity extends AppCompatActivity {

    private EditText otpView;
    private TextView phonelbl;
    private Button continueButton;
    private FirebaseAuth auth;
    private String verificationId;
    private String phoneNumber;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_o_t_p);
        auth = FirebaseAuth.getInstance();
        getSupportActionBar().hide();

        otpView = findViewById(R.id.otpView);
        continueButton = findViewById(R.id.continue_button);
        phonelbl = findViewById(R.id.phonelbl);

        phoneNumber = getIntent().getStringExtra("phoneNumber");
        phoneNumber = "+91"+phoneNumber;
        phonelbl.setText("Verify "+phoneNumber);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Sending OTP....");
        dialog.setCancelable(false);
        dialog.show();

        sendVerificationCodeToUser();

        continueButton.setOnClickListener(v -> {

            // sendVerifactionCodeToUser, we can get otp which we type in otpView field
            // so access in this continueButton.setOnClickListener not globally
            String code =otpView.getText().toString();

            Log.d("TAG", "onCreate: "+verificationId);
            Log.d("TAG2", "onCreate: "+code);
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithPhoneAuthCredential(credential);
        });

    }


    private void sendVerificationCodeToUser() {

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks()
                {
                    @Override
                    public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken)
                    {
                        verificationId=s;
                        dialog.dismiss();
                        Log.d("TAG ", "onCodeSent: "+s);
                    }

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential)
                    {
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                        Log.d("TAG", "onVerificationFailed: "+e.getMessage());
                    }
                });


    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if(task.isSuccessful()) {
                        Toast.makeText(OTPActivity.this,"Logged in",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(OTPActivity.this,SetupProfileActivity.class);
                        startActivity(intent);
                        finishAffinity(); // finished all previous activities
                    }
                    else{
                        Toast.makeText(OTPActivity.this,"Logged Failed",Toast.LENGTH_SHORT).show();

                    }
                });
    }



}
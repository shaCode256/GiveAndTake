package com.example.giveandtake;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class VerifyPhone extends AppCompatActivity {
    EditText phone, otp;
    Button btngenOTP, btnverify;
    FirebaseAuth mAuth;
    String verificationID;
    ProgressBar bar;
    String fullNameTxt;
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);
        EditText fullName = findViewById(R.id.fullname);
        phone = findViewById(R.id.phone);
        otp = findViewById(R.id.otp);
        btngenOTP = findViewById(R.id.btngenerateOTP);
        btnverify = findViewById(R.id.btnverifyOTP);
        mAuth = FirebaseAuth.getInstance();
        bar = findViewById(R.id.bar);
        btngenOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullNameTxt = fullName.getText().toString();
                if (TextUtils.isEmpty(phone.getText().toString()) || fullNameTxt.isEmpty()) {
                    Toast.makeText(VerifyPhone.this, "Enter Valid Phone No. and name", Toast.LENGTH_SHORT).show();
                } else {
                    String number = phone.getText().toString();
                    bar.setVisibility(View.VISIBLE);
                    sendverificationcode(number);
                }
            }
        });
        btnverify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(otp.getText().toString())) {
                    Toast.makeText(VerifyPhone.this, "Wrong OTP Entered", Toast.LENGTH_SHORT).show();
                } else
                    verifycode(otp.getText().toString());
            }
        });
    }

    private void sendverificationcode(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+972 "+phoneNumber.substring(1))  // Phone number to verify. converts 0501234567 to +972 501234567
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks
            mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            final String code = credential.getSmsCode();
            if (code != null) {
                verifycode(code);
            }
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            e.printStackTrace();
            Toast.makeText(VerifyPhone.this, "Verification Failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeSent(@NonNull String s,
                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
            super.onCodeSent(s, token);
            verificationID = s;
            Toast.makeText(VerifyPhone.this, "Code sent", Toast.LENGTH_SHORT).show();
            btnverify.setEnabled(true);
            bar.setVisibility(View.INVISIBLE);
        }
    };

    private void verifycode(String Code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationID, Code);
        signinbyCredentials(credential);
    }

    private void signinbyCredentials(PhoneAuthCredential credential) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(VerifyPhone.this, "Phone verified Successfully", Toast.LENGTH_SHORT).show();
                            databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    //check if phone is not registered before
                                    if(snapshot.hasChild(phone.getText().toString())){
                                        Toast.makeText(VerifyPhone.this, "Phone is already registered", Toast.LENGTH_SHORT).show();
                                    }
                                    else{
                                        Intent thisIntent= getIntent();
                                        String emailTxt= thisIntent.getStringExtra("emailTxt");
                                        registerUser(emailTxt, phone.getText().toString(), fullNameTxt);
                                        //show a success message and then finish the activity
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                    }
                });
    }


    private void registerUser(String emailTxt, String phoneTxt, String fullNameTxt) {
        if (emailTxt.endsWith("@manager.com") || emailTxt.endsWith("@msmail.ariel.ac.il")) {
            if (emailTxt.endsWith("@manager.com")) {
                databaseReference.child("users").child(phoneTxt).child("isManager").setValue("1");
            } else {
                databaseReference.child("users").child(phoneTxt).child("isManager").setValue("0");
            }
            databaseReference.child("users").child(phoneTxt).child("fullName").setValue(fullNameTxt);
            databaseReference.child("users").child(phoneTxt).child("isPhoneVerified").setValue("1");
            databaseReference.child("users").child(phoneTxt).child("email").setValue(emailTxt);
            databaseReference.child("users").child(phoneTxt).child("isBlocked").setValue("0");
            Toast.makeText(VerifyPhone.this, "User is successfully registered!", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(VerifyPhone.this, Login.class);
            startActivity(loginIntent);
        } else {
            Toast.makeText(VerifyPhone.this, "Can register only with ariel university or admin email", Toast.LENGTH_SHORT).show();
        }
    }

}

//Reference:
// https://www.youtube.com/watch?v=BWseVs2MXaI
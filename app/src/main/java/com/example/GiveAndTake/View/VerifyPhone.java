package com.example.giveandtake.View;
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

import com.example.giveandtake.Presenter.RegisterUser;
import com.example.giveandtake.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class VerifyPhone extends AppCompatActivity {
    EditText phone, otp;
    Button btngenOTP, btnverify;
    FirebaseAuth mAuth;
    String verificationID;
    ProgressBar bar;
    String fullNameTxt;

    String IPv4_Address= "http://10.0.0.3:8000/";

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
        btngenOTP.setOnClickListener(v -> {
            fullNameTxt = fullName.getText().toString();
            if (TextUtils.isEmpty(phone.getText().toString()) || fullNameTxt.isEmpty()) {
                Toast.makeText(VerifyPhone.this, "Enter Valid Phone No. and name", Toast.LENGTH_SHORT).show();
            } else {
                String number = phone.getText().toString();
                bar.setVisibility(View.VISIBLE);
                sendVerificationCode(number);
            }
        });
        btnverify.setOnClickListener(v -> {
            if (TextUtils.isEmpty(otp.getText().toString())) {
                Toast.makeText(VerifyPhone.this, "Wrong OTP Entered", Toast.LENGTH_SHORT).show();
            } else
                verifyCode(otp.getText().toString());
        });
    }

    private void sendVerificationCode(String phoneNumber) {
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
                verifyCode(code);
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

    private void verifyCode(String Code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationID, Code);
        signInByCredentials(credential);
    }

    private void signInByCredentials(PhoneAuthCredential credential) {
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
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                        else{
                            Toast.makeText(VerifyPhone.this, "Wrong code", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void registerUser(String emailTxt, String phoneTxt, String fullNameTxt) {
            RegisterUser registerUserPresenter= new RegisterUser();
            registerUserPresenter.registerUser(emailTxt, phoneTxt, fullNameTxt, databaseReference);
            Toast.makeText(VerifyPhone.this, "User is successfully registered, \nnow log in with your phone!", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(VerifyPhone.this, Login.class);
            startActivity(loginIntent);
    }

    public void checkIfPhoneExist(String phone) throws InterruptedException {
        new Thread(() -> {
            String urlString = IPv4_Address+"getDoesPhoneExist/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("phone", phone);
                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("error2");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                InputStream is = conn.getInputStream();
                String isExist= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("isNotificationTurnedOn? "+isExist);
                //  latch.countDown();
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
            }
        }).start();
    }


}

//Reference:
// https://www.youtube.com/watch?v=BWseVs2MXaI
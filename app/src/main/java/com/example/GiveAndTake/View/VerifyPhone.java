package com.example.giveandtake.View;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class VerifyPhone extends AppCompatActivity {
    EditText phone, otp;
    Button btngenOTP, btnverify;
    FirebaseAuth mAuth;
    String verificationID;
    ProgressBar bar;
    String fullNameTxt;

   String server_url= "https://giveandtake-server.df.r.appspot.com/";

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
                            Intent thisIntent= getIntent();
                            String emailTxt= thisIntent.getStringExtra("emailTxt");
                            addUserToDb(emailTxt, String.valueOf(phone.getText()), fullNameTxt);
                        }
                        else{
                            Toast.makeText(VerifyPhone.this, "Wrong code", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    public void addUserToDb(String email, String phone, String name) {
        new Thread(() -> {
            String urlString = server_url +"addUserToDb/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                VerifyPhone.this.runOnUiThread(() -> Toast.makeText(VerifyPhone.this, "Server is down, can't proccess the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                assert url != null;
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("name", name);
                json.put("phone", phone);

                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("error2");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                assert conn != null;
                InputStream is = conn.getInputStream();
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("result is: "+ result);
                result = result.replaceAll("\"", "");
                switch (result) {
                    case "Phone already is registered.":
                        VerifyPhone.this.runOnUiThread(() -> Toast.makeText(VerifyPhone.this, "Phone already is registered. please contact admin.", Toast.LENGTH_LONG).show());
                        break;
                    case "success":
                        VerifyPhone.this.runOnUiThread(() -> Toast.makeText(VerifyPhone.this, "Successfully registered! Now, please log in with your phone.", Toast.LENGTH_LONG).show());
                        break;
                    default:  //other error, coming from server
                        result= result.replaceAll("\"", "");
                        if (result.contains("message")){
                            result = result.substring(result.indexOf("message"), result.indexOf("domain"));
                            result= result.replaceAll("\"", "");
                            result= result.replaceAll("/","");
                        }
                        String finalResult = result;
                        VerifyPhone.this.runOnUiThread(() -> Toast.makeText(VerifyPhone.this, finalResult, Toast.LENGTH_SHORT).show());
                        break;
                }
                Intent loginIntent = new Intent(VerifyPhone.this, Login.class);
                startActivity(loginIntent);
                finish();
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }


    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(VerifyPhone.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }

}

//Reference:
// https://www.youtube.com/watch?v=BWseVs2MXaI
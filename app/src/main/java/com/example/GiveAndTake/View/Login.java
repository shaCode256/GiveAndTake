package com.example.giveandtake.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.giveandtake.R;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.firebase.auth.FirebaseAuth;
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
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Login extends AppCompatActivity {

    String server_url = "http://10.0.0.3:8000/";

    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EditText phone= findViewById(R.id.phone);
        EditText password= findViewById(R.id.password);
        Button loginBtn= findViewById(R.id.loginBtn);
        TextView registerNowBtn= findViewById(R.id.registerNowBtn);
        Button resetPasswordBtn= findViewById(R.id.resetPasswordBtn);
        auth = FirebaseAuth.getInstance();

        // reset password
        resetPasswordBtn.setOnClickListener(v -> startActivity(new Intent(Login.this, ResetPassword.class)));

    loginBtn.setOnClickListener(view -> {
        String phoneTxt = phone.getText().toString();
        String passwordTxt= password.getText().toString();

        if(phoneTxt.isEmpty() || passwordTxt.isEmpty()){
            Toast.makeText(Login.this, "Please enter mobile number and password ", Toast.LENGTH_SHORT).show();
        }

        else{
            databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String numericRegex = "\\d+";
                    String emailRegex= "^(.+)@(\\S+)$" ;
                    if (phoneTxt.matches(emailRegex)){
                        auth.signInWithEmailAndPassword(phoneTxt , passwordTxt).addOnCompleteListener(task -> {
                            if (task.isSuccessful()){
                                //if it's an email that is verified,
                                // user can pick a phone number,
                                //verify it and it will create a new user.
                                if (auth.getCurrentUser().isEmailVerified()) {
                                        Toast.makeText(Login.this, "Please verify your phone", Toast.LENGTH_SHORT).show();
                                        Intent verifyPhoneIntent = new Intent(Login.this, VerifyPhone.class);
                                        verifyPhoneIntent.putExtra("emailTxt", phoneTxt);
                                        startActivity(verifyPhoneIntent);
                                        finish();
                                }
                                else{
                                    Toast.makeText(Login.this, "Please click on the verification link sent to your email or click forgot password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(e -> Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show());

                    }
                    //check if mobile/phone exists in db
                    else if(phoneTxt.matches(numericRegex) && snapshot.hasChild(phoneTxt)){
                        //mobile num exists in db
                        //open MapsActivity on success
                        String emailTxt= snapshot.child(phoneTxt).child("email").getValue().toString();
                        if (Objects.requireNonNull(snapshot.child(phoneTxt).child("isBlocked").getValue(String.class)).equals("1")){
                                    Toast.makeText(Login.this, "You are blocked. contact the management", Toast.LENGTH_SHORT).show();
                            }
                            else {
                            loginUser(emailTxt, passwordTxt, phoneTxt);
                            //show a success message and then finish the activity
                            }
                        }
                        else{
                            Toast.makeText(Login.this, "Wrong details. try again?", Toast.LENGTH_SHORT).show();
                        }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    });


    registerNowBtn.setOnClickListener(view -> {
        // open Register activity
        startActivity(new Intent(Login.this, VerifyEmail.class));
    });

    }

    private void loginUser(String emailTxt, String passwordTxt, String phoneTxt) {
        auth.signInWithEmailAndPassword(emailTxt , passwordTxt).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                if (auth.getCurrentUser().isEmailVerified()) {
                    //check if is phone verified (if is in db, it is)
                    databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            //check if phone is verified before
                            if(snapshot.child(phoneTxt).hasChild("isPhoneVerified") && snapshot.child(phoneTxt).child("isPhoneVerified").getValue().toString().equals("1")){
                                //log the user in
                                Intent thisIntent = new Intent(Login.this, Map.class);
                                String isManager = snapshot.child(phoneTxt).child("isManager").getValue(String.class);
                                thisIntent.putExtra("userId", phoneTxt);
                                thisIntent.putExtra("isManager", isManager);
                                startActivity(thisIntent);
                                finish();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                }
                else{
                    Toast.makeText(Login.this, "Please click on the verification link sent to your email or click forgot password", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }



    public void login(String email, String password) {
        new Thread(() -> {
            String urlString = server_url +"login/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Login.this.runOnUiThread(() -> Toast.makeText(Login.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                json.put("password", password);
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
                switch (result) {
                    case "1":
                    case "0":  //success. login the user
                        //log the user in
                        Intent thisIntent = new Intent(Login.this, Map.class);
                        thisIntent.putExtra("userId", email);
                        thisIntent.putExtra("isManager", result);
                        startActivity(thisIntent);
                        finish();
                        break;
                    case "verify phone now":  //phone isn't verified
                        Toast.makeText(Login.this, "Please verify your phone", Toast.LENGTH_SHORT).show();
                        Intent verifyPhoneIntent = new Intent(Login.this, VerifyPhone.class);
                        verifyPhoneIntent.putExtra("emailTxt", email);
                        startActivity(verifyPhoneIntent);
                        finish();
                        break;
                    case "email is not verified":  //email isn't verified
                        Login.this.runOnUiThread(() -> Toast.makeText(Login.this, "Please click on the verification link sent to your email or click forgot password", Toast.LENGTH_SHORT).show());
                        break;
                    case "blocked":
                        Login.this.runOnUiThread(() -> Toast.makeText(Login.this, "You are blocked. contact the management", Toast.LENGTH_SHORT).show());
                        break;
                    default:  //other error, coming from server
                        Login.this.runOnUiThread(() -> Toast.makeText(Login.this, result, Toast.LENGTH_SHORT).show());
                        break;
                }
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }

    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(Login.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }

}
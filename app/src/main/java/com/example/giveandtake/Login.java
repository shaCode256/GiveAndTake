package com.example.giveandtake;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class Login extends AppCompatActivity {

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
                    String numericRegex = "[0-9]+";
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
}
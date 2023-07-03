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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class VerifyEmail extends AppCompatActivity {
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        EditText email= findViewById(R.id.email);
        EditText password= findViewById(R.id.password);
        EditText conPassword= findViewById(R.id.conPassword);
        Button registerBtn= findViewById(R.id.registerBtn);
        TextView loginNowBtn= findViewById(R.id.loginNow);
        auth= FirebaseAuth.getInstance();

        registerBtn.setOnClickListener(view -> {
            String emailTxt = email.getText().toString();
            String passwordTxt = password.getText().toString();
            String conPasswordTxt = conPassword.getText().toString();
            if(!passwordTxt.equals(conPasswordTxt)){
                Toast.makeText(VerifyEmail.this, "Passwords are not matching", Toast.LENGTH_SHORT).show();
            }
            else{
                registerUser(emailTxt, passwordTxt);
                }
        });

        loginNowBtn.setOnClickListener(view -> startActivity(new Intent(VerifyEmail.this, Login.class)));
    }

    private void registerUser(String emailTxt, String passwordTxt) {
        if(passwordTxt ==null || (passwordTxt != null && passwordTxt.trim().isEmpty())){
            Toast.makeText(VerifyEmail.this, "please enter password.", Toast.LENGTH_SHORT).show();
        }
        else if (emailTxt.endsWith("gmail.com") || emailTxt.endsWith("@msmail.ariel.ac.il")) {
            auth.createUserWithEmailAndPassword(emailTxt, passwordTxt).addOnSuccessListener(authResult -> {
                auth.getCurrentUser().sendEmailVerification().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(VerifyEmail.this, "please check email for verification.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(VerifyEmail.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }).addOnFailureListener(e -> Toast.makeText(VerifyEmail.this, e.getMessage(), Toast.LENGTH_SHORT).show());

        } else {
            Toast.makeText(VerifyEmail.this, "Can register only with ariel university or admin email", Toast.LENGTH_SHORT).show();
        }
    }
}
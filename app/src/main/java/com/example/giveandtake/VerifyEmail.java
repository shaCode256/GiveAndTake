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
            //check if user fills all the fields before sending data to Firebase
            //check if passwords are matching with each other
            //if not matching, show a toast message
            if(!passwordTxt.equals(conPasswordTxt)){
                Toast.makeText(VerifyEmail.this, "Passwords are not matching", Toast.LENGTH_SHORT).show();
            }
            else{
                registerUser(emailTxt, passwordTxt);
                //show a success message and then finish the activity
                }
        });

        loginNowBtn.setOnClickListener(view -> startActivity(new Intent(VerifyEmail.this, Login.class)));
    }

    private void registerUser(String emailTxt, String passwordTxt) {
        if (emailTxt.endsWith("gmail.com") || emailTxt.endsWith("@msmail.ariel.ac.il")) {
            auth.createUserWithEmailAndPassword(emailTxt, passwordTxt).addOnSuccessListener(authResult -> {
                //add listener if failed or not and if yes then change.
                //send verificationLink
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
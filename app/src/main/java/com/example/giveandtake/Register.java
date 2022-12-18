package com.example.giveandtake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Register extends AppCompatActivity {
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        EditText fullName = findViewById(R.id.fullname);
        EditText email= findViewById(R.id.email);
        EditText phone= findViewById(R.id.phone);
        EditText password= findViewById(R.id.password);
        EditText conPassword= findViewById(R.id.conPassword);
        Button registerBtn= findViewById(R.id.registerBtn);
        TextView loginNowBtn= findViewById(R.id.loginNow);
        auth= FirebaseAuth.getInstance();

        registerBtn.setOnClickListener(view -> {
            // get data from EditTexts into String variables
            String fullNameTxt = fullName.getText().toString();
            String emailTxt = email.getText().toString();
            String phoneTxt = phone.getText().toString();
            String passwordTxt = password.getText().toString();
            String conPasswordTxt = conPassword.getText().toString();
            //check if user fills all the fields before sending data to Firebase
            if(fullNameTxt.isEmpty() || emailTxt.isEmpty() || phoneTxt.isEmpty()){
                Toast.makeText(Register.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            }
            //check if passwords are matching with each other
            //if not matching, show a toast message
            else if(!passwordTxt.equals(conPasswordTxt)){
                Toast.makeText(Register.this, "Passwords are not matching", Toast.LENGTH_SHORT).show();
            }
            else if(!phoneTxt.matches("[0-9]+")){
                Toast.makeText(Register.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            }
            else{
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //check if phone is not registered before
                        if(snapshot.hasChild(phoneTxt)){
                            Toast.makeText(Register.this, "Phone is already registered", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            registerUser(emailTxt, passwordTxt, phoneTxt, fullNameTxt);
                            //show a success message and then finish the activity
                        }
                        }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        loginNowBtn.setOnClickListener(view -> {
            startActivity(new Intent(Register.this, Login.class));
        });
    }

    private void registerUser(String emailTxt, String passwordTxt, String phoneTxt, String fullNameTxt) {
        auth.createUserWithEmailAndPassword(emailTxt, passwordTxt).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                //add listener if failed or not and if yes then change.
                //send verificationLink


                auth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Register.this, "please check email for verification.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Register.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });



                if (emailTxt.endsWith("@manager.com")) {
                    databaseReference.child("users").child(phoneTxt).child("isManager").setValue("1");
                } else {
                    databaseReference.child("users").child(phoneTxt).child("isManager").setValue("0");
                }
                databaseReference.child("users").child(phoneTxt).child("fullName").setValue(fullNameTxt);
                databaseReference.child("users").child(phoneTxt).child("email").setValue(emailTxt);
                databaseReference.child("users").child(phoneTxt).child("isBlocked").setValue("0");
                //TODO: add check of university email
                Toast.makeText(Register.this, "User is successfully registered", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Register.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Add verify phone number


    }
}
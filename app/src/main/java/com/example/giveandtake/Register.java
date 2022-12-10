package com.example.giveandtake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Register extends AppCompatActivity {

    //create object of DatabaseReference class to access firebase's Realtime Database

    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");

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
            else{
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //check if phone is not registered before
                        if(snapshot.hasChild(phoneTxt)){
                            Toast.makeText(Register.this, "Phone is already registered", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            //sending data to firebase Realtime Database
                            // we are using phone number as unique identifier of every user
                            // so all the other details of user comes under phone number
                            if(emailTxt.endsWith("@manager.com")) {
                                databaseReference.child("users").child(phoneTxt).child("isManager").setValue("1");
                            }
                            else{
                                databaseReference.child("users").child(phoneTxt).child("isManager").setValue("0");
                            }
                            databaseReference.child("users").child(phoneTxt).child("fullName").setValue(fullNameTxt);
                            databaseReference.child("users").child(phoneTxt).child("isBlocked").setValue("0");
                            databaseReference.child("users").child(phoneTxt).child("email").setValue(emailTxt);
                            //TODO: add check of university email
                            databaseReference.child("users").child(phoneTxt).child("password").setValue(passwordTxt);
                            //show a success message and then finish the activity
                            Toast.makeText(Register.this, "user registered successfully.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        loginNowBtn.setOnClickListener(view -> {
            // open Register activity
            startActivity(new Intent(Register.this, Login.class));
        });
    }
}
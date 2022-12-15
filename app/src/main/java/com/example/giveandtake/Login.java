package com.example.giveandtake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
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
        resetPasswordBtn.setOnClickListener(v -> {
            Toast.makeText(Login.this, "You can reset your password now", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Login.this, ResetPassword.class));
        });

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
                    //check if mobile/phone exists in db
                    if(snapshot.hasChild(phoneTxt)){
                        //mobile num exists in db
                            //open MapsActivity on success
                        String emailTxt= snapshot.child(phoneTxt).child("email").getValue().toString();
                        if (Objects.requireNonNull(snapshot.child(phoneTxt).child("isBlocked").getValue(String.class)).equals("1")){
                                    Toast.makeText(Login.this, "You are blocked. contact the management", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                loginUser(emailTxt, passwordTxt, phoneTxt, snapshot);
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
        startActivity(new Intent(Login.this, Register.class));
    });


    }

    private void loginUser(String emailTxt, String passwordTxt, String phoneTxt, DataSnapshot snapshot) {
        auth.signInWithEmailAndPassword(emailTxt , passwordTxt).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    Intent myIntent = new Intent(Login.this, Map.class);
                    String isManager = snapshot.child(phoneTxt).child("isManager").getValue(String.class);
                    myIntent.putExtra("userId", phoneTxt);
                    myIntent.putExtra("isManager", isManager);
                    startActivity(myIntent);
                    finish();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
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

public class Login extends AppCompatActivity {

    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        final EditText phone= findViewById(R.id.phone);
        final EditText password= findViewById(R.id.password);
        final Button loginBtn= findViewById(R.id.loginBtn);
        final TextView registerNowBtn= findViewById(R.id.registerNowBtn);
        final Button resetPasswordBtn= findViewById(R.id.resetPasswordBtn);
       // reset password
        resetPasswordBtn.setOnClickListener(v -> {
            Toast.makeText(Login.this, "You can reset your password now", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Login.this, ResetPassword.class));
        });

    loginBtn.setOnClickListener(view -> {
        final String phoneTxt = phone.getText().toString();
        final String passwordTxt= password.getText().toString();

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
                        // now get password of user from db and match it with the entered password
                        final String getPassword= snapshot.child(phoneTxt).child("password").getValue(String.class);
                        assert getPassword != null;
                        if(getPassword.equals(passwordTxt)){
                            //open MapsActivity on success
                            Intent myIntent = new Intent(Login.this, Map.class);
                            myIntent.putExtra("userId",phoneTxt);
                            //TODO: add check if a regular user or a manager, and choose which map to forward to
                            // (will reveal more buttons, forward to other activities)
                            startActivity(myIntent);
                            finish();
                        }
                        else{
                            Toast.makeText(Login.this, "Wrong details. try again?", Toast.LENGTH_SHORT).show();
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
}
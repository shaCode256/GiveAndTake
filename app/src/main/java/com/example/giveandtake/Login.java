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
        String numericRegex= "[0-9]+";
        auth = FirebaseAuth.getInstance();
        // reset password
        resetPasswordBtn.setOnClickListener(v -> startActivity(new Intent(Login.this, ResetPassword.class)));

    loginBtn.setOnClickListener(view -> {
        String phoneTxt = phone.getText().toString();
        String passwordTxt= password.getText().toString();

        if(phoneTxt.isEmpty() || passwordTxt.isEmpty()){
            Toast.makeText(Login.this, "Please enter mobile number and password ", Toast.LENGTH_SHORT).show();
        }
//        else if(!phoneTxt.matches(numericRegex)){
//            Toast.makeText(Login.this, "Please enter a valid numeric number ", Toast.LENGTH_SHORT).show();
//        }
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
        startActivity(new Intent(Login.this, VerifyPhone.class));
    });


    }

    private void loginUser(String emailTxt, String passwordTxt, String phoneTxt, DataSnapshot snapshot) {
        auth.signInWithEmailAndPassword(emailTxt , passwordTxt).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                if (auth.getCurrentUser().isEmailVerified()) {
                    databaseReference.child("users").child(phoneTxt).child("isEmailVerified").setValue("1");
                    Intent thisIntent = new Intent(Login.this, Map.class);
                    String isManager = snapshot.child(phoneTxt).child("isManager").getValue(String.class);
                    thisIntent.putExtra("userId", phoneTxt);
                    thisIntent.putExtra("isManager", isManager);
                    startActivity(thisIntent);
                    finish();
                }
                else{
                    Toast.makeText(Login.this, "Please click on the verification link sent to your email", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
package com.example.giveandtake;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ResetPassword extends AppCompatActivity {
    private EditText inputEmail;
    private EditText mobileNum;
    private FirebaseAuth auth;
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        auth= FirebaseAuth.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        inputEmail = findViewById(R.id.editText_password_reset_email);
        mobileNum= findViewById(R.id.editText_password_reset_number);
        Button btnReset = findViewById(R.id.button_password_reset);
        Button returnToLogin = findViewById(R.id.loginBtn);

        returnToLogin.setOnClickListener(v -> startActivity(new Intent(ResetPassword.this, Login.class)));
        btnReset.setOnClickListener(v -> {
            String emailTxt= inputEmail.getText().toString();
            String phoneTxt= mobileNum.getText().toString();
            if (emailTxt.isEmpty()) {
                Toast.makeText(ResetPassword.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            }
            else{
                auth.sendPasswordResetEmail(emailTxt).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(ResetPassword.this, "Email was successfully sent", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else{
                            Toast.makeText(ResetPassword.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                });
            }

        });
    }
}
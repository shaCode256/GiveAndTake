package com.example.giveandtake.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.giveandtake.R;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPassword extends AppCompatActivity {
    private EditText inputEmail;
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        auth= FirebaseAuth.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        inputEmail = findViewById(R.id.editText_password_reset_email);
        Button btnReset = findViewById(R.id.button_password_reset);
        Button returnToLogin = findViewById(R.id.loginBtn);
        returnToLogin.setOnClickListener(v -> startActivity(new Intent(ResetPassword.this, Login.class)));
        btnReset.setOnClickListener(v -> {
            String emailTxt= inputEmail.getText().toString();
            if (emailTxt.isEmpty()) {
                Toast.makeText(ResetPassword.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            }
            else{
                auth.sendPasswordResetEmail(emailTxt).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Toast.makeText(ResetPassword.this, "Email was successfully sent", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else{
                        Toast.makeText(ResetPassword.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });
    }
}
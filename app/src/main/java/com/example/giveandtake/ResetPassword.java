package com.example.giveandtake;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        inputEmail = findViewById(R.id.editText_password_reset_email);
        mobileNum= findViewById(R.id.editText_password_reset_number);
        Button btnReset = findViewById(R.id.button_password_reset);
        Button returnToLogin = findViewById(R.id.loginBtn);

        returnToLogin.setOnClickListener(v -> startActivity(new Intent(ResetPassword.this, Login.class)));
        btnReset.setOnClickListener(v -> {
            //TODO: check on this feature. low priority
            Toast.makeText(ResetPassword.this, "If Details are correct- E-mail is sent ", Toast.LENGTH_SHORT).show();
            databaseReference.child("users").child(String.valueOf(mobileNum));
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"recipient@example.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "subject of email");
            i.putExtra(Intent.EXTRA_TEXT   , "body of email");
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(ResetPassword.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
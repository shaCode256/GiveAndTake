package com.example.giveandtake.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.giveandtake.R;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class VerifyEmail extends AppCompatActivity {
    private FirebaseAuth auth;

    String server_url = "http://10.0.0.3:8000/";

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
        if(passwordTxt ==null || passwordTxt.trim().isEmpty()){
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


    public void register(String email, String password) {
        new Thread(() -> {
            String urlString = server_url +"register/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                VerifyEmail.this.runOnUiThread(() -> Toast.makeText(VerifyEmail.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);
                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("error2");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                InputStream is = conn.getInputStream();
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                if(result.equals("success")){

                }
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }


    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(VerifyEmail.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }
}
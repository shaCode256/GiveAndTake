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
   String server_url= "https://giveandtake-server.df.r.appspot.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        EditText email= findViewById(R.id.email);
        EditText password= findViewById(R.id.password);
        EditText conPassword= findViewById(R.id.conPassword);
        Button registerBtn= findViewById(R.id.registerBtn);
        TextView loginNowBtn= findViewById(R.id.loginNow);

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
                register(emailTxt, passwordTxt);
        } else {
            Toast.makeText(VerifyEmail.this, "Can register only with ariel university or admin email", Toast.LENGTH_SHORT).show();
            register(emailTxt, passwordTxt);
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
                result= result.replaceAll("\"", "");
                if (result.contains("message")){
                    result = result.substring(result.indexOf("message"), result.indexOf("domain"));
                    result= result.replaceAll("\"", "");
                    result= result.replaceAll("/","");
                }
                String finalResult = result;
                runOnUiThread(() -> Toast.makeText(VerifyEmail.this, finalResult, Toast.LENGTH_SHORT).show());
                if (result.equals("Success. Please check email for verification.")){
                    //go to log in with your email now
                    Intent thisIntent = new Intent(VerifyEmail.this, Login.class);
                    startActivity(thisIntent);
                    finish();
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
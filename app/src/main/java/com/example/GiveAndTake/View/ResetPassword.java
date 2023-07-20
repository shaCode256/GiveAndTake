package com.example.giveandtake.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

public class ResetPassword extends AppCompatActivity {
    private EditText inputEmail;
    private FirebaseAuth auth;

   String server_url= "https://giveandtake-server.df.r.appspot.com/";

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
                resetPassword(emailTxt);
            }
        });
    }


    public void resetPassword(String email) {
        new Thread(() -> {
            String urlString = server_url +"resetPassword/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ResetPassword.this.runOnUiThread(() -> Toast.makeText(ResetPassword.this, "Server is down, can't process the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                if (result.contains("message")){
                    result = result.substring(result.indexOf("message"), result.indexOf("domain"));
                    result= result.replaceAll("\"", "");
                    result= result.replaceAll("/","");
                }
                String finalResult= result;;
                runOnUiThread(() -> Toast.makeText(ResetPassword.this, finalResult, Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }


    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(ResetPassword.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }
}
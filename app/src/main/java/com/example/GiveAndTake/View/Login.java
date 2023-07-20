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

public class Login extends AppCompatActivity {

   String server_url= "https://giveandtake-server.df.r.appspot.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EditText phone= findViewById(R.id.phone);
        EditText password= findViewById(R.id.password);
        Button loginBtn= findViewById(R.id.loginBtn);
        TextView registerNowBtn= findViewById(R.id.registerNowBtn);
        Button resetPasswordBtn= findViewById(R.id.resetPasswordBtn);

        // reset password
        resetPasswordBtn.setOnClickListener(v -> startActivity(new Intent(Login.this, ResetPassword.class)));

    loginBtn.setOnClickListener(view -> {
        String phoneTxt = phone.getText().toString();
        String passwordTxt= password.getText().toString();

        if(phoneTxt.isEmpty() || passwordTxt.isEmpty()){
            Toast.makeText(Login.this, "Please enter mobile number and password ", Toast.LENGTH_SHORT).show();
        }

        else{
             login(phoneTxt, passwordTxt);
        }
    });


    registerNowBtn.setOnClickListener(view -> {
        // open Register activity
        startActivity(new Intent(Login.this, VerifyEmail.class));
    });

    }

    public void login(String email, String password) {
        new Thread(() -> {
            String urlString = server_url +"login/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Login.this.runOnUiThread(() -> Toast.makeText(Login.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                assert url != null;
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
                assert conn != null;
                InputStream is = conn.getInputStream();
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("result is: "+ result);
                result = result.replaceAll("\"", "");
                switch (result) {
                    case "1":
                    case "0":  //success. login the user
                        //log the user in
                        System.out.println("isManager: "+result);
                        Intent mapIntent = new Intent(Login.this, Map.class);
                        mapIntent.putExtra("userId", email);
                        mapIntent.putExtra("isManager", result);
                        startActivity(mapIntent);
                        finish();
                        break;
                    case "phone is not verified, verified email":  //phone isn't verified
                        Login.this.runOnUiThread(() -> Toast.makeText(Login.this, "Please log in with email and verify your phone.", Toast.LENGTH_SHORT).show());
                        break;
                    case "verify phone now":  //phone isn't verified
                        Login.this.runOnUiThread(() -> Toast.makeText(Login.this, "Success, please enter phone now", Toast.LENGTH_SHORT).show());
                        System.out.println("isManager: "+result);
                        Intent verifyPhoneIntent  = new Intent(Login.this, VerifyPhone.class);
                        verifyPhoneIntent.putExtra("emailTxt", email);
                        startActivity(verifyPhoneIntent);
                        finish();
                        break;
                    case "email is not verified":  //email isn't verified
                        Login.this.runOnUiThread(() -> Toast.makeText(Login.this, "Please click on the verification link sent to your email or click forgot password", Toast.LENGTH_SHORT).show());
                        break;
                    case "blocked":
                        Login.this.runOnUiThread(() -> Toast.makeText(Login.this, "You are blocked, or not registered. contact management or register", Toast.LENGTH_SHORT).show());
                        break;
                    default:  //other error, coming from server
                        if (result.contains("message")){
                            result = result.substring(result.indexOf("message"), result.indexOf("domain"));
                            result= result.replaceAll("\"", "");
                            result= result.replaceAll("/","");
                        }
                        String finalResult= result;
                        Login.this.runOnUiThread(() -> Toast.makeText(Login.this, finalResult, Toast.LENGTH_SHORT).show());
                        break;
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
        runOnUiThread(() -> Toast.makeText(Login.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }

}
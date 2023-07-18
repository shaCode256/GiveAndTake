package com.example.giveandtake.View;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.giveandtake.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.firebase.firestore.GeoPoint;

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

public class Settings extends AppCompatActivity {
    String server_url = "http://10.0.0.3:8000/";
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        EditText distanceEditText = findViewById(R.id.closeRaidus);
        EditText latitudeInput= findViewById(R.id.latitude_input);
        EditText longitudeInput= findViewById(R.id.longitude_input);
        Button turnOnAutoDetectLocationBtn= findViewById(R.id.btn_turn_on_auto_detect_location);
        Button turnOffNotificationsBtn= findViewById(R.id.btn_turn_off_notification);
        Button turnOnNotificationsBtn= findViewById(R.id.btn_turn_on_notification);
        Button useSpecifiedLocationBtn= findViewById(R.id.btn_use_specified_location);
        Button useCurrLocationBtn= findViewById(R.id.btn_use_curr_location);
        Button backToMapBtn= findViewById(R.id.backToMap);
        Button applyKmBtn= findViewById(R.id.applyKmBtn);
        Intent thisIntent= getIntent();
        String userId = thisIntent.getStringExtra("userId");
        String isManager= thisIntent.getStringExtra("isManager");

        applyKmBtn.setOnClickListener(view -> {
            String distance = distanceEditText.getText().toString();
            if(distance.isEmpty()){
                Toast.makeText(Settings.this, "Please fill in valid distance in km", Toast.LENGTH_SHORT).show();
            }
            else{
              sendKmDistance(userId, distance);
            }
        });

        turnOnAutoDetectLocationBtn.setOnClickListener(view -> turnOnOffAutoDetectLocation(userId, "1"));

        useSpecifiedLocationBtn.setOnClickListener(view -> {
            // get data from EditTexts into String variables
            String latitudeTxt= latitudeInput.getText().toString();
            String longitudeTxt= longitudeInput.getText().toString();
            //check if they're valid numeric values
            boolean numeric = true;
            try {
                Double.parseDouble(longitudeTxt);
            } catch (NumberFormatException e) {
                numeric = false;
            }
            try {
                Double.parseDouble(latitudeTxt);
            } catch (NumberFormatException e) {
                numeric = false;
            }
            if (numeric) {
                double doubleLongitude = Double.parseDouble(longitudeTxt); // returns double primitive
                double doubleLatitude = Double.parseDouble(latitudeTxt); // returns double primitive
                if (doubleLatitude >= -90 && doubleLatitude <= 90 && doubleLongitude >= -180 && doubleLongitude <= 180) {
                    GeoPoint geoPoint = new GeoPoint(doubleLatitude, doubleLongitude);
                    sendUseSpecificLocation(userId, geoPoint);
                }
            }
        });

        backToMapBtn.setOnClickListener(view -> {
            Intent newIntent = new Intent(Settings.this, Map.class);
            newIntent.putExtra("userId",userId);
            newIntent.putExtra("isManager", isManager);
            startActivity(newIntent);
        });

        turnOnNotificationsBtn.setOnClickListener(view -> sendTurnOnOffNotifications(userId, "1")
        );

        turnOffNotificationsBtn.setOnClickListener(view -> sendTurnOnOffNotifications(userId, "0"));

        useCurrLocationBtn.setOnClickListener(view -> {
            // get curr location
            if (
                    ContextCompat.checkSelfPermission(Settings.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(Settings.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(Settings.this, "please enable permissions", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(Settings.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, 90);
            }
            else{
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                        .addOnSuccessListener(Settings.this, location -> {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                               sendUseCurrLocation(userId, location);
                            }
                            else{
                                Toast.makeText(Settings.this, "Can't use your location.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });


    }

    public void sendUseCurrLocation(String userId, Location location) {
        new Thread(() -> {
            String urlString = server_url +"useCurrLocationNotifications/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Settings.this.runOnUiThread(() -> Toast.makeText(Settings.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                json.put("latitude", location.getLatitude());
                json.put("longitude", location.getLongitude());
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
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(Settings.this, "Set use curr location successfully", Toast.LENGTH_SHORT).show();
    }

    public void sendTurnOnOffNotifications(String userId, String onOff) {
        new Thread(() -> {
            String urlString = server_url +"turnOnOffNotifications/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Settings.this.runOnUiThread(() -> Toast.makeText(Settings.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                json.put("onOff", onOff);
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
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(Settings.this, "Set On/Off notifications successfully", Toast.LENGTH_SHORT).show();
    }

    public void sendUseSpecificLocation(String userId, GeoPoint location) {
        new Thread(() -> {
            String urlString = server_url +"useSpecificLocationNotifications/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Settings.this.runOnUiThread(() -> Toast.makeText(Settings.this, "Server is down, can't process the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                json.put("latitude", location.getLatitude());
                json.put("longitude", location.getLongitude());
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
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(Settings.this, "Set use curr location successfully", Toast.LENGTH_SHORT).show();
    }

    public void turnOnOffAutoDetectLocation(String userId, String onOff) {
        new Thread(() -> {
            String urlString = server_url +"turnOnOffAutoDetectLocationNotifications/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Settings.this.runOnUiThread(() -> Toast.makeText(Settings.this, "Server is down, can't process the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                json.put("onOff", onOff);
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
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(Settings.this, "Set auto detect location successfully", Toast.LENGTH_SHORT).show();
    }

    public void sendKmDistance(String userId, String distance) {
        new Thread(() -> {
            String urlString = server_url +"setKmDistanceNotifications/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Settings.this.runOnUiThread(() -> Toast.makeText(Settings.this, "Server is down, can't process the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                json.put("distance", distance);
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
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(Settings.this, "Set use curr location successfully", Toast.LENGTH_SHORT).show();
    }


    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(Settings.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }

}
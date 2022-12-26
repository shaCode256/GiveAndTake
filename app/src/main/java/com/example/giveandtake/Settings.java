package com.example.giveandtake;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.GeoPoint;

public class Settings extends AppCompatActivity {
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    private FirebaseAuth auth;
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        EditText distanceEditText = findViewById(R.id.closeRaidus);
        EditText latitude_input= findViewById(R.id.latitude_input);
        EditText longitude_input= findViewById(R.id.longitude_input);
        Button turnOnAutoDetectLocationBtn= findViewById(R.id.btn_turn_on_auto_detect_location);
        Button turnOffNotificationsBtn= findViewById(R.id.btn_turn_off_notification);
        Button turnOnNotificationsBtn= findViewById(R.id.btn_turn_on_notification);
        Button useSpecifiedLocationBtn= findViewById(R.id.btn_use_specified_location);
        Button useCurrLocationBtn= findViewById(R.id.btn_use_curr_location);
        Button backToMapBtn= findViewById(R.id.backToMap);
        Button applyKmBtn= findViewById(R.id.applyKmBtn);
        auth= FirebaseAuth.getInstance();
        Intent myIntent= getIntent();
        String userId = myIntent.getStringExtra("userId");
        String isManager= myIntent.getStringExtra("isManager");


        applyKmBtn.setOnClickListener(view -> {
            String distance = distanceEditText.getText().toString();
            if(distance.isEmpty()){
                Toast.makeText(Settings.this, "Please fill in valid distance in km", Toast.LENGTH_SHORT).show();
            }
            else{
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        databaseReference.child("users").child(userId).child("settings").child("notifications").child("distance").setValue(distance);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });


        turnOnAutoDetectLocationBtn.setOnClickListener(view -> databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                databaseReference.child("users").child(userId).child("settings").child("notifications").child("auto_detect_location").setValue(1);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));

        useSpecifiedLocationBtn.setOnClickListener(view -> {
            // get data from EditTexts into String variables
            String latitudeTxt= latitude_input.getText().toString();
            String longitudeTxt= longitude_input.getText().toString();
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
                    databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            databaseReference.child("users").child(userId).child("settings").child("notifications").child("auto_detect_location").setValue(0);
                            databaseReference.child("users").child(userId).child("settings").child("notifications").child("specific_location").setValue(geoPoint);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

                });

        backToMapBtn.setOnClickListener(view -> {
            Intent newIntent = new Intent(Settings.this, Map.class);
            newIntent.putExtra("userId",userId);
            newIntent.putExtra("isManager", isManager);
            startActivity(newIntent);
        });

        turnOnNotificationsBtn.setOnClickListener(view -> databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                databaseReference.child("users").child(userId).child("settings").child("notifications").child("turned_on").setValue(1);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));

        turnOffNotificationsBtn.setOnClickListener(view -> databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                databaseReference.child("users").child(userId).child("settings").child("notifications").child("turned_on").setValue(0);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));

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
                                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                                        databaseReference.child("users").child(userId).child("settings").child("notifications").child("auto_detect_location").setValue(0);
                                        databaseReference.child("users").child(userId).child("settings").child("notifications").child("specific_location").setValue(geoPoint);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                            else{
                                Toast.makeText(Settings.this, "Can't use your location.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });


    }

}
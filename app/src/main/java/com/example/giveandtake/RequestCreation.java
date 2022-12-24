package com.example.giveandtake;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
// important import statements
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RequestCreation extends AppCompatActivity {
    protected Context context;
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO: what happens when we create a request in the same place?
        Intent mIntent = getIntent();
        FirebaseFirestore markersDb = FirebaseFirestore.getInstance();
        String userId = mIntent.getStringExtra("userId");
        String requestUserId = mIntent.getStringExtra("requestUserId");
        String isManager = mIntent.getStringExtra("isManager");
        String clickedLat = mIntent.getStringExtra("clickedLat");
        String clickedLong = mIntent.getStringExtra("clickedLong");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_creation);
        EditText subject = findViewById(R.id.create_request_input_subject);
        EditText body = findViewById(R.id.create_request_input_body);
        EditText longitude_input = findViewById(R.id.longitude_input);
        EditText latitude_input = findViewById(R.id.latitude_input);
        EditText contact_details = findViewById(R.id.create_request_input_contact_details);
        Button btnAddRequest = findViewById(R.id.button_add_request);
        Button btnBackToMap = findViewById(R.id.btn_back_to_map);
        Button addCurrLocation = findViewById(R.id.button_add_location);
        if (clickedLat != null && clickedLong != null) {
            longitude_input.setText(clickedLong, TextView.BufferType.EDITABLE);
            latitude_input.setText(clickedLat, TextView.BufferType.EDITABLE);
        }
        btnAddRequest.setOnClickListener(v -> {
            String subjectTxt = subject.getText().toString();
            String bodyTxt = body.getText().toString();
            //TODO: deal with converting double and string, also with intents. try to avoid some conversions
            String longitudeTxt = longitude_input.getText().toString();
            String latitudeTxt = latitude_input.getText().toString();
            String contact_detailsTxt = contact_details.getText().toString();
            int randomNum = ThreadLocalRandom.current().nextInt(0, 10000 + 1);
            String setRequestId = String.valueOf(randomNum);
            //to avoid repeating the same requestId
            HashSet<String> taken_requests_ids= (HashSet<String>)mIntent.getExtras().getSerializable("taken_requests_ids");
            while (taken_requests_ids.contains(setRequestId)){
                //change until it's a new request number
                randomNum = ThreadLocalRandom.current().nextInt(0, 10000 + 1);
                setRequestId = String.valueOf(randomNum);
            }
            if (subjectTxt.isEmpty() || bodyTxt.isEmpty() | latitudeTxt.isEmpty() | longitudeTxt.isEmpty() | contact_detailsTxt.isEmpty()) {
                Toast.makeText(RequestCreation.this, "Please fill in all the request details", Toast.LENGTH_SHORT).show();
            } else {
                // Add a new marker with the request ID
                //add to markersDb this location
                // Add a new document with a generated ID
                // Create a new user with a first, middle, and last name
                HashMap<String, Object> user = new HashMap<>();
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
                        //check valid latitude and longitude input
                        String creation_time= "";
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            creation_time = LocalDateTime.now().toString();
                        }
                        GeoPoint geoPointRequest = new GeoPoint(doubleLatitude, doubleLongitude);
                        user.put("geoPoint", geoPointRequest);
                        user.put("requestId", setRequestId);
                        user.put("userId", requestUserId);
                        user.put("isManager", isManager);
                        user.put("creationTime", creation_time);
                        markersDb.collection("MapsData")
                                .add(user)
                                .addOnSuccessListener(documentReference -> {
                                })
                                .addOnFailureListener(e -> {
                                });
                        String finalSetRequestId = setRequestId;
                        String finalCreation_time = creation_time;
                        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                //add the request in the db
                                //get currTime
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    databaseReference.child("users").child(requestUserId).child("requestId").child(finalSetRequestId).child("creation_time").setValue(finalCreation_time);
                                }
                                databaseReference.child("users").child(requestUserId).child("requestId").child(finalSetRequestId).child("subject").setValue(subjectTxt);
                                databaseReference.child("users").child(requestUserId).child("requestId").child(finalSetRequestId).child("body").setValue(bodyTxt);
                                databaseReference.child("users").child(requestUserId).child("requestId").child(finalSetRequestId).child("contact_details").setValue(contact_detailsTxt);
                                databaseReference.child("users").child(requestUserId).child("requestId").child(finalSetRequestId).child("location").setValue(geoPointRequest);
                                //   }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                        Intent myIntent = new Intent(RequestCreation.this, Map.class);
                        myIntent.putExtra("userId", userId);
                        myIntent.putExtra("isManager", isManager);
                        startActivity(myIntent);
                    }
                    else{
                        Toast.makeText(RequestCreation.this, "Please fill in valid longitude (-180 to 180) and latitude (-90 to 90)", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(RequestCreation.this, "Please fill in valid longitude (-180 to 180) and latitude (-90 to 90)", Toast.LENGTH_SHORT).show();
                }
            }
        });

        addCurrLocation.setOnClickListener(v -> {
                if (
                        ContextCompat.checkSelfPermission(RequestCreation.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(RequestCreation.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(RequestCreation.this, "please enable permissions", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(RequestCreation.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION}, 90);
                }
            else{
                    FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                    CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                            .addOnSuccessListener(RequestCreation.this, location -> {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    longitude_input.setText(String. valueOf(location.getLongitude()), TextView.BufferType.EDITABLE);
                                    latitude_input.setText(String. valueOf(location.getLatitude()), TextView.BufferType.EDITABLE);
                                }
                                else{
                                    Toast.makeText(RequestCreation.this, "Can't use your location.", Toast.LENGTH_SHORT).show();
                                }
                            });
            }
        });


        btnBackToMap.setOnClickListener(v -> {
            Intent myIntent = new Intent(RequestCreation.this, Map.class);
            myIntent.putExtra("userId", userId);
            myIntent.putExtra("isManager", isManager);
            startActivity(myIntent);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 90) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(RequestCreation.this, "permission is granted", Toast.LENGTH_SHORT).show();
                // Permission is granted. Continue the action or workflow
                // in your app.
            } else {
                Toast.makeText(RequestCreation.this, "permission is NOT granted", Toast.LENGTH_SHORT).show();
                // Explain to the user that the feature is unavailable because
                // the feature requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
            }
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }


}



//https://stackoverflow.com/questions/20438627/getlastknownlocation-returns-null

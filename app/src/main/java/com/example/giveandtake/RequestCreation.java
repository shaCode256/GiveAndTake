package com.example.giveandtake;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class RequestCreation extends AppCompatActivity {
    protected Context context;
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    //TODO: read it from stored cloud
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent mIntent = getIntent();
        FirebaseFirestore markersDb = FirebaseFirestore.getInstance();
        String userId = mIntent.getStringExtra("userId");
      //  String isManager = mIntent.getStringExtra("isManager");
        String clickedLat = mIntent.getStringExtra("clickedLat");
        String clickedLong = mIntent.getStringExtra("clickedLong");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_creation);
        EditText subject = findViewById(R.id.create_request_input_subject);
        EditText body = findViewById(R.id.create_request_input_body);
        //  final EditText location = findViewById(R.id.create_request_input_location_details);
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
            //  final String locationTxt = location.getText().toString();
            //TODO: deal with converting double and string, also with intents. try to avoid some conversions
            String longitudeTxt = longitude_input.getText().toString();
            String latitudeTxt = latitude_input.getText().toString();
            String contact_detailsTxt = contact_details.getText().toString();
            String getUserId = userId;
            int randomNum = ThreadLocalRandom.current().nextInt(0, 10000 + 1);
            String setRequestId = String.valueOf(randomNum);
            //to avoid repeating the same requestId
            HashSet<String> taken_requests_ids= (HashSet<String>)mIntent.getExtras().getSerializable("taken_requests_ids");
          //  Toast.makeText(RequestCreation.this, "Our HashSet: "+taken_requests_ids.toString(), Toast.LENGTH_SHORT).show();
            while (taken_requests_ids.contains(setRequestId)){
                //change until it's a new request number
                randomNum = ThreadLocalRandom.current().nextInt(0, 10000 + 1);
                setRequestId = String.valueOf(randomNum);
            }
         //   Toast.makeText(RequestCreation.this, "Request Num: "+setRequestId, Toast.LENGTH_SHORT).show();
            if (subjectTxt.isEmpty() || bodyTxt.isEmpty() | latitudeTxt.isEmpty() | longitudeTxt.isEmpty() | contact_detailsTxt.isEmpty()) {
                Toast.makeText(RequestCreation.this, "Please fill in all the request details", Toast.LENGTH_SHORT).show();
            } else {
               // Toast.makeText(RequestCreation.this, "Adding your request", Toast.LENGTH_SHORT).show();
                // Add a new marker with the request ID
                //add to markersDb this location
                // Add a new document with a generated ID
                // Create a new user with a first, middle, and last name
                HashMap<String, Object> user = new HashMap<>();
                //TODO: add check that this is a numeric value! the text we insert
                double doubleLongitude = Double.parseDouble(longitudeTxt); // returns double primitive
                double doubleLatitude = Double.parseDouble(latitudeTxt); // returns double primitive
                GeoPoint geoPointRequest = new GeoPoint(doubleLatitude, doubleLongitude);
                user.put("geoPoint", geoPointRequest);
                user.put("requestId", setRequestId);
                user.put("userId", userId);

                markersDb.collection("MapsData")
                        .add(user)
                        .addOnSuccessListener(documentReference -> {
                            //     Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        })
                        .addOnFailureListener(e -> {
                            //         Log.w(TAG, "Error adding document", e);
                        });
                String finalSetRequestId = setRequestId;
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //check if mobile/phone exists in db
                        databaseReference.child("users").child(getUserId).child("requestId").child(finalSetRequestId).child("subject").setValue(subjectTxt);
                        databaseReference.child("users").child(getUserId).child("requestId").child(finalSetRequestId).child("body").setValue(bodyTxt);
                        databaseReference.child("users").child(getUserId).child("requestId").child(finalSetRequestId).child("contact_details").setValue(contact_detailsTxt);
                        databaseReference.child("users").child(getUserId).child("requestId").child(finalSetRequestId).child("location").setValue(geoPointRequest);
                        //   }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                Intent myIntent = new Intent(RequestCreation.this, Map.class);
                myIntent.putExtra("userId", userId);
                startActivity(myIntent);
            }
        });
        addCurrLocation.setOnClickListener(v -> {
            //TODO: add tracking location ability
                if (
                        ContextCompat.checkSelfPermission(RequestCreation.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(RequestCreation.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(RequestCreation.this, "please enable permissions", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(RequestCreation.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION}, 90);
                }
            else{
                Toast.makeText(RequestCreation.this, "Good for you! you have the access fine location permission already ", Toast.LENGTH_SHORT).show();
                LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                String locationProvider = LocationManager.NETWORK_PROVIDER;
                // I suppressed the missing-permission warning because this wouldn't be executed in my
                // case without location services being enabled
                  Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
//                double userLat = lastKnownLocation.getLatitude();
//                double userLong = lastKnownLocation.getLongitude();
                Toast.makeText(RequestCreation.this, "last known location is "+lastKnownLocation, Toast.LENGTH_SHORT).show();
                //                Toast.makeText(RequestCreation.this, "last known location is " + userLat + " " + userLong, Toast.LENGTH_SHORT).show();

            }
        });

        btnBackToMap.setOnClickListener(v -> {
            Intent myIntent = new Intent(RequestCreation.this, Map.class);
            myIntent.putExtra("userId", userId);
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

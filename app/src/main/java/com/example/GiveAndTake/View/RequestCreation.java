package com.example.giveandtake.View;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.giveandtake.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RequestCreation extends AppCompatActivity {
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");

    String IPv4_Address= "http://10.0.0.3:8000/";
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //TODO: what happens when we create a request in the same place?
        Intent thisIntent = getIntent();
        FirebaseFirestore markersDb = FirebaseFirestore.getInstance();
        String userId = thisIntent.getStringExtra("userId");
        String requestUserId = thisIntent.getStringExtra("requestUserId");
        String isManager = thisIntent.getStringExtra("isManager");
        String clickedLat = thisIntent.getStringExtra("clickedLat");
        String clickedLong = thisIntent.getStringExtra("clickedLong");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_creation);
        EditText subject = findViewById(R.id.create_request_input_subject);
        EditText body = findViewById(R.id.create_request_input_body);
        EditText longitudeInput = findViewById(R.id.longitude_input);
        EditText latitudeInput = findViewById(R.id.latitude_input);
        EditText contactDetails = findViewById(R.id.create_request_input_contact_details);
        RelativeLayout btnAddRequest = findViewById(R.id.button_add_request);
        ImageView btnBackToMap = findViewById(R.id.btn_back_to_map);
        RelativeLayout addCurrLocation = findViewById(R.id.button_add_location);
        SearchView searchView;
        if (clickedLat != null && clickedLong != null) {
            longitudeInput.setText(clickedLong, TextView.BufferType.EDITABLE);
            latitudeInput.setText(clickedLat, TextView.BufferType.EDITABLE);
        }
        // initializing our search view.
        searchView = findViewById(R.id.idSearchView);

        // adding on query listener for our search view.
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // on below line we are getting the
                // location name from search view.
                String location = searchView.getQuery().toString();

                // below line is to create a list of address
                // where we will store the list of all address.
                List<Address> addressList = null;

                // checking if the entered location is null or not.
                if (location != null || location.equals("")) {
                    // on below line we are creating and initializing a geo coder.
                    Geocoder geocoder = new Geocoder(RequestCreation.this);
                    try {
                        // on below line we are getting location from the
                        // location name and adding that location to address list.
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(addressList!= null && addressList.size()!=0) {

                        // on below line we are getting the location
                        // from our list a first position.
                        Address address = addressList.get(0);

                        // on below line we are creating a variable for our location
                        // where we will add our locations latitude and longitude.
                        longitudeInput.setText(String.valueOf(address.getLongitude()));
                        latitudeInput.setText(String.valueOf(address.getLatitude()));
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        btnAddRequest.setOnClickListener(v -> {
            String subjectTxt = subject.getText().toString();
            String bodyTxt = body.getText().toString();
            //TODO: deal with converting double and string, also with intents. try to avoid some conversions
            String longitudeTxt = longitudeInput.getText().toString();
            String latitudeTxt = latitudeInput.getText().toString();
            String contactDetailsTxt = contactDetails.getText().toString();
            int randomNum = ThreadLocalRandom.current().nextInt(0, 10000 + 1);
            String setRequestId = String.valueOf(randomNum);
            //to avoid repeating the same requestId
            HashSet<String> takenRequestsIds = (HashSet<String>) thisIntent.getExtras().getSerializable("takenRequestsIds");
            while (takenRequestsIds.contains(setRequestId)) {
                //change until it's a new request number
                randomNum = ThreadLocalRandom.current().nextInt(0, 10000 + 1);
                setRequestId = String.valueOf(randomNum);
            }
            if (subjectTxt.isEmpty() || bodyTxt.isEmpty() | latitudeTxt.isEmpty() | longitudeTxt.isEmpty() | contactDetailsTxt.isEmpty()) {
                Toast.makeText(RequestCreation.this, "Please fill in all the request details", Toast.LENGTH_SHORT).show();
            } else {
                // Add a new marker with the request ID to markersDB
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
                        String creationTime = "";
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            creationTime = LocalDateTime.now().toString();
                        }
                        GeoPoint geoPointRequest = new GeoPoint(doubleLatitude, doubleLongitude);
                        HashMap<String, Object> user = new HashMap<>();
                        user.put("geoPoint", geoPointRequest);
                        user.put("requestId", setRequestId);
                        user.put("userId", requestUserId);
                        user.put("isManager", isManager);
                        user.put("creationTime", creationTime);
                        String finalSetRequestId = setRequestId;
                        String finalCreationTime = creationTime;
                       //  //Add by server
                        postRequest(finalSetRequestId, bodyTxt, userId, subjectTxt, contactDetailsTxt, String.valueOf(geoPointRequest.getLatitude()), String.valueOf(geoPointRequest.getLongitude()), finalCreationTime, requestUserId, isManager, markersDb);
                    } else {
                        Toast.makeText(RequestCreation.this, "Please fill in valid longitude (-180 to 180) and latitude (-90 to 90)", Toast.LENGTH_SHORT).show();
                    }
                } else {
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
            } else {
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                        .addOnSuccessListener(RequestCreation.this, location -> {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                longitudeInput.setText(String.valueOf(location.getLongitude()), TextView.BufferType.EDITABLE);
                                latitudeInput.setText(String.valueOf(location.getLatitude()), TextView.BufferType.EDITABLE);
                            } else {
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
            } else {
                Toast.makeText(RequestCreation.this, "permission is NOT granted", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void postRequest(String requestId, String body, String userId, String subject, String contactDetails, String locationLat, String locationLang, String creationTime, String requestUserId, String isManager, FirebaseFirestore markersDb) {
        new Thread(() -> {
            String urlString = IPv4_Address;
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address

            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                RequestCreation.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(RequestCreation.this, "Server is down, can't upload new request. please contact Shavit", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("requestId", requestId);
                json.put("body", body);
                json.put("subject", subject);
                json.put("contactDetails", contactDetails);
                json.put("userId", userId);
                json.put("creationTime", creationTime);
                json.put("locationLat", locationLat);
                json.put("locationLang", locationLang);
                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
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
                if (result.equals("\"success\"")) {
                    //add marker
                    GeoPoint geoPointRequest = new GeoPoint(Double.valueOf(locationLat), Double.valueOf(locationLang));
                    HashMap<String, Object> user = new HashMap<>();
                    user.put("geoPoint", geoPointRequest);
                    user.put("requestId", requestId);
                    user.put("userId", requestUserId);
                    user.put("isManager", isManager);
                    user.put("creationTime", creationTime);
                    databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            markersDb.collection("MapsData")
                                    .add(user)
                                    .addOnSuccessListener(documentReference -> {
                                    })
                                    .addOnFailureListener(e -> {
                                    });
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
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }

    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(RequestCreation.this, "Server is down, can't upload new request. please contact Shavit", Toast.LENGTH_SHORT).show());
    }
}




//https://stackoverflow.com/questions/20438627/getlastknownlocation-returns-null
//https://www.codejava.net/java-se/networking/java-socket-server-examples-tcp-ip
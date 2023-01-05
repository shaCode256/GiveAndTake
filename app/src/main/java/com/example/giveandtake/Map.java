package com.example.giveandtake;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class Map extends FragmentActivity implements OnMapReadyCallback {
    protected Context context;
    private GoogleMap mMap;
    FirebaseFirestore db;
    HashMap<String, Marker> markersHashmap = new HashMap<>();
    HashMap<String, String> markersRequestToDocId = new HashMap<>();
    public static HashSet<String> takenRequestsIds = new HashSet<>();
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel= new NotificationChannel("My notification", "My notification",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager= getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Button createRequestBtn= findViewById(R.id.btn_create_request_from_map);
        Button btnLocateMe= findViewById(R.id.btn_locate_me);
        Button btnMyRequests= findViewById(R.id.btn_my_requests);
        Button btnRequestReports= findViewById(R.id.btn_watch_request_reports);
        Button logOutBtn= findViewById(R.id.log_out_btn);
        Button settingsBtn= findViewById(R.id.btn_settings);
        Button manageUsersBtn= findViewById(R.id.manage_users_btn);
        db = FirebaseFirestore.getInstance();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        Intent thisIntent = getIntent();
        String userId = thisIntent.getStringExtra("userId");
        String isManager= thisIntent.getStringExtra("isManager");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String lastTimeSeenMap= LocalDateTime.now().toString();
            databaseReference.child("users").child(userId).child("lastTimeSeenMap").setValue(lastTimeSeenMap);
        }
        // if the user is not a manager
        if (isManager!=null &&isManager.equals("0")){
            manageUsersBtn.setVisibility(View.GONE);
            btnRequestReports.setVisibility(View.GONE);
        }

        btnRequestReports.setOnClickListener(view -> {
            Intent watchRequestReportsIntent = new Intent(Map.this, ViewReportedRequests.class);
            watchRequestReportsIntent.putExtra("userId",userId);
            watchRequestReportsIntent.putExtra("isManager", isManager);
            watchRequestReportsIntent.putExtra("markersRequestToDocId", markersRequestToDocId);
            startActivity(watchRequestReportsIntent);
        });

        createRequestBtn.setOnClickListener(view -> {
            Intent createRequestIntent = new Intent(Map.this, RequestCreation.class);
            createRequestIntent.putExtra("userId",userId);
            createRequestIntent.putExtra("isManager", isManager);
            createRequestIntent.putExtra("requestUserId", userId);
            createRequestIntent.putExtra("takenRequestsIds", takenRequestsIds);
            startActivity(createRequestIntent);
        });

        logOutBtn.setOnClickListener(view -> {
            stopNotificationService();
            Intent loginIntent = new Intent(Map.this, Login.class);
            startActivity(loginIntent);
        });

        manageUsersBtn.setOnClickListener(view -> {
            Intent manageUsersIntent = new Intent(Map.this, ManageUsers.class);
            manageUsersIntent.putExtra("markersRequestToDocId", markersRequestToDocId);
            manageUsersIntent.putExtra("userId", userId);
            manageUsersIntent.putExtra("isManager", isManager);
            startActivity(manageUsersIntent);
        });

        btnLocateMe.setOnClickListener(v -> {
            if (
                    ContextCompat.checkSelfPermission(Map.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(Map.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(Map.this, "please enable permissions", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(Map.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 90);
            }
            else{
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                        .addOnSuccessListener(Map.this, location -> {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17.0f));
                            }
                            else{
                                Toast.makeText(Map.this, "Can't use your location.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        btnMyRequests.setOnClickListener(view -> {
            Intent viewMyRequestsIntent = new Intent(Map.this, ViewMyRequests.class);
            viewMyRequestsIntent.putExtra("userId",userId);
            viewMyRequestsIntent.putExtra("requestUserId",userId);
            viewMyRequestsIntent.putExtra("isManager", isManager);
            viewMyRequestsIntent.putExtra("markersRequestToDocId", markersRequestToDocId);
            startActivity(viewMyRequestsIntent);
        });

        settingsBtn.setOnClickListener(view -> {
            Intent viewMyRequestsIntent = new Intent(Map.this, Settings.class);
            viewMyRequestsIntent.putExtra("userId",userId);
            viewMyRequestsIntent.putExtra("isManager", isManager);
            startActivity(viewMyRequestsIntent);
        });
    }


    public Bitmap resizeBitmap(String drawableName,int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(drawableName, "drawable", getPackageName()));
        //TODO: Try to fix Warning:(149, 89) Use of this function is discouraged because resource reflection makes it
        // harder to perform build optimizations and compile-time verification of code. It is much more efficient
        // to retrieve resources by identifier (e.g. `R.foo.bar`) than by name (e.g. `getIdentifier("bar", "foo", null)`).
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        double lat= 32.107193634412475;
        double lang= 35.20542059093714;
        LatLng defaultLocation = new LatLng(lat, lang);
        float DEFAULT_ZOOM= 15;
        mMap.moveCamera(CameraUpdateFactory
                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
        new Thread(){
            @Override
            public void run(){
                //Listen to multiple documents in a collection. adds markers of the requests in the db (docs)
                db.collection("MapsData")
                        .addSnapshotListener((value, e) -> {
                                    if (e != null) {
                                        Log.w(TAG, "Listen failed.", e);
                                        return;
                                    }
                                    assert value != null;
                                    BitmapDescriptor handIcon= BitmapDescriptorFactory.fromBitmap(resizeBitmap("hand", 85, 85));
                                    BitmapDescriptor starIcon= BitmapDescriptorFactory.fromBitmap(resizeBitmap("star_icon", 85, 85));
                            for (QueryDocumentSnapshot doc : value) {
                                        if (doc.get("geoPoint") != null) {
                                            //TODO: add a check id geoPoint is an instance of GeoPoint class! throws exception if not.
                                            GeoPoint geoPoint= doc.getGeoPoint("geoPoint");
                                            assert geoPoint != null;
                                            LatLng location = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                            String requestId= doc.getString("requestId");
                                            String requestUserId= doc.getString("userId");
                                            String isManager= doc.getString("isManager");
                                            String creationTime= doc.getString("creationTime");
                                            //to check if requestUseId is manager: change the icon
                                            BitmapDescriptor selectedIcon= handIcon;
                                            takenRequestsIds.add(requestId);
                                            if(isManager!=null && isManager.equals("1")) {
                                                selectedIcon= starIcon;
                                            }
                                            Marker newMarker = mMap.addMarker(new MarkerOptions().position(location).title(requestId).icon(selectedIcon).snippet(creationTime));
                                            assert newMarker != null;
                                            newMarker.setTag(requestUserId);
                                            markersHashmap.put(requestId,newMarker);
                                            markersRequestToDocId.put(requestId, doc.getId());
                                        }
                                    }
                                }
                        );
            }
        }.run();

        startNotificationService();

        // adding on click listener to marker of google maps.
        mMap.setOnMapLongClickListener(latLng -> {
            //passing userId to request
            Intent thisIntent = getIntent();
            String userId = thisIntent.getStringExtra("userId");
            String isManager= thisIntent.getStringExtra("isManager");
            Intent createRequestIntent = new Intent(Map.this, RequestCreation.class);
            createRequestIntent.putExtra("userId",userId);
            createRequestIntent.putExtra("requestUserId", userId);
            createRequestIntent.putExtra("clickedLat", String.valueOf(latLng.latitude));
            createRequestIntent.putExtra("clickedLong",String.valueOf(latLng.longitude));
            createRequestIntent.putExtra("takenRequestsIds", takenRequestsIds);
            createRequestIntent.putExtra("isManager", isManager);
            startActivity(createRequestIntent);
        });

        mMap.setOnMarkerClickListener(marker -> {
            String requestUserId = Objects.requireNonNull(marker.getTag()).toString();
            Intent thisIntent = getIntent();
            String requestId= marker.getTitle();
            String userId = thisIntent.getStringExtra("userId");
            String isManager= thisIntent.getStringExtra("isManager");
            if(requestId!=null) {
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String requestSubject = snapshot.child(requestUserId).child("requestId").child(requestId).child("subject").getValue(String.class);
                        String requestBody = snapshot.child(requestUserId).child("requestId").child(requestId).child("body").getValue(String.class);
                        String contactDetails = snapshot.child(requestUserId).child("requestId").child(requestId).child("contactDetails").getValue(String.class);
                        String requestLatitude = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("location").child("latitude").getValue(Double.class));
                        String requestLongitude = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("location").child("longitude").getValue(Double.class));
                        String creationTime = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("creationTime").getValue(String.class));
                        // open view request activity
                        if(markersHashmap.get(requestId)!=null) {
                            String docId = markersRequestToDocId.get(requestId);
                            Intent viewRequestIntent = new Intent(Map.this, ViewRequest.class);
                            viewRequestIntent.putExtra("requestSubject", requestSubject);
                            viewRequestIntent.putExtra("requestBody", requestBody);
                            viewRequestIntent.putExtra("contactDetails", contactDetails);
                            viewRequestIntent.putExtra("requestLatitude", requestLatitude);
                            viewRequestIntent.putExtra("requestLongitude", requestLongitude);
                            viewRequestIntent.putExtra("requestUserId", requestUserId);
                            viewRequestIntent.putExtra("userId", userId);
                            viewRequestIntent.putExtra("isManager", isManager);
                            viewRequestIntent.putExtra("docId", docId);
                            viewRequestIntent.putExtra("requestId", requestId);
                            viewRequestIntent.putExtra("creationTime", creationTime);
                            startActivity(viewRequestIntent);
                        }
                        else{
                            Toast.makeText(Map.this, "Oops! This request isn't available", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }
            return true;
        });

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 90) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(Map.this, "permission is granted", Toast.LENGTH_SHORT).show();
                // Permission is granted. Continue the action or workflow of app
            } else {
                Toast.makeText(Map.this, "permission is NOT granted", Toast.LENGTH_SHORT).show();
                // Explain to the user the importance of accepting
            }
        }
    }

    public void startNotificationService() {
        stopNotificationService();
        Intent serviceIntent = new Intent(this, NotificationService.class);
        Intent thisIntent = getIntent();
        String userId = thisIntent.getStringExtra("userId");
        //check if notifications are turned on in this user's settings
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(userId).child("settings").child("notifications").child("turnedOn").getValue()!=null) {
                    String notificationsTurnedOn = snapshot.child(userId).child("settings").child("notifications").child("turnedOn").getValue().toString();
                    if (notificationsTurnedOn.equals("1")) {
                        String isManager = thisIntent.getStringExtra("isManager");
                        serviceIntent.putExtra("userId", userId);
                        serviceIntent.putExtra("isManager", isManager);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            serviceIntent.putExtra("lastTimeSeenMapStr",  LocalDateTime.now().toString());
                        }
                        ContextCompat.startForegroundService(Map.this, serviceIntent);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void stopNotificationService() {
        Intent serviceIntent = new Intent(this, NotificationService.class);
        stopService(serviceIntent);
    }

}


//https://firebase.google.com/docs/firestore/query-data/listen
//https://stackoverflow.com/questions/52389072/google-maps-custom-marker-too-large
//https://www.youtube.com/watch?v=4BuRMScaaI4 //How to create Notification in Android tutorial
//https://stackoverflow.com/questions/72159435/how-to-get-location-using-fusedlocationclient-getcurrentlocation-method-in-kot/74254933#74254933
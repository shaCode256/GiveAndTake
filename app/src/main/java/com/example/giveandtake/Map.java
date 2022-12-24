package com.example.giveandtake;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class Map extends FragmentActivity implements OnMapReadyCallback {
    protected Context context;
    private GoogleMap mMap;
    FirebaseFirestore db;
    HashMap<String, Marker> markersHashmap = new HashMap<>();
    HashMap<String, String> markersRequestToDocId = new HashMap<>();
    public static HashSet<String> taken_requests_ids = new HashSet<>();
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
       // mMap.setMyLocationEnabled(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Button create_request_btn= findViewById(R.id.btn_create_request_from_map);
        Button btn_locate_me= findViewById(R.id.btn_locate_me);
        Button btn_my_requests= findViewById(R.id.btn_my_requests);
        Button btn_request_reports= findViewById(R.id.btn_watch_request_reports);
        Button log_out_btn= findViewById(R.id.log_out_btn);
        Button manage_users_btn= findViewById(R.id.manage_users_btn);
        db = FirebaseFirestore.getInstance();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        Intent myIntent = getIntent();
        String userId = myIntent.getStringExtra("userId");
        String isManager= myIntent.getStringExtra("isManager");
        // if the user is not a manager
        if (isManager!=null &&isManager.equals("0")){
            manage_users_btn.setVisibility(View.GONE);
            btn_request_reports.setVisibility(View.GONE);
        }

        btn_request_reports.setOnClickListener(view -> {
            Intent watch_request_reports_intent = new Intent(Map.this, ViewReportedRequests.class);
            watch_request_reports_intent.putExtra("userId",userId);
            watch_request_reports_intent.putExtra("isManager", isManager);
            watch_request_reports_intent.putExtra("markersRequestToDocId", markersRequestToDocId);
            startActivity(watch_request_reports_intent);
        });

        create_request_btn.setOnClickListener(view -> {
            Intent create_request_intent = new Intent(Map.this, RequestCreation.class);
            create_request_intent.putExtra("userId",userId);
            create_request_intent.putExtra("isManager", isManager);
            create_request_intent.putExtra("taken_requests_ids", taken_requests_ids);
            startActivity(create_request_intent);
        });

        log_out_btn.setOnClickListener(view -> {
            Intent login_intent = new Intent(Map.this, Login.class);
            startActivity(login_intent);
        });

        manage_users_btn.setOnClickListener(view -> {
            createNotification();
            Intent manage_users_intent = new Intent(Map.this, ManageUsers.class);
            manage_users_intent.putExtra("markersRequestToDocId", markersRequestToDocId);
            manage_users_intent.putExtra("userId", userId);
            manage_users_intent.putExtra("isManager", isManager);
            startActivity(manage_users_intent);
        });

        btn_locate_me.setOnClickListener(v -> {
            if (
                    ContextCompat.checkSelfPermission(Map.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(Map.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(Map.this, "please enable permissions", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(Map.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, 90);
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

        btn_my_requests.setOnClickListener(view -> {
            Intent view_my_requests_intent = new Intent(Map.this, ViewMyRequests.class);
            view_my_requests_intent.putExtra("userId",userId);
            view_my_requests_intent.putExtra("requestUserId",userId);
            view_my_requests_intent.putExtra("isManager", isManager);
            view_my_requests_intent.putExtra("markersRequestToDocId", markersRequestToDocId);
            startActivity(view_my_requests_intent);
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
                                            //to check if requestUseId is manager: change the icon
                                            BitmapDescriptor selected_icon= handIcon;
                                            taken_requests_ids.add(requestId);
                                            if(isManager!=null && isManager.equals("1")) {
                                                selected_icon= starIcon;
                                            }
                                            Marker newMarker = mMap.addMarker(new MarkerOptions().position(location).title(requestId).icon(selected_icon));
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
        // adding on click listener to marker of google maps.
        mMap.setOnMapLongClickListener(latLng -> {
            //passing userId to request
            Intent thisIntent = getIntent();
            String userId = thisIntent.getStringExtra("userId");
            String isManager= thisIntent.getStringExtra("isManager");
            Intent newIntent = new Intent(Map.this, RequestCreation.class);
            newIntent.putExtra("userId",userId);
            newIntent.putExtra("requestUserId", userId);
            newIntent.putExtra("clickedLat", String.valueOf(latLng.latitude));
            newIntent.putExtra("clickedLong",String.valueOf(latLng.longitude));
            newIntent.putExtra("taken_requests_ids", taken_requests_ids);
            newIntent.putExtra("isManager", isManager);
            startActivity(newIntent);
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
                        String contactDetails = snapshot.child(requestUserId).child("requestId").child(requestId).child("contact_details").getValue(String.class);
                        String requestLatitude = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("location").child("latitude").getValue(Double.class));
                        String requestLongitude = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("location").child("longitude").getValue(Double.class));
                        String creationTime = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("creation_time").getValue(String.class));
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

    private void createNotification() {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, Map.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent MapIntent = PendingIntent.getActivity(Map.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(Map.this, "My notification")
                .setSmallIcon(R.drawable.star_icon)
                .setContentTitle("New Requests!")
                .setContentText("Hello World!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(MapIntent)
                .setAutoCancel(true);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(Map.this);
        managerCompat.notify(1, builder.build());
    }
}


//https://firebase.google.com/docs/firestore/query-data/listen
//https://stackoverflow.com/questions/52389072/google-maps-custom-marker-too-large
//https://stackoverflow.com/questions/72159435/how-to-get-location-using-fusedlocationclient-getcurrentlocation-method-in-kot/74254933#74254933
package com.example.giveandtake;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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

    private GoogleMap mMap;
    FirebaseFirestore db;
    HashMap<String, Marker> markersHashmap = new HashMap<>();
    public static HashSet<String> taken_requests_ids = new HashSet<>();
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent thisIntent = getIntent();
        String userId = thisIntent.getStringExtra("userId");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Button create_request_btn= findViewById(R.id.btn_create_request_from_map);
        Button btn_locate_me= findViewById(R.id.btn_locate_me);
        Button btn_my_requests= findViewById(R.id.btn_my_requests);
        Button log_out_btn= findViewById(R.id.logOutBtn);
        // initializing our firebase FireStore.
        db = FirebaseFirestore.getInstance();
        Intent mIntent = getIntent();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        create_request_btn.setOnClickListener(view -> {
            // open create request activity
            Intent myIntent = new Intent(Map.this, RequestCreation.class);
            myIntent.putExtra("userId",userId);
            myIntent.putExtra("taken_requests_ids", taken_requests_ids);
            startActivity(myIntent);
        });

        log_out_btn.setOnClickListener(view -> {
            // open Login activity
            Intent myIntent = new Intent(Map.this, Login.class);
            myIntent.putExtra("userId",userId);
            startActivity(myIntent);
        });

        btn_locate_me.setOnClickListener(view -> {
//            // open Login activity
//            Intent myIntent = new Intent(Map.this, Login.class);
//            myIntent.putExtra("userId",userId);
//            startActivity(myIntent);
        });

        btn_my_requests.setOnClickListener(view -> {
//            // open Login activity
//            Intent myIntent = new Intent(Map.this, Login.class);
//            myIntent.putExtra("userId",userId);
//            startActivity(myIntent);
        });
    }

    public Bitmap resizeBitmap(String drawableName,int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(drawableName, "drawable", getPackageName()));
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        //Listen to multiple documents in a collection. adds markers of the requests in the db (docs)
        db.collection("MapsData")
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                            assert value != null;
                            for (QueryDocumentSnapshot doc : value) {
                        if (doc.get("geoPoint") != null) {
                            //TODO: add a check id geoPoint is an instance of GeoPoint class! throws exception if not.
                            GeoPoint geoPoint= doc.getGeoPoint("geoPoint");
                            assert geoPoint != null;
                            LatLng location = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                            String requestId= doc.getString("requestId");
                            String requestUserId= doc.getString("userId");
                            //resize pic to be a marker icon programmatically
                            taken_requests_ids.add(requestId);
                            Marker newMarker= mMap.addMarker(new MarkerOptions().position(location).title(requestId).icon(BitmapDescriptorFactory.fromBitmap(resizeBitmap("hand",85,85))));
                            newMarker.setTag(requestUserId);
                            markersHashmap.put(doc.getId(),newMarker);
                           // mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 10.0f));
                        }
                    }
                }
                );

        // adding on click listener to marker of google maps.
        mMap.setOnMapLongClickListener(latLng -> {
            //passing userId to request
            Intent thisIntent = getIntent();
            String userId = thisIntent.getStringExtra("userId");
            Intent newIntent = new Intent(Map.this, RequestCreation.class);
            newIntent.putExtra("userId",userId);
            newIntent.putExtra("clickedLat", String.valueOf(latLng.latitude));
            newIntent.putExtra("clickedLong",String.valueOf(latLng.longitude));
            newIntent.putExtra("taken_requests_ids", taken_requests_ids);
            startActivity(newIntent);
        });

        //TODO: add storing userId to marker and not only for this intent (for requests which are viewed by other users, not the creator)
        mMap.setOnMarkerClickListener(marker -> {
            // marker.remove(); //removes marker by clicking on it
            Intent thisIntent = getIntent();
            String requestUserId = marker.getTag().toString();
            String requestId= marker.getTitle();
            if(requestId!=null) {
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String getRequestSubject = snapshot.child(requestUserId).child("requestId").child(requestId).child("subject").getValue(String.class);
                        String getRequestBody = snapshot.child(requestUserId).child("requestId").child(requestId).child("body").getValue(String.class);
                        String getContactDetails = snapshot.child(requestUserId).child("requestId").child(requestId).child("contact_details").getValue(String.class);
                        String getRequestLatitude = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("location").child("latitude").getValue(Double.class));
                        String getRequestLongitude = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("location").child("longitude").getValue(Double.class));
                        Toast.makeText(Map.this, "Subject: " + getRequestSubject + "\n Body:" + getRequestBody + "\n Contact Details: " + getContactDetails + "\n Location Longitude: " + getRequestLongitude + "\n Location Latitude: " + getRequestLatitude, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }
            return true;
        });

    }


    public void removeMarker(String docId){
        //removes a marker by it's document id
        if(markersHashmap.get(docId)!=null){
            Objects.requireNonNull(markersHashmap.get(docId)).remove();
        }
    }


//    public void removeMarker(String markerId){
//        //removes a marker by it's document id
//        if(markersHashmap.get("7QWDor9vozLaHdFYV9kh")!=null){
//            markersHashmap.get("7QWDor9vozLaHdFYV9kh").remove();
//        }
//    }

}


//https://firebase.google.com/docs/firestore/query-data/listen
//https://stackoverflow.com/questions/52389072/google-maps-custom-marker-too-large
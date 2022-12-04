package com.example.giveandtake;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Objects;


public class Map extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    FirebaseFirestore db;
    HashMap<String, Marker> markersHashmap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        final Button create_rqst_btn= findViewById(R.id.btn_create_rqst_from_map);
        final Button btn_locate_me= findViewById(R.id.btn_locate_me);
        final Button btn_my_requests= findViewById(R.id.btn_my_requests);
        final Button log_out_btn= findViewById(R.id.logOutBtn);
        // initializing our firebase FireStore.
        db = FirebaseFirestore.getInstance();
        Intent mIntent = getIntent();
        String userId = mIntent.getStringExtra("userId");
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        create_rqst_btn.setOnClickListener(view -> {
            // open create request activity
            Intent myIntent = new Intent(Map.this, RequestCreation.class);
            myIntent.putExtra("userId",userId);
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
                            //resize pic to be a marker icon programmatically
                            markersHashmap.put(doc.getId(), mMap.addMarker(new MarkerOptions().position(location).title(geoPoint.getLatitude()+", "+geoPoint.getLongitude()).icon(BitmapDescriptorFactory.fromBitmap(resizeBitmap("hand",85,85)))));
                           // mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 10.0f));
                        }
                    }
                }
                );

        // adding on click listener to marker of google maps.
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                //passing userId to request
                Intent thisIntent = getIntent();
                String userId = thisIntent.getStringExtra("userId");
                Toast.makeText(Map.this, "Clicked location is " + latLng, Toast.LENGTH_SHORT).show();
                Intent newIntent = new Intent(Map.this, RequestCreation.class);
                newIntent.putExtra("userId",userId);
                newIntent.putExtra("clickedLat", String.valueOf(latLng.latitude));
                newIntent.putExtra("clickedLong",String.valueOf(latLng.longitude));
                startActivity(newIntent);
            }
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
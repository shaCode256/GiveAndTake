package com.example.giveandtake;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;


public class NotificationService extends Service {

    String userId= "";
    String isManager="";
    String lastTimeSeenMapStr;
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");

    public void onCreate(Intent intents, int flags, int startId) {
        super.onCreate();
        Intent intent = new Intent(this, Map.class);
        intent.putExtra("userId", userId);
        intent.putExtra("isManager", isManager);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent MapIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "My notification")
                .setSmallIcon(R.drawable.star_icon)
                .setContentTitle("New notification- onCreate!")
                .setContentText("in your area!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(MapIntent)
                .setAutoCancel(true);
        startForeground(1, builder.build());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userId = intent.getStringExtra("userId");
        isManager = intent.getStringExtra("isManager");
        lastTimeSeenMapStr = intent.getStringExtra("lastTimeSeenMapStr");
        intent.putExtra("userId", userId);
        intent.putExtra("isManager", isManager);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent MapIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "My notification")
                .setSmallIcon(R.drawable.star_icon)
                .setContentTitle("Searching for Requests...!")
                .setContentText("around you")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(MapIntent)
                .setAutoCancel(true);
        startForeground(1, builder.build());
        Timer myTimer = new Timer ();
        TimerTask myTask = new TimerTask () {
            @Override
            public void run() {
                HashMap<String, HashMap<LatLng, String>> markersHashmap = new HashMap<>();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
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
                                            GeoPoint geoPoint = doc.getGeoPoint("geoPoint");
                                            assert geoPoint != null;
                                            LatLng locationn = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                            String requestId = doc.getString("requestId");
                                            String requestUserId = doc.getString("userId");
                                            String creation_time = doc.getString("creationTime");
                                            //to check if requestUseId is manager: change the icon
                                            markersHashmap.put(requestId, new HashMap<LatLng, String>());
                                            markersHashmap.get(requestId).put(locationn, creation_time);
                                        }
                                    }
                                }
                        );
                createNotification(userId, isManager, lastTimeSeenMapStr, 20000000000000000f, markersHashmap);
            }
        };
        myTimer.scheduleAtFixedRate(myTask, 0l, 1 * (60*1000)); // Runs every 1 min
        return START_NOT_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void createNotification(String userId, String isManager, String lastTimeSeenMapStr, float distance, HashMap<String, HashMap<LatLng, String>> markersHashmap) {
        AtomicReference<Location> setLocation= new AtomicReference<>();
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(userId).child("settings").child("notifications").child("auto_detect_location").getValue()!=null) {
                    String auto_detect_location = snapshot.child(userId).child("settings").child("notifications").child("auto_detect_location").getValue().toString();
                    if (auto_detect_location.equals("1") || snapshot.child(userId).child("settings").child("notifications").child("specific_location").getValue() == null) {
                        if (
                                ContextCompat.checkSelfPermission(NotificationService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                                        ContextCompat.checkSelfPermission(NotificationService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(NotificationService.this, "please enable location permissions", Toast.LENGTH_SHORT).show();
                        } else {
                            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(NotificationService.this);
                            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                                    .addOnSuccessListener(location -> {
                                        if (location != null) {
                                            setLocation.set(location);
                                            if(snapshot.child(userId).child("settings").child("notifications").child("distance").getValue()!=null){
                                                float distanceSpecified= Float.parseFloat(snapshot.child(userId).child("settings").child("notifications").child("distance").getValue().toString()) *1000;
                                                showMarkersClose(setLocation.get(), distanceSpecified, lastTimeSeenMapStr, userId, isManager, markersHashmap);
                                            }
                                            else{
                                                if(setLocation.get()!=null) {
                                                    showMarkersClose(setLocation.get(), distance, lastTimeSeenMapStr, userId, isManager, markersHashmap);
                                                }
                                            }
                                        } else {
                                            Toast.makeText(NotificationService.this, "Can't use your location.", Toast.LENGTH_SHORT).show();
                                            // Get the location manager
                                            //showMarkersClose(setLocation.get(), distance, lastTimeSeenMapStr, userId, isManager, markersHashmap);
                                        }
                                    });
                        }
                    }
                    else{
                        //get the specific location2
                        String specified_latitude_str = snapshot.child(userId).child("settings").child("notifications").child("specific_location").child("latitude").getValue().toString();
                        String specified_longitude_str = snapshot.child(userId).child("settings").child("notifications").child("specific_location").child("longitude").getValue().toString();
                        Location location= new Location(LocationManager.GPS_PROVIDER);
                        location.setLongitude(Float.parseFloat(specified_latitude_str));
                        location.setLatitude(Float.parseFloat(specified_longitude_str));
                        setLocation.set(location);
                        if(snapshot.child(userId).child("settings").child("notifications").child("distance").getValue()!=null){
                            float distanceSpecified= Float.parseFloat(snapshot.child(userId).child("settings").child("notifications").child("distance").getValue().toString()) *1000;
                            showMarkersClose(setLocation.get(), distanceSpecified, lastTimeSeenMapStr, userId, isManager, markersHashmap);
                        }
                        else{
                            if(setLocation.get()!=null) {
                                showMarkersClose(setLocation.get(), distance, lastTimeSeenMapStr, userId, isManager, markersHashmap);
                            }
                        }
                    }


                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



    }

    private void showMarkersClose(Location location, float distance, String lastTimeSeenMapStr, String userId, String isManager,   HashMap<String, HashMap<LatLng, String>> markersHashmap) {
        //Listen to multiple documents in a collection. adds markers of the requests in the db (docs)
        for(HashMap<LatLng, String> marker : markersHashmap.values()) {
            for(LatLng markerPosition : marker.keySet()){
            double markerLat= markerPosition.latitude;
            double markerLong= markerPosition.longitude;
            Location markerLocation = new Location(LocationManager.GPS_PROVIDER);
            markerLocation.setLatitude(markerLat);
            markerLocation.setLongitude(markerLong);
            String markerCreationTime= marker.get(markerPosition);
                //convert string to time
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && markerCreationTime!=null) {
                LocalDateTime markerDateTime = LocalDateTime.parse(markerCreationTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                LocalDateTime lastTimeSeenMap = LocalDateTime.parse(lastTimeSeenMapStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                if (location!=null && markerLocation!=null && location.distanceTo(markerLocation) <= distance &&
                        markerDateTime.isAfter(lastTimeSeenMap)) {
                    // Create an explicit intent for an Activity in your app
                    Intent intent = new Intent(this, Map.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("isManager", isManager);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent MapIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "My notification")
                            .setSmallIcon(R.drawable.star_icon)
                            .setContentTitle("New Requests!")
                            .setContentText("in your area!")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            // Set the intent that will fire when the user taps the notification
                            .setContentIntent(MapIntent)
                            .setAutoCancel(true);
                    startForeground(1, builder.build());
                }
            }
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
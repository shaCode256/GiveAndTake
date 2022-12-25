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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;


public class NotificationService extends Service {

    String userId= "";
    String isManager="";
    HashMap<String, HashMap<LatLng, String>> markersHashmap = new HashMap<>();

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
        intent.putExtra("userId", userId);
        intent.putExtra("isManager", isManager);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent MapIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "My notification")
                .setSmallIcon(R.drawable.star_icon)
                .setContentTitle("Searching for Requests...!")
                .setContentText("around you")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(MapIntent)
                .setAutoCancel(true);
        startForeground(1, builder.build());
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
        createNotification(userId, isManager, 20000000000000000f );
        return START_NOT_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void createNotification(String userId, String isManager, float distance) {
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
        {
            Toast.makeText(this, "please enable location permissions", Toast.LENGTH_SHORT).show();
        }
        else{
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            String lastTimeSeenMapStr= null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                lastTimeSeenMapStr = LocalDateTime.now().toString();
                            }
                            //databaseReference.child("users").child(userId).child("lastTimeSeenMap").toString();
                            showMarkersClose(location, distance, lastTimeSeenMapStr, userId, isManager);
                        }
                        else{
                            Toast.makeText(this, "Can't use your location.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showMarkersClose(Location location, float distanceInMeteters, String lastTimeSeenMapStr, String userId, String isManager) {
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
                if (location.distanceTo(markerLocation) <= distanceInMeteters &&
                        markerDateTime.isBefore(lastTimeSeenMap)) {
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
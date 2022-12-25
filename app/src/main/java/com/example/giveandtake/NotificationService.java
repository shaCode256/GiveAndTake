package com.example.giveandtake;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.Executor;


public class NotificationService extends Service {

    HashMap<String, Marker> markersHashmap = new HashMap<>();
    String userId= "";
    String isManager="";

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
        onStartCommand(intent, flags, startId);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userId = intent.getStringExtra("userId");
        isManager = intent.getStringExtra("isManager");
        markersHashmap= (HashMap<String, Marker> )intent.getExtras().getSerializable("markersHashmap");
        intent.putExtra("userId", userId);
        intent.putExtra("isManager", isManager);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent MapIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "My notification")
                .setSmallIcon(R.drawable.star_icon)
                .setContentTitle("New Notification-onStart!")
                .setContentText("in your area!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(MapIntent)
                .setAutoCancel(true);
        startForeground(1, builder.build());
        Toast.makeText(this, "userId"+userId, Toast.LENGTH_SHORT).show();
        createNotification(userId, isManager, markersHashmap);
        return START_NOT_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void createNotification(String userId, String isManager, HashMap<String, Marker> markersHashmap) {
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
                            showMarkersClose(location, 20000000000000000f, lastTimeSeenMapStr, userId, isManager, markersHashmap);
                        }
                        else{
                            Toast.makeText(this, "Can't use your location.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showMarkersClose(Location location, float distanceInMeteters, String lastTimeSeenMapStr, String userId, String isManager,  HashMap<String, Marker> markersHashmap) {
        Toast.makeText(this, "markersHashmapKeyset"+markersHashmap.keySet(), Toast.LENGTH_SHORT).show();
        for(Marker marker : markersHashmap.values()) {
            LatLng markerPosition= marker.getPosition();
            Toast.makeText(this, "im in show markers.", Toast.LENGTH_SHORT).show();
            double markerLat= markerPosition.latitude;
            double markerLong= markerPosition.longitude;
            Location markerLocation = new Location(LocationManager.GPS_PROVIDER);
            markerLocation.setLatitude(markerLat);
            markerLocation.setLongitude(markerLong);
            String markerCreationTime= marker.getSnippet();
            //convert string to time
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && markerCreationTime!=null) {
                LocalDateTime markerDateTime = LocalDateTime.parse(markerCreationTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                LocalDateTime lastTimeSeenMap= LocalDateTime.parse(lastTimeSeenMapStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                if (location.distanceTo(markerLocation) <= distanceInMeteters &&
                        markerDateTime.isBefore(lastTimeSeenMap)) {
                    // Create an explicit intent for an Activity in your app
                    Toast.makeText(this, "yay"  , Toast.LENGTH_SHORT).show();
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
//                    NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
//                    managerCompat.notify(1, builder.build());
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
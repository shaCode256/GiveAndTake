package com.example.giveandtake.Service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.giveandtake.R;
import com.example.giveandtake.View.Map;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;


public class NotificationService extends Service {

    String userId= "";
    String isManager="";
    String lastTimeSeenMapStr;

   String server_url= "https://giveandtake-server.df.r.appspot.com/";

    public void onCreate(int startId) {
        super.onCreate();
        Intent mapIntent = new Intent(this, Map.class);
        mapIntent.putExtra("userId", userId);
        mapIntent.putExtra("isManager", isManager);
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent MapIntent = PendingIntent.getActivity(this, 0, mapIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "My notification")
                .setSmallIcon(R.drawable.star_icon)
                .setContentTitle("New notification- onCreate!")
                .setContentText("in your area!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(MapIntent)
                .setAutoCancel(true);
        startForeground(1, builder.build());
        System.out.println("start foreground 1");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userId = intent.getStringExtra("userId");
        isManager = intent.getStringExtra("isManager");
        lastTimeSeenMapStr = intent.getStringExtra("lastTimeSeenMapStr");
        Intent mapIntent= new Intent(this,  Map.class);
        mapIntent.putExtra("userId", userId);
        mapIntent.putExtra("isManager", isManager);
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingMapIntent = PendingIntent.getActivity(this, 0, mapIntent, PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "My notification")
                .setSmallIcon(R.drawable.star_icon)
                .setContentTitle("Searching for Requests...!")
                .setContentText("around you")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingMapIntent)
                .setAutoCancel(true);
        //        startForeground(2, builder.build());
        System.out.println("start foreground 2");
        startForeground(1, builder.build());
        Timer myTimer = new Timer ();
        TimerTask myTask = new TimerTask () {
            @Override
            public void run() {
                try {
                    getIsAutoDetectLocation(userId); //start chain process: get isAutoDetectlocation, location, specified distance, start searching
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        myTimer.scheduleAtFixedRate(myTask, 0L, (60 * 1000)); // Runs every 1 min
        return START_NOT_STICKY;
    }


    @SuppressLint("MissingPermission")
    private void createNotification(String userId, String isManager, String lastTimeSeenMapStr, float distance, HashMap<String, HashMap<LatLng, String>> markersHashmap, String autoDetectLocation, GeoPoint specificLocation) {
        AtomicReference<Location> setLocation= new AtomicReference<>();
                    if (autoDetectLocation!= null && autoDetectLocation.equals("1") || specificLocation == null) {
                        System.out.println("autoDetect is On");
                        if (
                                ContextCompat.checkSelfPermission(NotificationService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                        ContextCompat.checkSelfPermission(NotificationService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                        ContextCompat.checkSelfPermission(NotificationService.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                        ) {
                            System.out.println("no location permissions");
                        } else {
                            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(NotificationService.this);
                            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                                    .addOnSuccessListener(location -> {
                                        if (location != null) {
                                            setLocation.set(location);
                                            float distanceSpecified= distance *1000;
                                            showMarkersClose(setLocation.get(), distanceSpecified, lastTimeSeenMapStr, userId, isManager, markersHashmap);
                                        }
                                        else {
                                            Toast.makeText(NotificationService.this, "Can't use your location. turn on location services or enable location tracking in clicking on this app-> settings -> permissions -> location -> all the time", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    }
                    else {
                        System.out.println("autoDetect is Off");
                        //get the specific location
                        Location location = new Location(LocationManager.GPS_PROVIDER);
                        location.setLongitude(specificLocation.getLongitude());
                        location.setLatitude(specificLocation.getLatitude());
                        setLocation.set(location);
                        float distanceSpecified = distance * 1000;
                        showMarkersClose(setLocation.get(), distanceSpecified, lastTimeSeenMapStr, userId, isManager, markersHashmap);
                    }
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
                    if (location != null && location.distanceTo(markerLocation) <= distance && markerDateTime.isAfter(lastTimeSeenMap)) {
                        // Create an explicit intent for an Activity in your app
                        Intent mapIntent = new Intent(this, Map.class);
                        mapIntent.putExtra("userId", userId);
                        mapIntent.putExtra("isManager", isManager);
                        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent MapIntent = PendingIntent.getActivity(this, 0, mapIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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

    public void getMapsDataDocs(String isAutoDetectLocation, GeoPoint specificLocation, float distanceKm) {
        new Thread(() -> {
            String urlString = server_url +"getMapsDataDocs/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
             //   Map.this.runOnUiThread(() -> Toast.makeText(Map.this, "Server is down, can't process the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                assert url != null;
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("error2");
            }
            try {
                assert conn != null;
                InputStream is = conn.getInputStream();
                HashMap<String, HashMap<LatLng, String>> markersHashmap = new HashMap<>();
                String info= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                info= info.substring(2,info.length()-2);
                for (String docStr:
                        info.split(("\\|\\|##"))) {
                    if (docStr.startsWith("\",\"")) {
                        docStr = docStr.substring(3);
                    }
                    JSONObject doc = new JSONObject(docStr);
                    //TODO: add a check id geoPoint is an instance of GeoPoint class! throws exception if not.
                    String geoPointStr = (String) doc.get("geoPoint");
                    String[] geoPointParse = geoPointStr.split(",");
                    LatLng location = new LatLng(Double.parseDouble(geoPointParse[0]), Double.parseDouble(geoPointParse[1]));
                    String requestId = doc.getString("requestId");
                    String creationTime = doc.getString("creationTime");
                        //TODO: add a check id geoPoint is an instance of GeoPoint class! throws exception if not.
                    GeoPoint geoPoint = new GeoPoint(location.latitude, location.longitude);
                    LatLng markerLocation = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                        //to check if requestUseId is manager: change the icon
                    markersHashmap.put(requestId, new HashMap<>());
                    Objects.requireNonNull(markersHashmap.get(requestId)).put(markerLocation, creationTime);
                    createNotification(userId, isManager, lastTimeSeenMapStr, distanceKm, markersHashmap, isAutoDetectLocation, specificLocation);
                }
            } catch (JSONException | IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void getIsAutoDetectLocation(String userId) throws InterruptedException {
        new Thread(() -> {
            String urlString = server_url +"getIsAutoDetectLocation/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
            }
            HttpURLConnection conn = null;
            try {
                assert url != null;
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("error2");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            try {
                assert conn != null;
                InputStream is = conn.getInputStream();
                String isAutoDetectLocation= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                isAutoDetectLocation= isAutoDetectLocation.substring(1,isAutoDetectLocation.length()-1);
                System.out.println("isAutoDetectLocation: "+isAutoDetectLocation);
                getSpecificLocation(userId, isAutoDetectLocation);
                }
            catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void getSpecificLocation(String userId, String isAutoDetectLocation) throws InterruptedException {
        new Thread(() -> {
            String urlString = server_url +"getSpecificLocation/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                //   Map.this.runOnUiThread(() -> Toast.makeText(Map.this, "Server is down, can't process the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                assert url != null;
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("error2");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            try {
                assert conn != null;
                InputStream is = conn.getInputStream();
                String location= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                location= location.substring(1,location.length()-2);
                String [] locationArray= location.split(",");
                try {
                    GeoPoint specificLocation = new GeoPoint(Double.parseDouble(locationArray[0]), Double.parseDouble(locationArray[1]));
                    getDistance(userId, isAutoDetectLocation, specificLocation);
                }
                catch(Exception e){
                    System.out.println("problem with specific location: "+e);
                }
                }
             catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void getDistance(String userId, String isAutoDetectLocation, GeoPoint specificLocation) {
        new Thread(() -> {
            String urlString = server_url +"getDistance/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                //   Map.this.runOnUiThread(() -> Toast.makeText(Map.this, "Server is down, can't process the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                assert url != null;
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("error2");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            try {
                assert conn != null;
                InputStream is = conn.getInputStream();
                String distanceStr= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                float distanceKm= Float.parseFloat(distanceStr.substring(1,distanceStr.length()-1));
                getMapsDataDocs(isAutoDetectLocation, specificLocation, distanceKm);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }



}
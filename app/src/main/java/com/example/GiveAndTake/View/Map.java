package com.example.giveandtake.View;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.giveandtake.R;
import com.example.giveandtake.Service.NotificationService;
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
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Map extends FragmentActivity implements OnMapReadyCallback {
    String stringRequestDetails="";
   String server_url= "https://giveandtake-server.df.r.appspot.com/";
    private GoogleMap mMap;
    static String [] details;
    boolean isRunning= true;

    //  CountDownLatch latch = new CountDownLatch(1);

    String isNotificationsTurnedOn= "1";
    HashMap<String, Marker> markersHashmap = new HashMap<>();
    HashMap<String, String> markersRequestToDocId = new HashMap<>();
    public static HashSet<String> takenRequestsIds = new HashSet<>();


    SearchView searchView;
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
        RelativeLayout loadingPanel= findViewById(R.id.loadingPanel);
        loadingPanel.setVisibility(View.GONE);
        ImageView createRequestBtn= findViewById(R.id.btn_create_request_from_map);
        ImageView btnLocateMe= findViewById(R.id.btn_locate_me);
        ImageView btnMyRequests= findViewById(R.id.btn_my_requests);
        ImageView btnRequestReports= findViewById(R.id.btn_watch_request_reports);
        ImageView logOutBtn= findViewById(R.id.log_out_btn);
        ImageView settingsBtn= findViewById(R.id.btn_settings);
        ImageView manageUsersBtn= findViewById(R.id.manage_users_btn);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        Intent thisIntent = getIntent();
        String userId = thisIntent.getStringExtra("userId");
        String isManager= thisIntent.getStringExtra("isManager");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String lastTimeSeenMap= LocalDateTime.now().toString();
            try {
                setLastTimeSeenMap(userId, lastTimeSeenMap);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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


    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        if (
                ContextCompat.checkSelfPermission(Map.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(Map.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Map.this.runOnUiThread(() -> Toast.makeText(Map.this, "Please enable location permissions", Toast.LENGTH_SHORT).show());
            ActivityCompat.requestPermissions(Map.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 90);
        }
        mMap = googleMap;
        double lat= 32.107193634412475;
        double lang= 35.20542059093714;
        LatLng defaultLocation = new LatLng(lat, lang);
        float DEFAULT_ZOOM= 15;
        Intent thisIntent = getIntent();
        double goToLongLocation= thisIntent.getDoubleExtra("goToLongLocation", -999);
        double goToLatLocation= thisIntent.getDoubleExtra("goToLatLocation", -999);
        try {
            getMapsDataDocs(mMap);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // initializing our search view.
        searchView = findViewById(R.id.idSearchView);

        // Obtain the SupportMapFragment and get notified
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // adding on query listener for our search view.

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // on below line we are getting the
                // location name from search view.

                String location;
                location = searchView.getQuery().toString();

                // below line is to create a list of address
                // where we will store the list of all address.
                List<Address> addressList = null;

                // checking if the entered location is null or not.
                // on below line we are creating and initializing a geo coder.
                Geocoder geocoder = new Geocoder(Map.this);
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
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                    // below line is to animate camera to that position.
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        if(goToLongLocation != -999 && goToLatLocation != -999 ){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(goToLatLocation, goToLongLocation), 17.0f));
        }
        else {
            mMap.moveCamera(CameraUpdateFactory
                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
        }

        try {
            checkIfToStartNotificationService(thisIntent.getStringExtra("userId"));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // adding on click listener to marker of google maps.
        mMap.setOnMapLongClickListener(latLng -> {
            //passing userId to request
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
            RelativeLayout loadingPanel= findViewById(R.id.loadingPanel);
            String requestUserId = Objects.requireNonNull(marker.getTag()).toString();
            String requestId= marker.getTitle();
            String userId = thisIntent.getStringExtra("userId");
            String isManager= thisIntent.getStringExtra("isManager");
            if(requestId!=null) {
                if(markersHashmap.get(requestId)!=null) {
                    String docId = markersRequestToDocId.get(requestId);
                    try {
                        getRequestDetails(requestId, userId, requestUserId, isManager, docId, loadingPanel);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                else{
                    Toast.makeText(Map.this, "Oops! This request isn't available", Toast.LENGTH_SHORT).show();
                }
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
        String isManager = thisIntent.getStringExtra("isManager");
        serviceIntent.putExtra("userId", userId);
        serviceIntent.putExtra("isManager", isManager);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceIntent.putExtra("lastTimeSeenMapStr", LocalDateTime.now().toString());
        }
        ContextCompat.startForegroundService(Map.this, serviceIntent);
    }

    public void stopNotificationService() {
        Intent serviceIntent = new Intent(this, NotificationService.class);
        stopService(serviceIntent);
    }

    public void getRequestDetails(String requestId, String userId, String requestUserId, String isManager, String docId, RelativeLayout loadingPanel) throws InterruptedException {
        loadingPanel.setVisibility(View.VISIBLE);
        new Thread(() -> {
            String urlString = server_url +"getRequestDetails/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Map.this.runOnUiThread(() -> Toast.makeText(Map.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                json.put("userId", userId);
                json.put("requestUserId", requestUserId);
                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
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
                stringRequestDetails= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                // Removing first and last character
                // of a string using substring() method
                details = stringRequestDetails.split("\\|\\|##");
                isRunning=false;
                String requestSubject= details[1];
                String requestBody= details[0].substring(1);
                String contactDetails= details[2];
                String requestLatitude= details[4];
                String requestLongitude= details[3];
                String creationTime= details[5].substring(1,details[5].length()-1);
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
                Map.this.runOnUiThread(() -> loadingPanel.setVisibility(View.GONE));
                startActivity(viewRequestIntent);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }

    public void setLastTimeSeenMap(String userId, String time) throws InterruptedException {
        new Thread(() -> {
            String urlString = server_url +"setLastTimeSeenMap/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Map.this.runOnUiThread(() -> Toast.makeText(Map.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                json.put("time", time);
                String jsonInputString = json.toString();
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
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
                stringRequestDetails= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                isRunning=false;
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }

    public void checkIfToStartNotificationService(String userId) throws InterruptedException {
        new Thread(() -> {
            String urlString = server_url +"getIsNotificationsTurnedOn/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Map.this.runOnUiThread(() -> Toast.makeText(Map.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
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
                e.printStackTrace();
            }
            try {
                InputStream is = conn.getInputStream();
                isNotificationsTurnedOn= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                if(isNotificationsTurnedOn.equals("\"1\"")){
                    System.out.println("turn on notification");
                    new Thread(this::startNotificationService).start();
                }
                else{
                    System.out.println("turn off notification");
                    new Thread(this::stopNotificationService).start();
                }
                //  latch.countDown();
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
            }
        }).start();
    }

    public void getMapsDataDocs(GoogleMap mMap) throws InterruptedException {
        new Thread(() -> {
            String urlString = server_url +"getMapsDataDocs/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                Map.this.runOnUiThread(() -> Toast.makeText(Map.this, "Server is down, can't proccess the request. Please contact admin", Toast.LENGTH_SHORT).show());
            }
            HttpURLConnection conn = null;
            try {
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
                InputStream is = conn.getInputStream();
                String info= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                info= info.substring(2,info.length()-2);
                for (String docStr:
                        info.split(("\\|\\|##"))) {
                    if (docStr.startsWith("\",\"")) {
                        docStr = docStr.substring(3);
                    }
                    JSONObject doc = new JSONObject(docStr);
                    BitmapDescriptor handIcon = BitmapDescriptorFactory.fromBitmap(resizeBitmap("hand", 85, 85));
                    BitmapDescriptor starIcon = BitmapDescriptorFactory.fromBitmap(resizeBitmap("star_icon", 85, 85));
                    //TODO: add a check id geoPoint is an instance of GeoPoint class! throws exception if not.
                    String geoPointStr = (String) doc.get("geoPoint");
                    String[] geoPointParse = geoPointStr.split(",");
                    LatLng location = new LatLng(Double.parseDouble(geoPointParse[0]), Double.parseDouble(geoPointParse[1]));
                    String requestId = doc.getString("requestId");
                    String requestUserId = doc.getString("userId");
                    String isManager = doc.getString("isManager");
                    String creationTime = doc.getString("creationTime");
                    //to check if requestUseId is manager: change the icon
                    BitmapDescriptor selectedIcon = handIcon;
                    takenRequestsIds.add(requestId);
                    if (isManager.equals("1")) {
                        selectedIcon = starIcon;
                    }
                    BitmapDescriptor finalSelectedIcon = selectedIcon;
                    Map.this.runOnUiThread(() -> {
                    Marker newMarker = mMap.addMarker(new MarkerOptions().position(location).title(requestId).icon(finalSelectedIcon).snippet(creationTime));
                    assert newMarker != null;
                    newMarker.setTag(requestUserId);
                    markersHashmap.put(requestId, newMarker);
                    try {
                        markersRequestToDocId.put(requestId, (String) doc.get("id"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    });
                }
                } catch (JSONException | IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(Map.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }
}

//https://www.geeksforgeeks.org/how-to-add-searchview-in-google-maps-in-android/
//https://firebase.google.com/docs/firestore/query-data/listen
//https://stackoverflow.com/questions/52389072/google-maps-custom-marker-too-large
//https://www.youtube.com/watch?v=4BuRMScaaI4 //How to create Notification in Android tutorial
//https://stackoverflow.com/questions/72159435/how-to-get-location-using-fusedlocationclient-getcurrentlocation-method-in-kot/74254933#74254933
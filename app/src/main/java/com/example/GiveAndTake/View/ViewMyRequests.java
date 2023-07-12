package com.example.giveandtake.View;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.giveandtake.R;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ViewMyRequests extends ListActivity {
    String IPv4_Address= "10.0.0.3";

    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems= new ArrayList<>();
    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    ArrayList<String> myOpenRequestsInfo = new ArrayList<>();
    ArrayList<String> requestsUserJoinedInfo = new ArrayList<>();
    HashMap<String, String> requestsInfoToId = new HashMap<>();

    int openOrJoinedFlag=0;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_view_my_requests);
        Button btnShowMyOpenRequests = findViewById(R.id.showMyOpenRequestsBtn);
        Button btnShowRequestsIJoined = findViewById(R.id.showRequestsIJoinedBtn);
        Button btnBlockUser = findViewById(R.id.blockUser);
        Button btnUnblockUser = findViewById(R.id.unblockUser);
        ListView requestsList = findViewById(android.R.id.list);
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);
        Intent thisIntent = getIntent();
        HashMap<String, String> markersRequestToDocId = (HashMap<String, String>) thisIntent.getExtras().getSerializable("markersRequestToDocId");
        String userId = thisIntent.getStringExtra("userId");
        String isManager = thisIntent.getStringExtra("isManager");
        String requestUserId = thisIntent.getStringExtra("requestUserId");
        if (isManager != null && isManager.equals("0") || requestUserId.equals(userId)) {
            btnBlockUser.setVisibility(View.GONE);
            btnUnblockUser.setVisibility(View.GONE);
        }
        EditText userIdOfRequestEditTxt = findViewById(R.id.user_id_of_request);
        userIdOfRequestEditTxt.setText(requestUserId, TextView.BufferType.EDITABLE);
        userIdOfRequestEditTxt.setEnabled(false);
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot d : dataSnapshot.child(requestUserId).child("requestId").getChildren()) {
                        String requestId = d.getKey();
                        assert requestId != null;
                        if (dataSnapshot.child(requestUserId).child("requestId").child(requestId).child("subject").getValue() != null) {
                            String requestSubject = dataSnapshot.child(requestUserId).child("requestId").child(requestId).child("subject").getValue().toString();
                            myOpenRequestsInfo.add("Subject: " + requestSubject + " | Request Id: " + requestId);
                            requestsInfoToId.put("Subject: " + requestSubject + " | Request Id: " + requestId, requestId);
                        }
                    }
                    for (DataSnapshot d : dataSnapshot.child(requestUserId).child("requestsUserJoined").getChildren()) {
                        String requestId = d.getKey();
                        if (dataSnapshot.child(requestUserId).child("requestsUserJoined").child(requestId).child("requestUserId").getValue() != null) {
                            String creatorOfRequestUserJoined = dataSnapshot.child(requestUserId).child("requestsUserJoined").child(requestId).child("requestUserId").getValue().toString();
                            String requestSubject = dataSnapshot.child(creatorOfRequestUserJoined).child("requestId").child(requestId).child("subject").getValue().toString();
                            requestsUserJoinedInfo.add("Subject: " + requestSubject + " | Request Id: " + requestId);
                            requestsInfoToId.put("Subject: " + requestSubject + " | Request Id: " + requestId, requestId);
                        }
                    }
                }
            }//onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }//onCancelled
        });

        btnShowMyOpenRequests.setOnClickListener(view -> {
            addMyOpenRequests(view);
            //TODO: pass the isManager. or retrieve it in map
            btnShowMyOpenRequests.setVisibility(View.GONE);
            btnShowRequestsIJoined.setVisibility(View.VISIBLE);
        });

        btnShowRequestsIJoined.setOnClickListener(view -> {
            addRequestsUserJoined(view);
            //TODO: pass the isManager. or retrieve it in map
            btnShowMyOpenRequests.setVisibility(View.VISIBLE);
            btnShowRequestsIJoined.setVisibility(View.GONE);
        });

        btnBlockUser.setOnClickListener(view -> {
            try {
                blockUnblockUser(requestUserId, "1");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        btnUnblockUser.setOnClickListener(view -> {
            try {
                blockUnblockUser(requestUserId, "0");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        requestsList.setOnItemClickListener((parent, view, position, id) -> {
            String requestInfo = (String) parent.getAdapter().getItem(position);
            String requestId = requestsInfoToId.get(requestInfo);
            String docId = markersRequestToDocId.get(requestId);
            if (requestId != null) {
                String managerWatching = thisIntent.getStringExtra("managerWatching");
                if (managerWatching != null && managerWatching.equals("1") && openOrJoinedFlag == 1) {
                    //it's a manager watching another user's joined requests
                    //requestUserId= the user the manager views id
                    try {
                        getFinal1(requestId, userId, requestUserId, isManager, docId);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    if (openOrJoinedFlag == 1 && managerWatching == null) {
                        //  it's the viewer itself watching it's joined list
                        try {
                            getFinal2(requestId, userId, requestUserId, isManager, docId);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else{
                        try {
                            getRequestDetails(requestId, userId, requestUserId, isManager, docId);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                //else, it's open requests of a user, that the user opened, and it's requestUserId
                //TODO: fix this messy implementation
            }
        });
    }


    //METHOD WHICH WILL HANDLE DYNAMIC INSERTION
    public void addMyOpenRequests(View v) {
        //bug that addEventListener executes after this listItem.AddAll,
        // is fixed by putting addEventListener on create function
        openOrJoinedFlag=0;
        listItems.clear();
        listItems.addAll(myOpenRequestsInfo);
        adapter.notifyDataSetChanged();
    }

    public void addRequestsUserJoined(View v) {
        //bug that addEventListener executes after this listItem.AddAll,
        // is fixed by putting addEventListener on create function
        openOrJoinedFlag=1;
        listItems.clear();
        listItems.addAll(requestsUserJoinedInfo);
        adapter.notifyDataSetChanged();
    }

    public void blockUnblockUser(String userId, String blockUnblock) throws InterruptedException {
        new Thread(() -> {
            String urlString = "http://"+IPv4_Address+":8000/blockUnblockUser/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewMyRequests.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewMyRequests.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show();
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
                json.put("userId", userId);
                json.put("blockUnblock", blockUnblock);
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
                //  latch.countDown();
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                //showServerDownToast();
            }
        }).start();
        // Toast.makeText(Map.this, "Got request details successfully", Toast.LENGTH_SHORT).show();
    }

    public void getRequestDetails(String requestId, String userId, String requestUserId, String isManager, String docId) throws InterruptedException {
        new Thread(() -> {
            String urlString = "http://"+IPv4_Address+":8000/getRequestDetails/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewMyRequests.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewMyRequests.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show();
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
                json.put("userId", userId);
                json.put("requestUserId", requestUserId);
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
                String stringRequestDetails= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("details received: "+stringRequestDetails);
                // Removing first and last character
                // of a string using substring() method
                String [] details = stringRequestDetails.split("\\|\\|##");
                System.out.println("string array: ");
                System.out.println(Arrays.toString(details));
                String requestSubject= details[1];
                String requestBody= details[0].substring(1);
                String contactDetails= details[2];
                String requestLatitude= details[4];
                String requestLongitude= details[3];
                String creationTime= details[5].substring(1,details[5].length()-1);
                Intent viewRequestIntent = new Intent(ViewMyRequests.this, ViewRequest.class);
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
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }

    public void getFinal1(String requestId, String userId, String requestUserId, String isManager, String docId) throws InterruptedException {
        new Thread(() -> {
            String urlString = "http://"+IPv4_Address+":8000/getFinal1/";
            System.out.println("inget1");
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewMyRequests.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewMyRequests.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show();
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
                json.put("userId", userId);
                json.put("requestUserId", requestUserId);
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
                String finalRequestUserId= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("details final request: "+finalRequestUserId);
                try {
                    getRequestDetails(requestId, userId, finalRequestUserId, isManager, docId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Removing first and last character
                // of a string using substring() method

            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }

    public void getFinal2(String requestId, String userId, String requestUserId, String isManager, String docId) throws InterruptedException {
        new Thread(() -> {
            String urlString = "http://"+IPv4_Address+":8000/getFinal2/";
            //Wireless LAN adapter Wi-Fi:
            System.out.println("inget2");
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewMyRequests.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewMyRequests.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show();
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
                json.put("userId", userId);
                json.put("requestUserId", requestUserId);
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
                String finalRequestUserId= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("details final request is: "+finalRequestUserId);
                try {
                    getRequestDetails(requestId, userId, finalRequestUserId, isManager, docId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Removing first and last character
                // of a string using substring() method

            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }

    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(ViewMyRequests.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }

}


//https://stackoverflow.com/questions/44376390/firebase-get-all-key
//https://stackoverflow.com/questions/4540754/how-do-you-dynamically-add-elements-to-a-listview-on-android
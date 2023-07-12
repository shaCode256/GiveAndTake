package com.example.giveandtake.View;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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

public class ViewReportedRequests extends ListActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems= new ArrayList<>();

    String IPv4_Address= "10.0.0.3";

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    ArrayList<String> reportedRequestsInfo = new ArrayList<>();
    HashMap<String, String[]> reportedRequestsInfoToId = new HashMap<>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_view_reported_requests);
        Button btnShowReportedRequests= findViewById(R.id.showReportedRequestsBtn);
        ListView requestsList= findViewById(android.R.id.list);
        adapter= new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);
        Intent thisIntent = getIntent();
        HashMap<String, String> markersRequestToDocId= (HashMap<String, String>)thisIntent.getExtras().getSerializable("markersRequestToDocId");
        String userId = thisIntent.getStringExtra("userId");
        String isManager = thisIntent.getStringExtra("isManager");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for(DataSnapshot d : dataSnapshot.child("reportedRequests").getChildren()) {
                        String requestId= d.getKey();
                        String requestUserId= d.child("requestUserId").getValue().toString();
                        String reporters=  d.child("reporters").getValue().toString();
                        reporters= "{ Phone Number: "+reporters.substring(1);
                        reporters= reporters.replace("=", " | Name: ");
                        reporters= reporters.replace(",", ", Phone Number: ");
                        assert requestId != null;
                        if( dataSnapshot.child("users").child(requestUserId).child("requestId").child(requestId).child("subject").getValue()!=null) {
                            String requestSubject = dataSnapshot.child("users").child(requestUserId).child("requestId").child(requestId).child("subject").getValue().toString();
                            reportedRequestsInfo.add("Subject: " + requestSubject + " | Request Id: " + requestId+ " | reporters: "+reporters);
                            reportedRequestsInfoToId.put("Subject: " + requestSubject + " | Request Id: " + requestId+ " | reporters: "+reporters, new String[] {requestId, requestUserId});
                        }
                    }
                }
            }//onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }//onCancelled
        });

        btnShowReportedRequests.setOnClickListener(view -> {
            addReportedRequests(view);
            //TODO: pass the isManager. or retrieve it in map
            btnShowReportedRequests.setVisibility(View.GONE);
        });

        requestsList.setOnItemClickListener((parent, view, position, id) -> {
            String requestInfo= (String) parent.getAdapter().getItem(position);
            String requestId= reportedRequestsInfoToId.get(requestInfo)[0];
            String requestUserId=  reportedRequestsInfoToId.get(requestInfo)[1];
            String docId= markersRequestToDocId.get(requestId);
            if(requestId!=null) {
                try {
                    getRequestDetails(requestId, userId, requestUserId, isManager, docId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    //METHOD WHICH WILL HANDLE DYNAMIC INSERTION
    public void addReportedRequests(View v) {
        //bug that addEventListener executes after this listItem.AddAll,
        // is fixed by putting addEventListener on create function
        listItems.clear();
        listItems.addAll(reportedRequestsInfo);
        adapter.notifyDataSetChanged();
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
                ViewReportedRequests.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewReportedRequests.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show();
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
                Intent viewRequestIntent = new Intent(ViewReportedRequests.this, ViewRequest.class);
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

    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(ViewReportedRequests.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }

}


//https://stackoverflow.com/questions/44376390/firebase-get-all-key
//https://stackoverflow.com/questions/4540754/how-do-you-dynamically-add-elements-to-a-listview-on-android
package com.example.giveandtake.View;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.giveandtake.R;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ViewReportedRequests extends ListActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems= new ArrayList<>();

   String server_url= "https://giveandtake-server.df.r.appspot.com/";

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;
    ArrayList<String> reportedRequestsInfo = new ArrayList<>();
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_view_reported_requests);
        RelativeLayout loadingPanel= findViewById(R.id.loadingPanel);
        loadingPanel.setVisibility(View.GONE);
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

        try {
            addToReportsInfo();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        btnShowReportedRequests.setOnClickListener(view -> {
            addReportedRequests(view);
            //TODO: pass the isManager. or retrieve it in map
            if(!listItems.isEmpty()) {
                btnShowReportedRequests.setVisibility(View.GONE);
            }
        });

        requestsList.setOnItemClickListener((parent, view, position, id) -> {
            String requestInfo= (String) parent.getAdapter().getItem(position);
            String [] details= requestInfo.split("\\|");
            String requestId= details[1].replaceAll("\\D", "");
            String requestUserId= details[2].replaceAll("\\D", "");
            String docId= markersRequestToDocId.get(requestId);
                try {
                    getRequestDetails(requestId, userId, requestUserId, isManager, docId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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
            String urlString = server_url +"getRequestDetails/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewReportedRequests.this.runOnUiThread(() -> Toast.makeText(ViewReportedRequests.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
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

    public void addToReportsInfo() throws InterruptedException {
        new Thread(() -> {
            String urlString = server_url +"getReportedRequests/";
            //Wireless LAN adapter Wi-Fi:
            System.out.println("inAddToJoiners");
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewReportedRequests.this.runOnUiThread(() -> Toast.makeText(ViewReportedRequests.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                //add this to the array
                info= info.substring(2,info.length()-2);
                for (String report:
                        info.split(("\\|\\|##"))) {
                    if (report.startsWith("\",\"")){
                        report= report.substring(3);
                    }
                    reportedRequestsInfo.add(report);
                }
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
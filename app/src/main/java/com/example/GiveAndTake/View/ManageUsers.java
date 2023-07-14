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
import java.util.ArrayList;
import java.util.HashMap;

public class ManageUsers extends ListActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    String IPv4_Address= "http://10.0.0.3:8000/";
    ArrayList<String> listItems= new ArrayList<>();
    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;
    ArrayList<String> usersInfo= new ArrayList<>();
    HashMap<String, String> usersInfoToId= new HashMap<>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_manage_users);
        RelativeLayout loadingPanel= findViewById(R.id.loadingPanel);
        loadingPanel.setVisibility(View.GONE);
        Button btnShowUsers= findViewById(R.id.showBtn);
        ListView usersList= findViewById(android.R.id.list);
        adapter= new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);
        Intent thisIntent = getIntent();
        String userId = thisIntent.getStringExtra("userId");
        String isManager = thisIntent.getStringExtra("isManager");
        HashMap<String, String> markersRequestToDocId= (HashMap<String, String>)thisIntent.getExtras().getSerializable("markersRequestToDocId");

        try {
            addToUsersInfo();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        btnShowUsers.setOnClickListener(view -> {
            addItems(view);
            btnShowUsers.setVisibility(View.GONE);
        });

        usersList.setOnItemClickListener((parent, view, position, id) -> {
            String requestUserInfo= (String) parent.getAdapter().getItem(position);
            String requestUserId= requestUserInfo.substring(requestUserInfo.lastIndexOf(":")+2);
            Intent viewRequestsIntent = new Intent(ManageUsers.this, ViewMyRequests.class);
            viewRequestsIntent.putExtra("userId", userId);
            viewRequestsIntent.putExtra("isManager", isManager);
            viewRequestsIntent.putExtra("managerWatching", "1");
            viewRequestsIntent.putExtra("requestUserId", requestUserId);
            viewRequestsIntent.putExtra("markersRequestToDocId", markersRequestToDocId);
            startActivity(viewRequestsIntent);
        });
    }


    //METHOD WHICH WILL HANDLE DYNAMIC INSERTION
    public void addItems(View v) {
        //bug that addEventListener executes after this listItem.AddAll,
        // is fixed by putting addEventListener on create function
        listItems.addAll(usersInfo);
        adapter.notifyDataSetChanged();
    }

    public void addToUsersInfo() throws InterruptedException {
        new Thread(() -> {
            String urlString = IPv4_Address+"getUsers/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ManageUsers.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ManageUsers.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show();
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
                json.put("request", "request");
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
                String info= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                //add this to the array
                info= info.substring(2,info.length()-2);
                for (String user:
                        info.split(("\\|\\|##"))) {
                    if (user.startsWith("\",\"")){
                        user= user.substring(3);
                    }
                    usersInfo.add(user);
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
        runOnUiThread(() -> Toast.makeText(ManageUsers.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }
}


//https://stackoverflow.com/questions/44376390/firebase-get-all-key
//https://stackoverflow.com/questions/4540754/how-do-you-dynamically-add-elements-to-a-listview-on-android
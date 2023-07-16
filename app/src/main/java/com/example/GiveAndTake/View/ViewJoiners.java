package com.example.giveandtake.View;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
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

public class ViewJoiners extends ListActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    String IPv4_Address= "http://10.0.0.3:8000/";
    ArrayList<String> listItems= new ArrayList<>();
    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;
    ArrayList<String> joinersInfo= new ArrayList<>();
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_view_joiners);
        RelativeLayout loadingPanel= findViewById(R.id.loadingPanel);
        loadingPanel.setVisibility(View.VISIBLE);
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                loadingPanel.setVisibility(View.GONE);
//            }
//        },1000*5);
        Button btnShowJoiners = findViewById(R.id.showBtn);
        adapter= new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);
        Intent thisIntent = getIntent();
        String requestUserId= thisIntent.getStringExtra("requestUserId");
        String requestId= thisIntent.getStringExtra("requestId");
        EditText requestIdEditTxt = findViewById(R.id.request_id);
        requestIdEditTxt.setText(requestId, TextView.BufferType.EDITABLE);
        requestIdEditTxt.setEnabled(false);

        try {
            addToJoinersInfo(requestId, requestUserId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        btnShowJoiners.setOnClickListener(view -> {
            addItems(view);
            //TODO: pass the isManager. or retrieve it in map
            btnShowJoiners.setVisibility(View.GONE);
        });
    }

    //METHOD WHICH WILL HANDLE DYNAMIC INSERTION
    public void addItems(View v) {
        //bug that addEventListener executes after this listItem.AddAll,
        // is fixed by putting addEventListener on create function
        listItems.addAll(joinersInfo);
        adapter.notifyDataSetChanged();
    }

    public void addToJoinersInfo(String requestId, String requestUserId) throws InterruptedException {
        new Thread(() -> {
            String urlString = IPv4_Address+"getJoiners/";
            //Wireless LAN adapter Wi-Fi:
            System.out.println("inAddToJoiners");
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewJoiners.this.runOnUiThread(() -> Toast.makeText(ViewJoiners.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                String info= CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                //add this to the array
                info= info.substring(2,info.length()-2);
                for (String joiner:
                     info.split(("\\|\\|##"))) {
                    if (joiner.startsWith("\",\"")){
                        joiner= joiner.substring(3);
                    }
                    joinersInfo.add(joiner);
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
        runOnUiThread(() -> Toast.makeText(ViewJoiners.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }
}


//https://stackoverflow.com/questions/44376390/firebase-get-all-key
//https://stackoverflow.com/questions/4540754/how-do-you-dynamically-add-elements-to-a-listview-on-android
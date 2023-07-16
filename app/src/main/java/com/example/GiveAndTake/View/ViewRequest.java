package com.example.giveandtake.View;

import static java.lang.Double.parseDouble;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class ViewRequest extends AppCompatActivity {
    String IPv4_Address= "http://10.0.0.3:8000/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_request);
        Intent myIntent = getIntent();
        String userId = myIntent.getStringExtra("userId");
        String requestId = myIntent.getStringExtra("requestId");
        String isManager= myIntent.getStringExtra("isManager");
        String subject= myIntent.getStringExtra("requestSubject");
        String body= myIntent.getStringExtra("requestBody");
        String requestUserId= myIntent.getStringExtra("requestUserId");
        String contactDetails= myIntent.getStringExtra("contactDetails").replaceAll("\"", "");
        String latitude= myIntent.getStringExtra("requestLatitude");
        String longitude= myIntent.getStringExtra("requestLongitude");
        String docId= myIntent.getStringExtra("docId");
        String creationTime= myIntent.getStringExtra("creationTime");
        EditText subjectEditTxt = findViewById(R.id.view_request_input_subject);
        EditText bodyEditTxt = findViewById(R.id.view_request_input_body);
        EditText longitudeInputEditTxt = findViewById(R.id.longitude_input);
        EditText latitudeInputEditTxt = findViewById(R.id.latitude_input);
        EditText contactDetailsEditTxt = findViewById(R.id.view_request_input_contact_details);
        EditText creationTimeEditTxt = findViewById(R.id.creation_time);
        EditText userIdOfRequestEditTxt = findViewById(R.id.user_id_of_request);
        subjectEditTxt.setEnabled(false);
        bodyEditTxt.setEnabled(false);
        userIdOfRequestEditTxt.setEnabled(true);
        contactDetailsEditTxt.setEnabled(false);
        longitudeInputEditTxt.setEnabled(false);
        latitudeInputEditTxt.setEnabled(false);
        creationTimeEditTxt.setEnabled(false);
        ImageView btnBackToMap = findViewById(R.id.btn_back_to_map);
        ImageView btnShowOnMap = findViewById(R.id.btn_show_on_map);
        Button btnDeleteRequest = findViewById(R.id.btn_delete_request);
        Button btnViewJoiners = findViewById(R.id.btn_view_joiners);
        Button btnJoinRequest = findViewById(R.id.btn_join_request);
        Button btnUnjoinRequest = findViewById(R.id.btn_unjoin_request);
        Button btnReportRequest = findViewById(R.id.btn_report_request);
        Button btnUnReportRequest = findViewById(R.id.btn_unreport_request);
        btnDeleteRequest.setVisibility(View.GONE);
        subjectEditTxt.setText(subject, TextView.BufferType.EDITABLE);
        bodyEditTxt.setText(body, TextView.BufferType.EDITABLE);
        contactDetailsEditTxt.setText(contactDetails, TextView.BufferType.EDITABLE);
        latitudeInputEditTxt.setText(latitude, TextView.BufferType.EDITABLE);
        longitudeInputEditTxt.setText(longitude, TextView.BufferType.EDITABLE);
        creationTimeEditTxt.setText(creationTime, TextView.BufferType.EDITABLE);
        userIdOfRequestEditTxt.setText(requestUserId, TextView.BufferType.EDITABLE);
        btnDeleteRequest.setVisibility(View.GONE);
        btnViewJoiners.setVisibility(View.GONE);
        if (isManager!=null &&isManager.equals("1") || requestUserId!=null && requestUserId.equals(userId) )
        {
            //if I'm a manager, or it's my request
            btnDeleteRequest.setVisibility(View.VISIBLE);
            btnViewJoiners.setVisibility(View.VISIBLE);

            //if it's my request
            if(requestUserId!=null && requestUserId.equals(userId)){
            btnUnjoinRequest.setVisibility(View.GONE);
            btnJoinRequest.setVisibility(View.GONE);
            btnReportRequest.setVisibility(View.GONE);
            btnUnReportRequest.setVisibility(View.GONE);
        }
        }

        userIdOfRequestEditTxt.setOnClickListener(v -> {
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
            phoneIntent.setData(Uri.parse("tel:" + requestUserId));
            startActivity(phoneIntent);
        });

        userIdOfRequestEditTxt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
                phoneIntent.setData(Uri.parse("tel:" + requestUserId));
                startActivity(phoneIntent);
                return false;
            }
        });

        userIdOfRequestEditTxt.setOnLongClickListener(v -> {
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
            phoneIntent.setData(Uri.parse("tel:" + requestUserId));
            startActivity(phoneIntent);
            return false;
        });

        btnBackToMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(ViewRequest.this, Map.class);
                mapIntent.putExtra("userId", userId);
                mapIntent.putExtra("isManager", isManager);
            startActivity(mapIntent);
        });

        btnShowOnMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(ViewRequest.this, Map.class);
            mapIntent.putExtra("userId", userId);
            mapIntent.putExtra("isManager", isManager);
            mapIntent.putExtra("goToLatLocation", (parseDouble(latitude)));
            mapIntent.putExtra("goToLongLocation", (parseDouble(longitude)));
            startActivity(mapIntent);
        });

        btnDeleteRequest.setOnClickListener(v -> {
            //delete request by server
            if(requestId!=null) {
                deleteRequest(userId, requestId, docId, isManager);
            }
        });

        btnJoinRequest.setOnClickListener(
                v -> {
                    // Join by server
                   joinRequest(requestId, userId, requestUserId);
                });

        btnUnjoinRequest.setOnClickListener(v ->
                {
                 //UnJoin by server
                    unJoinRequest(requestId, userId, requestUserId);

                });

        btnViewJoiners.setOnClickListener(v -> {
            Intent viewJoinersIntent = new Intent(ViewRequest.this, ViewJoiners.class);
            viewJoinersIntent.putExtra("requestUserId", requestUserId);
            viewJoinersIntent.putExtra("requestId", requestId);
            viewJoinersIntent.putExtra("userId", userId);
            viewJoinersIntent.putExtra("isManager", isManager);
            startActivity(viewJoinersIntent);
        });

        btnReportRequest.setOnClickListener(v -> reportRequest(requestId,userId, requestUserId));

        btnUnReportRequest.setOnClickListener(v -> {
            //unreport request by server
           unReportRequest(requestId,userId);
        });
    }

    public void deleteRequest(String userId, String requestId, String docId, String isManager) {
        new Thread(() -> {
            String urlString = IPv4_Address+"delete/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(() -> Toast.makeText(ViewRequest.this, "Server is down, can't delete the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                json.put("docId", docId);
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
                if (result.equals("\"success\"")) {
                    Intent mapIntent = new Intent(ViewRequest.this, Map.class);
                    mapIntent.putExtra("userId", userId);
                    mapIntent.putExtra("isManager", isManager);
                    startActivity(mapIntent);
                }
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(ViewRequest.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
    }

    public void reportRequest(String requestId, String userId, String requestUserId) {
        new Thread(() -> {
            String urlString = IPv4_Address+"report/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(() -> Toast.makeText(ViewRequest.this, "Server is down, can't report the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(ViewRequest.this, "Reported successfully", Toast.LENGTH_SHORT).show();
    }


    public void unReportRequest(String requestId, String userId) {
        new Thread(() -> {
            String urlString = IPv4_Address+"unReport/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(() -> Toast.makeText(ViewRequest.this, "Server is down, can't unreport the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(ViewRequest.this, "Unreported successfully", Toast.LENGTH_SHORT).show();
    }


    public void joinRequest(String requestId, String userId, String requestUserId) {
        new Thread(() -> {
            String urlString = IPv4_Address+"join/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(() -> Toast.makeText(ViewRequest.this, "Server is down, can't join the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(ViewRequest.this, "Joined successfully", Toast.LENGTH_SHORT).show();
    }


    public void unJoinRequest(String requestId, String userId, String requestUserId) {
        new Thread(() -> {
            String urlString = IPv4_Address+"unJoin/";
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(() -> Toast.makeText(ViewRequest.this, "Server is down, can't unjoin the request. Please contact admin", Toast.LENGTH_SHORT).show());
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
        Toast.makeText(ViewRequest.this, "Unjoined successfully", Toast.LENGTH_SHORT).show();
    }

    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(ViewRequest.this, "Server is down, can't perform the request. Please contact admin", Toast.LENGTH_SHORT).show());
    }
}
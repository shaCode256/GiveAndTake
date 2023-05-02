package com.example.giveandtake.View;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.giveandtake.R;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.HashMap;

public class ViewRequest extends AppCompatActivity {
    String IPv4_Address= "10.0.0.3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_request);
        Intent myIntent = getIntent();
        String userId = myIntent.getStringExtra("userId");
        String requestId = myIntent.getStringExtra("requestId");
        String isManager= myIntent.getStringExtra("isManager");
        String subject= myIntent.getStringExtra("requestSubject");
        String body= myIntent.getStringExtra("requestBody");
        String requestUserId= myIntent.getStringExtra("requestUserId");
        String contactDetails= myIntent.getStringExtra("contactDetails");
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

        btnDeleteRequest.setOnClickListener(v -> {
            Intent mapIntent = new Intent(ViewRequest.this, Map.class);
            mapIntent.putExtra("userId", userId);
            mapIntent.putExtra("isManager", isManager);
            db.collection("MapsData").document(docId).delete(); //deletes from markersDb
            //remove this request from all it's joiners list of joined requests
//            deleteRequest(userId, requestId, docId);
            if(requestId!=null) {
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            //go through all joiners of this request
                            for(DataSnapshot d : dataSnapshot.child("users").child(requestUserId).child("requestId").child(requestId).child("joiners").getChildren()) {
                                String joinerId= (d.getKey());
                                //delete requestId from joiner's list
                                databaseReference.child("reportedRequests").child(requestId).removeValue();
                                databaseReference.child("users").child(joinerId).child("requestsUserJoined").child(requestId).getRef().removeValue();
                            }
                        }
                    }//onDataChange

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }//onCancelled
                });
                //remove requestId from usersDb
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        assert requestUserId != null;
                        snapshot.child(requestUserId).child("requestId").child(requestId).getRef().removeValue();

                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }
            startActivity(mapIntent);
        });

        btnJoinRequest.setOnClickListener(
                v -> {
                    databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {


            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assert requestUserId != null;
               // String joinerContactDetails=  snapshot.child("users").child(userId).child("email").getValue(String.class);
                String joinerContactDetails=  databaseReference.child("users").child(userId).child("email").toString();
                databaseReference.child("users").child(requestUserId).child("requestId").child(requestId).child("joiners").child(userId).child("contactDetails").setValue(joinerContactDetails);
                databaseReference.child("users").child(userId).child("requestsUserJoined").child(requestId).child("requestUserId").setValue(requestUserId);
                Toast.makeText(ViewRequest.this, "Joined successfully", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

// //Join by server
      //             joinRequest(requestId, userId, requestUserId);
                    ;});

        btnUnjoinRequest.setOnClickListener(v ->
                {
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assert requestUserId != null;
                databaseReference.child("users").child(requestUserId).child("requestId").child(requestId).child("joiners").child(userId).getRef().removeValue();
                databaseReference.child("users").child(userId).child("requestsUserJoined").child(requestId).getRef().removeValue();
                Toast.makeText(ViewRequest.this, "Unjoined successfully", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

                // //UnJoin by server
                    //unJoinRequest(requestId, userId, requestUserId);

                ;});

        btnViewJoiners.setOnClickListener(v -> {
            Intent viewJoinersIntent = new Intent(ViewRequest.this, ViewJoiners.class);
            viewJoinersIntent.putExtra("requestUserId", requestUserId);
            viewJoinersIntent.putExtra("requestId", requestId);
            viewJoinersIntent.putExtra("userId", userId);
            viewJoinersIntent.putExtra("isManager", isManager);
            startActivity(viewJoinersIntent);
        });

        btnReportRequest.setOnClickListener(v -> {

          //  reportRequest(requestId,userId, requestUserId);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //grab user's name
                    // and enter it to reports data
                    //also enter the request creator id
                    String fullName= dataSnapshot.child("users").child(userId).child("fullName").getValue(String.class);
                    databaseReference.child("reportedRequests").child(requestId).child("reporters").child(userId).setValue(fullName);
                    databaseReference.child("reportedRequests").child(requestId).child("requestUserId").setValue(requestUserId);
                    Toast.makeText(ViewRequest.this, "Reported successfully", Toast.LENGTH_SHORT).show();
                }//onDataChange

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }//onCancelled
            });
        });

        btnUnReportRequest.setOnClickListener(v -> {
           // unReportRequest(requestId,userId);

            databaseReference.child("reportedRequests").child(requestId).child("reporters").child(userId).removeValue();
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //if after deleting this report, there are no more reporting: remove this request from reported requests
                    //or if the unreporter is a manager (meaning the manager checked and approved the request)
                    if (dataSnapshot.exists() && ( !dataSnapshot.child("reportedRequests").child(requestId).child("reporters").hasChildren()
                            || isManager.equals("1"))) {
                        databaseReference.child("reportedRequests").child(requestId).removeValue();
                    }
                    Toast.makeText(ViewRequest.this, "Removed report successfully", Toast.LENGTH_SHORT).show();
                }//onDataChange
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }//onCancelled
            });
        });
    }

    public void deleteRequest(String userId, String requestId, String docId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        new Thread(() -> {
            String urlString = "http://"+IPv4_Address+":8000/delete/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewRequest.this, "Server is down, can't delete the request. please contact Shavit", Toast.LENGTH_SHORT).show();
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
                if (result.equals("\"success\"")) {
                    //add marker
                    HashMap<String, Object> user = new HashMap<>();
                    user.put("requestId", requestId);
                    db.collection("MapsData").document(docId).delete(); //deletes from markersDb
                }
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }

    public void reportRequest(String requestId, String userId, String requestUserId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        new Thread(() -> {
            String urlString = "http://"+IPv4_Address+":8000/report/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewRequest.this, "Server is down, can't delete the request. please contact Shavit", Toast.LENGTH_SHORT).show();
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }


    public void unReportRequest(String requestId, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        new Thread(() -> {
            String urlString = "http://"+IPv4_Address+":8000/unReport/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewRequest.this, "Server is down, can't delete the request. please contact Shavit", Toast.LENGTH_SHORT).show();
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }


    public void joinRequest(String requestId, String userId, String requestUserId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        new Thread(() -> {
            String urlString = "http://"+IPv4_Address+":8000/join/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewRequest.this, "Server is down, can't delete the request. please contact Shavit", Toast.LENGTH_SHORT).show();
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }


    public void unJoinRequest(String requestId, String userId, String requestUserId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        new Thread(() -> {
            String urlString = "http://"+IPv4_Address+":8000/unJoin/";
            //Wireless LAN adapter Wi-Fi:
            // IPv4 Address
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                System.out.println("error1");
                e.printStackTrace();
                ViewRequest.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewRequest.this, "Server is down, can't delete the request. please contact Shavit", Toast.LENGTH_SHORT).show();
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
                String result = CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
                System.out.println("yes:" + result);
            } catch (IOException e) {
                System.out.println("error3");
                e.printStackTrace();
                showServerDownToast();
            }
        }).start();
    }

    public void showServerDownToast()
    {
        runOnUiThread(() -> Toast.makeText(ViewRequest.this, "Server is down, can't delete the request. please contact Shavit", Toast.LENGTH_SHORT).show());
    }
}
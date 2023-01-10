package com.example.giveandtake.View;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.example.giveandtake.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ViewReportedRequests extends ListActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems= new ArrayList<>();
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
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //else, it's open requests of a user, that the user opened, and it's requestUserId
                        //TODO: fix this messy implementation
                        assert requestUserId != null;
                        String requestSubject = snapshot.child(requestUserId).child("requestId").child(requestId).child("subject").getValue(String.class);
                        String requestBody = snapshot.child(requestUserId).child("requestId").child(requestId).child("body").getValue(String.class);
                        String contactDetails = snapshot.child(requestUserId).child("requestId").child(requestId).child("contactDetails").getValue(String.class);
                        String requestLatitude = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("location").child("latitude").getValue(Double.class));
                        String requestLongitude = String.valueOf(snapshot.child(requestUserId).child("requestId").child(requestId).child("location").child("longitude").getValue(Double.class));
                        Intent viewRequestIntent = new Intent(ViewReportedRequests.this, ViewRequest.class);
                        viewRequestIntent.putExtra("requestSubject",requestSubject);
                        viewRequestIntent.putExtra("requestBody", requestBody);
                        viewRequestIntent.putExtra("contactDetails", contactDetails);
                        viewRequestIntent.putExtra("requestLatitude", requestLatitude);
                        viewRequestIntent.putExtra("requestLongitude", requestLongitude);
                        viewRequestIntent.putExtra("userId", userId);
                        viewRequestIntent.putExtra("requestUserId", requestUserId);
                        viewRequestIntent.putExtra("isManager", isManager);
                        viewRequestIntent.putExtra("docId", docId);
                        viewRequestIntent.putExtra("requestId", requestId);
                        startActivity(viewRequestIntent);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
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

}


//https://stackoverflow.com/questions/44376390/firebase-get-all-key
//https://stackoverflow.com/questions/4540754/how-do-you-dynamically-add-elements-to-a-listview-on-android
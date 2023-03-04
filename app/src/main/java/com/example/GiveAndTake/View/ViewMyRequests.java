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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ViewMyRequests extends ListActivity {
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
        Button btnShowMyOpenRequests= findViewById(R.id.showMyOpenRequestsBtn);
        Button btnShowRequestsIJoined= findViewById(R.id.showRequestsIJoinedBtn);
        Button btnBlockUser= findViewById(R.id.blockUser);
        Button btnUnblockUser= findViewById(R.id.unblockUser);
        ListView requestsList= findViewById(android.R.id.list);
        adapter= new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);
        Intent thisIntent = getIntent();
        HashMap<String, String> markersRequestToDocId= (HashMap<String, String>)thisIntent.getExtras().getSerializable("markersRequestToDocId");
        String userId = thisIntent.getStringExtra("userId");
        String isManager = thisIntent.getStringExtra("isManager");
        String requestUserId= thisIntent.getStringExtra("requestUserId");
        if (isManager!=null &&isManager.equals("0") || requestUserId.equals(userId)){
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
                    for(DataSnapshot d : dataSnapshot.child(requestUserId).child("requestId").getChildren()) {
                        String requestId= d.getKey();
                        assert requestId != null;
                        if( dataSnapshot.child(requestUserId).child("requestId").child(requestId).child("subject").getValue()!=null) {
                            String requestSubject = dataSnapshot.child(requestUserId).child("requestId").child(requestId).child("subject").getValue().toString();
                            myOpenRequestsInfo.add("Subject: " + requestSubject + " | Request Id: " + requestId);
                            requestsInfoToId.put("Subject: " + requestSubject + " | Request Id: " + requestId, requestId);
                        }
                    }
                    for(DataSnapshot d : dataSnapshot.child(requestUserId).child("requestsUserJoined").getChildren()) {
                        String requestId= d.getKey();
                        if(dataSnapshot.child(requestUserId).child("requestsUserJoined").child(requestId).child("requestUserId").getValue()!=null) {
                            String creatorOfRequestUserJoined = dataSnapshot.child(requestUserId).child("requestsUserJoined").child(requestId).child("requestUserId").getValue().toString();
                            String requestSubject = dataSnapshot.child(creatorOfRequestUserJoined).child("requestId").child(requestId).child("subject").getValue().toString();
                            requestsUserJoinedInfo.add("Subject: "+requestSubject + " | Request Id: " + requestId);
                            requestsInfoToId.put("Subject: "+requestSubject + " | Request Id: " + requestId, requestId);
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

        btnBlockUser.setOnClickListener(view -> databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check if phone is not registered before
                if (snapshot.hasChild(requestUserId)) {
                    Toast.makeText(ViewMyRequests.this, "Blocked successfully", Toast.LENGTH_SHORT).show();
                    databaseReference.child("users").child(requestUserId).child("isBlocked").setValue("1");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));

        btnUnblockUser.setOnClickListener(view -> databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check if phone is not registered before
                if (snapshot.hasChild(requestUserId)) {
                    Toast.makeText(ViewMyRequests.this, "Unblocked successfully", Toast.LENGTH_SHORT).show();
                    databaseReference.child("users").child(requestUserId).child("isBlocked").setValue("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));
        requestsList.setOnItemClickListener((parent, view, position, id) -> {
            String requestInfo= (String) parent.getAdapter().getItem(position);
            String requestId= requestsInfoToId.get(requestInfo);
            String docId= markersRequestToDocId.get(requestId);
            if(requestId!=null) {
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String finalRequestUserId= requestUserId;
                        String managerWatching= thisIntent.getStringExtra("managerWatching");
                        if(managerWatching!=null && managerWatching.equals("1") && openOrJoinedFlag== 1){
                            //it's a manager watching another user's joined requests
                            //requestUserId= the user the manager views id
                            finalRequestUserId= snapshot.child(requestUserId).child("requestsUserJoined").child(requestId).child("requestUserId").getValue(String.class);
                        }
                        if (openOrJoinedFlag== 1 && managerWatching==null){
                            //  it's the viewer itself watching it's joined list
                            finalRequestUserId= snapshot.child(userId).child("requestsUserJoined").child(requestId).child("requestUserId").getValue(String.class);
                        }
                        //else, it's open requests of a user, that the user opened, and it's requestUserId
                        //TODO: fix this messy implementation
                        assert finalRequestUserId != null;
                        String requestSubject = snapshot.child(finalRequestUserId).child("requestId").child(requestId).child("subject").getValue(String.class);
                        String requestBody = snapshot.child(finalRequestUserId).child("requestId").child(requestId).child("body").getValue(String.class);
                        String contactDetails = snapshot.child(finalRequestUserId).child("requestId").child(requestId).child("contactDetails").getValue(String.class);
                        String requestLatitude = String.valueOf(snapshot.child(finalRequestUserId).child("requestId").child(requestId).child("location").child("latitude").getValue(Double.class));
                        String requestLongitude = String.valueOf(snapshot.child(finalRequestUserId).child("requestId").child(requestId).child("location").child("longitude").getValue(Double.class));
                        String creationTime = String.valueOf(snapshot.child(finalRequestUserId).child("requestId").child(requestId).child("creationTime").getValue(String.class));
                        Intent viewRequestIntent = new Intent(ViewMyRequests.this, ViewRequest.class);
                        viewRequestIntent.putExtra("requestSubject",requestSubject);
                        viewRequestIntent.putExtra("requestBody", requestBody);
                        viewRequestIntent.putExtra("contactDetails", contactDetails);
                        viewRequestIntent.putExtra("requestLatitude", requestLatitude);
                        viewRequestIntent.putExtra("requestLongitude", requestLongitude);
                        viewRequestIntent.putExtra("userId", userId);
                        viewRequestIntent.putExtra("requestUserId", finalRequestUserId);
                        viewRequestIntent.putExtra("isManager", isManager);
                        viewRequestIntent.putExtra("docId", docId);
                        viewRequestIntent.putExtra("requestId", requestId);
                        viewRequestIntent.putExtra("creationTime", creationTime);
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
}


//https://stackoverflow.com/questions/44376390/firebase-get-all-key
//https://stackoverflow.com/questions/4540754/how-do-you-dynamically-add-elements-to-a-listview-on-android
package com.example.giveandtake;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Objects;

public class ViewMyRequests extends ListActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems=new ArrayList<String>();
    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    ArrayList<String> requestsIds=new ArrayList<String>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_view_my_requests);
        Button btn_show_requests= findViewById(R.id.showBtn);
        ListView requestsList= findViewById(android.R.id.list);
        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);
        Intent thisIntent = getIntent();
        HashMap<String, String> markersRequestToDocId= (HashMap<String, String>)thisIntent.getExtras().getSerializable("markersRequestToDocId");
        String userId = thisIntent.getStringExtra("userId");
        String isManager = thisIntent.getStringExtra("isManager");
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for(DataSnapshot d : dataSnapshot.child(userId).child("requestId").getChildren()) {
                        requestsIds.add(d.getKey().toString());
                       // Toast.makeText(ViewMyRequests.this, "list is: "+requestsIds.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            }//onDataChange

            @Override
            public void onCancelled(DatabaseError error) {

            }//onCancelled
        });

        btn_show_requests.setOnClickListener(view -> {
            // open Register activity
            addItems(view);
            //TODO: pass the isManager. or retreive it in map
            btn_show_requests.setVisibility(View.INVISIBLE);
                });

        requestsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                String requestId= (String) parent.getAdapter().getItem(position);
                String docId= markersRequestToDocId.get(requestId);
                if(requestId!=null) {
                    databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String getRequestSubject = snapshot.child(userId).child("requestId").child(requestId).child("subject").getValue(String.class);
                            String getRequestBody = snapshot.child(userId).child("requestId").child(requestId).child("body").getValue(String.class);
                            String getContactDetails = snapshot.child(userId).child("requestId").child(requestId).child("contact_details").getValue(String.class);
                            String getRequestLatitude = String.valueOf(snapshot.child(userId).child("requestId").child(requestId).child("location").child("latitude").getValue(Double.class));
                            String getRequestLongitude = String.valueOf(snapshot.child(userId).child("requestId").child(requestId).child("location").child("longitude").getValue(Double.class));
                            Intent thisIntent = getIntent();
                            Intent myIntent = new Intent(ViewMyRequests.this, ViewRequest.class);
                            myIntent.putExtra("getRequestSubject",getRequestSubject);
                            myIntent.putExtra("getRequestBody", getRequestBody);
                            myIntent.putExtra("getContactDetails", getContactDetails);
                            myIntent.putExtra("getRequestLatitude", getRequestLatitude);
                            myIntent.putExtra("getRequestLongitude", getRequestLongitude);
                            myIntent.putExtra("userId", userId);
                            myIntent.putExtra("getRequestUserId", userId);
                            myIntent.putExtra("isManager", isManager);
                            myIntent.putExtra("docId", docId);
                            myIntent.putExtra("requestId", requestId);
                            startActivity(myIntent);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
 //               Toast.makeText(ViewMyRequests.this, "entry is: "+entry, Toast.LENGTH_SHORT).show();
            }
        });
    }


    //METHOD WHICH WILL HANDLE DYNAMIC INSERTION
    public void addItems(View v) {
        //bug that addEventListener executes after this listItem.AddAll,
        // is fixed by putting addEventListener on create function
        listItems.addAll(requestsIds);
        adapter.notifyDataSetChanged();
    }
}


//https://stackoverflow.com/questions/44376390/firebase-get-all-key
//https://stackoverflow.com/questions/4540754/how-do-you-dynamically-add-elements-to-a-listview-on-android
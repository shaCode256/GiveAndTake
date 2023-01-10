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

public class ManageUsers extends ListActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems= new ArrayList<>();
    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    ArrayList<String> usersInfo= new ArrayList<>();
    HashMap<String, String> usersInfoToId= new HashMap<>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_manage_users);
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
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for(DataSnapshot d : dataSnapshot.getChildren()) {
                        usersInfo.add("Name: "+dataSnapshot.child(d.getKey()).child("fullName").getValue().toString()+" | Phone number: "+d.getKey());
                        usersInfoToId.put("Name: "+dataSnapshot.child(d.getKey()).child("fullName").getValue().toString()+" | Phone number: "+d.getKey(), d.getKey());
                    }
                }
            }//onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }//onCancelled
        });

        btnShowUsers.setOnClickListener(view -> {
            addItems(view);
            btnShowUsers.setVisibility(View.GONE);
        });

        usersList.setOnItemClickListener((parent, view, position, id) -> {
            String requestUserInfo= (String) parent.getAdapter().getItem(position);
            String requestUserId= usersInfoToId.get(requestUserInfo);
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
}


//https://stackoverflow.com/questions/44376390/firebase-get-all-key
//https://stackoverflow.com/questions/4540754/how-do-you-dynamically-add-elements-to-a-listview-on-android
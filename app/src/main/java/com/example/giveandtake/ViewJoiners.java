package com.example.giveandtake;

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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ViewJoiners extends ListActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems= new ArrayList<>();
    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;
    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
    ArrayList<String> requestsIds= new ArrayList<>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_view_joiners);
        Button btn_show_joiners= findViewById(R.id.showBtn);
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
        String requestId= thisIntent.getStringExtra("requestId");
        EditText request_idEditTxt = findViewById(R.id.request_id);
        request_idEditTxt.setText(requestId, TextView.BufferType.EDITABLE);
        request_idEditTxt.setEnabled(false);
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for(DataSnapshot d : dataSnapshot.child(requestUserId).child("requestId").child(requestId).child("joiners").getChildren()) {
                        requestsIds.add(d.getKey());
                    }
                }
            }//onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }//onCancelled
        });

        btn_show_joiners.setOnClickListener(view -> {
            addItems(view);
            //TODO: pass the isManager. or retrieve it in map
            btn_show_joiners.setVisibility(View.GONE);
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
package com.example.giveandtake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class ViewRequest extends AppCompatActivity {

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
        String subject= myIntent.getStringExtra("getRequestSubject");
        String body= myIntent.getStringExtra("getRequestBody");
        String contactDetails= myIntent.getStringExtra("getContactDetails");
        String latitude= myIntent.getStringExtra("getRequestLatitude");
        String longitude= myIntent.getStringExtra("getRequestLongitude");
        String docId= myIntent.getStringExtra("docId");
        EditText subjectEditTxt = findViewById(R.id.create_request_input_subject);
        EditText bodyEditTxt = findViewById(R.id.create_request_input_body);
        //  final EditText location = findViewById(R.id.create_request_input_location_details);
        EditText longitude_inputEditTxt = findViewById(R.id.longitude_input);
        EditText latitude_inputEditTxt = findViewById(R.id.latitude_input);
        EditText contact_detailsEditTxt = findViewById(R.id.create_request_input_contact_details);
        subjectEditTxt.setEnabled(false);
        bodyEditTxt.setEnabled(false);
        longitude_inputEditTxt.setEnabled(false);
        latitude_inputEditTxt.setEnabled(false);
        contact_detailsEditTxt.setEnabled(false);
     //   Button btnAddRequest = findViewById(R.id.button_add_request);
        Button btnBackToMap = findViewById(R.id.btn_back_to_map);
        Button btnDeleteRequest = findViewById(R.id.btn_delete_request);
        btnDeleteRequest.setVisibility(View.GONE);
        subjectEditTxt.setText(subject, TextView.BufferType.EDITABLE);
        bodyEditTxt.setText(body, TextView.BufferType.EDITABLE);
        contact_detailsEditTxt.setText(contactDetails, TextView.BufferType.EDITABLE);
        latitude_inputEditTxt.setText(latitude, TextView.BufferType.EDITABLE);
        longitude_inputEditTxt.setText(longitude, TextView.BufferType.EDITABLE);

        if (isManager!=null &&isManager.equals("1")){
            //show button to let manager delete the request
            btnDeleteRequest.setVisibility(View.VISIBLE);
            //then import the logic of delete
        }
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
            //remove requestId from usersDb
            if(requestId!=null) {
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        snapshot.child(userId).child("requestId").child(requestId).getRef().removeValue();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }
            startActivity(mapIntent);
        });
    }
}
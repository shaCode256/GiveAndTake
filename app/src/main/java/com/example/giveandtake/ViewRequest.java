package com.example.giveandtake;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

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
        String subject= myIntent.getStringExtra("requestSubject");
        String body= myIntent.getStringExtra("requestBody");
        String requestUserId= myIntent.getStringExtra("requestUserId");
        String contactDetails= myIntent.getStringExtra("contactDetails");
        String latitude= myIntent.getStringExtra("requestLatitude");
        String longitude= myIntent.getStringExtra("requestLongitude");
        String docId= myIntent.getStringExtra("docId");
        EditText subjectEditTxt = findViewById(R.id.create_request_input_subject);
        EditText bodyEditTxt = findViewById(R.id.create_request_input_body);
        EditText longitude_inputEditTxt = findViewById(R.id.longitude_input);
        EditText latitude_inputEditTxt = findViewById(R.id.latitude_input);
        EditText contact_detailsEditTxt = findViewById(R.id.create_request_input_contact_details);
        EditText user_id_of_requestEditTxt = findViewById(R.id.user_id_of_request);
        subjectEditTxt.setEnabled(false);
        bodyEditTxt.setEnabled(false);
        user_id_of_requestEditTxt.setEnabled(false);
        longitude_inputEditTxt.setEnabled(false);
        latitude_inputEditTxt.setEnabled(false);
        contact_detailsEditTxt.setEnabled(false);
        Button btnBackToMap = findViewById(R.id.btn_back_to_map);
        Button btnDeleteRequest = findViewById(R.id.btn_delete_request);
        Button btnViewJoiners = findViewById(R.id.btn_view_joiners);
        Button btnJoinRequest = findViewById(R.id.btn_join_request);
        Button btnUnjoinRequest = findViewById(R.id.btn_unjoin_request);
        Button btnReportRequest = findViewById(R.id.btn_report_request);
        btnDeleteRequest.setVisibility(View.GONE);
        subjectEditTxt.setText(subject, TextView.BufferType.EDITABLE);
        bodyEditTxt.setText(body, TextView.BufferType.EDITABLE);
        contact_detailsEditTxt.setText(contactDetails, TextView.BufferType.EDITABLE);
        latitude_inputEditTxt.setText(latitude, TextView.BufferType.EDITABLE);
        longitude_inputEditTxt.setText(longitude, TextView.BufferType.EDITABLE);
        user_id_of_requestEditTxt.setText(requestUserId, TextView.BufferType.EDITABLE);
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
        }
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
            //remove this request from all it's joiners list of joined requests
            if(requestId!=null) {
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            //go through all joiners of this request
                            for(DataSnapshot d : dataSnapshot.child(requestUserId).child("requestId").child(requestId).child("joiners").getChildren()) {
                                String joinerId= (d.getKey());
                                //delete requestId from joiner's list
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

        btnJoinRequest.setOnClickListener(v -> {
                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        assert requestUserId != null;
                       // String joinerContactDetails=  snapshot.child("users").child(userId).child("email").getValue(String.class);
                        String joinerContactDetails=  databaseReference.child("users").child(userId).child("email").toString();
                        //TODO: maybe add contact details
                        databaseReference.child("users").child(requestUserId).child("requestId").child(requestId).child("joiners").child(userId).child("contact_details").setValue(joinerContactDetails);
                        databaseReference.child("users").child(userId).child("requestsUserJoined").child(requestId).child("requestUserId").setValue(requestUserId);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
        });

        btnUnjoinRequest.setOnClickListener(v -> {
            databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    assert requestUserId != null;
                    databaseReference.child("users").child(requestUserId).child("requestId").child(requestId).child("joiners").child(userId).getRef().removeValue();
                    databaseReference.child("users").child(userId).child("requestsUserJoined").child(requestId).getRef().removeValue();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        });

        btnViewJoiners.setOnClickListener(v -> {
            Intent viewJoinersIntent = new Intent(ViewRequest.this, ViewJoiners.class);
            viewJoinersIntent.putExtra("requestUserId", requestUserId);
            viewJoinersIntent.putExtra("requestId", requestId);
            viewJoinersIntent.putExtra("userId", userId);
            viewJoinersIntent.putExtra("isManager", isManager);
            startActivity(viewJoinersIntent);
        });

        btnReportRequest.setOnClickListener(v -> {
          databaseReference.child("reportedRequests").child(requestId).child("reporters").child("userId").setValue(userId);
          databaseReference.child("reportedRequests").child(requestId).child("requestUserId").setValue(requestUserId);
        });
    }
}
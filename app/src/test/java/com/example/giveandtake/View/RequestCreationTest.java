package com.example.giveandtake.View;

import static org.junit.Assert.*;

import android.os.Build;

import androidx.annotation.NonNull;

import com.example.giveandtake.Model.Request;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.database.FirebaseDatabase;


import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.HashMap;

public class RequestCreationTest {

    @Before
    public void setUp() throws Exception {
        Request request= new Request();
        String creationTime = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            creationTime = LocalDateTime.now().toString();
        }
        String finalCreationTime= creationTime;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            request.setCreationTime(finalCreationTime);
        }
        String subjectTxt= "sampSubject";
        String bodyTxt= "sampBody";
        String contactDetailsTxt= "sampContactDetails";
        GeoPoint geoPointRequest= new GeoPoint(31.7,31.8);
        request.setSubject(subjectTxt);
        request.setBody(bodyTxt);
        request.setContactDetails(contactDetailsTxt);
        request.setLocation(geoPointRequest);
    }

    @Test
    public void onRequestPermissionsResult() {
    }

    @Test
    public void postRequestTest() {
        String creationTime = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            creationTime = LocalDateTime.now().toString();
        }
        String finalCreationTime= creationTime;
        String subjectTxt= "sampSubject";
        String bodyTxt= "sampBody";
        String contactDetailsTxt= "sampContactDetails";
        String finalSetRequestId= "requestId";
        String userId= "userId";
        String requestUserId= "requestUserId";
        String isManager= "0";
        String markersDb= null;
        GeoPoint geoPointRequest= new GeoPoint(31.7,31.8);
       // postRequest(finalSetRequestId, bodyTxt, userId, subjectTxt, contactDetailsTxt, String.valueOf(geoPointRequest.getLatitude()), String.valueOf(geoPointRequest.getLongitude()), finalCreationTime, requestUserId, isManager, markersDb);
    //    assertTrue(searchForRequest(finalSetRequestId, bodyTxt, requestUserId));
    }

    @Test
    public void showServerDownToast() {
    }


    public boolean searchForRequest(String requestId, String body, String requestUserId) {
        final boolean[] found = {false};
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //look for the desired request in the db
                    if (dataSnapshot.child("users").child(requestUserId).child("requestId").child(requestId).child("body").getValue().equals(body)) {
                        found[0] = true;
                    }
                }
            }//onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }//onCancelled
        });
        return found[0];
    }
}
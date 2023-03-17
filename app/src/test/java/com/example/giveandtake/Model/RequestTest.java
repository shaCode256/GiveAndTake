package com.example.giveandtake.Model;

import static org.junit.Assert.*;

import com.google.firebase.firestore.GeoPoint;

import org.junit.Before;
import org.junit.Test;

public class RequestTest {
    Request request1= new Request();

    @Before
    public void setUp() {
        request1.setContactDetails("contact1");
        request1.setLocation(new GeoPoint(31,31));
        request1.setBody("body1");
        request1.setSubject("subject1");
        request1.setCreationTime("11:11");
    }

    @Test
    public void getContactDetails() {
        assertEquals( "contact1", request1.getContactDetails());
    }

    @Test
    public void setContactDetails() {
        request1.setContactDetails("contact2");
        assertEquals( "contact2", request1.getContactDetails());
    }

    @Test
    public void getBody() {
        assertEquals( "body1", request1.getBody());
    }

    @Test
    public void setBody() {
        request1.setBody("Body2");
        assertEquals( "Body2", request1.getBody());
    }

    @Test
    public void getSubject() {
        assertEquals("subject1", request1.getSubject());
    }

    @Test
    public void setSubject() {
        request1.setSubject("Subject2");
        assertEquals( "Subject2", request1.getSubject());
    }

    @Test
    public void getCreationTime() {
        assertEquals( "11:11", request1.getCreationTime());
    }

    @Test
    public void setCreationTime() {
        request1.setCreationTime("22:22");
        assertEquals( "22:22", request1.getCreationTime());
    }

    @Test
    public void getLocation() {
        assertEquals( new GeoPoint(31,31), request1.getLocation());
    }

    @Test
    public void setLocation() {
        request1.setLocation(new GeoPoint(30,30));
        assertEquals( new GeoPoint(30,30), request1.getLocation());
    }

    @Test
    public void testToString() {
        assertEquals( "Request [ Subject=" + request1.getSubject() + ", body=" + request1.getBody()
                + ", contactDetails=" + request1.getContactDetails()+ ", creationTime=" + request1.getCreationTime()
                + "]", request1.toString());
    }
}
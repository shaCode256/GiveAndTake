package com.example.giveandtake.Model;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;

public class Request implements Serializable {

    private String  contactDetails;

    private String  subject;

    private String  body;

    private String  creationTime;

    private GeoPoint location;

    public String getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subjectTxt) {
        this.subject = subjectTxt;
    }

    public String getCreationTime() {
        return creationTime;
    }
    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }



    @Override
    public String toString() {
        return "Request [ Subject=" + subject + ", body=" + body
                + ", contactDetails=" + contactDetails+ ", creationTime=" + creationTime
                + "]";
    }




}

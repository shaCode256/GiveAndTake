package com.example.lib;

import java.io.*;
import java.net.*;
import java.util.Date;
//import com.example.giveandtake.Model.Request;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.GeoPoint;


public class Server implements Serializable {


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
            this.subject = subject;
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


    //ServerPostRequest.java 6868

    public static void main(String[] args) {
        if (args.length < 1) return;

        int port = Integer.parseInt(args[0]);
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                InputStream inputStream = socket.getInputStream();
                int text= inputStream.read();
//                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
//                Request request = (Request) objectInputStream.readObject();
                String requestUserId= "demoRequestUserId";
                String finalSetRequestId= "finalSetRequestId";
                System.out.println("Got data from client: "+text);
//                System.out.println(request.getContactDetails());
//                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://giveandtake-31249-default-rtdb.firebaseio.com/");
//                databaseReference.child("users").child(requestUserId).child("requestId").child(finalSetRequestId).setValue(request);
                socket.close();
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
        }
    }
}

package com.example.giveandtake.Presenter;

import java.io.*;
import java.net.*;
import java.util.Date;
import com.example.giveandtake.Model.Request;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ServerPostRequest implements Serializable {

    //ServerPostRequest.java 6868

//    Request request;
//    String requestUserId;
//    String requestId;


//    public ServerPostRequest(Request request, String requestUserId, String requestId){
//        this.request= request;
//        this.requestUserId= requestUserId;
//        this.requestId= requestId;
//    }

    public ServerPostRequest(){
    }
    public static void main(String[] args){
        int port = 8080;
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

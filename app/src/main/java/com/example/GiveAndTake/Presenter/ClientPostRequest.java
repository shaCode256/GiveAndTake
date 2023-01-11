package com.example.giveandtake.Presenter;

import com.example.giveandtake.Model.Request;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.*;

public class ClientPostRequest {


    /**
     * This program demonstrates a simple TCP/IP socket client.
     *
     * @author www.codejava.net
     */

    static Request request;
    static String requestUserId;
    static String requestId;
    DatabaseReference databaseReference;

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestUserId() {
        return this.requestUserId;
    }

    public void setRequestUserId(String requestUserId) {
        this.requestUserId = requestUserId;
    }

    public DatabaseReference getDatabaseReference() {
        return this.databaseReference;
    }

    public void setDatabaseReference(DatabaseReference databaseReference) {
        this.databaseReference= databaseReference;
    }

    public ClientPostRequest(Request request){
        this.setRequest(request);
     };

    public void post() {

        String hostname = "10.102.0.7";
        int port = 8080;
        System.out.println("come on");

        try (Socket socket = new Socket(hostname, port)) {
            OutputStream outputStream = socket.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

            Request request;
            request= this.getRequest();
            objectOutputStream.writeObject(request);
            System.out.println("Sending to server");

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }

}



//https://stackoverflow.com/questions/5495534/java-net-connectexception-localhost-127-0-0-18080-connection-refused
//helped me figure out the IP Address for server instead of localhost

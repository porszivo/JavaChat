package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by Marcel on 26.10.2016.
 */
public class UserModel {

    private String name;
    private String password;
    private boolean isLoggedIn;
    private ObjectOutputStream out;
    private Socket socket;

    private String lastReceivedPrivateMessage;

    public UserModel(String name, String password) {
        this.name = name;
        this.password = password;
        isLoggedIn = false;
        lastReceivedPrivateMessage = "No message received!";
    }
    
    public synchronized void setTCPConnection(Socket socket) {
    	this.socket = socket;
    	try {
			out = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized boolean isLoggedIn() {
        return isLoggedIn;
    }

    public boolean checkPassword(String password) {
        if(password.equals(this.password)) {
            isLoggedIn = true;
        }
        return isLoggedIn;
    }

    public synchronized void logout() {
        isLoggedIn = false;
    }

    public synchronized void setLastReceivedPrivateMessage(String message) {
        this.lastReceivedPrivateMessage = message;
    }

    public synchronized String getlastReceivedPrivateMessage() {
        return lastReceivedPrivateMessage;
    }
    
    public synchronized void write(String message) {
    	System.out.println(name + "empfaengt: " + message);
    	try {
			out.writeObject(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
}
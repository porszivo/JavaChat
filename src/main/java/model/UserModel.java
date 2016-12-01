package model;

import java.net.Socket;

/**
 * Created by Marcel on 26.10.2016.
 */
public class UserModel {

    private String name;
    private String password;
    private boolean isLoggedIn;
    private Socket socket;

    private String adr;
    private int port;

    private String lastReceivedPrivateMessage;

    public UserModel(String name, String password) {
        this.name = name;
        this.password = password;
        isLoggedIn = false;
        port = -1;
        lastReceivedPrivateMessage = "No message received yet.";
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized boolean isLoggedIn() {
        return isLoggedIn;
    }

    public synchronized boolean isRegistered() { return port != -1; }

    public boolean checkPassword(String password) {
        if(password.equals(this.password)) {
            isLoggedIn = true;
        }
        return isLoggedIn;
    }

    public synchronized void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public synchronized void logout() {
        isLoggedIn = false;
        socket = null;
        port = -1;
    }

    public synchronized void setAddress(String adr) {

        String parts[] = adr.split(":");

        if(parts.length == 2) {
            this.adr  = parts[0];

            try {
                this.port = Integer.parseInt(parts[1]);
            } catch(NumberFormatException e) {
                System.err.println("ERROR: Port is not an Integer");
            }

        } else {
            System.err.println("ERROR: IP address does not have the right format");
        }

    }

    public synchronized String getAddress() {
        return adr + ":" + port;
    }

    public synchronized void setLastReceivedPrivateMessage(String message) {
        this.lastReceivedPrivateMessage = message;
    }

    public synchronized String getlastReceivedPrivateMessage() {
        return lastReceivedPrivateMessage;
    }

    public synchronized int getPort() {
        return port;
    }

    public void setLastMsg(String lastMsg) {
        this.lastReceivedPrivateMessage = lastMsg;
    }
}
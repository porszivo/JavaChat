package listener;

import client.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Queue;

public class ClientListenerTCP implements Runnable {

    private Socket socket;
    private Client client;
    private BufferedReader in;
    private Queue<String> messageQueue;

    public ClientListenerTCP(Socket socket, Client client, Queue<String> messageQueue) {
        this.socket = socket;
        this.client = client;
        this.messageQueue = messageQueue;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void run() {
        while(true) {
            try {
                String input;
                if((input = in.readLine()) != null) receiveMessage(input);
            } catch (IOException e) {
            }
        }
    }

    private synchronized void receiveMessage(String message) {
        messageQueue.add(message);
    }

    public void close() {
        try {
            if(in != null) {
                in.close();
            }
        } catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
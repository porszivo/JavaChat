package listener;

import channel.IChannel;
import client.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Queue;

public class ClientListenerTCP implements Runnable {

    private Socket socket;
    private Client client;
    //private BufferedReader in;
    private Queue<String> messageQueue;
    private IChannel channel;

    public ClientListenerTCP(IChannel channel, Client client, Queue<String> messageQueue) {
        //this.socket = socket;
        this.client = client;
        this.messageQueue = messageQueue;
        this.channel = channel;

        /*
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        */

    }
    private boolean running = false;

    @Override
    public void run() {
        while(!running) {

            try {
                String input = new String(channel.receive());
                receiveMessage(input);
            } catch (IOException e) {
            }
        }
    }

    private synchronized void receiveMessage(String message) {
        messageQueue.add(message);
    }

    public void setChannel(IChannel channel) {

        this.channel = channel;
    }

    public void close() {
        channel.close();
        running = true;
    }

}
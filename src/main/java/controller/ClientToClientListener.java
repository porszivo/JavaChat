package controller;

import java.io.BufferedReader;
import java.util.Queue;

public class ClientToClientListener implements Runnable {

    private Queue<String> messageQueue;
    private BufferedReader reader;

    public ClientToClientListener(BufferedReader reader, Queue<String> messageQueue) {
        this.reader = reader;
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {
        while(true) {

        }
    }
}

package channel;

import client.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientToClientChannel implements Runnable {

    private ServerSocket serverSocket;
    private Socket socket;
    private Client client;
    private Boolean running = false;
    private Boolean isServer;

    // Servermode
    public ClientToClientChannel(int port, Client client, Boolean isServer) {

        this.client = client;
        this.isServer = isServer;

        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("ERROR: Could not establish ClientToClientServer\n" + e.getMessage());
        }

        running = true;

    }

    // Clientmode
    public ClientToClientChannel(String host, int port, Client client, Boolean isServer) {

        this.client = client;
        this.isServer = isServer;

        try {
            this.socket = new Socket(host, port);
        } catch (IOException e) {
            System.err.println("ERROR: Could not establish ClientToClientServer\n" + e.getMessage());
        }

    }

    @Override
    public void run() {
        try {
            while (running) {
                if (isServer) {
                    Socket socket = null;
                    socket = serverSocket.accept();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                    String request;
                    if ((request = reader.readLine()) != null) {
                        client.addNewMessage(request);
                        writer.println("!ack");
                        running = false;
                    }
                }
                if(!isServer) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String request;
                    if((request = reader.readLine()) != null) {
                        client.addNewMessage(request);
                        running = false;
                    }
                }
            }
        } catch (IOException e) {
            System.err
                    .println("Error occurred while waiting for/communicating with client: "
                            + e.getMessage());
        }
    }

    public void close() {
        running = false;
        try {
            socket.close();
            if(isServer) serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error");
        }
    }

    public void send(String message) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
    }
}
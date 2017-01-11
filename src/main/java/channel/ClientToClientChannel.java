package channel;

import client.Client;
import org.bouncycastle.util.encoders.Base64;
import util.Config;
import util.Keys;

import javax.crypto.Mac;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ClientToClientChannel implements Runnable {

    private ServerSocket serverSocket;
    private Socket socket;
    private Client client;
    private Boolean running = false;
    private Boolean isServer;
    private Config config;

    // Servermode
    public ClientToClientChannel(int port, Client client, Boolean isServer) {

        this.config = new Config("client");

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

                    //Check HMAC
                    if ((request = reader.readLine()) != null) {

                        String[] parts = request.split(" ");
                        String receivedHMAC = parts[0];

                        byte[] decodedHMAC = Base64.decode(receivedHMAC);

                        try{

                            byte[] computedHMAC = generateHMAC(request.substring(receivedHMAC.length()+1));

                            if(MessageDigest.isEqual(decodedHMAC, computedHMAC)) {
                                client.addNewMessage(request);
                                writer.println(addHMAC("!ack"));
                                running = false;
                            } else {
                                client.addNewMessage(request);
                                writer.println(addHMAC("!tempered"));
                                running = false;
                            }

                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(!isServer) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String request;
                    if((request = reader.readLine()) != null) {
                        System.out.println("Message an client");
                        System.out.println(client);
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

    private String addHMAC(String message) throws IOException, NoSuchAlgorithmException, InvalidKeyException {

        Key key = Keys.readSecretKey(new File(config.getString("hmac.key")));

        byte[] hashMac = generateHMAC(message);

        if(hashMac == null) {
            return message;
        }

        byte[] encryptedMessage = Base64.encode(hashMac);

        return encryptedMessage.toString() + " " + message;

    }

    private byte[] generateHMAC(String message) throws IOException, NoSuchAlgorithmException, InvalidKeyException {

        Key key = Keys.readSecretKey(new File(config.getString("hmac.key")));

        try {
            Mac hMac = Mac.getInstance("HmacSHA256");
            hMac.init(key);
            hMac.update(message.getBytes());
            byte[] hashMac = hMac.doFinal();

            return hashMac;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
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
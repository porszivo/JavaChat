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

        this.running = true;

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

                        byte[] receivedHMAC = parts[0].getBytes();
                        byte[] decodedHMAC = Base64.decode(receivedHMAC);

                        try{

                            byte[] computedHMAC = generateHMAC(request.substring(parts[0].length() + 1));

                            /*byte[] hash = Base64.encode(computedHMAC);

                            client.send("Received: " + new String(receivedHMAC));
                            client.send("Generated: " + new String(hash));*/

                            //Arrays.equals(h,h2)
                            if(MessageDigest.isEqual(decodedHMAC, computedHMAC)) {
                                client.addNewMessage(request);
                                writer.println(addHMAC("!ack " + request.substring(parts[0].length() + 1)));
                                running = false;
                            } else {
                                client.addNewMessage(request);
                                writer.println(addHMAC("!tampered " + request.substring(parts[0].length() + 1)));
                                running = false;
                            }

                            socket.close();

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
                        //client.write(request);
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

        byte[] hashMac = generateHMAC(message);

        if(hashMac == null) {
            return message;
        }

        byte[] encryptedMessage = Base64.encode(hashMac);

        return new String(encryptedMessage) + " " + message;

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
package controller;

import channel.AesEncryption;
import channel.IChannel;
import channel.RsaEncryption;
import model.UserMap;
import model.UserModel;
import org.bouncycastle.util.encoders.Base64;
import util.Config;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class MessageControllerTCP implements Runnable {

    private Socket socket;
    private UserMap userMap;
    private UserModel user;
    private IChannel channel;
    private String chatserverChallenge;


    public MessageControllerTCP(Socket socket, UserMap userMap) {
        this.socket = socket;
        this.userMap = userMap;
        channel = new RsaEncryption(socket);
        ((RsaEncryption)channel).setPrivateKey(new Config("chatserver").getString("keys.dir") + "/chatserver.pem");
    }

    @Override
    public void run() {
        try {

            //BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            //String request;
            while (true) {

                String request = new String(channel.receive());

                if(chatserverChallenge!= null){
                    if (!chatserverChallenge.equals(request)){
                        System.out.println("ChatserverChallenge mismatch!");
                        return;
                    }else{
                        System.out.println("Authenticate successful!");
                    }

                }

                String[] cmd = request.split("\\s");

                switch (cmd[0]) {
                    case "!login":
                        if (cmd.length == 3) {
                            // out.println(login(cmd[1], cmd[2]));
                        }
                        break;
                    case "!authenticate":
                        if (cmd.length == 3) {
                            authenticate(cmd[1], cmd[2]);
                        }
                        break;
                    case "!logout":
                        // out.println(logout());
                        break;
                    case "!send":
                        send(request);
                        break;
                    case "!msg":
                        if (cmd.length > 3) {
                            String message = request.replace("!msg " + cmd[1] + " ", "");
                            // out.println(msg(cmd[1]) + "_" + message);
                        } else {
                            //out.println("ERROR: Private message has wrong format");
                        }
                        break;
                    case "!lookup":
                        // out.println(lookup(request.replace("!lookup ", "")));
                        break;
                    case "!register":
                        // out.println(register(request));
                        break;
                    case "!lastMsg":
                        // out.println(lastMsg());
                        break;
                    default:
                        // out.println("Command does not have the expected format or is unknown!\n" );//+ request);
                        break;
                }

            }

        } catch (IOException e) {
            e.getMessage();
        }


    }

    public String login(String username, String password) throws IOException {
        String out = "";
        UserModel user = userMap.getUser(username);
        if (user == null) {
            out = "Wrong username or password.";
        } else if (user.isLoggedIn()) {
            out = "Already logged in.";
        } else if (user.checkPassword(password)) {
            this.user = user;
            this.user.setSocket(socket);
            out = "Successfully logged in.";
        }

        return out.equals("") ? "Wrong username or password." : out;

    }

    public String logout() {
        if (user == null) {
            return "Not logged in.";
        } else {
            this.user.logout();
            this.user = null;
            return "Successfully logged out .";
        }
    }

    public String send(String message) throws IOException {
        message = message.replace("!send ", "");
        for (UserModel user : userMap.getOnlineUser()) {

            PrintWriter socketOut = new PrintWriter(user.getSocket().getOutputStream(), true);
            socketOut.println(message);
            user.setLastReceivedPrivateMessage(message);

        }
        return message;
    }

    public String msg(String username) throws IOException {
        String output = lookup(username);
        if (output.equals("Wrong username or user not registered.")) return "Wrong username or user not reachable.";
        return "!msg_" + output;
    }

    public String lookup(String username) throws IOException {
        if (userMap.contains(username)) {
            UserModel user = userMap.getUser(username);
            if (user.isRegistered()) {
                return user.getAddress();
            }
        }
        return "Wrong username or user not registered.";
    }

    public String register(String privateAddress) throws IOException {

        privateAddress = privateAddress.replace("!register ", "");
        String[] parts = privateAddress.split(":");

        if (parts.length != 2) return "Address is not in a valid format.";
        user.setAddress(privateAddress);

        return "C2C_Successful_" + user.getPort();

    }

    public String lastMsg() throws IOException {
        return user.getlastReceivedPrivateMessage();
    }

    public String authenticate(String username, String clientChallenge) throws IOException {

        chatserverChallenge = new String(generateByteRandomNumber(32));
        byte[] encodedIvParameter = generateByteRandomNumber(16);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(Base64.decode(encodedIvParameter));
        KeyGenerator generator = null;
        try {
            generator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        generator.init(256);
        SecretKey secretKey = generator.generateKey();
        String encodedSecretKey = new String(Base64.encode(secretKey.getEncoded()));
        byte[] responseMessageToEncrypt = ("!ok " + clientChallenge + " " + chatserverChallenge + " " + encodedSecretKey + " " + new String(encodedIvParameter)).getBytes();

        ((RsaEncryption)channel).setPublicKey(new Config("chatserver").getString("keys.dir") + "/" + username + ".pub.pem");
        channel.send(responseMessageToEncrypt);
        channel = new AesEncryption(socket,secretKey,ivParameterSpec);

        return null;
    }

    private byte[] generateByteRandomNumber(int bytes) {

        SecureRandom secureRandom = new SecureRandom();
        final byte[] number = new byte[bytes];
        secureRandom.nextBytes(number);

        return Base64.encode(number);

    }
}
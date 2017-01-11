package controller;

import channel.AesEncryption;
import channel.IChannel;
import channel.RsaEncryption;
import model.UserMap;
import model.UserModel;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import org.bouncycastle.util.encoders.Base64;
import util.Config;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class MessageControllerTCP implements Runnable {

    private Socket socket;
    private UserMap userMap;
    private UserModel user;
    private INameserverForChatserver rootNameserver;
    private IChannel channel;
    private byte[] chatserverChallenge;
    private String username;


    public MessageControllerTCP(Socket socket, UserMap userMap, INameserverForChatserver rootNameserver) {
        this.socket = socket;
        this.userMap = userMap;
        this.rootNameserver = rootNameserver;
        channel = new RsaEncryption(socket);
        try {
            ((RsaEncryption)channel).setPrivateKey(new Config("chatserver").getString("keys.dir") + "/chatserver.pem");
        } catch (FileNotFoundException e) {
            System.out.println("No such private key found");
        }
    }

    @Override
    public void run() {
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            //String request;
            while (true) {

                String request = new String(channel.receive());

                if(chatserverChallenge!= null){
                    if (!Arrays.equals(chatserverChallenge,request.getBytes())){
                        System.out.println("ChatserverChallenge mismatch!");
                        return;
                    }else{
                        String status = login(username);
                        if (status.equals("Already logged in.")) {
                            channel.send(status.getBytes("UTF-8"));
                            channel = null;
                            return;

                        }
                        System.out.println(status);
                        System.out.println("Authenticate successful!");
                        chatserverChallenge = null;
                        username = null;
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
                    case "!send":

                        String response = send(request);
                        channel.send(response.getBytes("UTF-8"));
                        break;
                    case "!register":
                        channel.send(register(request).getBytes("UTF-8"));
                        break;
                    case "!lookup":
                        channel.send(lookup(request.replace("!lookup ", "")).getBytes("UTF-8"));
                        break;
                    case "!logout":
                        channel.send(logout().getBytes("UTF-8"));
                        break;
                    case "!lastMsg":
                        channel.send(lastMsg().getBytes("UTF-8"));
                        break;
                    case "!msg":
                        if (cmd.length > 3) {
                            String message = request.replace("!msg " + cmd[1] + " ", "");
                             channel.send((msg(cmd[1]) + " " + message).getBytes("UTF-8"));
                        } else {
                            out.println("ERROR: Private message has wrong format");
                        }
                        break;

                    default:
                        //System.out.println("Command does not have the expected format or is unknown!\n" );//+ request);
                        break;

                }

            }

        } catch (IOException e) {
            e.getMessage();
        } catch (AlreadyRegisteredException e) {
            e.printStackTrace();
        } catch (InvalidDomainException e) {
            e.printStackTrace();
        }


    }

    public String login(String username) throws IOException {
        String out = "";
        UserModel user = userMap.getUser(username);
        if (user == null) {
            out = "Wrong username";
        } else if (user.isLoggedIn()) {
            out = "Already logged in.";
        } else {
            user.setLoggedIn(true);
            this.user = user;
            this.user.setSocket(socket);
            this.user.setMessageControllerTCP(this);
            out = "Successfully logged in.";
        }

        return out.equals("") ? "Wrong username." : out;

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
        System.out.println("Anzahl  online user:" + userMap.getOnlineUser().size());
        for (UserModel user : userMap.getOnlineUser()) {
            user.getMessageControllerTCP().receiveMessage(message);
            user.setLastReceivedPrivateMessage(message);
        }
        return "Message sent to all online Users!";
    }

    public void receiveMessage(String message){
        try {
            channel.send(message.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public String msg(String username) throws IOException {
        String output = lookup(username);
        if (output.equals("Wrong username or user not registered.")) return "Wrong username or user not reachable.";
        return "!msg_  " + output;
    }

    public String lookup(String username) throws IOException {
        if(userMap.contains(username)) {
            UserModel user = userMap.getUser(username);
            if(user.isRegistered()) {
                return rootNameserver.lookup(username).equals("") ? "Wrong username or user not registered." : rootNameserver.lookup(username);
            }
        }
        return "Wrong username or user not registered.";
    }

    public String register(String privateAddress) throws IOException, AlreadyRegisteredException, InvalidDomainException {
        privateAddress = privateAddress.replace("!register ", "");
        System.out.println(user.getName()+ " privateaddress: " + privateAddress);

        String[] parts = privateAddress.split(":");

        if (parts.length != 2) return "Address is not in a valid format.";
        user.setAddress(privateAddress);
        rootNameserver.registerUser(user.getName(), privateAddress);

        return "C2C_Successful_" + user.getPort();

    }

    public String lastMsg() throws IOException {
        return user.getlastReceivedPrivateMessage();
    }

    public void authenticate(String username, String clientChallenge) throws IOException {

        this.username = username;
        chatserverChallenge = generateByteRandomNumber(32);
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
        byte[] responseMessageToEncrypt = ("!ok " + clientChallenge + " " + new String(chatserverChallenge) + " " + encodedSecretKey + " " + new String(encodedIvParameter)).getBytes("UTF-8");

        ((RsaEncryption)channel).setPublicKey(new Config("chatserver").getString("keys.dir") + "/" + username + ".pub.pem");
        channel.send(responseMessageToEncrypt);
        channel.send("dummy".getBytes());
        channel = new AesEncryption(socket,secretKey,ivParameterSpec);

    }

    private byte[] generateByteRandomNumber(int bytes) {

        SecureRandom secureRandom = new SecureRandom();
        final byte[] number = new byte[bytes];
        secureRandom.nextBytes(number);

        return Base64.encode(number);

    }
}
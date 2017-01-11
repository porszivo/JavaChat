package client;

import channel.*;
import cli.Command;
import cli.Shell;
import listener.ClientListenerTCP;
import org.bouncycastle.util.encoders.Base64;
import util.Config;
import util.Keys;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client implements IClientCli, Runnable {

    private String componentName;
    private Config config;

    private InputStream userRequestStream;
    private PrintStream userResponseStream;
    private Shell shell;

    private int tcpPortNumber;
    private int udpPortNumber;
    private String serverHost;
    private DatagramSocket datagramSocket;
    private Socket clientSocket = null;
    private ClientToClientChannel privMessageClient;
    private ClientToClientChannel privMessageServer;
    private ClientListenerTCP clientListenerTCP;

    private Thread ctcServer = null;
    private Thread ctcClient = null;

    //private BufferedReader in;
    //private BufferedReader stdIn;
    //private PrintWriter out;


    private String user = null;
    private Boolean isLoggedIn = false;

    private Queue<String> messageQueue;

    private ExecutorService pool;

    private IChannel channel;
    private byte[] encodedClientChallenge;


    /**
     * @param componentName      the name of the component - represented in the prompt
     * @param config             the configuration to use
     * @param userRequestStream  the input stream to read user input from
     * @param userResponseStream the output stream to write the console output to
     */
    public Client(String componentName, Config config,
                  InputStream userRequestStream, PrintStream userResponseStream) {
        this.componentName = componentName;
        this.config = config;
        this.userRequestStream = userRequestStream; //in
        this.userResponseStream = userResponseStream; //out
        this.messageQueue = new ConcurrentLinkedQueue<>();

        this.tcpPortNumber = this.config.getInt("chatserver.tcp.port");
        this.udpPortNumber = this.config.getInt("chatserver.udp.port");
        this.serverHost = this.config.getString("chatserver.host");
        try {

            clientSocket = new Socket(serverHost, tcpPortNumber);
            datagramSocket = new DatagramSocket();
            //in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));  // messages received by server
            //stdIn = new BufferedReader(new InputStreamReader(System.in));  // user input
            //out = new PrintWriter(clientSocket.getOutputStream(), true);  // send data through the clientSocket


            pool = Executors.newCachedThreadPool();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void run() {
        channel = new RsaEncryption(clientSocket);
        clientListenerTCP = new ClientListenerTCP(channel, this, messageQueue);

        shell = new Shell(componentName, userRequestStream, userResponseStream);
        shell.register(this);

        new Thread(shell).start();
        new Thread(clientListenerTCP).start();

        while (true) {
            if (messageQueue.peek() != null) {
                //write(
                checkOutput(getNextMessage());
            }
        }

    }

    private void checkOutput(String nextMessage) {

        String[] cmd = nextMessage.split("\\s");


        if (nextMessage.contains("!ok")) {
            ok(cmd[1], cmd[2], cmd[3], cmd[4]);
        } else if (nextMessage.contains("dummy")) {

        } else if (nextMessage.contains("Already")) {
            clientListenerTCP.close();
        } else if (nextMessage.contains("C2C_Successful_")) {

            write(nextMessage);
            if (ctcServer != null) {
                privMessageServer.close();
                ctcServer = null;
            }

            //String[] parts = nextMessage.split(" ");
            privMessageServer = new ClientToClientChannel(Integer.parseInt(cmd[1]), this, true);
            ctcServer = new Thread(privMessageServer);
            ctcServer.start();


        } else if (nextMessage.contains("!ack") || nextMessage.contains("!tampered")) { //Schreibfehler gewesen: Hattest !ark statt !ack
            write(nextMessage);
            privMessageClient.close();

        } else if (nextMessage.contains("!msg_")) {

            //"!msg_" + username + "_> " + user + ": " + message)


            String[] adr = cmd[1].split(":");
            String message = nextMessage.substring(cmd[1].length() + 7);


            try {
                //sign Message with HMAC
                String encryptedMessage = addHMAC(message);
                privMessageClient = new ClientToClientChannel(adr[0], Integer.parseInt(adr[1]), this, false);
                ctcClient = new Thread(privMessageClient);
                ctcClient.start();
                privMessageClient.send(encryptedMessage);
                //return encryptedMessage;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } catch (InvalidKeyException e) {
                System.out.println(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.out.println(e.getMessage());
            }
        } else {
            write(nextMessage);
        }

    }


    private String addHMAC(String message) throws IOException, InvalidKeyException, NoSuchAlgorithmException {

        byte[] hashMac = generateHMAC(message);

        if (hashMac == null) {
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

    private synchronized String getNextMessage() {
        try {
            while (messageQueue.size() == 0) {
                wait();
            }
            return messageQueue.poll();
        } catch (InterruptedException e) {
            return null;
        }
    }

    public synchronized void addNewMessage(String message) {
        messageQueue.add(message);
    }

    public void write(String message) {
        userResponseStream.println(message);
    }

    @Command
    @Override
    public String login(String username, String password) throws IOException {
        write("Login no longer available, please start with !authenticate <username>");
        //out.println("!login " + username + " " + password);
        //user = username;

        return null;

    }

    @Command
    @Override
    public String logout() throws IOException {
        if (isLoggedIn) {
            channel.send(("!logout " + user).getBytes());
            user = null;
            isLoggedIn = false;
            return null;
        } else {
            return "Not logged in.";
        }
    }

    @Command
    @Override
    public String send(String message) throws IOException {

        channel.send(("!send " + user + ": " + message).getBytes());

        return null;


    }

    @Command
    @Override
    public String list() throws IOException {
        String s = "!list";
        byte[] data = s.getBytes();

        DatagramPacket packet = new DatagramPacket(data,
                data.length,
                InetAddress.getByName(serverHost),
                udpPortNumber);

        datagramSocket.send(packet);

        byte[] buffer = new byte[1024];

        packet = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(packet);

        return new String(packet.getData(), 0, packet.getLength());
    }

    @Command
    @Override
    public String msg(String username, String message) throws IOException {
        if (isLoggedIn) channel.send(("!msg " + username + " " + user + ": " + message).getBytes());
        else return "Not logged in.";
        return null;
    }

    @Command
    @Override
    public String lookup(String username) throws IOException {
        if (isLoggedIn) channel.send(("!lookup " + username).getBytes());
        else return "Not logged in.";
        return null;
    }

    @Command
    @Override
    public String register(String privateAddress) throws IOException {
        if (isLoggedIn) channel.send(("!register " + privateAddress).getBytes());
        else return "Not logged in.";
        return null;
    }

    @Command
    @Override
    public String lastMsg() throws IOException {
        if (isLoggedIn) channel.send(("!lastMsg").getBytes());
        return null;
    }

    @Command
    @Override
    public String exit() throws IOException {
        logout();
        //in.close();
        //out.close();
        clientListenerTCP.close();
        //channel.close();
        //clientSocket.close();
        shell.close();
        if (ctcServer != null) ctcServer.interrupt();
        if (ctcClient != null) ctcClient.interrupt();
        userResponseStream.close();
        userRequestStream.close();
        user = null;
        isLoggedIn = false;
        Thread.currentThread().interrupt();
        return "Good bye";
    }

    /**
     * @param args the first argument is the name of the {@link Client} component
     */
    public static void main(String[] args) {
        Client client = new Client(args[0], new Config("client"), System.in,
                System.out);
        new Thread(client).start();
    }

    // --- Commands needed for Lab 2. Please note that you do not have to
    // implement them for the first submission. ---

    @Command
    @Override
    public String authenticate(String username) throws IOException {

        try {
            ((RsaEncryption) channel).setPrivateKey(new Config("client").getString("keys.dir") + "/" + username + ".pem");

        } catch (FileNotFoundException e) {
            return "No such user, please enter an exiting user!";
        }


        this.user = username;
        encodedClientChallenge = generate32ByteRandomNumber();
        byte[] messageToEncrypt = ("!authenticate " + username + " " + new String(encodedClientChallenge)).getBytes("UTF-8");

        ((RsaEncryption) channel).setPublicKey(new Config("client").getString("keys.dir") + "/chatserver.pub.pem");
        channel.send(messageToEncrypt);


        return null;
    }

    public void ok(String clientChallenge, String chatserverChallenge, String secretKey, String ivParameter) {

        if (!Arrays.equals(clientChallenge.getBytes(), encodedClientChallenge)) {
            try {
                shell.writeLine("Clientchallenge mismatch!");
            } catch (IOException e) {
                System.out.println("Shell problem");
            }
            return;
        }


        byte[] decodedKey = Base64.decode(secretKey);
        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        byte[] decodedIvParameter = Base64.decode(ivParameter);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(decodedIvParameter);
        channel = new AesEncryption(clientSocket, originalKey, ivParameterSpec);
        clientListenerTCP.setChannel(channel);
        channel.send(chatserverChallenge.getBytes());
        isLoggedIn = true;

    }


    private byte[] generate32ByteRandomNumber() {

        // generates a 32 byte secure random number
        SecureRandom secureRandom = new SecureRandom();
        final byte[] number = new byte[32];
        secureRandom.nextBytes(number);

        return Base64.encode(number);

    }

}
package client;

import channel.AesEncryption;
import channel.IChannel;
import channel.RsaEncryption;
import cli.Command;
import cli.Shell;
import listener.ClientListenerTCP;
import channel.ClientToClientChannel;
import org.bouncycastle.util.encoders.Base64;
import util.Config;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
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

    private BufferedReader in;
    private BufferedReader stdIn;
    private PrintWriter out;


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
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));  // messages received by server
            stdIn = new BufferedReader(new InputStreamReader(System.in));  // user input
            out = new PrintWriter(clientSocket.getOutputStream(), true);  // send data through the clientSocket


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
                write(checkOutput(getNextMessage()));
            }
        }

    }

    private String checkOutput(String nextMessage) {

        String[] cmd = nextMessage.split("\\s");

        switch (cmd[0]) {
            case "!ok":
                ok(cmd[1], cmd[2], cmd[3], cmd[4]);
                break;

            case "dummy":
                break;


        }

       /*
        if (nextMessage.equals("Successfully logged in.")) {

            isLoggedIn = true;

        } else if (nextMessage.equals("Successfully logged out.")) {

            isLoggedIn = false;

        } else if (nextMessage.contains("C2C_Successful_")) {

            if(ctcServer != null) {
                privMessageServer.close();
                ctcServer = null;
            }

            String[] parts = nextMessage.split("_");
            privMessageServer = new ClientToClientChannel(Integer.parseInt(parts[2]), this, true);
            ctcServer = new Thread(privMessageServer);
            ctcServer.start();

            return "Successfully registered address for " + user;

        } else if (nextMessage.equals("!ark")) {

                privMessageClient.close();

        } else if (nextMessage.contains("!msg")) {

            String[] parts = nextMessage.split("_");
            String[] adr   = parts[1].split(":");
            try {
                privMessageClient = new ClientToClientChannel(adr[0], Integer.parseInt(adr[1]), this, false);
                privMessageClient.send(parts[2]);
                return parts[2].replace(">", "<");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }

*/
        return nextMessage;

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
        if (!isLoggedIn) {
            out.println("!login " + username + " " + password);
            user = username;
            return null;
        } else {
            return "Already logged in.";
        }
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
        if (isLoggedIn) channel.send(("!msg " + username + " > " + user + ": " + message).getBytes());
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
        in.close();
        out.close();
        clientSocket.close();
        shell.close();
        if (ctcServer != null) ctcServer.interrupt();
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

        this.user = username;
        ((RsaEncryption) channel).setPrivateKey(new Config("client").getString("keys.dir") + "/" + username + ".pem");

        encodedClientChallenge = generate32ByteRandomNumber();
        byte[] messageToEncrypt = ("!authenticate " + username + " " + new String(encodedClientChallenge)).getBytes("UTF-8");

        ((RsaEncryption) channel).setPublicKey(new Config("client").getString("keys.dir") + "/chatserver.pub.pem");
        channel.send(messageToEncrypt);


        return null;
    }

    public void ok(String clientChallenge, String chatserverChallenge, String secretKey, String ivParameter) {

            if (!Arrays.equals(clientChallenge.getBytes(),encodedClientChallenge)) {
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


    }


    private byte[] generate32ByteRandomNumber() {

        // generates a 32 byte secure random number
        SecureRandom secureRandom = new SecureRandom();
        final byte[] number = new byte[32];
        secureRandom.nextBytes(number);

        return Base64.encode(number);

    }

}

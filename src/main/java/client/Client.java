package client;

import channel.Base64Channel;
import channel.IChannel;
import channel.RsaChannel;
import cli.Command;
import cli.Shell;
import listener.ClientListenerTCP;
import channel.ClientToClientChannel;
import org.bouncycastle.util.encoders.Base64;
import util.Config;
import util.Keys;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
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

    private RsaChannel channel;

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
            clientListenerTCP = new ClientListenerTCP(clientSocket, this, messageQueue);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));  // messages received by server
            stdIn = new BufferedReader(new InputStreamReader(System.in));  // user input
            out = new PrintWriter(clientSocket.getOutputStream(), true);  // send data through the clientSocket

            channel = new RsaChannel(clientSocket);


            pool = Executors.newCachedThreadPool();


        } catch(IOException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void run() {

        shell = new Shell(componentName, userRequestStream, userResponseStream);
        shell.register(this);

        new Thread(shell).start();
        new Thread(clientListenerTCP).start();

        while(true) {
            if(messageQueue.peek() != null) {
                write(checkOutput(getNextMessage()));
            }
        }

    }

    private String checkOutput(String nextMessage) {
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

        return nextMessage;
    }

    private synchronized String getNextMessage() {
        try {
            while (messageQueue.size() == 0) {
                wait();
            }
            return messageQueue.poll();
        } catch(InterruptedException e) {
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
            out.println("!logout " + user);
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
        if(isLoggedIn) {
            out.println(("!send " + user + ": " + message));
            return null;
        } else {
            return "Not logged in.";
        }

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
        if(isLoggedIn) out.println("!msg " + username + " > " + user + ": " + message);
        else return "Not logged in.";
        return null;
    }

    @Command
    @Override
    public String lookup(String username) throws IOException {
        if(isLoggedIn) out.println("!lookup " + username);
        else return "Not logged in.";
        return null;
    }

    @Command
    @Override
    public String register(String privateAddress) throws IOException {
        if(isLoggedIn) out.println("!register " + privateAddress);
        else return "Not logged in.";
        return null;
    }

    @Command
    @Override
    public String lastMsg() throws IOException {
        if(isLoggedIn) out.println("!lastMsg");
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
        if(ctcServer != null) ctcServer.interrupt();
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



            byte[] message = ("!authenticate " + username + " ").getBytes();
            byte[] encodedClientChallenge = generate32ByteRandomNumber();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(message);
            outputStream.write(encodedClientChallenge);
            byte messageToEncrypt[] = outputStream.toByteArray();

            channel.setPublicKey(new Config("client").getString("keys.dir") + "/chatserver.pub.pem");
            channel.send(messageToEncrypt);




        return null;
    }

    private byte[] generate32ByteRandomNumber(){

        // generates a 32 byte secure random number
        SecureRandom secureRandom = new SecureRandom();
        final byte[] number = new byte[32];
        secureRandom.nextBytes(number);

        return Base64.encode(number);

    }

}

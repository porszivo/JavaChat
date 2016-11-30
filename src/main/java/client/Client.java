package client;

import util.Config;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

public class Client implements IClientCli, Runnable {

    private String componentName;

    private Config config;
    private Config userConfig;
    private HashMap<String, Boolean> userMap;

    private InputStream userRequestStream;
    private PrintStream userResponseStream;

    private int tcpPortNumber;
    private int udpPortNumber;
    private String serverHost;
    private DatagramSocket datagramSocket;
    private Socket clientSocket = null;

    private BufferedReader in;
    private BufferedReader stdIn;
    private PrintWriter out;

    private String user = null;
    private Boolean isLoggedIn = false;

    private Boolean closed = false;

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
        this.userRequestStream = userRequestStream;
        this.userResponseStream = userResponseStream;

        this.tcpPortNumber = this.config.getInt("chatserver.tcp.port");
        this.udpPortNumber = this.config.getInt("chatserver.udp.port");
        this.serverHost = this.config.getString("chatserver.host");

    }

    @Override
    public void run() {
        try {

            clientSocket = new Socket(serverHost, tcpPortNumber);
            datagramSocket = new DatagramSocket();
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));  // messages received by server
            stdIn = new BufferedReader(new InputStreamReader(System.in));  // messages client enters
            out = new PrintWriter(clientSocket.getOutputStream(), true);  // send data through the clientSocket

            System.out.println("Welcome!\nPlease !login");

            while (!closed) {

                String input = stdIn.readLine();

                String[] cmd = input.split(" ");

                switch (cmd[0]) {
                    case "!login":
                        if (cmd.length == 3) {
                            System.out.println(login(cmd[1], cmd[2]));
                        } else {
                            System.out.println("Not enough parameter.");
                        }
                        break;
                    case "!logout":
                        System.out.println(logout());
                        break;
                    case "!send":
                        System.out.println(send(input));
                        break;
                    case "!list":
                        System.out.println(list());
                        break;
                    case "!msg":
                        break;
                    case "!lookup":
                        break;
                    case "!register":
                        break;
                    case "!lastMsg":
                        break;
                    case "!exit":
                        break;
                    default:
                        out.println("Command does not have the expected format or is unknown!");
                        break;
                }

            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public String login(String username, String password) throws IOException {
        if (!isLoggedIn) {
            out.println("!login " + username + " " + password);
            String response = in.readLine();
            if (response.equals("Successfully logged in.")) {
                isLoggedIn = true;
                user = username;
            }
            return response;
        } else {
            return "Already logged in.";
        }
    }

    @Override
    public String logout() throws IOException {
        if (isLoggedIn) {
            out.println("!logout " + user);
            String response = in.readLine();
            if (response.equals("Successfully logged out.")) {
                user = null;
                isLoggedIn = false;
            }
            return response;
        } else {
            return "Not logged in.";
        }
    }

    @Override
    public String send(String message) throws IOException {
        if(isLoggedIn) {
            out.println(message.replaceFirst("!send ", "!send " + user + ": "));
            return message.replaceFirst("!send ", user + ": ");
        } else {
            return "Not logged in.";
        }
    }

    @Override
    public String list() throws IOException {
        String s = "!list";
        byte[] data = s.getBytes();

        DatagramPacket packet = new DatagramPacket(data,
                data.length,
                InetAddress.getByName(serverHost),
                udpPortNumber);

        System.out.println(new String(packet.getData()));

        datagramSocket.send(packet);

        byte[] buffer = new byte[1024];

        packet = new DatagramPacket(buffer, buffer.length);
        datagramSocket.receive(packet);

        return new String(packet.getData(), 0, packet.getLength());
    }

    @Override
    public String msg(String username, String message) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String lookup(String username) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String register(String privateAddress) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String lastMsg() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String exit() throws IOException {
        // TODO Auto-generated method stub
        in.close();
        out.close();
        stdIn.close();
        clientSocket.close();
        closed = true;
        return null;
    }

    /**
     * @param args the first argument is the name of the {@link Client} component
     */
    public static void main(String[] args) {
        Client client = new Client(args[0], new Config("client"), System.in,
                System.out);
        client.run();
    }

    // --- Commands needed for Lab 2. Please note that you do not have to
    // implement them for the first submission. ---

    @Override
    public String authenticate(String username) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}

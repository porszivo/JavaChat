package controller;

import model.UserMap;
import model.UserModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class MessageControllerTCP implements Runnable {

    private Socket socket;
    private UserMap userMap;
    private UserModel user;
    private ArrayList<MessageControllerTCP> messageControllerPool;
    private BufferedReader in;
    private PrintWriter out;

    public MessageControllerTCP(Socket socket, UserMap userMap, ArrayList<MessageControllerTCP> messageControllerPool) {
        this.socket = socket;
        this.userMap = userMap;
        this.messageControllerPool = messageControllerPool;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String request;
            while((request = in.readLine()) != null) {


                System.out.println(request);

                String[] cmd = request.split(" ");

                switch (cmd[0]) {
                    case "!login":
                        if(cmd.length == 3) {
                            out.println(login(cmd[1], cmd[2]));
                        }
                        break;
                    case "!logout":
                        out.println(logout());
                        break;
                    case "!send":
                        send(request);
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
            e.getMessage();
        }

    }

    public String login(String username, String password) throws IOException {
    	
        String out = "";
        UserModel user = userMap.getUser(username);
        if(user == null) {
            out = "Wrong username or password.";
        } else if(user.isLoggedIn()) {
            out = "Already logged in.";
        } else if(user.checkPassword(password)) {
            this.user = user;
            setupTCPConnection();
            out = "Successfully logged in.";
        }

        return out.equals("") ? "Wrong username or password." : out;

    }

    private synchronized void setupTCPConnection() {
    	userMap.getUser(user.getName()).setTCPConnection(socket);
	}

	public String logout() {
        if(user==null) {
            return "Not logged in.";
        } else {
            unregisterController();
            this.user.logout();
            this.user = null;
            return "Successfully logged out .";
        }
    }

    public String send(String message) throws IOException {
        message = message.replace("!send ", "");
        for(UserModel user : userMap.getOnlineUser()) {
            user.write(message);
        }
        return message;
    }

    public String msg(String username, String message) throws IOException {
        return null;
    }

    public String lookup(String username) throws IOException {
        return null;
    }

    public String register(String privateAddress) throws IOException {
        return null;
    }

    public String lastMsg() throws IOException {
        String out = "Not logged in.";
        if(user != null) out = user.getlastReceivedPrivateMessage();
        return out;
    }

    public String exit() {
        if(user != null) {
            logout();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String authenticate(String username) throws IOException {
        return null;
    }

    public synchronized void registerController() {
        messageControllerPool.add(this);
    }

    public synchronized void unregisterController() {
        messageControllerPool.remove(this);
    }

    public synchronized ArrayList<MessageControllerTCP> getMessageControllerPool() {
        return messageControllerPool;
    }

}

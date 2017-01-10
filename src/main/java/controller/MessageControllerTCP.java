package controller;

import model.UserMap;
import model.UserModel;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

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
    private INameserverForChatserver rootNameserver;

    public MessageControllerTCP(Socket socket, UserMap userMap, INameserverForChatserver rootNameserver) {
        this.socket = socket;
        this.userMap = userMap;
        this.rootNameserver = rootNameserver;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String request;
            while((request = in.readLine()) != null) {



                String[] cmd = request.split("\\s");

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
                        if(cmd.length > 3) {
                            String message = request.replace("!msg " + cmd[1] + " ", "");
                            out.println(msg(cmd[1]) + "_" + message);
                        } else {
                            out.println("ERROR: Private message has wrong format");
                        }
                        break;
                    case "!lookup":
                        out.println(lookup(request.replace("!lookup ", "")));
                        break;
                    case "!register":
                        out.println(register(request));
                        break;
                    case "!lastMsg":
                        out.println(lastMsg());
                        break;
                    default:
                        out.println("Command does not have the expected format or is unknown!\n" + request);
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

    public String login(String username, String password) throws IOException {
        String out = "";
        UserModel user = userMap.getUser(username);
        if(user == null) {
            out = "Wrong username or password.";
        } else if(user.isLoggedIn()) {
            out = "Already logged in.";
        } else if(user.checkPassword(password)) {
            this.user = user;
            this.user.setSocket(socket);
            out = "Successfully logged in.";
        }

        return out.equals("") ? "Wrong username or password." : out;

    }

    public String logout() {
        if(user==null) {
            return "Not logged in.";
        } else {
            this.user.logout();
            this.user = null;
            return "Successfully logged out .";
        }
    }

    public String send(String message) throws IOException {
        message = message.replace("!send ", "");
        for(UserModel user : userMap.getOnlineUser()) {

            PrintWriter socketOut = new PrintWriter(user.getSocket().getOutputStream(), true);
            socketOut.println(message);
            user.setLastReceivedPrivateMessage(message);

        }
        return message;
    }

    public String msg(String username) throws IOException {
        String output = lookup(username);
        if(output.equals("Wrong username or user not registered.")) return "Wrong username or user not reachable.";
        return "!msg_" + output;
    }

    public String lookup(String username) throws IOException {
        if(userMap.contains(username)) {
            UserModel user = userMap.getUser(username);
            if(user.isRegistered()) {
                System.out.println(rootNameserver.lookup(username));
                return rootNameserver.lookup(username).equals("") ? "Wrong username or user not registered." : rootNameserver.lookup(username);
            }
        }
        return "Wrong username or user not registered.";
    }

    public String register(String privateAddress) throws IOException, AlreadyRegisteredException, InvalidDomainException {

        privateAddress = privateAddress.replace("!register ", "");
        String[] parts = privateAddress.split(":");

        if(parts.length != 2) return "Address is not in a valid format.";
        user.setAddress(privateAddress);
        rootNameserver.registerUser(user.getName(), privateAddress);

        return "C2C_Successful_" + user.getPort();

    }

    public String lastMsg() throws IOException {
        return user.getlastReceivedPrivateMessage();
    }

    public String authenticate(String username) throws IOException {
        return null;
    }
}
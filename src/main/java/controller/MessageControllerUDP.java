package controller;

import model.UserMap;
import model.UserModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 * Created by marce on 07.11.2016.
 */
public class MessageControllerUDP implements Runnable {

    private DatagramPacket packet;
    private UserMap userMap;
    private String request;
    private DatagramSocket datagramSocket;
    private int port;
    private InetAddress inetAddress;

    public MessageControllerUDP(DatagramSocket datagramSocket, DatagramPacket packet,UserMap userMap) {
        this.packet = packet;
        this.userMap = userMap;
        this.port = packet.getPort();
        this.inetAddress = packet.getAddress();
        this.userMap = userMap;
        this.datagramSocket = datagramSocket;
    }

    @Override
    public void run() {
        byte[] data;
        String out = null;

        request = new String(packet.getData(), 0, packet.getLength());

        String[] cmd = request.split(" ");

        switch (cmd[0]) {
            case "!list":
                out = list();
                break;
            default:
                out = "Command does not have the expected format or is unknown!";
                break;

        }

        data = out.getBytes();
        packet = new DatagramPacket(data, data.length, inetAddress, port);
        try {
            datagramSocket.send(packet);
        } catch (IOException e) {
            e.getMessage();
        }
    }

    private String list() {
        ArrayList<UserModel> onlineUser = userMap.getOnlineUser();
        StringBuilder out = new StringBuilder();
        out.append("Online users: \n");
        String newline = "";
        if(onlineUser.size() == 0) return out.append("No User online.").toString();
        for(UserModel user : onlineUser) {
            out.append(newline)
                    .append(user.getName());
            newline = "\n";
        }
        return out.toString().trim();
    }

}

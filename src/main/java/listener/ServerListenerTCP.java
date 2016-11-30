package listener;

import controller.MessageControllerTCP;
import model.UserMap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class ServerListenerTCP implements Runnable {

    private UserMap userMap;
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    public ServerListenerTCP(ServerSocket serverSocket, ExecutorService executorService, UserMap userMap) {
        this.userMap = userMap;
        this.serverSocket = serverSocket;
        this.executorService = executorService;
    }

    public void run() {
        while(true) {
            try {

                MessageControllerTCP messageControllerTCP = new MessageControllerTCP(serverSocket.accept(), userMap);

                executorService.execute(messageControllerTCP);

            } catch (IOException e) {
                System.out.println(e.getMessage());
                break;
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void exit() {
        try {
            serverSocket.close();
            executorService.shutdownNow();
        } catch (IOException e) {
            e.getMessage();
        }
    }

}

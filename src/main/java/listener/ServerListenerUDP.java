package listener;

import controller.MessageControllerTCP;
import controller.MessageControllerUDP;
import model.UserMap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

/**
 * Created by marce on 07.11.2016.
 */
public class ServerListenerUDP implements Runnable {

    private DatagramSocket socket;
    private ExecutorService executorService;
    private UserMap userMap;
    private DatagramPacket packet;
    private Boolean running = true;

    public ServerListenerUDP(DatagramSocket socket, ExecutorService executorService, UserMap userMap) {
        this.socket = socket;
        this.executorService = executorService;
        this.userMap = userMap;
    }

    @Override
    public void run() {
            try {
                byte[] buffer;

                while(running && !socket.isClosed()) {

                    buffer = new byte[1024];
                    packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    MessageControllerUDP messageControllerUDP = new MessageControllerUDP(socket, packet, userMap);

                    executorService.execute(messageControllerUDP);
                }

            } catch (IOException e) {
                System.err.println(e.getMessage());
            } finally {
                running = false;
                socket.close();
            }

    }

    public void exit() {
        running = false;
        socket.close();
        executorService.shutdownNow();
    }

}
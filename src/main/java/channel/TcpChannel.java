package channel;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class TcpChannel implements IChannel {

    private Socket socket;
    private BufferedReader in = null;
    private PrintWriter out = null;

    public TcpChannel(Socket socket) {

        this.socket = socket;

    }


    @Override
    public void send(byte[] bytes) {
        try {
            out = new PrintWriter(socket.getOutputStream(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        out.println(new String(bytes));
    }

    @Override
    public byte[] receive() throws IOException {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String request = in.readLine();

        return request.getBytes();
    }

}

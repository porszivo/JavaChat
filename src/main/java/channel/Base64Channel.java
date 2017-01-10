package channel;

import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.Socket;


public class Base64Channel implements IChannel{

    private TcpChannel tcpChannel;


    public Base64Channel(Socket socket){
        tcpChannel = new TcpChannel(socket);
    }


    @Override
    public void send(byte[] bytes) {
    tcpChannel.send(Base64.encode(bytes));

    }

    @Override
    public byte[] receive() throws IOException {
        return Base64.decode(tcpChannel.receive());

    }

    public void close(){

        tcpChannel.close();


    }
}

package channel;

import java.io.IOException;

/**
 * Created by goekhandasdemir on 26.12.16.
 */
public interface IChannel {



    void send(byte[] bytes);

    byte[] receive() throws IOException;

    void close();

}

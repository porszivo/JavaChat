package channel;

import util.Config;
import util.Keys;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;


public class RsaEncryption implements IChannel {

    private Key publicKey;
    private Key privateKey;
    private IChannel base64Channel;
    private Socket socket;
    private Cipher cipher;
    private final static String RSA_ALGORITHM = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";


    public RsaEncryption(Socket socket) {
        this.base64Channel = new Base64Channel(socket);
        try {
            cipher = Cipher.getInstance(RSA_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void send(byte[] bytes) {
        try {
            byte[] encryptedMessage = null;

            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            encryptedMessage = cipher.doFinal(bytes);

            base64Channel.send(encryptedMessage);


        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }


    }

    @Override
    public byte[] receive() throws IOException {
        byte[] messageToDecrypt = base64Channel.receive();
        byte[] decryptedMessage = null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            decryptedMessage = cipher.doFinal(messageToDecrypt);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return decryptedMessage;


    }

    public void setPublicKey(String pathToPublicKey) {
        try {
            this.publicKey = Keys.readPublicPEM(new File(pathToPublicKey));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPrivateKey(String pathToPrivateKey) throws FileNotFoundException {
        try {
            this.privateKey = Keys.readPrivatePEM(new File(pathToPrivateKey));
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
    }


    public void close() {
        base64Channel.close();

    }


}

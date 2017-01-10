package channel;

import util.Keys;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class AesEncryption implements IChannel {

    private Key secretKey;
    private IvParameterSpec ivParameterSpec;
    private IChannel base64Channel;
    private Cipher cipher;
    private final static String AES_ALGORITHM = "AES/CTR/NoPadding";


    public AesEncryption(Socket socket, SecretKey key, IvParameterSpec ivParameterSpec) {
        this.base64Channel = new Base64Channel(socket);
        this.secretKey = key;
        this.ivParameterSpec = ivParameterSpec;
        try {
            cipher = Cipher.getInstance(AES_ALGORITHM);
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

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            encryptedMessage = cipher.doFinal(bytes);

            base64Channel.send(encryptedMessage);


        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] receive() throws IOException {
        byte[] messageToDecrypt = base64Channel.receive();
        byte[] decryptedMessage = null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            decryptedMessage = cipher.doFinal(messageToDecrypt);

        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return decryptedMessage;
    }

    public void close(){
        base64Channel.close();
    }

}

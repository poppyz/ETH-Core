package core.utils.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

class AesCiper {
    private IvParameterSpec ips;
    private final SecretKeySpec skeySpec;
    private final Cipher cipher;
    private boolean cryptMode;

    public AesCiper(boolean mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        SecureRandom random = new SecureRandom();
        byte[] ipRandom = new byte[16];
        byte[] keyRandom = new byte[32];
        random.nextBytes(ipRandom);
        random.nextBytes(keyRandom);
        this.ips = new IvParameterSpec(ipRandom);
        this.skeySpec = new SecretKeySpec(keyRandom, "AES");
        this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        this.cryptMode = mode;
        this.cipher.init(this.cryptMode ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, this.skeySpec, this.ips);

    }

    public AesCiper(AesCiper aesCiper)
    {
        this.ips = aesCiper.ips;
        this.skeySpec = aesCiper.skeySpec;
        this.cipher = aesCiper.cipher;
    }

    public AesCiper(byte[] key, byte[] iv, boolean mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (key == null || !(key.length == 16 || key.length == 24 || key.length == 32)) {
            throw new RuntimeException("input params key must be 16byte or 24byte or 32byte bytes array");
        }
        if (iv.length != 16) {
            throw new RuntimeException("input params iv must be 16byte bytes array");
        }
        this.ips = new IvParameterSpec(iv);
        this.skeySpec = new SecretKeySpec(key, "AES");
        this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        this.cryptMode = mode;
        this.cipher.init(this.cryptMode ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, this.skeySpec, this.ips);

    }

    public void setIv(byte[] iv) throws InvalidAlgorithmParameterException, InvalidKeyException {
        if (iv == null || iv.length != 16) {
            throw new RuntimeException("input params iv must be 16byte bytes array");
        }
        this.ips = new IvParameterSpec(iv);
        this.cipher.init(this.cryptMode ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, this.skeySpec, this.ips);
    }

    public void setMode(boolean mode) throws InvalidAlgorithmParameterException, InvalidKeyException {
        if (this.cryptMode != mode) {
            this.cryptMode = mode;
            this.cipher.init(this.cryptMode ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, this.skeySpec, this.ips);
        }
    }
    public String getName() {
        return String.format("Algorithm AES %d bit Mode %s", this.skeySpec.getEncoded().length * 8, this.cryptMode ? "ENCRYPT_MODE" : "DECRYPT_MODE");
    }
    public byte[] getIv()
    {
        return this.cipher.getIV();
    }
    public byte[] calculate(byte[] raw) throws IllegalBlockSizeException, BadPaddingException {
        byte[] data = this.cipher.doFinal(raw);
        this.ips = new IvParameterSpec(this.cipher.getIV());
        return data;
    }
    public byte[] start() throws IllegalBlockSizeException, BadPaddingException {
        byte[] data = this.cipher.doFinal();
        return this.cipher.getIV();
    }
    public byte[] update(byte[] raw)
    {
        byte[] data = this.cipher.update(raw);
        return data;
    }
    public byte[] doFinal(byte[] raw) throws IllegalBlockSizeException, BadPaddingException {
        byte[] data = this.cipher.doFinal(raw);
        return data;
    }


}




//
//class FileSlice
//{
//    private byte[] iv;  //该文件片段加密的IV
//    private byte[] rawHash;//该文件片段的hash
//    private byte[] rawLen;
//    private byte[] cryptedHash;//该文件片段加密后的hash
//    private long number;//该文件片段的ID
//    private AesCiper ciper;
//    InputStream in;
//    public FileSlice (AesCiper ciper)
//    {
//
//    }
//    public boolean start(InputStream inputStream)
//    {
//        ciper.start()
//    }
//}
//
//class FileData
//{
//    private
//}
//


public class AesCbc {
    private AesCiper encryptAesCiper;
    private AesCiper decryptAesCiper;
    private static final int DEFAULT_BLOCK_SIZE = 15 * 1024;

    public AesCbc(byte[] key) {

    }
    public void build(byte[] key, byte[] encryptIv,byte[] decryptIv) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        this.encryptAesCiper = new AesCiper(key,encryptIv,true);
        this.decryptAesCiper = new AesCiper(key,decryptIv,false);
    }
    public void build(byte[] key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        SecureRandom random = new SecureRandom();
        byte[] ipRandom = new byte[16];
        random.nextBytes(ipRandom);
        build(key,ipRandom,ipRandom);
    }


    public void crypto(InputStream inputStream,OutputStream outputStream,boolean mode) throws IOException, IllegalBlockSizeException, BadPaddingException {
        byte[] buffer = new byte[DEFAULT_BLOCK_SIZE];
        int len = inputStream.read(buffer,0,DEFAULT_BLOCK_SIZE);
        AesCiper ciper = mode?this.encryptAesCiper:this.decryptAesCiper;
        outputStream.write(ciper.start(),0,16);
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
            byte[] data = ciper.update(buffer);
            outputStream.write(data);
        }
        outputStream.flush();
    }

//
//
//
//    public boolean decrypt(InputStream in, OutputStream out) throws IOException {
//
//        this.
//        out.write();
//
//        return false;
//    }
//    public byte[] decrypt(byte[] data)
//    {
//        int outSize = data.length%16==0?(data.length/16): (data.length/16+1);
//        byte[] out = new byte[outSize];
//
//        synchronized (this.decryptAesCiper)
//        {
//            System.arraycopy(this.decryptAesCiper.getIv(), 0, out, 0, 16);
//            try
//            {
//                byte[] temp = this.decryptAesCiper.calculate(data);
//                System.arraycopy(temp ,0,out,16,temp.length);
//                return out;
//            } catch (IllegalBlockSizeException | BadPaddingException  e) {
//                throw new RuntimeException("AES encrypt exception");
//            }
//        }
//    }
//    public byte[] encrypt(byte[] data)
//    {
//        synchronized (this.encryptAesCiper)
//        {
//            try
//            {
//                return this.encryptAesCiper.calculate(data);
//            } catch (IllegalBlockSizeException | BadPaddingException  e) {
//                throw new RuntimeException("AES encrypt exception");
//            }
//        }
//    }
}
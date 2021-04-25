package core.utils.crypto;

import javax.crypto.*;
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

    public byte[] start() throws IllegalBlockSizeException, BadPaddingException {
        byte[] data = this.cipher.doFinal();
        //加密前返回IV
        if(this.cryptMode)
        {
            return this.cipher.getIV();
        }
        return null;
    }
    public byte[] update(byte[] raw,int len)
    {
        byte[] data = this.cipher.update(raw,0,len);
        return data;
    }
    public byte[] doFinal(byte[] raw,int len) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {
        if(raw == null)
        {
            byte[] data = this.cipher.doFinal();
            return null;
        }
        else
        {
            byte[] cc= new byte[len];
            System.arraycopy(raw,0,cc,0,len);
            byte[] data = this.cipher.doFinal(cc);
            return data;
        }
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

    public AesCbc() {

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


    public void decrypt(InputStream inputStream,OutputStream outputStream) throws IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, ShortBufferException {
        byte[] buffer = new byte[DEFAULT_BLOCK_SIZE];
        AesCiper ciper = this.decryptAesCiper;
        byte[] iv = new byte[16];
        inputStream.read(iv);
        ciper.setIv(iv);
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
            byte[] data = null;
            data = n == DEFAULT_BLOCK_SIZE ? ciper.update(buffer,n) : ciper.doFinal(buffer,n);
            outputStream.write(data);
        }
        outputStream.flush();
    }

    public void encrypt(InputStream inputStream,OutputStream outputStream) throws IOException, IllegalBlockSizeException, BadPaddingException, ShortBufferException {
        byte[] buffer = new byte[DEFAULT_BLOCK_SIZE];
        AesCiper ciper = this.encryptAesCiper;
        outputStream.write(ciper.start(),0,16);
        long total = 0 ;
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
            byte[] data = null;
            data = n == DEFAULT_BLOCK_SIZE ? ciper.update(buffer,n) : ciper.doFinal(buffer,n);
            outputStream.write(data);
        }
        outputStream.flush();
    }
}